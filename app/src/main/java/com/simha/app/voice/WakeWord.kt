package com.simha.app.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.simha.app.Actions
import com.simha.app.data.Prefs
import com.simha.app.data.simhaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

object WakeWordPrefs {
    val KEY = booleanPreferencesKey("hey_simha")

    fun flow(ctx: Context): Flow<Boolean> = ctx.applicationContext.simhaStore.data.map { it[KEY] ?: false }

    suspend fun isEnabled(ctx: Context): Boolean = flow(ctx).first()

    suspend fun set(ctx: Context, on: Boolean) {
        ctx.applicationContext.simhaStore.edit { it[KEY] = on }
    }
}

object WakeWordMatcher {
    private val phrases = listOf(
        "simha", "simba", "sinha",
        "सिम्हा", "सिंहा",
        "సింహ", "సింహా",
        "ಸಿಂಹ", "ಸಿಂಹಾ",
        "சிம்ஹா", "சிம்மா", "சிம்ம",
    )

    fun matches(text: String): Boolean {
        val t = text.lowercase()
        return phrases.any { t.contains(it) }
    }
}

private object WakeWordEngine {
    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var rec: SpeechRecognizer? = null
    private var running = false
    private var want = 0
    private var cb: (() -> Unit)? = null
    private var app: Context? = null
    private var lang = "en"

    fun start(ctx: Context, onWake: () -> Unit) {
        val a = ctx.applicationContext
        scope.launch {
            val my = ++want
            release()
            if (!WakeWordPrefs.isEnabled(a)) return@launch
            if (!Actions.has(a, Manifest.permission.RECORD_AUDIO)) return@launch
            if (!SpeechRecognizer.isRecognitionAvailable(a)) return@launch
            val l = Prefs(a).snap().lang
            if (want != my) return@launch
            app = a
            cb = onWake
            lang = l
            running = true
            listen(my)
        }
    }

    fun stop() {
        scope.launch {
            want++
            release()
        }
    }

    private fun release() {
        running = false
        cb = null
        rec?.let { runCatching { it.destroy() } }
        rec = null
    }

    private fun listen(my: Int) {
        val a = app
        if (want != my || !running || a == null) return
        val r = try {
            SpeechRecognizer.createSpeechRecognizer(a)
        } catch (e: Exception) {
            release()
            return
        }
        rec = r
        var woke = false
        fun check(b: Bundle?) {
            if (woke || want != my) return
            val heard = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            if (heard.any { WakeWordMatcher.matches(it) }) {
                woke = true
                val f = cb
                release()
                f?.invoke()
            }
        }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onPartialResults(partialResults: Bundle?) = check(partialResults)

            override fun onResults(results: Bundle?) {
                check(results)
                if (!woke) again(r, my, 300L)
            }

            override fun onError(error: Int) {
                if (woke) return
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    release()
                    return
                }
                val busy = error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_SERVER
                again(r, my, if (busy) 3000L else 400L)
            }
        })
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Langs.tag(lang))
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        try {
            r.startListening(i)
        } catch (e: Exception) {
            again(r, my, 3000L)
        }
    }

    private fun again(r: SpeechRecognizer, my: Int, delayMs: Long) {
        runCatching { r.destroy() }
        if (rec === r) rec = null
        main.postDelayed({ listen(my) }, delayMs)
    }

}

object WakeWord {
    fun startWakeWord(ctx: Context, onHeard: () -> Unit) = WakeWordEngine.start(ctx, onHeard)

    fun stopWakeWord() = WakeWordEngine.stop()
}

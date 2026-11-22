package com.simha.app.voice

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.simha.app.data.Repo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

enum class VoiceIssue { None, LangMissing, Unavailable }

enum class VoiceKind { Unavailable, NoSpeech, Failed }

class VoiceFail(val kind: VoiceKind) : Exception(kind.name)

object Langs {
    fun tag(code: String): String = when (code) {
        "hi" -> "hi-IN"
        "te" -> "te-IN"
        "kn" -> "kn-IN"
        "ta" -> "ta-IN"
        else -> "en-IN"
    }
}

class Voice(private val app: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    private val queue = ArrayList<() -> Unit>()
    private val active = ConcurrentHashMap.newKeySet<String>()
    private val pending = MutableStateFlow(0)
    private var langCode = ""
    @Volatile private var rec: SpeechRecognizer? = null
    val issue = MutableStateFlow(VoiceIssue.None)

    init {
        tts = TextToSpeech(app) { s -> main.post { onInit(s) } }
    }

    private fun onInit(s: Int) {
        val t = tts ?: return
        if (s != TextToSpeech.SUCCESS) {
            issue.value = VoiceIssue.Unavailable
            queue.forEach { it() }
            queue.clear()
            return
        }
        t.setSpeechRate(0.9f)
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) = finish(utteranceId)

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?) = finish(utteranceId)
            override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = finish(utteranceId)
        })
        ready = true
        queue.forEach { it() }
        queue.clear()
    }

    private fun finish(id: String?) {
        if (id != null && active.remove(id)) pending.value = active.size
    }

    fun speak(text: String, lang: String) {
        if (text.isBlank()) return
        val id = UUID.randomUUID().toString()
        active.add(id)
        pending.value = active.size
        main.post {
            val run = {
                val t = tts
                if (t == null) {
                    finish(id)
                } else {
                    applyLang(t, lang)
                    t.speak(text, TextToSpeech.QUEUE_ADD, null, id)
                }
            }
            if (ready) run() else if (issue.value == VoiceIssue.Unavailable) finish(id) else queue.add { run() }
        }
    }

    private fun applyLang(t: TextToSpeech, lang: String) {
        if (lang == langCode) return
        val r = t.setLanguage(Locale.forLanguageTag(Langs.tag(lang)))
        issue.value = if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) VoiceIssue.LangMissing else VoiceIssue.None
        langCode = lang
    }

    suspend fun awaitIdle() {
        pending.first { it == 0 }
    }

    suspend fun say(text: String, lang: String) {
        speak(text, lang)
        try {
            awaitIdle()
        } catch (e: CancellationException) {
            stopSpeaking()
            throw e
        }
    }

    fun stopSpeaking() {
        active.clear()
        pending.value = 0
        main.post { tts?.stop() }
    }

    fun cancelListening() {
        main.post {
            rec?.destroy()
            rec = null
        }
    }

    suspend fun listen(lang: String, onPartial: (String) -> Unit): Result<String> = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { cont ->
            if (!SpeechRecognizer.isRecognitionAvailable(app)) {
                cont.resume(Result.failure(VoiceFail(VoiceKind.Unavailable)))
                return@suspendCancellableCoroutine
            }
            val r = SpeechRecognizer.createSpeechRecognizer(app)
            rec = r
            r.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}

                override fun onError(error: Int) {
                    r.destroy()
                    val kind = if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) VoiceKind.NoSpeech else VoiceKind.Failed
                    if (cont.isActive) cont.resume(Result.failure(VoiceFail(kind)))
                }

                override fun onResults(results: Bundle?) {
                    r.destroy()
                    val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (cont.isActive) {
                        cont.resume(if (t.isNullOrBlank()) Result.failure(VoiceFail(VoiceKind.NoSpeech)) else Result.success(t))
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onPartial)
                }
            })
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Langs.tag(lang))
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            r.startListening(i)
            cont.invokeOnCancellation { main.post { r.destroy() } }
        }
    }
}

class Recorder(private val app: Context, private val voice: Voice, private val repo: Repo) {
    private var mr: MediaRecorder? = null
    private var file: File? = null
    val recording = MutableStateFlow(false)

    suspend fun start(lang: String, notice: String): Result<Unit> {
        try {
            voice.say(notice, lang)
            val f = File(app.filesDir, "rec_" + System.currentTimeMillis() + ".m4a")
            @Suppress("DEPRECATION")
            val m = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(app) else MediaRecorder()
            m.setAudioSource(MediaRecorder.AudioSource.MIC)
            m.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            m.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            m.setOutputFile(f.absolutePath)
            m.prepare()
            m.start()
            mr = m
            file = f
            recording.value = true
            return Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun stopAndUpload(): Result<String> {
        val m = mr
        val f = file
        if (m == null || f == null) return Result.failure(IllegalStateException("Not recording"))
        mr = null
        file = null
        recording.value = false
        try {
            m.stop()
        } catch (e: RuntimeException) {
            m.release()
            f.delete()
            return Result.failure(IllegalStateException("Recording was too short"))
        }
        m.release()
        return repo.uploadRecording(f)
    }
}

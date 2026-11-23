package com.simha.app.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
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
import com.simha.app.llm.LlmStore
import com.simha.app.llm.SarvamConfig
import com.simha.app.wake.WakeAudio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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

    fun name(code: String): String = when (code) {
        "hi" -> "Hindi"
        "te" -> "Telugu"
        "kn" -> "Kannada"
        "ta" -> "Tamil"
        else -> "English"
    }
}

private class SpeakItem(val id: String, val text: String, val lang: String, val audio: Deferred<Result<ByteArray>>)

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
    val sarvamError = MutableStateFlow<String?>(null)
    private val store = LlmStore(app)
    private val client = SarvamClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val speakCh = Channel<SpeakItem>(Channel.UNLIMITED)
    private val fetches = ConcurrentHashMap<String, Deferred<Result<ByteArray>>>()
    @Volatile private var sarvam = SarvamConfig()
    @Volatile private var sarvamAt = 0L
    @Volatile private var current: Job? = null

    init {
        tts = TextToSpeech(app) { s -> main.post { onInit(s) } }
        refreshSarvam()
        scope.launch {
            for (item in speakCh) playItem(item)
        }
    }

    fun refreshSarvam() {
        sarvamAt = System.currentTimeMillis()
        scope.launch(Dispatchers.IO) { sarvam = store.sarvam() }
    }

    private suspend fun playItem(item: SpeakItem) {
        fetches.remove(item.id)
        if (item.id !in active) {
            item.audio.cancel()
            return
        }
        val r = try {
            item.audio.await()
        } catch (e: CancellationException) {
            if (!currentCoroutineContext().isActive) throw e
            return
        }
        val bytes = r.getOrNull()
        if (bytes == null) {
            sarvamError.value = r.exceptionOrNull()?.message
            speakAndroid(item.id, item.text, item.lang)
            return
        }
        sarvamError.value = null
        val j = scope.launch { play(bytes) }
        current = j
        j.join()
        finish(item.id)
    }

    suspend fun playAudio(bytes: ByteArray, ext: String = ".m4a") = play(bytes, ext)

    private fun attrs(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(if (WakeAudio.alarmMode) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private suspend fun play(bytes: ByteArray, ext: String = ".wav") = suspendCancellableCoroutine<Unit> { cont ->
        val f = File.createTempFile("tts", ext, app.cacheDir)
        val mp = MediaPlayer()
        mp.setAudioAttributes(attrs())
        var closed = false
        fun end() {
            if (closed) return
            closed = true
            try { mp.release() } catch (e: Exception) { }
            f.delete()
            if (cont.isActive) cont.resume(Unit)
        }
        try {
            f.writeBytes(bytes)
            mp.setDataSource(f.absolutePath)
            mp.setOnCompletionListener { end() }
            mp.setOnErrorListener { _, _, _ -> end(); true }
            mp.setOnPreparedListener { it.start() }
            mp.prepareAsync()
        } catch (e: Exception) {
            end()
        }
        cont.invokeOnCancellation {
            if (!closed) {
                closed = true
                try { mp.stop() } catch (e: Exception) { }
                try { mp.release() } catch (e: Exception) { }
                f.delete()
            }
        }
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
        if (System.currentTimeMillis() - sarvamAt > 3000) refreshSarvam()
        val key = sarvam.apiKey
        if (key.isNotBlank()) {
            val d = scope.async(Dispatchers.IO) { client.tts(key, text, Langs.tag(lang)) }
            fetches[id] = d
            speakCh.trySend(SpeakItem(id, text, lang, d))
        } else {
            speakAndroid(id, text, lang)
        }
    }

    private fun speakAndroid(id: String, text: String, lang: String) {
        main.post {
            val run = {
                val t = tts
                if (t == null) {
                    finish(id)
                } else {
                    applyLang(t, lang)
                    t.setAudioAttributes(attrs())
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
        fetches.values.forEach { it.cancel() }
        fetches.clear()
        main.post {
            current?.cancel()
            tts?.stop()
        }
    }

    fun cancelListening() {
        main.post {
            rec?.destroy()
            rec = null
        }
    }

    suspend fun listen(lang: String, onPartial: (String) -> Unit): Result<String> {
        sarvam = withContext(Dispatchers.IO) { store.sarvam() }
        sarvamAt = System.currentTimeMillis()
        if (sarvam.apiKey.isNotBlank()) {
            val r = listenSarvam(lang, sarvam.apiKey)
            val kind = (r.exceptionOrNull() as? VoiceFail)?.kind
            if (r.isSuccess || kind == VoiceKind.NoSpeech) {
                if (r.isSuccess) sarvamError.value = null
                return r
            }
        }
        return listenAndroid(lang, onPartial)
    }

    private suspend fun listenSarvam(lang: String, key: String): Result<String> = withContext(Dispatchers.IO) {
        val rate = 16000
        val frame = rate * 30 / 1000
        val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (androidx.core.content.ContextCompat.checkSelfPermission(app, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return@withContext Result.failure<String>(VoiceFail(VoiceKind.Failed))
        }
        val rec = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, frame * 8))
        } catch (e: Exception) {
            return@withContext Result.failure<String>(VoiceFail(VoiceKind.Failed))
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return@withContext Result.failure<String>(VoiceFail(VoiceKind.Failed))
        }
        val pcm = ByteArrayOutputStream()
        val ep = Endpointer()
        val buf = ShortArray(frame)
        var end = EpState.Waiting
        try {
            rec.startRecording()
            while (end == EpState.Waiting) {
                ensureActive()
                var n = 0
                while (n < frame) {
                    val r = rec.read(buf, n, frame - n)
                    if (r <= 0) return@withContext Result.failure<String>(VoiceFail(VoiceKind.Failed))
                    n += r
                }
                for (i in 0 until frame) {
                    pcm.write(buf[i].toInt() and 0xff)
                    pcm.write((buf[i].toInt() shr 8) and 0xff)
                }
                end = ep.feed(Wav.rms(buf, frame))
            }
        } catch (e: IllegalStateException) {
            return@withContext Result.failure<String>(VoiceFail(VoiceKind.Failed))
        } finally {
            try { rec.stop() } catch (e: Exception) { }
            rec.release()
        }
        if (end == EpState.Timeout) return@withContext Result.failure<String>(VoiceFail(VoiceKind.NoSpeech))
        client.stt(key, Wav.encode(pcm.toByteArray(), rate), Langs.tag(lang)).fold(
            onSuccess = { text ->
                if (text.isBlank()) Result.failure<String>(VoiceFail(VoiceKind.NoSpeech)) else Result.success(text)
            },
            onFailure = {
                sarvamError.value = it.message
                Result.failure<String>(VoiceFail(VoiceKind.Failed))
            },
        )
    }

    private suspend fun listenAndroid(lang: String, onPartial: (String) -> Unit): Result<String> = withContext(Dispatchers.Main.immediate) {
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

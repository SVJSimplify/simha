package com.simha.app.ui.talk

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simha.app.Actions
import com.simha.app.R
import com.simha.app.SimhaApp
import com.simha.app.data.jo
import com.simha.app.forLang
import com.simha.app.llm.Agent
import com.simha.app.llm.ChatApi
import com.simha.app.llm.ContactInfo
import com.simha.app.llm.HealthMemory
import com.simha.app.llm.LlmException
import com.simha.app.llm.Msg
import com.simha.app.llm.ParsedReply
import com.simha.app.llm.ToolHost
import com.simha.app.logic.Answer
import com.simha.app.logic.Reply
import com.simha.app.monitor.Work
import com.simha.app.data.CallResult
import com.simha.app.data.Repo
import com.simha.app.voice.VoiceFail
import com.simha.app.voice.VoiceKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream

enum class TalkMode { Idle, Listening, Thinking, Speaking }

data class ChatLine(val fromUser: Boolean, val text: String)

data class UiMsg(val res: Int, val arg: String? = null)

sealed interface TalkEvent {
    data object OpenEmergency : TalkEvent
}

object Prompt {
    fun langName(code: String): String = when (code) {
        "hi" -> "Hindi"
        "te" -> "Telugu"
        "kn" -> "Kannada"
        "ta" -> "Tamil"
        else -> "English"
    }

    fun build(name: String, lang: String, conditions: String, medicines: String, allergies: String, doctor: String, checkin: Boolean): String {
        val sb = StringBuilder()
        sb.append("You are Simha, a warm companion for ").append(name).append(", who may be elderly and living alone. ")
        sb.append("Talk like a caring friend, never like a machine.\n")
        sb.append("- The person's language is ").append(langName(lang)).append(". Every reply must be written only in ").append(langName(lang)).append(" (in its own script), even if the person types or speaks another language, unless they ask you to switch. Use short natural spoken sentences. No lists, no markdown, no emojis.\n")
        sb.append("- Use ").append(name).append("'s name now and then. Ask only one thing at a time. Remember what they told you earlier in this conversation.\n")
        sb.append("- When they ask you to do something (call someone, tell family, remember something, look at something with the camera, record a message), do it straight away with your tools. Do not make them open screens or press things, unless a tool result says they must press something, such as send or call.\n")
        sb.append("- After acting, say in plain words what you did, for example: I have told your daughter Priya. Only say you did something if the tool result confirms it. If a tool says refused or error, say so simply and kindly.\n")
        sb.append("- When the person mentions a health condition, a medicine they take, or an allergy in passing, save it straight away with remember_health_detail (kind: condition, medicine or allergy), then confirm briefly in one short sentence, for example: I have noted that.\n")
        sb.append("- Never mention tools, functions, JSON or anything technical.\n")
        sb.append("- You are not a doctor. Do not diagnose and do not advise treatment. If there are danger signs, stay calm, open the emergency screen and help them call 112. Emergency decisions are not yours: a fixed rules engine handles escalation.\n")
        sb.append("- Keep every reply brief.\n")
        sb.append("Medical profile. Conditions: ").append(conditions.ifBlank { "none given" })
        sb.append(". Medicines: ").append(medicines.ifBlank { "none given" })
        sb.append(". Allergies: ").append(allergies.ifBlank { "none given" })
        sb.append(". Doctor: ").append(doctor.ifBlank { "none given" }).append(".\n")
        if (checkin) {
            sb.append("This is a check-in. You just asked if they are okay. If they say they are fine, call mark_checkin_ok. If they are not, gently ask what is wrong, save it with log_symptom, and help them.\n")
        }
        return sb.toString()
    }
}

class TalkViewModel(app: Application) : AndroidViewModel(app) {
    private val sl = (app as SimhaApp).sl
    val lines = mutableStateListOf<ChatLine>()
    var mode by mutableStateOf(TalkMode.Idle)
    var partial by mutableStateOf("")
    var live by mutableStateOf("")
    var error by mutableStateOf<UiMsg?>(null)
    var configured by mutableStateOf(false)
    var checkedIn by mutableStateOf(false)
    var recording by mutableStateOf(false)
    var checkinMode = false
    val events = MutableSharedFlow<TalkEvent>(extraBufferCapacity = 4)
    val cameraRequest = MutableStateFlow<CompletableDeferred<Bitmap?>?>(null)

    private val history = mutableListOf<JsonObject>()
    private var job: Job? = null
    private var gen = 0
    private var started = false

    init {
        viewModelScope.launch { sl.recorder.recording.collect { recording = it } }
    }

    fun refreshConfig() {
        configured = sl.llmStore.config().configured
    }

    fun begin(checkin: Boolean) {
        if (started) return
        started = true
        checkinMode = checkin
        refreshConfig()
        restart(null, true)
    }

    fun tapMic() {
        if (mode == TalkMode.Listening) return
        refreshConfig()
        restart(null, false)
    }

    fun submitText(t: String) {
        val s = t.trim()
        if (s.isEmpty()) return
        refreshConfig()
        restart(s, false)
    }

    fun stop() {
        gen++
        job?.cancel()
        sl.voice.stopSpeaking()
        sl.voice.cancelListening()
        mode = TalkMode.Idle
        partial = ""
        live = ""
        if (recording) viewModelScope.launch { finishRecording() }
    }

    fun fine() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val snap = sl.prefs.snap()
            sl.repo.checkinOk()
            checkedIn = true
            val msg = ctx.forLang(snap.lang).getString(R.string.ci_thanks)
            lines += ChatLine(false, msg)
            sl.voice.speak(msg, snap.lang)
        }
    }

    fun help() {
        viewModelScope.launch {
            sl.repo.logEvent("help", jo("source" to "checkin_button"))
            events.emit(TalkEvent.OpenEmergency)
        }
    }

    fun camera() {
        refreshConfig()
        if (!configured) return
        gen++
        job?.cancel()
        sl.voice.stopSpeaking()
        sl.voice.cancelListening()
        val my = gen
        job = viewModelScope.launch {
            try {
                val snap = sl.prefs.snap()
                val lc = getApplication<Application>().forLang(snap.lang)
                val q = lc.getString(R.string.talk_camera_q)
                mode = TalkMode.Thinking
                val bmp = requestPhoto()
                if (bmp == null) {
                    error = UiMsg(R.string.talk_no_photo)
                    return@launch
                }
                val b64 = encode(bmp)
                lines += ChatLine(true, q)
                val idx = history.size
                history += Msg.userWithImage(q, b64)
                respond(snap.lang, lc)
                if (idx < history.size) history[idx] = Msg.user(q)
                conversation(null, snap.lang, lc)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiMsg(R.string.talk_error, e.message)
            } finally {
                if (gen == my) mode = TalkMode.Idle
            }
        }
    }

    private fun restart(initial: String?, greet: Boolean) {
        gen++
        val my = gen
        job?.cancel()
        sl.voice.stopSpeaking()
        sl.voice.cancelListening()
        error = null
        job = viewModelScope.launch {
            try {
                val snap = sl.prefs.snap()
                val lc = getApplication<Application>().forLang(snap.lang)
                if (greet && checkinMode) {
                    val g = lc.getString(R.string.ci_greet, snap.name)
                    lines += ChatLine(false, g)
                    mode = TalkMode.Speaking
                    sl.voice.say(g, snap.lang)
                }
                conversation(initial, snap.lang, lc)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiMsg(R.string.talk_error, e.message)
            } finally {
                if (gen == my) mode = TalkMode.Idle
            }
        }
    }

    private fun canListen(): Boolean = Actions.has(getApplication<Application>(), android.Manifest.permission.RECORD_AUDIO)

    private suspend fun conversation(initial: String?, lang: String, lc: android.content.Context) {
        if (!configured && !checkinMode && initial == null) return
        var next = initial
        var silent = 0
        while (currentCoroutineContext().isActive) {
            if (sl.recorder.recording.value) return
            val text: String
            if (next != null) {
                text = next
                next = null
            } else {
                if (!canListen()) {
                    error = UiMsg(R.string.talk_mic_needed)
                    return
                }
                mode = TalkMode.Listening
                partial = ""
                val r = sl.voice.listen(lang) { partial = it }
                partial = ""
                val heard = r.getOrNull()
                if (heard == null) {
                    when ((r.exceptionOrNull() as? VoiceFail)?.kind) {
                        VoiceKind.NoSpeech -> {
                            silent++
                            if (silent >= 3) {
                                error = UiMsg(R.string.talk_silent)
                                return
                            }
                            continue
                        }
                        VoiceKind.Unavailable -> {
                            error = UiMsg(R.string.voice_unavailable)
                            return
                        }
                        else -> {
                            error = UiMsg(R.string.talk_voice_failed)
                            return
                        }
                    }
                }
                silent = 0
                text = heard
            }
            if (Reply.isStop(text)) {
                val b = lc.getString(R.string.talk_bye)
                lines += ChatLine(false, b)
                mode = TalkMode.Speaking
                sl.voice.say(b, lang)
                return
            }
            lines += ChatLine(true, text)
            if (!handle(text, lang, lc)) return
        }
    }

    private suspend fun handle(text: String, lang: String, lc: android.content.Context): Boolean {
        if (!configured) {
            if (!checkinMode) return false
            when (Reply.classify(text)) {
                Answer.Fine -> {
                    sl.repo.checkinOk()
                    checkedIn = true
                    val m = lc.getString(R.string.ci_thanks)
                    lines += ChatLine(false, m)
                    mode = TalkMode.Speaking
                    sl.voice.say(m, lang)
                    return false
                }
                Answer.Help -> {
                    sl.repo.logEvent("help", jo("source" to "checkin_voice"))
                    events.emit(TalkEvent.OpenEmergency)
                    return false
                }
                Answer.Unknown -> {
                    val m = lc.getString(R.string.ci_not_understood)
                    lines += ChatLine(false, m)
                    mode = TalkMode.Speaking
                    sl.voice.say(m, lang)
                    return true
                }
            }
        }
        history += Msg.user(text)
        return respond(lang, lc)
    }

    private suspend fun respond(lang: String, lc: android.content.Context): Boolean {
        mode = TalkMode.Thinking
        val cfg = sl.llmStore.config()
        val p = sl.repo.profileNow()
        val snap = sl.prefs.snap()
        val system = Prompt.build(
            snap.name, lang, p.conditions, p.medicines, p.allergies,
            listOf(p.doctorName, p.doctorPhone).filter { it.isNotBlank() }.joinToString(" "), checkinMode,
        )
        val api = object : ChatApi {
            override suspend fun chat(messages: JsonArray, tools: JsonArray, onDelta: (String) -> Unit): ParsedReply =
                sl.llm.chat(cfg, messages, tools, onDelta)
        }
        live = ""
        try {
            val reply = Agent.run(
                api, system, history, Host(lang, lc),
                onSentence = { s ->
                    mode = TalkMode.Speaking
                    live = if (live.isEmpty()) s else live + " " + s
                    sl.voice.speak(s, lang)
                },
                onToolStart = { sl.voice.speak(lc.getString(R.string.talk_filler), lang) },
            )
            live = ""
            if (reply.isNotBlank()) lines += ChatLine(false, reply)
            trimHistory()
            mode = TalkMode.Speaking
            sl.voice.awaitIdle()
            return true
        } catch (e: LlmException) {
            live = ""
            error = UiMsg(R.string.talk_error, e.message)
            return false
        }
    }

    private fun trimHistory() {
        val t = com.simha.app.llm.History.trim(history)
        if (t.size != history.size) {
            val copy = t.toList()
            history.clear()
            history.addAll(copy)
        }
    }

    private suspend fun requestPhoto(): Bitmap? {
        val d = CompletableDeferred<Bitmap?>()
        cameraRequest.value = d
        return d.await()
    }

    private suspend fun encode(b: Bitmap): String = withContext(Dispatchers.Default) {
        val max = 1024
        val scale = minOf(1f, max.toFloat() / maxOf(b.width, b.height))
        val bm = if (scale < 1f) Bitmap.createScaledBitmap(b, (b.width * scale).toInt(), (b.height * scale).toInt(), true) else b
        val out = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 80, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    fun stopRecording() {
        viewModelScope.launch { finishRecording() }
    }

    private suspend fun finishRecording() {
        val ctx = getApplication<Application>()
        val snap = sl.prefs.snap()
        val lc = ctx.forLang(snap.lang)
        sl.recorder.stopAndUpload().fold(
            onSuccess = { lines += ChatLine(false, lc.getString(R.string.em_saved)) },
            onFailure = { lines += ChatLine(false, lc.getString(R.string.em_save_failed, it.message ?: "")) },
        )
    }

    override fun onCleared() {
        sl.voice.stopSpeaking()
        sl.voice.cancelListening()
        super.onCleared()
    }

    private inner class Host(val lang: String, val lc: android.content.Context) : ToolHost {
        private val app = getApplication<Application>()
        private val repo = sl.repo

        override suspend fun getProfile(): JsonObject {
            val p = repo.profileNow()
            val s = sl.prefs.snap()
            return jo(
                "name" to s.name, "conditions" to p.conditions, "medicines" to p.medicines,
                "allergies" to p.allergies, "doctor_name" to p.doctorName, "doctor_phone" to p.doctorPhone,
            )
        }

        override suspend fun getContacts(): JsonArray = buildJsonArray {
            repo.contactsNow().forEach { add(jo("name" to it.name, "role" to it.role, "approved_to_call" to it.approved)) }
        }

        override suspend fun getStatus(): JsonObject {
            val s = sl.prefs.snap()
            val now = System.currentTimeMillis()
            val recent = repo.recentEvents(5).getOrNull()
            return jo(
                "last_checkin_minutes_ago" to (if (s.lastCheckinOk > 0) (now - s.lastCheckinOk) / 60_000 else null),
                "last_phone_activity_minutes_ago" to (if (s.lastActivity > 0) (now - s.lastActivity) / 60_000 else null),
                "inactivity_limit_minutes" to s.inactivity,
                "recent_events" to (recent?.map { it.type + " " + it.createdAt }?.toString() ?: "unavailable offline"),
            )
        }

        override suspend fun logSymptom(text: String): String {
            repo.logEvent("help", jo("kind" to "symptom", "text" to text))
            return "Symptom saved for the doctor and family."
        }

        override suspend fun markCheckinOk(): String {
            repo.checkinOk()
            checkedIn = true
            return "Check-in recorded as fine."
        }

        override suspend fun openEmergencyScreen(): String {
            repo.logEvent("help", jo("source" to "assistant"))
            events.emit(TalkEvent.OpenEmergency)
            return "Emergency screen is opening."
        }

        override suspend fun dialEmergency(): String =
            if (repo.dialEmergency("assistant")) "The dialer is open with 112. The person must press call." else "Could not open the dialer."

        override suspend fun contactByName(name: String): ContactInfo? {
            val n = name.trim().lowercase()
            val all = repo.contactsNow()
            val c = all.firstOrNull { it.name.lowercase() == n } ?: all.firstOrNull { it.name.lowercase().contains(n) || n.contains(it.name.lowercase()) }
            return c?.let { ContactInfo(it.name, it.phone, it.role, it.approved) }
        }

        override suspend fun callContact(c: ContactInfo): String = when (repo.placeCall(c.phone, c.approved, "assistant")) {
            CallResult.Called -> "Calling " + c.name + " now."
            CallResult.Dialer -> "The dialer is open for " + c.name + ". The person must press call."
            CallResult.Failed -> "Could not start the call."
        }

        override suspend fun sendFamilyAlert(message: String): String {
            val numbers = repo.contactsNow().filter { it.role in Repo.FAMILY_ROLES }.map { it.phone }
            if (numbers.isEmpty()) return "There are no family contacts to alert."
            val loc = Actions.lastLocation(app)
            val link = loc?.let { " " + lc.getString(R.string.sms_loc, "https://maps.google.com/?q=" + it.latitude + "," + it.longitude) } ?: ""
            val ok = Actions.sms(app, numbers, message + link)
            repo.logEvent("alert", jo("sms_prepared" to ok, "source" to "assistant", "location_included" to (loc != null)))
            return if (ok) "A text message to family is ready. The person must press send." else "Could not open the messaging app."
        }

        override suspend fun recordingApproved(): Boolean = sl.prefs.snap().record

        override suspend fun startRecording(): String {
            if (!canListen()) return "Microphone permission is not allowed."
            sl.voice.stopSpeaking()
            sl.voice.cancelListening()
            val r = sl.recorder.start(lang, lc.getString(R.string.rec_notice))
            return if (r.isSuccess) "Recording has started. The person presses Stop when finished." else "Could not start recording: " + (r.exceptionOrNull()?.message ?: "")
        }

        override suspend fun lookWithCamera(question: String): String {
            if (!Actions.has(app, android.Manifest.permission.CAMERA)) return "Camera permission is not allowed."
            val bmp = requestPhoto() ?: return "No photo was taken."
            val cfg = sl.llmStore.config()
            val msgs = JsonArray(
                listOf(
                    Msg.system("Describe the photo briefly and plainly for an elderly person. Do not diagnose."),
                    Msg.userWithImage(question, encode(bmp)),
                )
            )
            return sl.llm.complete(cfg, msgs, null).content ?: "The model gave no description."
        }

        override suspend fun setReminder(text: String, minutes: Int): String {
            Work.reminder(app, text, minutes)
            return "Reminder set for $minutes minutes from now."
        }

        override suspend fun updateProfileField(field: String, value: String): String {
            val p = repo.profileNow()
            fun add(old: String) = if (old.isBlank()) value else "$old, $value"
            val n = when (field) {
                "conditions" -> p.copy(conditions = add(p.conditions))
                "medicines" -> p.copy(medicines = add(p.medicines))
                "allergies" -> p.copy(allergies = add(p.allergies))
                "doctor_name" -> p.copy(doctorName = value)
                else -> p.copy(doctorPhone = value)
            }
            repo.saveProfile(n)
            return "Profile updated."
        }

        override suspend fun rememberHealthDetail(kind: String, text: String): String {
            val p = repo.profileNow()
            val (merged, changed) = when (kind) {
                "condition" -> HealthMemory.merge(p.conditions, text)
                "medicine" -> HealthMemory.merge(p.medicines, text)
                else -> HealthMemory.merge(p.allergies, text)
            }
            if (!changed) return "Already saved."
            repo.saveProfile(
                when (kind) {
                    "condition" -> p.copy(conditions = merged)
                    "medicine" -> p.copy(medicines = merged)
                    else -> p.copy(allergies = merged)
                }
            )
            return "Saved to the health profile."
        }

        override suspend fun audit(tool: String, args: String, result: String) {
            repo.audit("tool_$tool", jo("args" to args.take(500), "result" to result.take(300)))
        }
    }
}

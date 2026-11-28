package com.simha.app.ui.talk

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simha.app.Actions
import com.simha.app.R
import com.simha.app.SimhaApp
import com.simha.app.data.CallResult
import com.simha.app.data.Medicine
import com.simha.app.data.Repo
import com.simha.app.data.Snap
import com.simha.app.data.jo
import com.simha.app.forLang
import com.simha.app.llm.Agent
import com.simha.app.llm.ChatApi
import com.simha.app.llm.ContactInfo
import com.simha.app.llm.ContactLine
import com.simha.app.llm.DiaryMerge
import com.simha.app.llm.DoseSlots
import com.simha.app.llm.FillerGate
import com.simha.app.llm.Guardian
import com.simha.app.llm.GuardianContext
import com.simha.app.llm.HealthMemory
import com.simha.app.llm.LlmException
import com.simha.app.llm.MedLine
import com.simha.app.llm.Open
import com.simha.app.llm.Msg
import com.simha.app.llm.ParsedReply
import com.simha.app.llm.ToolHost
import com.simha.app.logic.Answer
import com.simha.app.logic.FirstAid
import com.simha.app.logic.MedicalProfile
import com.simha.app.logic.Reply
import com.simha.app.monitor.Work
import com.simha.app.voice.VoiceFail
import com.simha.app.voice.VoiceKind
import com.simha.app.wake.Wake
import com.simha.app.wake.WakeReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

enum class TalkMode { Idle, Listening, Thinking, Speaking }

data class ChatLine(val fromUser: Boolean, val text: String)

data class UiMsg(val res: Int, val arg: String? = null)

sealed interface TalkEvent {
    data object OpenEmergency : TalkEvent
    data object Done : TalkEvent
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
    var undoVisibleUntil by mutableLongStateOf(0L)
    val events = MutableSharedFlow<TalkEvent>(extraBufferCapacity = 4)
    val cameraRequest = MutableStateFlow<CompletableDeferred<Bitmap?>?>(null)

    private val history = mutableListOf<JsonObject>()
    private var job: Job? = null
    private var gen = 0
    private var started = false
    private var reason: WakeReason? = null
    private var extra: String? = null
    private var unwell = false
    private var doseLogged = false
    private var startedAt = 0L
    private val fillers = FillerGate(4)
    private val care = com.simha.app.care.CareEngine(app, sl)
    private var lastUser = ""
    private var reviewed = 0
    private val runner = com.simha.app.intent.Runner(app, sl)
    private var emergencyDone = false
    private val stopWords = listOf("stop", "ruko", "रुको", "बस", "नहीं", "ఆపు", "వద్దు", "ನಿಲ್ಲಿಸು", "ಬೇಡ", "நிறுத்து", "வேண்டாம்")

    private val closesOnFine: Boolean get() = reason == WakeReason.Checkin || reason == WakeReason.Fall

    init {
        viewModelScope.launch { sl.recorder.recording.collect { recording = it } }
    }

    private fun reviewNow() {
        val t = lines.drop(reviewed).map { it.fromUser to it.text }
        reviewed = lines.size
        if (t.none { it.first }) return
        val ctx = getApplication<Application>()
        appScope.launch { runCatching { com.simha.app.care.Reviewer.review(ctx, sl, t) } }
    }

    fun reset() {
        reviewNow()
        stop()
        care.reset()
        runner.clear()
        emergencyDone = false
        undoVisibleUntil = 0L
        reviewed = 0
        history.clear()
        lines.clear()
        started = false
        checkedIn = false
        doseLogged = false
        error = null
    }

    fun refreshConfig() {
        configured = sl.llmStore.config().configured
    }

    fun begin(reason: WakeReason?, extra: String?, unwell: Boolean = false) {
        if (started) return
        started = true
        startedAt = System.currentTimeMillis()
        this.reason = reason
        this.extra = extra
        this.unwell = unwell
        checkinMode = reason == WakeReason.Checkin
        refreshConfig()
        restart(null, true)
    }

    fun tapMic() {
        if (mode == TalkMode.Listening) return
        Wake.noteReply()
        refreshConfig()
        restart(null, false)
    }

    fun submitText(t: String) {
        val s = t.trim()
        if (s.isEmpty()) return
        Wake.noteReply()
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
        Wake.noteReply()
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

    fun undo() {
        undoVisibleUntil = 0L
        viewModelScope.launch {
            val lang = sl.prefs.snap().lang
            val lc = getApplication<Application>().forLang(lang)
            val u = com.simha.app.phone.Journal.undo()
            val m = lc.getString(if (u != null) R.string.ix_undone else R.string.ix_nothing_undo)
            lines += ChatLine(false, m)
            sl.voice.speak(m, lang)
        }
    }

    fun help() {
        Wake.noteReply()
        viewModelScope.launch {
            sl.repo.logEvent("help", jo("source" to "checkin_button"))
            events.emit(TalkEvent.OpenEmergency)
        }
    }

    fun camera() {
        refreshConfig()
        if (!configured) return
        Wake.noteReply()
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
        live = ""
        job = viewModelScope.launch {
            try {
                val snap = sl.prefs.snap()
                val lc = getApplication<Application>().forLang(snap.lang)
                if (greet) opening(snap, lc)
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

    private suspend fun opening(snap: Snap, lc: android.content.Context) {
        val r = reason
        val c = guardianContext(snap, snap.lang)
        val k = if (configured) c.known() else c.known().copy(newPerson = false, medicines = true)
        val med = if (r == WakeReason.Medicine) extra else null
        val say = com.simha.app.wake.Wake.spoken(extra)
        val text = when {
            say != null -> snap.name + ", " + say
            r != null -> {
                val plan = if (r == WakeReason.Morning) planText() else null
                val unplayed = if (r == WakeReason.Morning) unplayedCount() else 0
                Guardian.openingLine(lc, r, snap.name, med, plan, unplayed, null, k, c.hook)
            }
            unwell -> lc.getString(R.string.gd_open_unwell, snap.name)
            configured -> Guardian.openingLine(lc, null, snap.name, known = k)
            else -> return
        }
        if (text.isBlank()) return
        if (!unwell || r != null) {
            when (Guardian.openingKey(r, k, med)) {
                Open.Intro -> runCatching { sl.guardian.addMemory("fact", "asked:intro") }
                Open.AskMedicines -> runCatching { sl.guardian.addMemory("fact", "asked:medicines") }
                else -> Unit
            }
        }
        lines += ChatLine(false, text)
        history += Msg.assistant(text)
        live = text
        mode = TalkMode.Speaking
        try {
            sl.voice.say(text, snap.lang)
        } finally {
            live = ""
        }
    }

    private suspend fun planText(): String? {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val items = sl.guardian.medicines().first().filter { it.active }.flatMap { m ->
            DoseSlots.slotsToday(m.times, now, zone).filter { it.second > now }.map { (label, at) -> at to (m.name + " " + label) }
        }.sortedBy { it.first }.map { it.second }
        return items.take(6).joinToString(", ").ifBlank { null }
    }

    private suspend fun unplayedCount(): Int =
        withTimeoutOrNull(4000) { sl.guardian.fetchFamilyMessages().getOrNull()?.count { !it.played } } ?: 0

    private fun canListen(): Boolean = Actions.has(getApplication<Application>(), android.Manifest.permission.RECORD_AUDIO)

    private fun finishUp() {
        reviewNow()
        events.tryEmit(TalkEvent.Done)
    }

    private suspend fun conversation(initial: String?, lang: String, lc: android.content.Context) {
        if (!configured && !checkinMode && initial == null && reason != null) {
            finishUp()
            return
        }
        var next = initial
        var silent = 0
        var failed = 0
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
                                if (reason != null) {
                                    if (reason == WakeReason.Checkin) sl.repo.audit("checkin_no_reply", jo("source" to "conversation"))
                                    finishUp()
                                } else {
                                    error = UiMsg(R.string.talk_silent)
                                }
                                return
                            }
                            continue
                        }
                        VoiceKind.Unavailable -> {
                            error = UiMsg(R.string.voice_unavailable)
                            return
                        }
                        else -> {
                            failed++
                            if (failed <= 2) {
                                kotlinx.coroutines.delay(700)
                                continue
                            }
                            error = UiMsg(R.string.talk_voice_failed)
                            return
                        }
                    }
                }
                silent = 0
                failed = 0
                text = heard
            }
            Wake.noteReply()
            if (Reply.isStop(text)) {
                val b = lc.getString(R.string.talk_bye)
                lines += ChatLine(false, b)
                mode = TalkMode.Speaking
                sl.voice.say(b, lang)
                finishUp()
                return
            }
            lines += ChatLine(true, text)
            if (!handle(text, lang, lc)) {
                if (checkedIn && closesOnFine) finishUp()
                return
            }
            if ((closesOnFine && checkedIn) || (reason == WakeReason.Medicine && doseLogged)) {
                finishUp()
                return
            }
        }
    }

    private suspend fun sayLine(m: String, lang: String) {
        lines += ChatLine(false, m)
        mode = TalkMode.Speaking
        live = m
        try {
            sl.voice.say(m, lang)
        } finally {
            live = ""
        }
    }

    private suspend fun heardStop(lang: String, seconds: Int): Boolean {
        val end = System.currentTimeMillis() + seconds * 1000L
        while (currentCoroutineContext().isActive) {
            val left = end - System.currentTimeMillis()
            if (left <= 0 || !canListen()) return false
            mode = TalkMode.Listening
            val r = withTimeoutOrNull(left) { sl.voice.listen(lang) { partial = it } }
            partial = ""
            sl.voice.cancelListening()
            val t = r?.getOrNull()?.lowercase()?.trim() ?: continue
            if (FirstAid.match(t) != null || com.simha.app.intent.Parser.parse(t, java.time.ZonedDateTime.now()) is com.simha.app.intent.Cmd.Help) return false
            val words = t.trim('.', '!', '?', '।', ',').split(' ').filter { it.isNotBlank() }
            return words.size <= 4 && (com.simha.app.care.Slots.isCancel(t) || Reply.isStop(t) || words.any { it in stopWords })
        }
        return false
    }

    private suspend fun runEmergency(lang: String) {
        if (emergencyDone) return
        emergencyDone = true
        com.simha.app.care.Emergency.start(
            getApplication<Application>(), sl, lang,
            say = { sayLine(it, lang) },
            listenStop = { heardStop(lang, it) },
        )
    }

    private suspend fun handle(text: String, lang: String, lc: android.content.Context): Boolean {
        if (!configured && checkinMode) {
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
                Answer.Unknown -> Unit
            }
        }
        if (closesOnFine && !checkedIn && Reply.classify(text) == Answer.Fine && FirstAid.match(text) == null) {
            sl.repo.checkinOk()
            checkedIn = true
        }
        lastUser = text
        com.simha.app.care.CareStore.talked(getApplication())
        val sit = FirstAid.match(text)
        if (sit != null && FirstAid.dangerous(sit, text)) {
            sl.repo.logEvent("help", jo("source" to "danger_gate", "situation" to sit.name))
            runEmergency(lang)
            events.emit(TalkEvent.OpenEmergency)
        }
        val out = try {
            runner.handle(text, lc, lang)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (out != null) {
            history += Msg.user(text)
            history += Msg.assistant(out.say)
            if (out.undo) undoVisibleUntil = System.currentTimeMillis() + 10_000
            sayLine(out.say, lang)
            val c = out.countdown
            if (c != null) {
                if (heardStop(lang, c.seconds)) sayLine(c.stoppedSay, lang)
                else {
                    val res = try {
                        c.run()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        ""
                    }
                    if (res.isNotBlank()) sayLine(res, lang)
                }
            }
            if (out.emergency) {
                runEmergency(lang)
                events.emit(TalkEvent.OpenEmergency)
            }
            return true
        }
        val filled = runCatching { care.fillPending(text, lc) }.getOrNull()
        if (filled != null) {
            history += Msg.user(text)
            history += Msg.assistant(filled)
            sayLine(filled, lang)
            return true
        }
        if (!configured) {
            sayLine(lc.getString(if (checkinMode) R.string.ci_not_understood else R.string.talk_offline), lang)
            return true
        }
        history += Msg.user(text)
        return respond(lang, lc)
    }

    private suspend fun guardianContext(snap: Snap, lang: String): GuardianContext {
        val p = sl.repo.profileNow()
        val g = sl.guardian
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val logs = g.doses(dayStart).first()
        val meds = g.medicines().first().filter { it.active }.map { MedLine(it.name, it.dose, it.times, DoseSlots.status(it, logs, now, zone)) }
        val all = g.memories(200)
        val asked = all.filter { it.text.startsWith("asked:") }.map { it.text.removePrefix("asked:").trim().lowercase() }.toSet()
        val names = sl.repo.contactsNow().map { it.name }
        val mem = runCatching { sl.graph.retrieve(lastUser, names + listOfNotNull(extra), 16) }.getOrDefault(emptyList())
        val t = LocalTime.now()
        return GuardianContext(
            name = snap.name,
            language = lang,
            profile = MedicalProfile(p.conditions, p.medicines, p.allergies),
            doctor = listOf(p.doctorName, p.doctorPhone).filter { it.isNotBlank() }.joinToString(" "),
            medicines = meds,
            memories = mem,
            todaySig = g.signalLine(today),
            yesterdaySig = g.signalLine(today.minusDays(1)),
            timeOfDay = Guardian.timeOfDay(t.hour, t.minute),
            reason = reason,
            extra = extra,
            unwell = unwell,
            asked = asked,
            hook = Guardian.hookFrom(g.diaryFor(today.minusDays(1))?.sig),
            contacts = sl.repo.contactsNow().map { ContactLine(it.name, it.role, it.approved) },
            brief = runCatching { care.brief() }.getOrDefault(emptyList()),
        )
    }

    private suspend fun respond(lang: String, lc: android.content.Context): Boolean {
        mode = TalkMode.Thinking
        val cfg = sl.llmStore.config()
        val snap = sl.prefs.snap()
        val system: suspend () -> String = { Guardian.systemPrompt(guardianContext(sl.prefs.snap(), lang)) }
        val api = object : ChatApi {
            override suspend fun chat(messages: JsonArray, tools: JsonArray, onDelta: (String) -> Unit): ParsedReply =
                sl.llm.chat(cfg, messages, tools, onDelta)
        }
        live = ""
        fillers.newTurn()
        care.newTurn()
        val graphLines = runCatching { sl.graph.retrieve(lastUser) }.getOrDefault(emptyList())
        val graphTypes = runCatching { sl.graph.types() }.getOrDefault(emptySet())
        val toolSet = com.simha.app.care.ToolIndex.select(lastUser, care.openFormTypes(), com.simha.app.care.ToolIndex.forGraph(graphLines, graphTypes)) +
            (if (closesOnFine) listOf("mark_checkin_ok") else emptyList()) + (if (reason == WakeReason.Medicine) listOf("take_medicine", "list_medicines") else emptyList())
        val fillerText = lc.resources.getStringArray(R.array.r4b_fillers)
        try {
            val reply = Agent.run(
                api, system, history, Host(lang, lc),
                onSentence = { s ->
                    mode = TalkMode.Speaking
                    live = if (live.isEmpty()) s else live + " " + s
                    sl.voice.speak(s, lang)
                },
                onToolStart = {},
                slowMs = fillers.delayMs,
                onSlow = {
                    val i = fillers.pick(lines.any { !it.fromUser } || live.isNotEmpty(), kotlin.random.Random.nextInt(1000))
                    if (i != null && fillerText.isNotEmpty()) sl.voice.speak(fillerText[i % fillerText.size], lang)
                },
                tools = toolSet,
                codeReply = { care.reply() },
                codeAsks = { care.asked },
            )
            if (care.urgent) events.emit(TalkEvent.OpenEmergency)
            if (reply.isNotBlank()) lines += ChatLine(false, reply)
            trimHistory()
            mode = TalkMode.Speaking
            sl.voice.awaitIdle()
            live = ""
            return true
        } catch (e: LlmException) {
            live = ""
            error = UiMsg(R.string.talk_error, e.message)
            val online = runCatching {
                val cm = getApplication<Application>().getSystemService(android.net.ConnectivityManager::class.java)
                cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            }.getOrDefault(true)
            val m = lc.getString(if (online) R.string.talk_err_spoken else R.string.talk_offline)
            lines += ChatLine(false, m)
            mode = TalkMode.Speaking
            sl.voice.say(m, lang)
            return true
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
        reviewNow()
        sl.voice.stopSpeaking()
        sl.voice.cancelListening()
        super.onCleared()
    }

    private inner class Host(val lang: String, val lc: android.content.Context) : ToolHost {
        private val app = getApplication<Application>()
        private val repo = sl.repo
        private val g = sl.guardian

        private fun dayStart(): Long = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        override suspend fun getProfile(): JsonObject {
            val p = repo.profileNow()
            val s = sl.prefs.snap()
            return jo(
                "name" to s.name, "conditions" to p.conditions, "medicines" to p.medicines,
                "allergies" to p.allergies, "doctor_name" to p.doctorName, "doctor_phone" to p.doctorPhone,
            )
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

        override suspend fun allContacts(): List<ContactInfo> =
            repo.contactsNow().map { ContactInfo(it.name, it.phone, it.role, it.approved, it.id) }

        override suspend fun callContact(c: ContactInfo): String = when (repo.placeCall(c.phone, c.approved, "assistant")) {
            CallResult.Called -> "Calling " + c.name + " now."
            CallResult.Dialer -> "The dialer is open for " + c.name + ". The person must press call."
            CallResult.Failed -> "Could not start the call."
        }

        override suspend fun dialContact(c: ContactInfo): String = when (repo.placeCall(c.phone, false, "assistant")) {
            CallResult.Failed -> "Could not open the dialer."
            else -> "The dialer is open for " + c.name + ". The person must press call."
        }

        override suspend fun addContact(label: String, role: String, phone: String): String {
            repo.addContact(label, phone, role)
            return "Saved " + label + " as " + (if (role == "neighbor") "neighbour" else role) + "."
        }

        override suspend fun setCallApproval(c: ContactInfo, allowed: Boolean): String {
            val e = repo.contactsNow().firstOrNull { it.id == c.id } ?: throw IllegalArgumentException("Contact not found")
            sl.db.dao().upsertContact(e.copy(approved = allowed))
            repo.enqueue("upsert", "emergency_contacts", jo("id" to e.id, "name" to e.name, "phone" to e.phone, "role" to e.role, "approved_to_call" to allowed))
            repo.audit("contact_approval_changed", jo("role" to e.role, "granted" to allowed, "by" to "user_voice"))
            return if (allowed) "Calls to " + e.name + " are now allowed." else "Calls to " + e.name + " are no longer automatic."
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

        override suspend fun medicalProfile(): MedicalProfile {
            val p = repo.profileNow()
            val names = g.medicines().first().filter { it.active }.joinToString(", ") { it.name }
            return MedicalProfile(p.conditions, listOf(p.medicines, names).filter { it.isNotBlank() }.joinToString(", "), p.allergies)
        }

        override suspend fun logDiary(update: JsonObject): String {
            val day = LocalDate.now()
            g.upsertDiary(day, DiaryMerge.toSig(update))
            return "Saved to today's diary."
        }

        override suspend fun remember(kind: String, text: String): String =
            if (g.addMemory(kind, text)) "Remembered." else "Already remembered."

        override suspend fun takeMedicine(name: String, status: String): String {
            val n = name.trim().lowercase()
            val meds = g.medicines().first().filter { it.active }
            val m = meds.firstOrNull { it.name.lowercase() == n }
                ?: meds.firstOrNull { it.name.lowercase().contains(n) || n.contains(it.name.lowercase()) }
                ?: throw IllegalArgumentException("No medicine with that name is on the list")
            val now = System.currentTimeMillis()
            val logs = g.doses(dayStart()).first().filter { it.medicineId == m.id }
            var at = DoseSlots.pick(m.times, logs, now, ZoneId.systemDefault())
            if (reason == WakeReason.Medicine) at = maxOf(at, startedAt)
            g.logDose(m.id, at, status)
            if (status == "taken") doseLogged = true
            return "Recorded: " + m.name + " " + status + "."
        }

        override suspend fun listMedicines(): JsonArray {
            val now = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()
            val logs = g.doses(dayStart()).first()
            return buildJsonArray {
                g.medicines().first().filter { it.active }.forEach { m ->
                    add(
                        jo(
                            "name" to m.name, "dose" to m.dose,
                            "times" to JsonArray(m.times.map { JsonPrimitive(it) }),
                            "today" to DoseSlots.status(m, logs, now, zone),
                        )
                    )
                }
            }
        }

        override suspend fun playFamilyMessages(): String {
            val all = g.fetchFamilyMessages().getOrElse { return "Could not reach the server to get the messages." }
            val un = all.filter { !it.played }.reversed()
            if (un.isEmpty()) return "There are no new family messages."
            sl.voice.stopSpeaking()
            var n = 0
            for (m in un) {
                val bytes = g.downloadFamilyMessage(m.storageRef).getOrNull() ?: continue
                sl.voice.playAudio(bytes)
                g.markPlayed(m.id)
                n++
            }
            return if (n == un.size) "Played $n family message(s)." else "Played $n of ${un.size} family message(s). The rest could not be loaded."
        }

        override suspend fun addMedicine(name: String, dose: String, times: List<String>): String {
            val old = g.medicines().first().firstOrNull { it.name.equals(name.trim(), true) }
            g.saveMedicine(Medicine(old?.id ?: UUID.randomUUID().toString(), name.trim(), dose, times, old?.stock, true))
            Wake.scheduleAll(app)
            return "Saved " + name.trim() + (if (dose.isBlank()) "" else " $dose") + " at " + times.joinToString(", ") + ". Reminders are set."
        }

        override suspend fun noteTask(args: JsonObject): String = care.note(args, lc)

        override suspend fun audit(tool: String, args: String, result: String) {
            repo.audit("tool_$tool", jo("args" to args.take(500), "result" to result.take(300)))
        }
    }

    companion object {
        private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

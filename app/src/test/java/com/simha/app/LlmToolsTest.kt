package com.simha.app

import com.simha.app.llm.Agent
import com.simha.app.llm.ChatApi
import com.simha.app.llm.ContactInfo
import com.simha.app.llm.ContactMatch
import com.simha.app.llm.History
import com.simha.app.llm.LlmException
import com.simha.app.llm.LlmParse
import com.simha.app.llm.Msg
import com.simha.app.llm.ParsedReply
import com.simha.app.llm.SentenceSplitter
import com.simha.app.llm.SimhaTools
import com.simha.app.llm.StreamAcc
import com.simha.app.llm.ToolCall
import com.simha.app.llm.ToolHost
import com.simha.app.logic.MedicalProfile
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

private class FakeHost(
    private val contacts: List<ContactInfo> = emptyList(),
    private val recordApproved: Boolean = false,
) : ToolHost {
    val calls = mutableListOf<String>()
    val audits = mutableListOf<Triple<String, String, String>>()

    override suspend fun getProfile() = JsonObject(emptyMap())
    override suspend fun getStatus() = JsonObject(emptyMap())
    override suspend fun logSymptom(text: String): String { calls += "symptom:$text"; return "saved" }
    override suspend fun markCheckinOk(): String { calls += "checkin_ok"; return "ok" }
    override suspend fun openEmergencyScreen(): String { calls += "emergency_screen"; return "ok" }
    override suspend fun dialEmergency(): String { calls += "dial_emergency"; return "ok" }
    override suspend fun allContacts() = contacts
    override suspend fun dialContact(c: ContactInfo): String { calls += "dial:${c.name}"; return "dialer" }
    override suspend fun addContact(label: String, role: String, phone: String): String { calls += "add:$label:$role:$phone"; return "saved" }
    override suspend fun setCallApproval(c: ContactInfo, allowed: Boolean): String { calls += "approve:${c.name}:$allowed"; return "ok" }
    override suspend fun callContact(c: ContactInfo): String { calls += "call:${c.name}"; return "calling" }
    override suspend fun sendFamilyAlert(message: String): String { calls += "alert:$message"; return "ready" }
    override suspend fun recordingApproved() = recordApproved
    override suspend fun startRecording(): String { calls += "record"; return "recording" }
    override suspend fun lookWithCamera(question: String): String { calls += "camera"; return "a cup" }
    override suspend fun setReminder(text: String, minutes: Int): String { calls += "reminder:$minutes"; return "set" }
    override suspend fun updateProfileField(field: String, value: String): String { calls += "profile:$field"; return "ok" }
    override suspend fun rememberHealthDetail(kind: String, text: String): String { calls += "health:$kind:$text"; return "saved" }
    override suspend fun medicalProfile() = MedicalProfile(allergies = "peanuts", medicines = "EpiPen")
    override suspend fun logDiary(update: JsonObject): String { calls += "diary:" + update.keys.sorted().joinToString("+"); return "ok" }
    override suspend fun remember(kind: String, text: String): String { calls += "remember:$kind:$text"; return "ok" }
    override suspend fun takeMedicine(name: String, status: String): String { calls += "dose:$name:$status"; return "ok" }
    override suspend fun listMedicines() = JsonArray(emptyList())
    override suspend fun playFamilyMessages(): String { calls += "play"; return "ok" }
    override suspend fun addMedicine(name: String, dose: String, times: List<String>): String { calls += "med:$name:$dose:" + times.joinToString("+"); return "ok" }
    override suspend fun audit(tool: String, args: String, result: String) { audits += Triple(tool, args, result) }
}

class LlmToolsTest {
    private fun dispatch(host: FakeHost, name: String, args: String) =
        runBlocking { SimhaTools.dispatch(name, args, host) }

    @Test
    fun parsesPlainReply() {
        val r = LlmParse.parse("""{"choices":[{"message":{"role":"assistant","content":"Hello there"}}]}""")
        assertEquals("Hello there", r.content)
        assertTrue(r.toolCalls.isEmpty())
    }

    @Test
    fun parsesToolCalls() {
        val body = """{"choices":[{"message":{"role":"assistant","content":null,"tool_calls":[
            {"id":"c1","type":"function","function":{"name":"call_contact","arguments":"{\"who\":\"Priya\"}"}},
            {"id":"c2","type":"function","function":{"name":"get_profile","arguments":""}}]}}]}"""
        val r = LlmParse.parse(body)
        assertNull(r.content)
        assertEquals(2, r.toolCalls.size)
        assertEquals(ToolCall("c1", "call_contact", """{"who":"Priya"}"""), r.toolCalls[0])
        assertEquals("get_profile", r.toolCalls[1].name)
    }

    @Test
    fun parsesContentParts() {
        val r = LlmParse.parse("""{"choices":[{"message":{"content":[{"type":"text","text":"a"},{"type":"text","text":"b"}]}}]}""")
        assertEquals("ab", r.content)
    }

    @Test
    fun errorBodyThrows() {
        try {
            LlmParse.parse("""{"error":{"message":"bad key"}}""")
            fail()
        } catch (e: LlmException) {
            assertEquals("bad key", e.message)
        }
    }

    @Test
    fun invalidJsonThrows() {
        try {
            LlmParse.parse("<html>")
            fail()
        } catch (e: LlmException) {
            assertTrue(e.message!!.isNotEmpty())
        }
    }

    @Test
    fun httpErrorUsesMessage() {
        assertEquals("HTTP 401: Invalid key", LlmParse.httpError(401, """{"error":{"message":"Invalid key"}}"""))
        assertEquals("HTTP 500: oops", LlmParse.httpError(500, "oops"))
    }

    @Test
    fun streamAccumulatesTextAndToolCalls() {
        val acc = StreamAcc()
        val d1 = acc.feed("""data: {"choices":[{"delta":{"content":"Hel"}}]}""")
        val d2 = acc.feed("""data: {"choices":[{"delta":{"content":"lo."}}]}""")
        acc.feed("""data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"t1","function":{"name":"mark_checkin_ok","arguments":"{"}}]}}]}""")
        acc.feed("""data: {"choices":[{"delta":{"tool_calls":[{"index":0,"function":{"arguments":"}"}}]}}]}""")
        acc.feed("data: [DONE]")
        assertEquals("Hel", d1)
        assertEquals("lo.", d2)
        assertTrue(acc.done)
        val r = acc.result()
        assertEquals("Hello.", r.content)
        assertEquals(listOf(ToolCall("t1", "mark_checkin_ok", "{}")), r.toolCalls)
    }

    @Test
    fun streamIgnoresNonDataLinesAndThrowsOnError() {
        val acc = StreamAcc()
        assertNull(acc.feed(": keep-alive"))
        assertNull(acc.feed(""))
        try {
            acc.feed("""data: {"error":{"message":"rate limited"}}""")
            fail()
        } catch (e: LlmException) {
            assertEquals("rate limited", e.message)
        }
    }

    @Test
    fun sentenceSplitterWaitsForBoundary() {
        val (a, rest) = SentenceSplitter.split("Hello there. How are")
        assertEquals(listOf("Hello there."), a)
        assertEquals(" How are", rest)
        val (b, rest2) = SentenceSplitter.split("Hi. Fine! Ok?")
        assertEquals(listOf("Hi.", "Fine!"), b)
        assertEquals(" Ok?", rest2)
        val (c, _) = SentenceSplitter.split("No boundary yet")
        assertTrue(c.isEmpty())
    }

    @Test
    fun historyTrimKeepsLastUserTurns() {
        val h = mutableListOf<JsonObject>()
        for (i in 1..30) {
            h += Msg.user("u$i")
            h += Msg.assistant("a$i")
        }
        val t = History.trim(h, 20)
        assertEquals(40, t.size)
        assertEquals("u11", t.first()["content"]!!.jsonPrimitive.content)
        assertEquals(h, History.trim(h, 40))
    }

    @Test
    fun unapprovedContactCallIsRefused() {
        val host = FakeHost(contacts = listOf(ContactInfo("Priya", "999", "family", approved = false)))
        val out = dispatch(host, "call_contact", """{"who":"Priya"}""")
        assertTrue(out.contains("refused"))
        assertFalse(host.calls.any { it.startsWith("call:") })
        assertEquals(1, host.audits.size)
        assertEquals("call_contact", host.audits[0].first)
    }

    @Test
    fun approvedContactCallGoesThrough() {
        val host = FakeHost(contacts = listOf(ContactInfo("Priya", "999", "family", approved = true)))
        val out = dispatch(host, "call_contact", """{"who":"priya"}""")
        assertTrue(out.contains("result"))
        assertEquals(listOf("call:Priya"), host.calls)
    }

    @Test
    fun unknownContactIsAnErrorNotACall() {
        val host = FakeHost()
        val out = dispatch(host, "call_contact", """{"who":"Nobody"}""")
        assertTrue(out.contains("error"))
        assertTrue(host.calls.isEmpty())
    }

    @Test
    fun recordingRefusedWithoutApproval() {
        val host = FakeHost(recordApproved = false)
        val out = dispatch(host, "start_recording", "{}")
        assertTrue(out.contains("refused"))
        assertTrue(host.calls.isEmpty())
    }

    @Test
    fun recordingAllowedWithApproval() {
        val host = FakeHost(recordApproved = true)
        dispatch(host, "start_recording", "{}")
        assertEquals(listOf("record"), host.calls)
    }

    @Test
    fun badArgumentsAndUnknownToolsAreErrorsAndAudited() {
        val host = FakeHost()
        assertTrue(dispatch(host, "log_symptom", "{not json").contains("error"))
        assertTrue(dispatch(host, "log_symptom", "{}").contains("error"))
        assertTrue(dispatch(host, "launch_missiles", "{}").contains("Unknown tool"))
        assertTrue(dispatch(host, "set_reminder", """{"text":"x","minutes":0}""").contains("error"))
        assertTrue(dispatch(host, "update_profile_field", """{"field":"password","value":"x"}""").contains("error"))
        assertTrue(host.calls.isEmpty())
        assertEquals(5, host.audits.size)
    }

    @Test
    fun everyDispatchIsAudited() {
        val host = FakeHost()
        dispatch(host, "get_profile", "{}")
        dispatch(host, "dial_emergency", "{}")
        dispatch(host, "log_symptom", """{"text":"dizzy"}""")
        assertEquals(listOf("get_profile", "dial_emergency", "log_symptom"), host.audits.map { it.first })
        assertEquals(listOf("dial_emergency", "symptom:dizzy"), host.calls)
    }

    @Test
    fun firstAidWithDangerOpensEmergencyScreen() {
        val host = FakeHost()
        val out = dispatch(host, "get_first_aid", """{"situation":"chest pain since morning"}""")
        assertTrue(out.contains("\"call_112_now\":true"))
        assertTrue(out.contains("steps"))
        assertEquals(listOf("emergency_screen"), host.calls)
    }

    @Test
    fun firstAidWithoutDangerDoesNotEscalate() {
        val host = FakeHost()
        val out = dispatch(host, "get_first_aid", """{"situation":"low sugar, feeling shaky"}""")
        assertTrue(out.contains("\"call_112_now\":false"))
        assertTrue(host.calls.isEmpty())
        val bad = dispatch(host, "get_first_aid", """{"situation":"hello there"}""")
        assertTrue(bad.contains("error"))
    }

    @Test
    fun firstAidIsPersonalised() {
        val host = FakeHost()
        val out = dispatch(host, "get_first_aid", """{"situation":"allergic reaction"}""")
        assertTrue(out.contains("peanuts"))
    }

    @Test
    fun diaryRememberAndMedicineValidation() {
        val host = FakeHost()
        assertTrue(dispatch(host, "log_diary", "{}").contains("error"))
        dispatch(host, "log_diary", """{"meals":"idli, tea","mood":"happy"}""")
        dispatch(host, "remember", """{"kind":"person","text":"Priya is the daughter"}""")
        assertTrue(dispatch(host, "remember", """{"kind":"summary","text":"x"}""").contains("error"))
        dispatch(host, "take_medicine", """{"name":"Metformin","status":"taken"}""")
        assertTrue(dispatch(host, "take_medicine", """{"name":"Metformin","status":"missed"}""").contains("error"))
        assertEquals(listOf("diary:meals+mood", "remember:person:Priya is the daughter", "dose:Metformin:taken"), host.calls)
    }

    @Test
    fun scamCheckIsStructuredAndNeedsNoHost() {
        val host = FakeHost()
        val out = dispatch(host, "scam_check", """{"description":"A caller said my bank KYC is blocked and asked for my OTP"}""")
        assertTrue(out.contains("\"risk\":\"high\""))
        assertTrue(out.contains("rules"))
        assertTrue(host.calls.isEmpty())
    }

    private val book = listOf(
        ContactInfo("Priya", "999", "family", approved = true, id = "1"),
        ContactInfo("Ravi", "888", "neighbor", approved = false, id = "2"),
        ContactInfo("Dr Rao", "777", "doctor", approved = true, id = "3"),
        ContactInfo("Meena", "666", "neighbor", approved = true, id = "4"),
    )

    private fun names(who: String) = ContactMatch.find(who, book).map { it.name }

    @Test
    fun fuzzyMatchesLabelNameAndRole() {
        assertEquals(listOf("Priya"), names("priya"))
        assertEquals(listOf("Ravi"), names("my neighbour Ravi"))
        assertEquals(listOf("Priya"), names("my family"))
        assertEquals(listOf("Dr Rao"), names("the doctor"))
        assertEquals(listOf("Dr Rao"), names("डॉक्टर"))
        assertEquals(listOf("Ravi", "Meena"), names("पड़ोसी"))
        assertEquals(listOf("Ravi", "Meena"), names("பக்கத்து வீட்டு"))
        assertTrue(names("plumber").isEmpty())
    }

    @Test
    fun multipleMatchesReturnCandidatesWithoutCalling() {
        val h = FakeHost(contacts = book)
        val out = dispatch(h, "call_contact", """{"who":"my neighbour"}""")
        assertTrue(out.contains("candidates"))
        assertTrue(out.contains("Ravi") && out.contains("Meena"))
        assertFalse(out.contains("888"))
        assertTrue(h.calls.isEmpty())
    }

    @Test
    fun unapprovedMatchIsRefusedAndDialerWorks() {
        val h = FakeHost(contacts = book)
        assertTrue(dispatch(h, "call_contact", """{"who":"Ravi"}""").contains("refused"))
        assertTrue(h.calls.isEmpty())
        dispatch(h, "dial_contact", """{"who":"Ravi"}""")
        assertEquals(listOf("dial:Ravi"), h.calls)
        assertTrue(dispatch(h, "call_contact", """{"who":"the doctor"}""").contains("result"))
        assertEquals(listOf("dial:Ravi", "call:Dr Rao"), h.calls)
    }

    @Test
    fun listContactsHasNoNumbers() {
        val out = dispatch(FakeHost(contacts = book), "list_contacts", "{}")
        assertTrue(out.contains("Ravi") && out.contains("approved_to_call"))
        assertFalse(out.contains("888"))
    }

    @Test
    fun addContactValidatesAndNeedsConfirmation() {
        val h = FakeHost()
        assertTrue(dispatch(h, "add_contact", """{"label":"Ravi","role":"neighbour","phone":"12345"}""").contains("error"))
        assertTrue(dispatch(h, "add_contact", """{"label":"Ravi","role":"boss","phone":"9876543210"}""").contains("error"))
        assertTrue(dispatch(h, "add_contact", """{"label":"Ravi","role":"neighbour","phone":"9876543210"}""").contains("refused"))
        assertTrue(h.calls.isEmpty())
        dispatch(h, "add_contact", """{"label":"Ravi","role":"neighbour","phone":"98765 43210","confirmed":true}""")
        assertEquals(listOf("add:Ravi:neighbor:+919876543210"), h.calls)
    }

    @Test
    fun setCallApprovalNeedsExplicitYes() {
        val h = FakeHost(contacts = book)
        assertTrue(dispatch(h, "set_call_approval", """{"who":"Ravi","allowed":true}""").contains("refused"))
        assertTrue(h.calls.isEmpty())
        dispatch(h, "set_call_approval", """{"who":"Ravi","allowed":true,"confirmed":true}""")
        assertEquals(listOf("approve:Ravi:true"), h.calls)
        assertEquals("set_call_approval", h.audits.last().first)
    }

    @Test
    fun addMedicineParsesTimesAndValidates() {
        val h = FakeHost()
        dispatch(h, "add_medicine", """{"name":"Metformin","dose":"500mg","times":["8:00","20:30"]}""")
        assertEquals(listOf("med:Metformin:500mg:08:00+20:30"), h.calls)
        assertTrue(dispatch(h, "add_medicine", """{"name":"X","times":["soon"]}""").contains("error"))
        assertTrue(dispatch(h, "add_medicine", """{"times":["08:00"]}""").contains("error"))
        assertEquals(listOf("08:00", "21:05"), SimhaTools.parseTimes(listOf("21:05", "8.00", "25:00", "08:00")))
    }

    @Test
    fun definitionsCoverAllTools() {
        val names = SimhaTools.definitions.map { it.jsonObject["function"]!!.jsonObject["name"]!!.jsonPrimitive.content }.toSet()
        val expected = setOf(
            "get_profile", "list_contacts", "get_status", "log_symptom", "mark_checkin_ok", "open_emergency_screen",
            "dial_emergency", "call_contact", "dial_contact", "add_contact", "set_call_approval", "send_family_alert", "start_recording", "look_with_camera",
            "set_reminder", "update_profile_field", "remember_health_detail", "get_first_aid", "log_diary", "remember",
            "take_medicine", "list_medicines", "scam_check", "play_family_messages", "add_medicine", "note_task", "find_tool",
        )
        assertEquals(expected, names)
    }

    @Test
    fun agentRunsToolThenAnswers() {
        val host = FakeHost()
        val replies = ArrayDeque(
            listOf(
                ParsedReply(null, listOf(ToolCall("c1", "mark_checkin_ok", "{}"))),
                ParsedReply("Glad you are fine.", emptyList()),
            )
        )
        val api = object : ChatApi {
            override suspend fun chat(messages: JsonArray, tools: JsonArray, onDelta: (String) -> Unit): ParsedReply {
                val r = replies.removeFirst()
                r.content?.let(onDelta)
                return r
            }
        }
        val history = mutableListOf(Msg.user("I am fine"))
        val spoken = mutableListOf<String>()
        var fillers = 0
        val out = runBlocking { Agent.run(api, { "sys" }, history, host, { spoken += it }, { fillers++ }) }
        assertEquals("Glad you are fine.", out)
        assertEquals(listOf("checkin_ok"), host.calls)
        assertEquals(listOf("Glad you are fine."), spoken)
        assertEquals(1, fillers)
        assertEquals(4, history.size)
    }

    @Test
    fun agentStopsAfterFiveToolRounds() {
        val host = FakeHost()
        var n = 0
        val api = object : ChatApi {
            override suspend fun chat(messages: JsonArray, tools: JsonArray, onDelta: (String) -> Unit): ParsedReply {
                n++
                return ParsedReply(null, listOf(ToolCall("c$n", "get_profile", "{}")))
            }
        }
        try {
            runBlocking { Agent.run(api, { "sys" }, mutableListOf(Msg.user("hi")), host, {}, {}) }
            fail()
        } catch (e: LlmException) {
            assertEquals(Agent.MAX_STEPS + 1, n)
        }
    }

    @Test
    fun agentRebuildsSystemPromptBeforeEveryRequest() {
        val host = FakeHost()
        var built = 0
        val seen = mutableListOf<String>()
        val replies = ArrayDeque(listOf(ParsedReply(null, listOf(ToolCall("c1", "get_profile", "{}"))), ParsedReply("ok", emptyList())))
        val api = object : ChatApi {
            override suspend fun chat(messages: JsonArray, tools: JsonArray, onDelta: (String) -> Unit): ParsedReply {
                seen += messages[0].jsonObject["content"]!!.jsonPrimitive.content
                return replies.removeFirst()
            }
        }
        runBlocking { Agent.run(api, { "sys" + (++built) }, mutableListOf(Msg.user("hi")), host, {}, {}) }
        assertEquals(listOf("sys1", "sys2"), seen)
    }
}

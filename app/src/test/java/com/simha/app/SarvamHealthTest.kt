package com.simha.app

import com.simha.app.llm.ContactInfo
import com.simha.app.llm.HealthMemory
import com.simha.app.llm.SarvamConfig
import com.simha.app.llm.SimhaTools
import com.simha.app.llm.ToolHost
import com.simha.app.voice.EpState
import com.simha.app.voice.Endpointer
import com.simha.app.voice.Sarvam
import com.simha.app.voice.SarvamException
import com.simha.app.voice.Wav
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.Base64

private class HealthHost(private val approved: Boolean = false) : ToolHost {
    val saved = mutableListOf<Pair<String, String>>()
    val calls = mutableListOf<String>()
    val audits = mutableListOf<String>()
    override suspend fun getProfile() = JsonObject(emptyMap())
    override suspend fun getContacts() = JsonArray(emptyList())
    override suspend fun getStatus() = JsonObject(emptyMap())
    override suspend fun logSymptom(text: String) = "ok"
    override suspend fun markCheckinOk() = "ok"
    override suspend fun openEmergencyScreen() = "ok"
    override suspend fun dialEmergency() = "ok"
    override suspend fun contactByName(name: String) = ContactInfo("Priya", "9876543210", "family", approved)
    override suspend fun callContact(c: ContactInfo): String { calls += "call"; return "calling" }
    override suspend fun sendFamilyAlert(message: String) = "ok"
    override suspend fun recordingApproved() = false
    override suspend fun startRecording(): String { calls += "record"; return "ok" }
    override suspend fun lookWithCamera(question: String) = "ok"
    override suspend fun setReminder(text: String, minutes: Int) = "ok"
    override suspend fun updateProfileField(field: String, value: String) = "ok"
    override suspend fun rememberHealthDetail(kind: String, text: String): String { saved += kind to text; return "saved" }
    override suspend fun medicalProfile() = com.simha.app.logic.MedicalProfile()
    override suspend fun logDiary(update: JsonObject) = "ok"
    override suspend fun remember(kind: String, text: String) = "ok"
    override suspend fun takeMedicine(name: String, status: String) = "ok"
    override suspend fun listMedicines() = JsonArray(emptyList())
    override suspend fun familyContact(label: String?): ContactInfo? = null
    override suspend fun playFamilyMessages() = "ok"
    override suspend fun addMedicine(name: String, dose: String, times: List<String>) = "ok"
    override suspend fun audit(tool: String, args: String, result: String) { audits += tool }
}

class SarvamHealthTest {
    private fun run(h: HealthHost, name: String, args: String) = runBlocking { SimhaTools.dispatch(name, args, h) }

    @Test
    fun mergeAppendsAndDedupes() {
        assertEquals("diabetes" to true, HealthMemory.merge("", "diabetes"))
        assertEquals("diabetes, Asthma" to true, HealthMemory.merge("diabetes", "Asthma"))
        assertEquals("diabetes, asthma" to false, HealthMemory.merge("diabetes, asthma", "  DIABETES "))
        assertEquals("metformin, aspirin, statin" to true, HealthMemory.merge("metformin", "aspirin and Metformin, statin"))
        assertEquals("a" to false, HealthMemory.merge("a", "  "))
    }

    @Test
    fun rememberToolValidatesAndAudits() {
        val h = HealthHost()
        val ok = run(h, "remember_health_detail", """{"kind":"Medicine","text":"metformin"}""")
        assertTrue(ok.contains("result"))
        assertEquals(listOf("medicine" to "metformin"), h.saved)
        assertEquals(listOf("remember_health_detail"), h.audits)
        assertTrue(run(h, "remember_health_detail", """{"kind":"mood","text":"x"}""").contains("error"))
        assertTrue(run(h, "remember_health_detail", """{"kind":"allergy"}""").contains("error"))
        assertEquals(1, h.saved.size)
    }

    @Test
    fun toolDefinitionListsRememberHealthDetail() {
        val names = SimhaTools.definitions.map { it.jsonObject["function"]!!.jsonObject["name"]!!.jsonPrimitive.content }
        assertTrue("remember_health_detail" in names)
    }

    @Test
    fun refusalStillWorks() {
        val h = HealthHost(approved = false)
        assertTrue(run(h, "call_approved_contact", """{"name":"Priya"}""").contains("refused"))
        assertTrue(run(h, "start_recording", "{}").contains("refused"))
        assertTrue(h.calls.isEmpty())
    }

    @Test
    fun ttsBodyShape() {
        val o = Json.parseToJsonElement(Sarvam.ttsBody("hello", "hi-IN")).jsonObject
        assertEquals("hello", o["text"]!!.jsonPrimitive.content)
        assertEquals("hi-IN", o["language_code"]!!.jsonPrimitive.content)
        assertEquals("bulbul:v3", o["model"]!!.jsonPrimitive.content)
        assertEquals("wav", o["output_audio_codec"]!!.jsonPrimitive.content)
        val long = Json.parseToJsonElement(Sarvam.ttsBody("a".repeat(3000), "en-IN")).jsonObject
        assertEquals(2500, long["text"]!!.jsonPrimitive.content.length)
    }

    @Test
    fun parseTtsDecodesAudio() {
        val raw = byteArrayOf(1, 2, 3, 4)
        val b64 = Base64.getEncoder().encodeToString(raw)
        assertArrayEquals(raw, Sarvam.parseTts("""{"request_id":"r","audios":["$b64"]}"""))
        try {
            Sarvam.parseTts("""{"audios":[]}""")
            fail()
        } catch (e: SarvamException) {
        }
        try {
            Sarvam.parseTts("nope")
            fail()
        } catch (e: SarvamException) {
        }
    }

    @Test
    fun parseSttAndErrors() {
        assertEquals("hello", Sarvam.parseStt("""{"request_id":null,"transcript":" hello ","language_code":"en-IN"}"""))
        try {
            Sarvam.parseStt("""{"language_code":"en-IN"}""")
            fail()
        } catch (e: SarvamException) {
        }
        assertEquals("HTTP 403: bad key", Sarvam.errorText(403, """{"error":{"request_id":"x","message":"bad key","code":"forbidden"}}"""))
        assertTrue(Sarvam.errorText(500, "oops").startsWith("HTTP 500: oops"))
    }

    @Test
    fun outboundRequestAndResponse() {
        val c = SarvamConfig("k", "ak", "org1", "ws1", "app1", "3", "conn1", "+918000000000")
        assertTrue(c.agentReady)
        assertFalse(c.copy(appVersion = "x").agentReady)
        assertEquals("https://apps.sarvam.ai/api/outbounds/v1/orgs/org1/workspaces/ws1/outbounds", Sarvam.outboundUrl(c))
        val o = Json.parseToJsonElement(Sarvam.outboundBody(c, "9876543210", "Hello", "Hindi", mapOf("person_name" to "Asha"))).jsonObject
        val app = o["app_config"]!!.jsonObject
        assertEquals("app1", app["app_id"]!!.jsonPrimitive.content)
        assertEquals(3, app["app_version"]!!.jsonPrimitive.content.toInt())
        assertEquals("conn1", app["connection_config"]!!.jsonObject["connection_id"]!!.jsonPrimitive.content)
        assertEquals("+918000000000", app["connection_config"]!!.jsonObject["agent_phone_number"]!!.jsonPrimitive.content)
        assertEquals("Asha", app["agent_variables"]!!.jsonObject["person_name"]!!.jsonPrimitive.content)
        assertEquals("Hello", app["app_overrides"]!!.jsonObject["initial_bot_message"]!!.jsonPrimitive.content)
        assertEquals("Hindi", app["app_overrides"]!!.jsonObject["initial_language_name"]!!.jsonPrimitive.content)
        assertEquals("+919876543210", o["user_config"]!!.jsonObject["user_phone_number"]!!.jsonPrimitive.content)
        assertEquals("att1", Sarvam.parseAttempt("""{"attempt_id":"att1"}"""))
        try {
            Sarvam.parseAttempt("{}")
            fail()
        } catch (e: SarvamException) {
        }
    }

    @Test
    fun e164Formats() {
        assertEquals("+919876543210", Sarvam.e164("98765 43210"))
        assertEquals("+919876543210", Sarvam.e164("+91 9876543210"))
        assertEquals("+919876543210", Sarvam.e164("919876543210"))
        assertEquals("+919876543210", Sarvam.e164("09876543210"))
    }

    @Test
    fun wavHeader() {
        val w = Wav.encode(ByteArray(10), 16000)
        assertEquals(54, w.size)
        assertEquals("RIFF", String(w, 0, 4))
        assertEquals("WAVE", String(w, 8, 4))
        assertEquals(16000, (w[24].toInt() and 0xff) or ((w[25].toInt() and 0xff) shl 8))
        assertEquals(10, w[40].toInt())
    }

    @Test
    fun endpointerEndsAfterSilence() {
        val e = Endpointer()
        repeat(8) { assertEquals(EpState.Waiting, e.feed(50.0)) }
        repeat(20) { assertEquals(EpState.Waiting, e.feed(3000.0)) }
        var s = EpState.Waiting
        var n = 0
        while (s == EpState.Waiting && n < 100) {
            s = e.feed(40.0)
            n++
        }
        assertEquals(EpState.Done, s)
        assertEquals(40, n)
    }

    @Test
    fun endpointerTimesOutWithoutSpeech() {
        val e = Endpointer()
        var s = EpState.Waiting
        var n = 0
        while (s == EpState.Waiting && n < 1000) {
            s = e.feed(60.0)
            n++
        }
        assertEquals(EpState.Timeout, s)
    }
}

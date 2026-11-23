package com.simha.app

import com.simha.app.data.DoseLog
import com.simha.app.data.Medicine
import com.simha.app.llm.DiaryMerge
import com.simha.app.llm.DoseSlots
import com.simha.app.llm.Scam
import com.simha.app.logic.FirstAid
import com.simha.app.logic.MedicalProfile
import com.simha.app.logic.Situation
import com.simha.app.voice.WakeWordMatcher
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GuardianTest {
    private val none = MedicalProfile()

    @Test
    fun everySituationHasStepsAndDangerSigns() {
        Situation.entries.forEach {
            val g = FirstAid.guide(it, none)
            assertTrue(it.key, g.steps.size >= 4)
            assertTrue(it.key, g.danger.isNotEmpty())
            assertEquals(it.alwaysUrgent, g.call112Now)
        }
    }

    @Test
    fun matchesEnglish() {
        assertEquals(Situation.Fall, FirstAid.match("I fell in the bathroom"))
        assertEquals(Situation.ChestPain, FirstAid.match("my chest hurts and feels heavy"))
        assertEquals(Situation.Stroke, FirstAid.match("her face droop and speech slurred"))
        assertEquals(Situation.LowSugar, FirstAid.match("my sugar is low"))
        assertEquals(Situation.HighSugar, FirstAid.match("high sugar today"))
        assertEquals(Situation.Choking, FirstAid.match("he is choking on food"))
        assertEquals(Situation.Anaphylaxis, FirstAid.match("swollen tongue after the nuts"))
        assertEquals(Situation.Allergy, FirstAid.match("itchy rash after eating"))
        assertEquals(Situation.Breathless, FirstAid.match("short of breath"))
        assertEquals(Situation.Fainting, FirstAid.match("she fainted"))
        assertEquals(Situation.Fever, FirstAid.match("I have a fever"))
        assertEquals(Situation.Burn, FirstAid.match("I burned my hand with hot oil"))
        assertEquals(Situation.Bleeding, FirstAid.match("the cut is bleeding"))
        assertEquals(Situation.MedicineError, FirstAid.match("I took my tablet twice by mistake"))
        assertEquals(Situation.Dizziness, FirstAid.match("feeling dizzy"))
        assertEquals(Situation.Heat, FirstAid.match("I think it is a sunstroke"))
        assertNull(FirstAid.match("what is the weather"))
        assertNull(FirstAid.match(""))
    }

    @Test
    fun matchesIndianLanguages() {
        assertEquals(Situation.ChestPain, FirstAid.match("मेरे सीने में दर्द है"))
        assertEquals(Situation.Fall, FirstAid.match("मैं गिर गया"))
        assertEquals(Situation.Fever, FirstAid.match("मुझे बुखार है"))
        assertEquals(Situation.ChestPain, FirstAid.match("ఛాతీ నొప్పి వస్తోంది"))
        assertEquals(Situation.Fever, FirstAid.match("నాకు జ్వరం ఉంది"))
        assertEquals(Situation.Fall, FirstAid.match("నేను కింద పడ్డాను"))
        assertEquals(Situation.ChestPain, FirstAid.match("ಎದೆ ನೋವು ಇದೆ"))
        assertEquals(Situation.Fever, FirstAid.match("ನನಗೆ ಜ್ವರ ಇದೆ"))
        assertEquals(Situation.Fall, FirstAid.match("ನಾನು ಬಿದ್ದೆ"))
        assertEquals(Situation.ChestPain, FirstAid.match("நெஞ்சு வலி இருக்கிறது"))
        assertEquals(Situation.Fever, FirstAid.match("எனக்கு காய்ச்சல்"))
        assertEquals(Situation.Fall, FirstAid.match("நான் கீழே விழுந்தேன்"))
        assertEquals(Situation.Stroke, FirstAid.match("पक्षाघात जैसा लग रहा है"))
        assertEquals(Situation.LowSugar, FirstAid.match("शुगर कम हो गई"))
    }

    @Test
    fun dangerSignsEscalateOnlyWhenPresent() {
        assertTrue(FirstAid.dangerous(Situation.ChestPain, "chest pain"))
        assertTrue(FirstAid.dangerous(Situation.Stroke, "stroke"))
        assertFalse(FirstAid.dangerous(Situation.Fever, "fever since morning"))
        assertTrue(FirstAid.dangerous(Situation.Fever, "fever and confused"))
        assertFalse(FirstAid.dangerous(Situation.Fall, "fell but nothing hurts"))
        assertTrue(FirstAid.dangerous(Situation.Fall, "fell and hit my head"))
        assertTrue(FirstAid.dangerous(Situation.Fall, "fell and cannot get up"))
        assertTrue(FirstAid.dangerous(Situation.Dizziness, "dizzy with chest pain"))
        assertFalse(FirstAid.dangerous(Situation.Dizziness, "dizzy after standing"))
    }

    @Test
    fun guideIsPersonalised() {
        val p = MedicalProfile("diabetes, asthma", "Metformin, Insulin, Asthalin inhaler, Warfarin", "peanuts, aspirin")
        assertTrue(FirstAid.guide(Situation.Anaphylaxis, p).personal.any { it.contains("peanuts") })
        assertTrue(FirstAid.guide(Situation.Breathless, p).personal.any { it.contains("inhaler") })
        assertTrue(FirstAid.guide(Situation.LowSugar, p).personal.any { it.contains("insulin") })
        assertTrue(FirstAid.guide(Situation.ChestPain, p).personal.any { it.contains("do NOT take aspirin") })
        assertTrue(FirstAid.guide(Situation.Fall, p).personal.any { it.contains("blood thinner") })
        assertTrue(FirstAid.guide(Situation.Anaphylaxis, MedicalProfile(medicines = "EpiPen")).personal.any { it.contains("EpiPen") })
        assertTrue(FirstAid.guide(Situation.Fall, none).personal.isEmpty())
    }

    @Test
    fun scamCheckFlagsHighRiskAndKeepsRules() {
        val high = Scam.check("Someone calling from the police said I will be arrested unless I send money by UPI")
        assertTrue(high.high)
        assertTrue(high.signals.isNotEmpty())
        assertTrue(high.rules.any { it.contains("OTP") })
        val low = Scam.check("My neighbour invited me for tea")
        assertFalse(low.high)
        assertTrue(low.signals.isEmpty())
        assertTrue(Scam.check("They asked me to share the OTP").high)
        assertTrue(Scam.check("a link about a parcel and a refund").high)
    }

    private fun obj(vararg p: Pair<String, List<String>>) = JsonObject(p.associate { (k, v) -> k to JsonArray(v.map { JsonPrimitive(it) }) })

    @Test
    fun diaryUpdateSplitsListsAndSkipsBlanks() {
        val u = DiaryMerge.update(mapOf("meals" to "idli, tea", "mood" to "happy", "pain" to " ", "water" to null))
        assertEquals(setOf("meals", "mood"), u.keys)
        assertEquals(listOf("idli", "tea"), u["meals"]!!.jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun diaryMergeAppendsListsDedupesAndOverwritesScalars() {
        val existing = JsonObject(
            mapOf("meals" to JsonArray(listOf(JsonPrimitive("Idli"))), "mood" to JsonPrimitive("tired"), "visitors" to JsonArray(listOf(JsonPrimitive("Priya")))),
        )
        val u = DiaryMerge.update(mapOf("meals" to "idli, rice", "mood" to "better", "water" to "4 glasses"))
        val m = DiaryMerge.merge(existing, u)
        assertEquals(listOf("Idli", "rice"), m["meals"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals("better", m["mood"]!!.jsonPrimitive.content)
        assertEquals("4 glasses", m["water"]!!.jsonPrimitive.content)
        assertEquals(listOf("Priya"), m["visitors"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals(setOf("meals", "mood", "water", "visitors"), m.keys)
    }

    @Test
    fun diaryMergeWithNothingExistingAndDerivedKeys() {
        val m = DiaryMerge.merge(null, DiaryMerge.update(mapOf("activities" to "walk, prayer", "mood" to "calm")))
        val d = DiaryMerge.withDerived(m)
        assertEquals("walk, prayer", d["activity"]!!.jsonPrimitive.content)
        assertTrue(d["summary"]!!.jsonPrimitive.content.contains("Mood: calm"))
        val again = DiaryMerge.withDerived(DiaryMerge.merge(d, DiaryMerge.update(mapOf("activities" to "tv"))))
        assertEquals("walk, prayer, tv", again["activity"]!!.jsonPrimitive.content)
        assertEquals(DiaryMerge.summary(again), again["summary"]!!.jsonPrimitive.content)
    }

    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(h: Int, m: Int = 0) = LocalDate.of(2026, 11, 23).atTime(h, m).atZone(zone).toInstant().toEpochMilli()
    private val med = Medicine("m1", "Metformin", "500mg", listOf("08:00", "20:00"))

    @Test
    fun doseStatusAndSlotPick() {
        val now = at(9)
        assertEquals("08:00 due, not logged yet, 20:00 not yet due", DoseSlots.status(med, emptyList(), now, zone))
        assertEquals(at(8), DoseSlots.pick(med.times, emptyList(), now, zone))
        val logs = listOf(DoseLog("d1", "m1", at(8), "taken", at(8, 5)))
        assertEquals("08:00 taken, 20:00 not yet due", DoseSlots.status(med, logs, now, zone))
        assertEquals(at(20), DoseSlots.pick(med.times, logs, now, zone))
        assertEquals(now, DoseSlots.pick(emptyList(), emptyList(), now, zone))
    }

    @Test
    fun wakeWordMatches() {
        assertTrue(WakeWordMatcher.matches("Hey Simha"))
        assertTrue(WakeWordMatcher.matches("ok simha please"))
        assertTrue(WakeWordMatcher.matches("hey simba"))
        assertTrue(WakeWordMatcher.matches("हे सिम्हा"))
        assertTrue(WakeWordMatcher.matches("హే సింహా"))
        assertTrue(WakeWordMatcher.matches("ಹೇ ಸಿಂಹ"))
        assertTrue(WakeWordMatcher.matches("ஹே சிம்ஹா"))
        assertFalse(WakeWordMatcher.matches("good morning"))
    }
}

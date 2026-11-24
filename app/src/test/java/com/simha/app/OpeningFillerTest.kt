package com.simha.app

import com.simha.app.llm.FillerGate
import com.simha.app.llm.ContactLine
import com.simha.app.llm.Guardian
import com.simha.app.llm.GuardianContext
import com.simha.app.llm.MedLine
import com.simha.app.llm.Known
import com.simha.app.llm.Open
import com.simha.app.logic.MedicalProfile
import com.simha.app.voice.Levels
import com.simha.app.wake.WakeReason
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningFillerTest {
    private val none = Known(medicines = false)

    @Test
    fun noMedicinesNeverAsksAboutTakingOne() {
        assertEquals(Open.Checkin, Guardian.openingKey(WakeReason.Medicine, none, null))
        assertEquals(Open.Checkin, Guardian.openingKey(WakeReason.Medicine, none, " "))
        assertEquals(Open.Medicine, Guardian.openingKey(WakeReason.Medicine, none, "Metformin 500mg"))
    }

    @Test
    fun asksAboutMedicinesOnlyOnceWhenNoneKnown() {
        assertEquals(Open.AskMedicines, Guardian.openingKey(WakeReason.Morning, none))
        assertEquals(Open.AskMedicines, Guardian.openingKey(null, none))
        val asked = none.copy(asked = setOf("medicines"))
        assertEquals(Open.Morning, Guardian.openingKey(WakeReason.Morning, asked))
        assertEquals(Open.None, Guardian.openingKey(null, asked))
        assertEquals(Open.Morning, Guardian.openingKey(WakeReason.Morning, Known(medicines = true)))
        assertEquals(Open.None, Guardian.openingKey(null, Known(medicines = true)))
    }

    @Test
    fun newPersonGetsIntroOnce() {
        val c = GuardianContext("Ravi", "en", MedicalProfile())
        assertTrue(c.known().newPerson)
        assertEquals(Open.Intro, Guardian.openingKey(null, c.known()))
        assertEquals(Open.Intro, Guardian.openingKey(WakeReason.Morning, c.known()))
        val after = c.copy(asked = setOf("intro"))
        assertFalse(after.known().newPerson)
        assertFalse(c.copy(memories = listOf("Priya is the daughter")).known().newPerson)
        assertFalse(c.copy(medicines = listOf(MedLine("A", "", listOf("08:00"), ""))).known().newPerson)
        assertEquals(Open.Evening, Guardian.openingKey(WakeReason.Evening, c.known()))
    }

    @Test
    fun promptBranchesOnKnownData() {
        val bare = Guardian.systemPrompt(GuardianContext("Ravi", "en", MedicalProfile()))
        assertTrue(bare.contains("Never ask whether they took their medicine"))
        assertTrue(bare.contains("add_medicine"))
        assertTrue(bare.contains("asked:conditions"))
        val full = Guardian.systemPrompt(
            GuardianContext(
                "Ravi", "en", MedicalProfile("bp", "", "none"), doctor = "Dr Rao",
                medicines = listOf(MedLine("Amlodipine", "5mg", listOf("08:00"), "08:00 taken")), asked = setOf("medicines"),
            )
        )
        assertFalse(full.contains("Never ask whether they took their medicine"))
        assertFalse(full.contains("Do you take any medicines every day"))
        assertFalse(full.contains("asked:conditions"))
        assertFalse(full.contains("asked:doctor"))
    }

    @Test
    fun promptListsContactsWithoutNumbers() {
        val p = Guardian.systemPrompt(
            GuardianContext(
                "Ravi", "en", MedicalProfile(),
                contacts = listOf(ContactLine("Meena", "neighbor", false), ContactLine("Priya", "family", true)),
            )
        )
        assertTrue(p.contains("- Meena, neighbour, calls not approved"))
        assertTrue(p.contains("- Priya, family, calls approved"))
        assertTrue(p.contains("call_contact"))
        assertFalse(p.contains("+91"))
        assertTrue(Guardian.systemPrompt(GuardianContext("Ravi", "en", MedicalProfile())).contains("Contacts: none saved yet."))
    }

    @Test
    fun variantIsStableAndInRange() {
        assertEquals(Guardian.variant(5, 3), Guardian.variant(5, 3))
        assertEquals(2, Guardian.variant(5, 3))
        assertEquals(1, Guardian.variant(-2, 3))
    }

    @Test
    fun hookFromPain() {
        assertEquals("back", Guardian.hookFrom(com.simha.app.logic.Sig(pain = mapOf("knee" to 1, "back" to 3))))
        assertNull(Guardian.hookFrom(com.simha.app.logic.Sig()))
        assertNull(Guardian.hookFrom(null))
    }

    @Test
    fun fillerOncePerTurnNeverBeforeReplyNeverSameTwice() {
        val g = FillerGate(4)
        assertNull(g.pick(false, 1))
        val a = g.pick(true, 1)
        assertEquals(1, a)
        assertNull(g.pick(true, 2))
        g.newTurn()
        val b = g.pick(true, 1)
        assertNotEquals(a, b)
        var last = b
        repeat(20) {
            g.newTurn()
            val n = g.pick(true, 3)
            assertNotEquals(last, n)
            last = n
        }
    }

    @Test
    fun levelsNormalise() {
        assertEquals(0f, Levels.fromDb(-2f), 0.001f)
        assertEquals(1f, Levels.fromDb(10f), 0.001f)
        assertEquals(0f, Levels.fromDb(-30f), 0.001f)
        assertEquals(0f, Levels.fromRms(0.0), 0.001f)
        assertEquals(1f, Levels.fromRms(9000.0), 0.001f)
        assertEquals(0, Levels.envelope(ByteArray(10)).size)
        val pcm = ByteArray(16000 * 2 / 2) { if (it % 2 == 0) 0x40 else 0x10 }
        val wav = com.simha.app.voice.Wav.encode(pcm, 16000)
        val e = Levels.envelope(wav)
        assertEquals(12, e.size)
        assertTrue(e.all { it > 0.5f })
    }
}

package com.simha.app

import com.simha.app.logic.Ago
import com.simha.app.logic.Answer
import com.simha.app.logic.EmergencyPlan
import com.simha.app.logic.Reply
import com.simha.app.logic.StepId
import com.simha.app.logic.TimeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlansTest {
    private fun ids(c: String = "", m: String = "", a: String = "", f: String? = null, r: Boolean = false) =
        EmergencyPlan.build(c, m, a, f, r).map { it.id }

    @Test
    fun baseStepsAlwaysPresentAndInOrder() {
        assertEquals(listOf(StepId.Calm, StepId.Call112, StepId.Unlock), ids())
    }

    @Test
    fun heartConditionAddsHeartStep() {
        assertTrue(StepId.Heart in ids(c = "High Blood Pressure"))
        assertTrue(StepId.Heart in ids(c = "heart problem"))
    }

    @Test
    fun diabetesAddsDiabetesStep() {
        assertTrue(StepId.Diabetes in ids(c = "Diabetes type 2"))
        assertTrue(StepId.Diabetes in ids(c = "sugar"))
    }

    @Test
    fun asthmaAddsBreathStep() {
        assertTrue(StepId.Breath in ids(c = "Asthma"))
    }

    @Test
    fun medicinesAndAllergiesCarryText() {
        val s = EmergencyPlan.build("", " Metformin ", " penicillin ", null, false)
        assertEquals("Metformin", s.first { it.id == StepId.Meds }.arg)
        assertEquals("penicillin", s.first { it.id == StepId.Allergy }.arg)
    }

    @Test
    fun blankFieldsAddNothing() {
        val r = ids(m = "  ", a = "")
        assertFalse(StepId.Meds in r)
        assertFalse(StepId.Allergy in r)
    }

    @Test
    fun familyStepOnlyWithName() {
        assertFalse(StepId.Family in ids(f = null))
        assertFalse(StepId.Family in ids(f = " "))
        assertTrue(StepId.Family in ids(f = "Priya"))
    }

    @Test
    fun recordStepOnlyWhenAllowed() {
        assertFalse(StepId.Record in ids(r = false))
        assertEquals(StepId.Record, ids(r = true).last())
    }

    @Test
    fun classifyFineAndHelp() {
        assertEquals(Answer.Fine, Reply.classify("I am fine"))
        assertEquals(Answer.Fine, Reply.classify("yes okay"))
        assertEquals(Answer.Help, Reply.classify("I need help"))
        assertEquals(Answer.Help, Reply.classify("I am not fine"))
        assertEquals(Answer.Help, Reply.classify("no"))
        assertEquals(Answer.Unknown, Reply.classify("the weather is nice"))
        assertEquals(Answer.Unknown, Reply.classify("a bookshelf"))
    }

    @Test
    fun classifyIndianLanguages() {
        assertEquals(Answer.Fine, Reply.classify("हाँ ठीक हूँ"))
        assertEquals(Answer.Help, Reply.classify("मदद चाहिए"))
        assertEquals(Answer.Help, Reply.classify("సహాయం కావాలి"))
        assertEquals(Answer.Help, Reply.classify("உதவி வேண்டும்"))
    }

    @Test
    fun stopWords() {
        assertTrue(Reply.isStop("okay bye"))
        assertTrue(Reply.isStop("Stop"))
        assertTrue(Reply.isStop("अलविदा"))
        assertFalse(Reply.isStop("tell me a story"))
    }

    @Test
    fun agoBuckets() {
        val now = 10_000_000_000L
        assertEquals(Ago.Never, TimeUtil.ago(now, null))
        assertEquals(Ago.Never, TimeUtil.ago(now, 0L))
        assertEquals(Ago.Now, TimeUtil.ago(now, now - 30_000))
        assertEquals(Ago.Minutes(5), TimeUtil.ago(now, now - 5 * 60_000))
        assertEquals(Ago.Hours(2), TimeUtil.ago(now, now - 125 * 60_000))
        assertEquals(Ago.Days(3), TimeUtil.ago(now, now - 3L * 24 * 60 * 60_000))
        assertEquals(Ago.Now, TimeUtil.ago(now, now + 60_000))
    }

    @Test
    fun isoParsing() {
        assertEquals(0L, TimeUtil.isoMillis("1970-01-01T00:00:00+00:00"))
        assertNull(TimeUtil.isoMillis("not a date"))
    }
}

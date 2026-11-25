package com.simha.app

import com.simha.app.care.CareFacts
import com.simha.app.care.Checks
import com.simha.app.care.Clock
import com.simha.app.care.Flag
import com.simha.app.care.FormStack
import com.simha.app.care.GEdge
import com.simha.app.care.GNode
import com.simha.app.care.GraphRank
import com.simha.app.care.NodeType
import com.simha.app.care.ReviewParse
import com.simha.app.care.Step
import com.simha.app.care.TaskType
import com.simha.app.care.ToolIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class CareTest {
    @Test
    fun clockResolvesSpokenTimes() {
        assertEquals("17:00", Clock.resolve("5", LocalTime.of(15, 0)))
        assertEquals("17:00", Clock.resolve("5", LocalTime.of(9, 0)))
        assertEquals("20:00", Clock.resolve("8", LocalTime.of(10, 0)))
        assertEquals("08:00", Clock.resolve("8", LocalTime.of(7, 0)))
        assertEquals("20:30", Clock.resolve("8.30 pm", LocalTime.of(7, 0)))
        assertEquals("00:15", Clock.resolve("12:15 am", LocalTime.of(7, 0)))
        assertEquals("17:00", Clock.resolve("17:00", LocalTime.of(7, 0)))
        assertNull(Clock.resolve("soon", LocalTime.of(7, 0)))
        assertNull(Clock.resolve("25:00", LocalTime.of(7, 0)))
    }

    @Test
    fun formAsksMissingSlotThenCompletes() {
        val f = FormStack()
        f.offer(TaskType.Medicine, mapOf("time" to "17:00"))
        val s1 = f.drain()
        assertEquals(1, s1.size)
        assertEquals("name", (s1[0] as Step.Ask).slot)
        f.offer(TaskType.Medicine, mapOf("name" to "Amlodipine"))
        val s2 = f.drain()
        assertTrue(s2[0] is Step.Done)
        assertEquals("Amlodipine", (s2[0] as Step.Done).form.slots["name"])
        assertTrue(f.forms.isEmpty())
    }

    @Test
    fun twoTasksInOneSentence() {
        val f = FormStack()
        f.offer(TaskType.Medicine, mapOf("time" to "17:00"))
        f.offer(TaskType.Reminder, mapOf("what" to "pick up laundry", "time" to "18:00"))
        val steps = f.drain()
        assertTrue(steps[0] is Step.Done)
        assertEquals(TaskType.Reminder, (steps[0] as Step.Done).form.type)
        assertEquals("name", (steps[1] as Step.Ask).slot)
        assertEquals(TaskType.Medicine to "name", f.pending())
    }

    @Test
    fun conflictingValuesOpenNewForm() {
        val f = FormStack()
        f.offer(TaskType.Medicine, mapOf("name" to "A"))
        f.offer(TaskType.Medicine, mapOf("name" to "B"))
        assertEquals(2, f.forms.size)
    }

    @Test
    fun waterChecksUseContext() {
        val evening = CareFacts(hour = 19)
        assertEquals(Flag.WaterLow, Checks.intake("water", "1 litre", evening))
        assertNull(Checks.intake("water", "2.5 litres", evening))
        assertEquals(Flag.WaterLow, Checks.intake("water", "1.8 L", evening.copy(heatDay = true)))
        assertEquals(Flag.WaterLow, Checks.intake("water", "3 glasses", evening))
        assertNull(Checks.intake("water", "1 litre", CareFacts(hour = 9)))
        val heart = CareFacts(conditions = "heart failure", hour = 19)
        assertNull(Checks.intake("water", "1 litre", heart))
        assertEquals(Flag.WaterHigh, Checks.intake("water", "2 litres", heart))
    }

    @Test
    fun sleepPainMoodMeal() {
        assertEquals(Flag.SleepShort, Checks.intake("sleep", "4 hours", CareFacts()))
        assertNull(Checks.intake("sleep", "7 hours", CareFacts()))
        assertEquals(Flag.PainUp, Checks.intake("pain", "7", CareFacts(yesterdayPain = 4)))
        assertNull(Checks.intake("pain", "mild", CareFacts(yesterdayPain = 4)))
        assertEquals(Flag.MoodLow, Checks.intake("mood", "feeling lonely", CareFacts()))
        assertEquals(Flag.MealSkipped, Checks.intake("meal", "skipped lunch", CareFacts()))
        assertTrue(Checks.fluidRestricted(CareFacts(conditions = "CKD stage 3")))
    }

    private fun n(id: Long, t: NodeType, label: String, w: Int = 1) = GNode(id, t.code, label.lowercase(), label, w, t.pinned, 1_000_000)

    @Test
    fun graphRetrievesConnectedFacts() {
        val nodes = listOf(
            n(1, NodeType.Person, "me", 3), n(2, NodeType.Condition, "high BP", 3), n(3, NodeType.Medicine, "Amlodipine", 2),
            n(4, NodeType.Person, "Priya", 2), n(5, NodeType.Place, "Pune"), n(6, NodeType.Fact, "likes gardening"),
        )
        val now = 2_000_000L
        val edges = listOf(
            GEdge(1, 2, "has", 1f, now), GEdge(3, 2, "treats", 1f, now), GEdge(4, 1, "daughter_of", 1f, now),
            GEdge(4, 5, "lives_in", 1f, now), GEdge(1, 6, "likes", 1f, now),
        )
        val lines = GraphRank.retrieve(nodes, edges, "when is Priya visiting", emptyList(), now)
        assertTrue(lines.any { it.contains("Priya") && it.contains("Pune") })
        assertTrue(lines.any { it.contains("high BP") })
        assertTrue(lines.size <= 16)
    }

    @Test
    fun pruneKeepsPinnedAndCaps() {
        val now = 100L * 86_400_000
        val nodes = (1..230L).map { GNode(it, NodeType.Fact.code, "f$it", "f$it", if (it < 10) 5 else 1, false, now) } +
            GNode(999, NodeType.Allergy.code, "penicillin", "penicillin", 1, true, 0)
        val drop = GraphRank.prune(nodes, emptyList(), now)
        assertFalse(999L in drop)
        assertEquals(nodes.size - GraphRank.MAX_NODES, drop.size)
        assertTrue((1L..9L).none { it in drop })
    }

    @Test
    fun toolIndexLoadsOnlyWhatIsNeeded() {
        val med = ToolIndex.select("I need to take my tablet at 5", emptySet())
        assertTrue("add_medicine" in med)
        assertTrue(ToolIndex.core.all { it in med })
        assertFalse("scam_check" in med)
        assertTrue("take_medicine" in ToolIndex.select("मुझे दवा लेनी है", emptySet()))
        assertTrue("scam_check" in ToolIndex.select("someone from the bank asked my OTP", emptySet()))
        assertTrue("log_diary" in ToolIndex.select("ok", setOf(TaskType.Intake)))
        assertTrue("scam_check" in ToolIndex.search("check a fraud call"))
        assertEquals(ToolIndex.core.size, ToolIndex.select("hello", emptySet()).size)
    }

    @Test
    fun reviewParsesModelJson() {
        val raw = """Sure: {"nodes":[{"type":"person","label":"Priya"}],"links":[{"a":{"type":"person","label":"Priya"},"rel":"daughter_of","b":{"type":"person","label":"me"}}],"concerns":[{"text":"knee pain","check_in_minutes":120,"question":"How is your knee now?"}],"resolved":[]}"""
        val r = ReviewParse.parse(raw)
        assertNotNull(r)
        assertEquals(1, r!!.nodes.size)
        assertEquals("daughter_of", r.links[0].rel)
        assertEquals(120, r.concerns[0].minutes)
        assertNull(ReviewParse.parse("no json here"))
        val bad = ReviewParse.parse("""{"nodes":[{"type":"alien","label":"x"}],"concerns":[{"text":"a"}]}""")
        assertEquals(0, bad!!.nodes.size)
        assertEquals(0, bad.concerns.size)
    }
}

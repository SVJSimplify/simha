package com.simha.app

import com.simha.app.data.Diary
import com.simha.app.data.DoseLog
import com.simha.app.data.EventRow
import com.simha.app.ui.home.weekStats
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CaregiverStatsTest {
    private val z = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 11, 23)
    private fun ms(d: LocalDate) = d.atTime(9, 0).atZone(z).toInstant().toEpochMilli()

    @Test
    fun weekly() {
        val doses = listOf(
            DoseLog("1", "a", ms(today), "taken", 0),
            DoseLog("2", "a", ms(today.minusDays(1)), "missed", 0),
            DoseLog("3", "a", ms(today.minusDays(20)), "missed", 0),
        )
        val ev = listOf(EventRow("checkin_missed", null, "2026-11-22T10:00:00Z"), EventRow("checkin_missed", null, "2026-10-01T10:00:00Z"))
        val dia = listOf(Diary(today, JsonObject(mapOf("mood" to JsonPrimitive("4"), "walking" to JsonPrimitive("20 min"))), "", 0))
        val w = weekStats(ev, doses, dia, today, z)
        assertEquals(50, w.dosePct)
        assertEquals(1, w.missedCheckins)
        assertEquals("4.0", w.avgMood)
        assertEquals(listOf("2026-11-23: 20 min"), w.notes)
        assertNull(weekStats(emptyList(), emptyList(), emptyList(), today, z).dosePct)
    }
}

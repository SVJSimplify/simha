package com.simha.app

import com.simha.app.data.Diary
import com.simha.app.data.EventRow
import com.simha.app.logic.Sig
import com.simha.app.ui.home.dayStats
import com.simha.app.ui.home.weekStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CaregiverStatsTest {
    private val z = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 11, 23)

    @Test
    fun weekly() {
        val dia = listOf(
            Diary(today, Sig(mood = 4, taken = 1, due = 2, ciMiss = 1, note = "20 min walk", exc = setOf("fall"), pain = mapOf("knee" to 2)), 0),
            Diary(today.minusDays(1), Sig(mood = 3, taken = 2, due = 2), 0),
            Diary(today.minusDays(20), Sig(mood = 1, taken = 0, due = 5, ciMiss = 9), 0),
        )
        val w = weekStats(dia, today)
        assertEquals(75, w.dosePct)
        assertEquals(1, w.missedCheckins)
        assertEquals("3.5", w.avgMood)
        assertEquals(listOf("2026-11-23: 20 min walk"), w.notes)
        assertEquals(1, w.falls)
        assertEquals(listOf("knee"), w.pain)
        assertNull(weekStats(emptyList(), today).dosePct)
    }

    @Test
    fun daily() {
        val ev = listOf(EventRow("checkin_ok", null, "2026-11-23T10:00:00Z"), EventRow("checkin_missed", null, "2026-11-22T10:00:00Z"))
        val d = dayStats(ev, Diary(today, Sig(mood = 5, taken = 1, due = 3), 0), today, z)
        assertEquals("5", d.mood)
        assertEquals(1, d.fine)
        assertEquals(0, d.missed)
        assertEquals(1, d.taken)
        assertEquals(2, d.dosesMissed)
        assertNull(dayStats(ev, null, today, z).mood)
    }
}

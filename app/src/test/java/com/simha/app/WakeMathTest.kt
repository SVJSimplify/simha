package com.simha.app

import com.simha.app.data.Weather
import com.simha.app.data.WeatherReport
import com.simha.app.wake.SlotKind
import com.simha.app.wake.WakeMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class WakeMathTest {
    private val z = ZoneId.of("Asia/Kolkata")
    private fun ms(h: Int, m: Int, d: Int = 23) = LocalDateTime.of(2026, 11, d, h, m).atZone(z).toInstant().toEpochMilli()

    @Test
    fun checkinFollowsLastActivityPlusInactivity() {
        assertEquals(ms(12, 0), WakeMath.checkinAt(ms(9, 0), 0, 180, 5, 0, ms(10, 0)))
    }

    @Test
    fun checkinNeverInThePast() {
        assertEquals(ms(10, 0) + WakeMath.MIN_LEAD_MS, WakeMath.checkinAt(ms(5, 0), 0, 180, 5, 0, ms(10, 0)))
    }

    @Test
    fun checkinWhileAwaitingAnswerUsesWait() {
        assertEquals(ms(10, 5), WakeMath.checkinAt(ms(9, 0), ms(10, 0), 180, 5, 0, ms(10, 1)))
    }

    @Test
    fun noCheckinAlarmAfterEscalation() {
        assertNull(WakeMath.checkinAt(ms(9, 0), ms(10, 0), 180, 5, ms(10, 0), ms(10, 30)))
    }

    @Test
    fun planHasDailySlotsAndDistinctMedicineTimes() {
        val s = WakeMath.plan(ms(9, 0), z, ms(12, 0), listOf("08:00", "14:00", "14:00", "bad", "20:15"), null)
        assertEquals(ms(7, 30, 24), s.first { it.kind == SlotKind.Morning }.at)
        assertEquals(ms(19, 30), s.first { it.kind == SlotKind.Evening }.at)
        assertEquals(ms(12, 30), s.first { it.kind == SlotKind.Heat }.at)
        val meds = s.filter { it.kind == SlotKind.Medicine }
        assertEquals(listOf("08:00", "14:00", "20:15"), meds.map { it.tag })
        assertEquals(listOf(ms(8, 0, 24), ms(14, 0), ms(20, 15)), meds.map { it.at })
        assertEquals(3, meds.map { it.code }.toSet().size)
        assertNotNull(s.firstOrNull { it.kind == SlotKind.Checkin })
    }

    @Test
    fun learnedWakeTimeMovesMorning() {
        val s = WakeMath.plan(ms(5, 0), z, null, emptyList(), LocalTime.of(6, 15))
        assertEquals(ms(6, 15), s.first { it.kind == SlotKind.Morning }.at)
        assertTrue(s.none { it.kind == SlotKind.Checkin })
    }

    @Test
    fun slotJustPassedRollsToTomorrow() {
        val s = WakeMath.plan(ms(7, 30), z, null, listOf("07:30"), null)
        assertEquals(ms(7, 30, 24), s.first { it.kind == SlotKind.Medicine }.at)
    }

    @Test
    fun signatureChangesWithPlan() {
        val a = WakeMath.signature(WakeMath.plan(ms(9, 0), z, ms(12, 0), listOf("08:00"), null))
        val b = WakeMath.signature(WakeMath.plan(ms(9, 0), z, ms(12, 0), listOf("08:00", "09:30"), null))
        assertFalse(a == b)
        assertEquals(a, WakeMath.signature(WakeMath.plan(ms(9, 0), z, ms(12, 0), listOf("08:00"), null)))
    }

    @Test
    fun volumeIsAtLeastSeventyPercentAndGrows() {
        assertEquals(5, WakeMath.volumeTarget(7, 5, 1))
        assertEquals(5, WakeMath.volumeTarget(7, 0, 1))
        assertEquals(6, WakeMath.volumeTarget(7, 0, 2))
        assertEquals(7, WakeMath.volumeTarget(7, 0, 3))
        assertEquals(7, WakeMath.volumeTarget(7, 7, 1))
    }

    @Test
    fun parsesOpenMeteo() {
        assertEquals(41.3, Weather.parseMax("""{"daily":{"time":["2026-11-23"],"temperature_2m_max":[41.3]}}""")!!, 0.001)
        assertEquals(162, Weather.parseAqi("""{"current":{"time":"x","us_aqi":162}}"""))
        assertNull(Weather.parseMax("not json"))
        assertNull(Weather.parseAqi("""{"current":{}}"""))
    }

    @Test
    fun heatAndAirThresholds() {
        assertNotNull(Weather.noteFor(WeatherReport("d", 40.0, null)))
        assertNull(Weather.noteFor(WeatherReport("d", 39.9, 149)))
        assertNotNull(Weather.noteFor(WeatherReport("d", 30.0, 150)))
    }
}

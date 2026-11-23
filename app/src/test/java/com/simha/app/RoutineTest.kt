package com.simha.app

import com.simha.app.logic.NightState
import com.simha.app.logic.Routine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class RoutineTest {
    private val z = ZoneId.of("Asia/Kolkata")
    private fun ms(d: Int, h: Int, m: Int) = LocalDateTime.of(2026, 11, d, h, m).atZone(z).toInstant().toEpochMilli()

    private fun days(first: Int, last: Int, from: Int, to: Int) = (from..to).flatMap { d -> listOf(ms(d, first, 0), ms(d, 12, 0), ms(d, last, 30)) }

    @Test
    fun learnsMedianWakeAndSleep() {
        val r = Routine.learn(days(6, 22, 10, 16), ms(17, 9, 0), z)
        assertEquals(LocalTime.of(6, 0), r.wake)
        assertEquals(LocalTime.of(22, 30), r.sleep)
        assertEquals(7, r.days)
    }

    @Test
    fun tooFewDaysIsUnknown() {
        val r = Routine.learn(days(6, 22, 14, 16), ms(17, 9, 0), z)
        assertNull(r.wake)
        assertNull(r.expectedActiveBy(LocalDate.of(2026, 11, 17), z))
    }

    @Test
    fun ignoresToday() {
        val ts = days(6, 22, 10, 16) + listOf(ms(17, 3, 59 + 0), ms(17, 5, 0))
        val r = Routine.learn(ts, ms(17, 9, 0), z)
        assertEquals(LocalTime.of(6, 0), r.wake)
    }

    @Test
    fun lateNightActivityBelongsToPreviousDay() {
        val ts = (10..16).flatMap { d -> listOf(ms(d, 7, 0), ms(d + 1, 1, 15)) }
        val r = Routine.learn(ts, ms(18, 9, 0), z)
        assertEquals(LocalTime.of(7, 0), r.wake)
        assertEquals(LocalTime.of(1, 15), r.sleep)
    }

    @Test
    fun outlierDayDoesNotMoveMedian() {
        val ts = days(6, 22, 10, 15) + listOf(ms(16, 11, 0), ms(16, 12, 0))
        assertEquals(LocalTime.of(6, 0), Routine.learn(ts, ms(17, 9, 0), z).wake)
    }

    @Test
    fun expectedActiveByIsWakePlus90() {
        val r = Routine.learn(days(6, 22, 10, 16), ms(17, 9, 0), z)
        assertEquals(ms(17, 7, 30), r.expectedActiveBy(LocalDate.of(2026, 11, 17), z))
    }

    @Test
    fun oldDaysOutsideWindowAreIgnored() {
        val old = (1..3).flatMap { d -> listOf(ms(d, 5, 0)) }
        val r = Routine.learn(old, ms(28, 9, 0), z)
        assertNull(r.wake)
    }

    @Test
    fun nightWindowBounds() {
        assertTrue(Routine.inNightWindow(LocalTime.of(1, 0)))
        assertTrue(Routine.inNightWindow(LocalTime.of(4, 29)))
        assertFalse(Routine.inNightWindow(LocalTime.of(0, 59)))
        assertFalse(Routine.inNightWindow(LocalTime.of(4, 30)))
    }

    @Test
    fun nightWatchFiresOnceAfterFiveMinutesOfUse() {
        var s = NightState()
        var fired = 0
        var t = ms(18, 2, 0)
        repeat(10) {
            val (n, f) = Routine.nightStep(s, "2026-11-18", true, true, t)
            s = n
            if (f) fired++
            t += 60_000L
        }
        assertEquals(1, fired)
        assertTrue(s.fired)
    }

    @Test
    fun nightWatchIgnoresScreenOffAndGaps() {
        var s = NightState()
        var t = ms(18, 2, 0)
        repeat(20) {
            val (n, f) = Routine.nightStep(s, "2026-11-18", true, false, t)
            s = n
            assertFalse(f)
            t += 60_000L
        }
        val (n2, f2) = Routine.nightStep(s, "2026-11-18", true, true, t + 3_600_000L)
        assertFalse(f2)
        assertEquals(0L, n2.ms)
    }

    @Test
    fun nightWatchResetsOnNewNight() {
        val fired = NightState("2026-11-18", 400_000L, true, 0L)
        val (n, f) = Routine.nightStep(fired, "2026-11-19", true, true, ms(19, 2, 0))
        assertFalse(f)
        assertFalse(n.fired)
        assertNotNull(n)
    }
}

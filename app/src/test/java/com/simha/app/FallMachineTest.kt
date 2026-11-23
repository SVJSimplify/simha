package com.simha.app

import com.simha.app.monitor.FallMachine
import com.simha.app.monitor.SafeMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class FallMachineTest {
    private fun run(m: FallMachine, from: Long, to: Long, g: Float, step: Long = 20L): Boolean {
        var t = from
        var hit = false
        while (t <= to) {
            if (m.feed(t, g)) hit = true
            t += step
        }
        return hit
    }

    @Test
    fun freeFallImpactStillnessIsAFall() {
        val m = FallMachine()
        assertFalse(run(m, 0, 300, 0.2f))
        assertFalse(m.feed(400, 3.2f))
        assertTrue(run(m, 420, 11_000, 1.0f))
    }

    @Test
    fun movementAfterImpactCancels() {
        val m = FallMachine()
        run(m, 0, 300, 0.2f)
        m.feed(400, 3.2f)
        run(m, 420, 2000, 1.0f)
        assertFalse(m.feed(2100, 1.8f))
        assertFalse(run(m, 2120, 15_000, 1.0f))
    }

    @Test
    fun impactWithoutFreeFallIsIgnored() {
        val m = FallMachine()
        assertFalse(m.feed(0, 1.0f))
        assertFalse(m.feed(20, 3.5f))
        assertFalse(run(m, 40, 15_000, 1.0f))
    }

    @Test
    fun impactTooLongAfterFreeFallIsIgnored() {
        val m = FallMachine()
        run(m, 0, 200, 0.2f)
        run(m, 220, 1500, 1.0f)
        m.feed(1520, 3.5f)
        assertFalse(run(m, 1540, 15_000, 1.0f))
    }

    @Test
    fun weakImpactIsIgnored() {
        val m = FallMachine()
        run(m, 0, 200, 0.2f)
        m.feed(300, 2.0f)
        assertFalse(run(m, 320, 15_000, 1.0f))
    }

    @Test
    fun sensorGapResets() {
        val m = FallMachine()
        run(m, 0, 200, 0.2f)
        m.feed(300, 3.0f)
        assertFalse(m.feed(20_000, 1.0f))
    }

    @Test
    fun canDetectTwice() {
        val m = FallMachine()
        run(m, 0, 200, 0.2f)
        m.feed(300, 3.0f)
        assertTrue(run(m, 320, 11_000, 1.0f))
        run(m, 20_000, 20_200, 0.2f)
        m.feed(20_300, 3.0f)
        assertTrue(run(m, 20_320, 31_000, 1.0f))
    }

    @Test
    fun distanceOneKm() {
        val d = SafeMath.distanceM(12.9716, 77.5946, 12.9716 + 0.009, 77.5946)
        assertEquals(1000.0, d, 15.0)
    }

    @Test
    fun safeAreaAlertsAtNightOrAfterThreeHours() {
        val z = ZoneId.of("Asia/Kolkata")
        fun ms(h: Int, m: Int) = LocalDateTime.of(2026, 11, 18, h, m).atZone(z).toInstant().toEpochMilli()
        assertTrue(SafeMath.shouldAlert(ms(21, 30), ms(21, 40), z))
        assertTrue(SafeMath.shouldAlert(ms(5, 0), ms(5, 10), z))
        assertFalse(SafeMath.shouldAlert(ms(12, 0), ms(14, 0), z))
        assertTrue(SafeMath.shouldAlert(ms(10, 0), ms(13, 1), z))
    }
}

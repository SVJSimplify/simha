package com.simha.app

import com.simha.app.agenda.Recur
import com.simha.app.agenda.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class AgendaTest {
    private val ist = ZoneId.of("Asia/Kolkata")
    private val ny = ZoneId.of("America/New_York")
    private fun z(y: Int, m: Int, d: Int, h: Int, mi: Int, zone: ZoneId = ist) = ZonedDateTime.of(y, m, d, h, mi, 0, 0, zone)
    private val sat = z(2026, 11, 28, 10, 0)
    private val anchor = z(2026, 11, 28, 0, 0).toInstant().toEpochMilli()

    @Test fun onceFuture() = assertEquals(z(2026, 11, 28, 18, 0), Recur.next(Rule.ONCE, "18:00", "2026-11-28", 0, sat))
    @Test fun onceFutureDay() = assertEquals(z(2026, 12, 1, 7, 30), Recur.next(Rule.ONCE, "07:30", "2026-12-01", 0, sat))
    @Test fun oncePast() = assertNull(Recur.next(Rule.ONCE, "08:00", "2026-11-28", 0, sat))
    @Test fun onceExactlyNowIsNotAfter() = assertNull(Recur.next(Rule.ONCE, "10:00", "2026-11-28", 0, sat))
    @Test fun onceBlankDateMeansToday() = assertEquals(z(2026, 11, 28, 18, 0), Recur.next(Rule.ONCE, "18:00", "", 0, sat))
    @Test fun onceBlankDatePastIsNull() = assertNull(Recur.next(Rule.ONCE, "09:00", "", 0, sat))

    @Test fun dailyBefore() = assertEquals(z(2026, 11, 28, 18, 0), Recur.next(Rule.DAILY, "18:00", "", 0, sat))
    @Test fun dailyAfter() = assertEquals(z(2026, 11, 29, 8, 0), Recur.next(Rule.DAILY, "08:00", "", 0, sat))
    @Test fun dailyEqualGoesTomorrow() = assertEquals(z(2026, 11, 29, 10, 0), Recur.next(Rule.DAILY, "10:00", "", 0, sat))

    @Test fun weekdaysFridayEveningToMonday() =
        assertEquals(z(2026, 11, 30, 8, 0), Recur.next(Rule.WEEKDAYS, "08:00", "", 0, z(2026, 11, 27, 18, 0)))
    @Test fun weekdaysSaturdayToMonday() = assertEquals(z(2026, 11, 30, 8, 0), Recur.next(Rule.WEEKDAYS, "08:00", "", 0, sat))
    @Test fun weekdaysSundayToMonday() =
        assertEquals(z(2026, 11, 30, 8, 0), Recur.next(Rule.WEEKDAYS, "08:00", "", 0, z(2026, 11, 29, 9, 0)))
    @Test fun weekdaysFridayMorningSameDay() =
        assertEquals(z(2026, 11, 27, 8, 0), Recur.next(Rule.WEEKDAYS, "08:00", "", 0, z(2026, 11, 27, 7, 0)))
    @Test fun weekdaysMondayLaterToday() =
        assertEquals(z(2026, 11, 30, 11, 0), Recur.next(Rule.WEEKDAYS, "11:00", "", 0, z(2026, 11, 30, 10, 0)))

    @Test fun daysRuleBuilder() = assertEquals("days:1,3,5", Rule.days(setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)))
    @Test fun daysFromSaturday() = assertEquals(z(2026, 11, 30, 9, 0), Recur.next("days:1,3,5", "09:00", "", 0, sat))
    @Test fun daysFromMondayAfterTime() = assertEquals(z(2026, 12, 2, 9, 0), Recur.next("days:1,3,5", "09:00", "", 0, z(2026, 11, 30, 10, 0)))
    @Test fun daysFromWednesdayAfterTime() = assertEquals(z(2026, 12, 4, 9, 0), Recur.next("days:1,3,5", "09:00", "", 0, z(2026, 12, 2, 10, 0)))
    @Test fun daysFromFridayAfterTimeWrapsToMonday() = assertEquals(z(2026, 12, 7, 9, 0), Recur.next("days:1,3,5", "09:00", "", 0, z(2026, 12, 4, 10, 0)))
    @Test fun daysSundayOnly() = assertEquals(z(2026, 11, 29, 9, 0), Recur.next("days:7", "09:00", "", 0, sat))
    @Test fun daysEmptyIsNull() = assertNull(Recur.next("days:", "09:00", "", 0, sat))
    @Test fun daysOutOfRangeIsNull() = assertNull(Recur.next("days:9", "09:00", "", 0, sat))

    @Test fun hoursBeforeBase() = assertEquals(z(2026, 11, 28, 6, 0), Recur.next("hours:6", "06:00", "", anchor, z(2026, 11, 28, 5, 0)))
    @Test fun hoursNextSlot() = assertEquals(z(2026, 11, 28, 12, 0), Recur.next("hours:6", "06:00", "", anchor, sat))
    @Test fun hoursExactSlotIsStrictlyAfter() = assertEquals(z(2026, 11, 28, 18, 0), Recur.next("hours:6", "06:00", "", anchor, z(2026, 11, 28, 12, 0)))
    @Test fun hoursNextDay() = assertEquals(z(2026, 11, 29, 0, 0), Recur.next("hours:6", "06:00", "", anchor, z(2026, 11, 28, 18, 0)))
    @Test fun hoursInvalidIsNull() {
        assertNull(Recur.next("hours:0", "06:00", "", anchor, sat))
        assertNull(Recur.next("hours:25", "06:00", "", anchor, sat))
        assertNull(Recur.next("hours:x", "06:00", "", anchor, sat))
    }

    @Test fun monthly31InThirtyDayMonth() = assertEquals(z(2026, 11, 30, 9, 0), Recur.next("monthly:31", "09:00", "", 0, z(2026, 11, 10, 10, 0)))
    @Test fun monthly31InFebruary() = assertEquals(z(2027, 2, 28, 9, 0), Recur.next("monthly:31", "09:00", "", 0, z(2027, 2, 10, 10, 0)))
    @Test fun monthly31InLeapFebruary() = assertEquals(z(2028, 2, 29, 9, 0), Recur.next("monthly:31", "09:00", "", 0, z(2028, 2, 10, 10, 0)))
    @Test fun monthly31RollsToNextMonth() = assertEquals(z(2026, 12, 31, 9, 0), Recur.next("monthly:31", "09:00", "", 0, z(2026, 11, 30, 10, 0)))
    @Test fun monthlyPastDayRollsOver() = assertEquals(z(2026, 12, 15, 9, 0), Recur.next("monthly:15", "09:00", "", 0, sat))
    @Test fun monthlyThisMonth() = assertEquals(z(2026, 11, 29, 9, 0), Recur.next("monthly:29", "09:00", "", 0, sat))
    @Test fun monthlyYearRollover() = assertEquals(z(2027, 1, 5, 9, 0), Recur.next("monthly:5", "09:00", "", 0, z(2026, 12, 20, 10, 0)))
    @Test fun monthlyInvalidIsNull() {
        assertNull(Recur.next("monthly:0", "09:00", "", 0, sat))
        assertNull(Recur.next("monthly:32", "09:00", "", 0, sat))
    }

    @Test fun invalidTimeOrRuleIsNull() {
        assertNull(Recur.next(Rule.DAILY, "25:00", "", 0, sat))
        assertNull(Recur.next(Rule.DAILY, "abc", "", 0, sat))
        assertNull(Recur.next("weird", "09:00", "", 0, sat))
    }

    @Test fun dstGapDaily() {
        val after = z(2026, 3, 7, 3, 0, ny)
        val r = Recur.next(Rule.DAILY, "02:30", "", 0, after)
        assertNotNull(r)
        assertTrue(r!!.isAfter(after))
    }

    @Test fun dstGapDailySameDay() {
        val after = z(2026, 3, 8, 0, 30, ny)
        val r = Recur.next(Rule.DAILY, "02:30", "", 0, after)
        assertNotNull(r)
        assertTrue(r!!.isAfter(after))
        assertEquals(8, r.dayOfMonth)
    }

    @Test fun dstGapOnce() {
        val after = z(2026, 3, 7, 12, 0, ny)
        val r = Recur.next(Rule.ONCE, "02:30", "2026-03-08", 0, after)
        assertNotNull(r)
        assertTrue(r!!.isAfter(after))
    }

    @Test fun dstGapWeekdaysAcrossSpringForward() {
        val after = z(2026, 3, 6, 12, 0, ny)
        val r = Recur.next(Rule.WEEKDAYS, "02:30", "", 0, after)
        assertNotNull(r)
        assertEquals(9, r!!.dayOfMonth)
        assertEquals(2, r.hour)
        assertEquals(30, r.minute)
    }

    @Test fun dstGapSundayRule() {
        val after = z(2026, 3, 1, 12, 0, ny)
        val r = Recur.next("days:7", "02:30", "", 0, after)
        assertNotNull(r)
        assertTrue(r!!.isAfter(after))
        assertEquals(8, r.dayOfMonth)
    }

    @Test fun dstGapHours() {
        val a = z(2026, 3, 8, 0, 0, ny).toInstant().toEpochMilli()
        val after = z(2026, 3, 8, 1, 0, ny)
        val r = Recur.next("hours:6", "02:30", "", a, after)
        assertNotNull(r)
        assertTrue(r!!.isAfter(after))
    }

    @Test fun dstFallBackOverlap() {
        val after = z(2026, 10, 31, 12, 0, ny)
        val r = Recur.next(Rule.DAILY, "01:30", "", 0, after)
        assertNotNull(r)
        assertTrue(r!!.isAfter(after))
        assertEquals(1, r.monthValue - 10)
    }
}

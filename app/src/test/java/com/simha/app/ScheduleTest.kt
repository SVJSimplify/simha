package com.simha.app

import com.simha.app.logic.isDuplicateMemory
import com.simha.app.logic.nextOccurrence
import com.simha.app.logic.parseTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ScheduleTest {
    private val z = ZoneId.of("Asia/Kolkata")
    private fun ms(h: Int, m: Int, d: Int = 23) = LocalDateTime.of(2026, 11, d, h, m).atZone(z).toInstant().toEpochMilli()

    @Test
    fun picksNextTimeToday() = assertEquals(ms(14, 0), nextOccurrence(listOf("08:00", "14:00", "20:00"), ms(9, 30), z))

    @Test
    fun wrapsToTomorrow() = assertEquals(ms(8, 0, 24), nextOccurrence(listOf("08:00", "20:00"), ms(21, 0), z))

    @Test
    fun exactNowIsNotNext() = assertEquals(ms(20, 0), nextOccurrence(listOf("08:00", "20:00"), ms(8, 0), z))

    @Test
    fun ignoresInvalidAndEmpty() {
        assertNull(nextOccurrence(emptyList(), ms(9, 0), z))
        assertNull(nextOccurrence(listOf("25:00", "ab", "8:60"), ms(9, 0), z))
        assertEquals(ms(10, 5), nextOccurrence(listOf("xx", "10:05"), ms(9, 0), z))
    }

    @Test
    fun parsesTimes() {
        assertEquals(8, parseTime("8:05")?.hour)
        assertNull(parseTime("24:00"))
    }

    @Test
    fun dedupeIgnoresCaseAndSpaces() {
        assertTrue(isDuplicateMemory(listOf("Likes tea"), "  likes TEA "))
        assertFalse(isDuplicateMemory(listOf("Likes tea"), "Likes coffee"))
        assertFalse(isDuplicateMemory(emptyList(), "x"))
    }
}

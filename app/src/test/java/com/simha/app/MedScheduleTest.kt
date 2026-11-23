package com.simha.app

import com.simha.app.data.DoseLog
import com.simha.app.data.Medicine
import com.simha.app.ui.meds.nextDose
import com.simha.app.ui.meds.todayDoses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class MedScheduleTest {
    private val z = ZoneId.of("Asia/Kolkata")
    private fun ms(h: Int, m: Int) = LocalDateTime.of(2026, 11, 23, h, m).atZone(z).toInstant().toEpochMilli()
    private val med = Medicine("a", "Metformin", "500", listOf("08:00", "20:00"))

    @Test
    fun dueSortedAndMatched() {
        val log = DoseLog("l", "a", ms(8, 0), "taken", 0)
        val d = todayDoses(listOf(med), listOf(log), ms(9, 0), z)
        assertEquals(listOf("taken", null), d.map { it.status })
        assertEquals(ms(20, 0), nextDose(d, ms(9, 0))?.at)
    }

    @Test
    fun inactiveIgnoredAndNoneLeft() {
        assertEquals(0, todayDoses(listOf(med.copy(active = false)), emptyList(), ms(9, 0), z).size)
        val all = listOf(DoseLog("1", "a", ms(8, 0), "taken", 0), DoseLog("2", "a", ms(20, 0), "skipped", 0))
        assertNull(nextDose(todayDoses(listOf(med), all, ms(9, 0), z), ms(9, 0)))
    }
}

package com.simha.app.wake

import com.simha.app.logic.nextOccurrence
import com.simha.app.logic.parseTime
import java.time.LocalTime
import java.time.ZoneId

enum class SlotKind { Checkin, Morning, Evening, Heat, Medicine }

data class Slot(val code: Int, val kind: SlotKind, val at: Long, val tag: String = "")

object WakeMath {
    const val CODE_CHECKIN = 1
    const val CODE_MORNING = 2
    const val CODE_EVENING = 3
    const val CODE_HEAT = 4
    const val CODE_RETRY = 5
    const val CODE_MED = 1000
    const val MIN_LEAD_MS = 30_000L
    const val FIRST_RETRY_MS = 90_000L
    const val RETRY_MS = 60_000L
    const val MAX_ATTEMPTS = 3
    val MORNING: LocalTime = LocalTime.of(7, 30)
    val EVENING: LocalTime = LocalTime.of(19, 30)
    val HEAT: LocalTime = LocalTime.of(12, 30)

    fun hhmm(t: LocalTime): String = t.hour.toString().padStart(2, '0') + ":" + t.minute.toString().padStart(2, '0')

    fun checkinAt(last: Long, lastPrompt: Long, inactivityMin: Int, waitMin: Int, escalatedFor: Long, now: Long): Long? {
        val awaiting = lastPrompt > last
        if (awaiting && escalatedFor == lastPrompt) return null
        val t = if (awaiting) lastPrompt + waitMin.coerceAtLeast(1) * 60_000L else last + inactivityMin.coerceAtLeast(1) * 60_000L
        return maxOf(t, now + MIN_LEAD_MS)
    }

    fun plan(now: Long, zone: ZoneId, checkinAt: Long?, medTimes: Collection<String>, morning: LocalTime?, evening: LocalTime = EVENING): List<Slot> {
        val out = ArrayList<Slot>()
        if (checkinAt != null) out.add(Slot(CODE_CHECKIN, SlotKind.Checkin, checkinAt))
        fun daily(code: Int, kind: SlotKind, t: LocalTime) {
            nextOccurrence(listOf(hhmm(t)), now, zone)?.let { out.add(Slot(code, kind, it)) }
        }
        daily(CODE_MORNING, SlotKind.Morning, morning ?: MORNING)
        daily(CODE_EVENING, SlotKind.Evening, evening)
        daily(CODE_HEAT, SlotKind.Heat, HEAT)
        medTimes.mapNotNull { parseTime(it) }.distinct().sorted().forEach { t ->
            nextOccurrence(listOf(hhmm(t)), now, zone)?.let { out.add(Slot(CODE_MED + t.hour * 60 + t.minute, SlotKind.Medicine, it, hhmm(t))) }
        }
        return out
    }

    fun signature(slots: List<Slot>): String = slots.joinToString("|") { it.code.toString() + "@" + it.at }

    fun volumePct(attempt: Int): Int = when {
        attempt <= 1 -> 70
        attempt == 2 -> 85
        else -> 100
    }

    fun volumeTarget(max: Int, cur: Int, attempt: Int): Int {
        if (max <= 0) return cur
        val want = Math.ceil(max * volumePct(attempt) / 100.0).toInt()
        return maxOf(cur, want).coerceAtMost(max)
    }
}

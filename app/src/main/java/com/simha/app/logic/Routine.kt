package com.simha.app.logic

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class NightState(val day: String = "", val ms: Long = 0L, val fired: Boolean = false, val lastTick: Long = 0L)

class Routine(val wake: LocalTime?, val sleep: LocalTime?, val days: Int) {
    fun expectedActiveBy(day: LocalDate, zone: ZoneId): Long? =
        wake?.let { day.atTime(it).plusMinutes(GRACE_MIN).atZone(zone).toInstant().toEpochMilli() }

    companion object {
        const val WINDOW_DAYS = 14L
        const val MIN_DAYS = 4
        const val GRACE_MIN = 90L
        const val NIGHT_LIMIT_MS = 5L * 60_000L
        private const val DAY_START_MIN = 240
        private const val DAY_MIN = 1440

        private fun local(t: Long, zone: ZoneId) = Instant.ofEpochMilli(t).atZone(zone)

        private fun activityDay(t: Long, zone: ZoneId): LocalDate = local(t, zone).minusHours(4).toLocalDate()

        private fun offset(t: Long, zone: ZoneId): Int {
            val lt = local(t, zone).toLocalTime()
            return (lt.hour * 60 + lt.minute - DAY_START_MIN + DAY_MIN) % DAY_MIN
        }

        private fun fromOffset(m: Int): LocalTime {
            val x = (m + DAY_START_MIN) % DAY_MIN
            return LocalTime.of(x / 60, x % 60)
        }

        private fun median(v: List<Int>): Int = v.sorted()[v.size / 2]

        fun dayStart(day: LocalDate, zone: ZoneId): Long = day.atTime(4, 0).atZone(zone).toInstant().toEpochMilli()

        fun inNightWindow(t: LocalTime): Boolean = !t.isBefore(LocalTime.of(1, 0)) && t.isBefore(LocalTime.of(4, 30))

        fun learn(timestamps: List<Long>, now: Long, zone: ZoneId): Routine {
            val today = activityDay(now, zone)
            val byDay = timestamps.filter { it <= now }
                .groupBy { activityDay(it, zone) }
                .filterKeys { it < today && !it.isBefore(today.minusDays(WINDOW_DAYS)) }
            if (byDay.size < MIN_DAYS) return Routine(null, null, byDay.size)
            val firsts = byDay.values.map { v -> v.minOf { offset(it, zone) } }
            val lasts = byDay.values.map { v -> v.maxOf { offset(it, zone) } }
            return Routine(fromOffset(median(firsts)), fromOffset(median(lasts)), byDay.size)
        }

        fun nightStep(s: NightState, day: String, inWindow: Boolean, interactive: Boolean, now: Long): Pair<NightState, Boolean> {
            var st = if (s.day == day) s else NightState(day = day)
            if (!inWindow) return st.copy(lastTick = 0L) to false
            val dt = if (st.lastTick > 0L && now - st.lastTick in 1..180_000L) now - st.lastTick else 0L
            st = st.copy(lastTick = now, ms = if (interactive) st.ms + dt else st.ms)
            if (!st.fired && st.ms > NIGHT_LIMIT_MS) return st.copy(fired = true) to true
            return st to false
        }
    }
}

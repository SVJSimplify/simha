package com.simha.app.ui.home

import com.simha.app.data.Diary
import com.simha.app.data.EventRow
import com.simha.app.logic.Sig
import com.simha.app.logic.Signal
import com.simha.app.logic.TimeUtil
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

data class DayStats(val mood: String?, val fine: Int, val missed: Int, val taken: Int, val dosesMissed: Int, val sig: Sig?)

data class WeekStats(val dosePct: Int?, val missedCheckins: Int, val avgMood: String?, val notes: List<String>, val falls: Int = 0, val pain: List<String> = emptyList())

private fun dayOf(ms: Long, z: ZoneId): LocalDate = Instant.ofEpochMilli(ms).atZone(z).toLocalDate()

fun dayStats(events: List<EventRow>, diary: Diary?, today: LocalDate, z: ZoneId): DayStats {
    val ev = events.mapNotNull { e -> TimeUtil.isoMillis(e.createdAt)?.let { e.type to dayOf(it, z) } }.filter { it.second == today }
    val s = diary?.sig
    return DayStats(
        s?.mood?.toString(),
        ev.count { it.first == "checkin_ok" },
        ev.count { it.first == "checkin_missed" },
        s?.taken ?: 0,
        ((s?.due ?: 0) - (s?.taken ?: 0)).coerceAtLeast(0),
        s,
    )
}

fun weekStats(diaries: List<Diary>, today: LocalDate): WeekStats {
    val from = today.minusDays(6)
    val week = diaries.filter { it.day in from..today }.sortedBy { it.day }
    val r = Signal.parseRollup(Signal.rollup(week.map { it.sig }))
    val notes = week.mapNotNull { d -> d.sig.note?.let { d.day.toString() + ": " + it } }
    return WeekStats(r.mdPct, r.ciMiss, r.mood?.let { String.format(Locale.US, "%.1f", it) }, notes, r.falls, r.pain)
}

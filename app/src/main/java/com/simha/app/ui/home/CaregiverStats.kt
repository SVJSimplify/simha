package com.simha.app.ui.home

import com.simha.app.data.Diary
import com.simha.app.data.DoseLog
import com.simha.app.data.EventRow
import com.simha.app.logic.TimeUtil
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

data class DayStats(val mood: String?, val fine: Int, val missed: Int, val taken: Int, val dosesMissed: Int, val summary: String)

data class WeekStats(val dosePct: Int?, val missedCheckins: Int, val avgMood: String?, val notes: List<String>)

private fun Diary.text(key: String): String? = (data[key] as? JsonPrimitive)?.content?.trim()?.takeIf { it.isNotEmpty() }

private fun dayOf(ms: Long, z: ZoneId): LocalDate = Instant.ofEpochMilli(ms).atZone(z).toLocalDate()

fun dayStats(events: List<EventRow>, doses: List<DoseLog>, diary: Diary?, today: LocalDate, z: ZoneId): DayStats {
    val ev = events.mapNotNull { e -> TimeUtil.isoMillis(e.createdAt)?.let { e.type to dayOf(it, z) } }.filter { it.second == today }
    val ds = doses.filter { dayOf(it.scheduledAt, z) == today }
    return DayStats(
        diary?.text("mood"),
        ev.count { it.first == "checkin_ok" },
        ev.count { it.first == "checkin_missed" },
        ds.count { it.status == "taken" },
        ds.count { it.status == "missed" },
        diary?.summary.orEmpty(),
    )
}

fun weekStats(events: List<EventRow>, doses: List<DoseLog>, diaries: List<Diary>, today: LocalDate, z: ZoneId): WeekStats {
    val from = today.minusDays(6)
    val ds = doses.filter { dayOf(it.scheduledAt, z) in from..today }
    val pct = if (ds.isEmpty()) null else ds.count { it.status == "taken" } * 100 / ds.size
    val missed = events.count { e ->
        e.type == "checkin_missed" && TimeUtil.isoMillis(e.createdAt)?.let { dayOf(it, z) in from..today } == true
    }
    val moods = diaries.sortedBy { it.day }.mapNotNull { it.text("mood") }
    val nums = moods.mapNotNull { it.toDoubleOrNull() }
    val avg = when {
        nums.isNotEmpty() -> String.format(Locale.US, "%.1f", nums.average())
        else -> moods.lastOrNull()
    }
    val notes = diaries.sortedBy { it.day }.mapNotNull { d ->
        (d.text("activity") ?: d.text("walking"))?.let { d.day.toString() + ": " + it }
    }
    return WeekStats(pct, missed, avg, notes)
}

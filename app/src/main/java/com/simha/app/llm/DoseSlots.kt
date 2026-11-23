package com.simha.app.llm

import com.simha.app.data.DoseLog
import com.simha.app.data.Medicine
import com.simha.app.logic.parseTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

object DoseSlots {
    private fun day(ms: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    fun slotsToday(times: List<String>, now: Long, zone: ZoneId): List<Pair<String, Long>> {
        val d = day(now, zone)
        return times.mapNotNull { s -> parseTime(s)?.let { t -> t.toString() to d.atTime(t).atZone(zone).toInstant().toEpochMilli() } }
            .distinctBy { it.second }
            .sortedBy { it.second }
    }

    private fun assigned(slots: List<Pair<String, Long>>, logs: List<DoseLog>, now: Long, zone: ZoneId): Map<Int, DoseLog> {
        val out = HashMap<Int, DoseLog>()
        if (slots.isEmpty()) return out
        val today = day(now, zone)
        logs.filter { day(it.scheduledAt, zone) == today }.sortedBy { it.createdAt }.forEach { l ->
            val i = slots.indices.minByOrNull { abs(slots[it].second - l.scheduledAt) }!!
            out[i] = l
        }
        return out
    }

    fun pick(times: List<String>, logs: List<DoseLog>, now: Long, zone: ZoneId): Long {
        val slots = slotsToday(times, now, zone)
        val used = assigned(slots, logs, now, zone)
        val free = slots.indices.filter { it !in used }
        val i = free.minByOrNull { abs(slots[it].second - now) } ?: return now
        return slots[i].second
    }

    fun status(m: Medicine, logs: List<DoseLog>, now: Long, zone: ZoneId): String {
        val mine = logs.filter { it.medicineId == m.id }
        val slots = slotsToday(m.times, now, zone)
        if (slots.isEmpty()) {
            val today = day(now, zone)
            val n = mine.count { day(it.scheduledAt, zone) == today && it.status == "taken" }
            return if (n > 0) "taken $n time(s) today" else "no fixed times, nothing logged today"
        }
        val used = assigned(slots, mine, now, zone)
        return slots.mapIndexed { i, (label, at) ->
            val l = used[i]
            when {
                l != null -> "$label ${l.status}"
                at > now -> "$label not yet due"
                else -> "$label due, not logged yet"
            }
        }.joinToString(", ")
    }
}

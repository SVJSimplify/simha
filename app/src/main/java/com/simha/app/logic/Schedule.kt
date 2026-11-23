package com.simha.app.logic

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

fun parseTime(s: String): LocalTime? {
    val m = Regex("^(\\d{1,2}):(\\d{2})$").matchEntire(s.trim()) ?: return null
    val h = m.groupValues[1].toInt()
    val min = m.groupValues[2].toInt()
    return if (h in 0..23 && min in 0..59) LocalTime.of(h, min) else null
}

fun nextOccurrence(times: List<String>, nowMillis: Long, zone: ZoneId): Long? {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    return times.mapNotNull { parseTime(it) }
        .flatMap { t -> listOf(today, today.plusDays(1)).map { it.atTime(t).atZone(zone).toInstant().toEpochMilli() } }
        .filter { it > nowMillis }
        .minOrNull()
}

fun isDuplicateMemory(existing: List<String>, text: String): Boolean {
    val t = text.trim()
    return existing.any { it.trim().equals(t, ignoreCase = true) }
}

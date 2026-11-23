package com.simha.app.ui.meds

import com.simha.app.data.DoseLog
import com.simha.app.data.Medicine
import com.simha.app.logic.parseTime
import java.time.ZoneId

data class DueDose(val medicine: Medicine, val at: Long, val status: String?)

fun todayDoses(meds: List<Medicine>, doses: List<DoseLog>, nowMillis: Long, zone: ZoneId): List<DueDose> {
    val day = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    return meds.filter { it.active }.flatMap { m ->
        m.times.mapNotNull { parseTime(it) }.distinct().map { t ->
            val at = day.atTime(t).atZone(zone).toInstant().toEpochMilli()
            val log = doses.firstOrNull { it.medicineId == m.id && kotlin.math.abs(it.scheduledAt - at) < 60_000L }
            DueDose(m, at, log?.status)
        }
    }.sortedBy { it.at }
}

fun nextDose(due: List<DueDose>, nowMillis: Long): DueDose? =
    due.firstOrNull { it.status == null && it.at >= nowMillis } ?: due.firstOrNull { it.status == null }


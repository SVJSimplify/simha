package com.simha.app.agenda

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Entity(tableName = "agenda")
data class AgendaItem(
    @PrimaryKey val id: String,
    val kind: String,
    val what: String,
    val time: String,
    val rule: String,
    val date: String,
    val nextAt: Long,
    val active: Boolean,
    val createdAt: Long,
)

@Dao
interface AgendaDao {
    @Query("SELECT * FROM agenda WHERE active = 1 ORDER BY nextAt")
    suspend fun active(): List<AgendaItem>

    @Query("SELECT * FROM agenda WHERE id = :id")
    suspend fun get(id: String): AgendaItem?

    @Upsert
    suspend fun put(a: AgendaItem)

    @Query("DELETE FROM agenda WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM agenda WHERE active = 0 AND createdAt < :before")
    suspend fun purge(before: Long)
}

object Rule {
    const val ONCE = "once"
    const val DAILY = "daily"
    const val WEEKDAYS = "weekdays"

    fun days(d: Collection<DayOfWeek>) = "days:" + d.map { it.value }.distinct().sorted().joinToString(",")
    fun hours(n: Int) = "hours:$n"
    fun monthly(day: Int) = "monthly:$day"
}

object Recur {
    fun next(rule: String, hhmm: String, date: String, anchor: Long, after: ZonedDateTime): ZonedDateTime? {
        val t = runCatching { LocalTime.parse(hhmm) }.getOrNull() ?: return null
        val zone = after.zone
        val today = after.toLocalDate()
        fun at(d: LocalDate): ZonedDateTime = d.atTime(t).atZone(zone)
        return when {
            rule == Rule.ONCE -> {
                val d = runCatching { LocalDate.parse(date) }.getOrNull() ?: today
                at(d).takeIf { it.isAfter(after) }
            }
            rule == Rule.DAILY -> at(today).let { if (it.isAfter(after)) it else at(today.plusDays(1)) }
            rule == Rule.WEEKDAYS -> firstAfter(today, t, zone, after) { d -> d.dayOfWeek.value <= 5 }
            rule.startsWith("days:") -> {
                val set = rule.removePrefix("days:").split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 }.toSet()
                if (set.isEmpty()) null else firstAfter(today, t, zone, after) { d -> d.dayOfWeek.value in set }
            }
            rule.startsWith("hours:") -> {
                val n = rule.removePrefix("hours:").toIntOrNull()?.takeIf { it in 1..24 } ?: return null
                val base = Instant.ofEpochMilli(anchor).atZone(zone).toLocalDate().atTime(t).atZone(zone)
                if (base.isAfter(after)) base else base.plusHours((ChronoUnit.MINUTES.between(base, after) / (n * 60L) + 1) * n)
            }
            rule.startsWith("monthly:") -> {
                val dom = rule.removePrefix("monthly:").toIntOrNull()?.takeIf { it in 1..31 } ?: return null
                var m = today.withDayOfMonth(1)
                repeat(3) {
                    val c = at(m.withDayOfMonth(minOf(dom, m.lengthOfMonth())))
                    if (c.isAfter(after)) return c
                    m = m.plusMonths(1)
                }
                null
            }
            else -> null
        }
    }

    private fun firstAfter(from: LocalDate, t: LocalTime, zone: ZoneId, after: ZonedDateTime, ok: (LocalDate) -> Boolean): ZonedDateTime? =
        (0..8).map { from.plusDays(it.toLong()) }.filter(ok).map { it.atTime(t).atZone(zone) }.firstOrNull { it.isAfter(after) }
}

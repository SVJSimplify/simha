package com.simha.app.data

import com.simha.app.logic.Fact
import com.simha.app.logic.Facts
import com.simha.app.logic.Sig
import com.simha.app.logic.Signal
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

class GuardianRepo(
    private val dao: GuardianDao,
    private val repo: Repo,
    private val remote: Remote,
) {
    private val gate = Mutex()
    private var cleaned: LocalDate? = null

    private fun MedicineEntity.toModel() = Medicine(id, name, dose, times.split(",").filter { it.isNotBlank() }, stock, active)

    private fun DailySignalEntity.toDiary() = Diary(LocalDate.parse(day), Signal.decode(t), updatedAt)

    private fun DailySignalRow.toDiary() = Diary(
        LocalDate.parse(day), Signal.decode(t),
        if (updatedAt.isBlank()) 0L else OffsetDateTime.parse(updatedAt).toInstant().toEpochMilli(),
    )

    private fun detail(s: String): List<DoseLog> = s.split(';').mapNotNull { e ->
        val p = e.split('|')
        if (p.size != 5) null else DoseLog(p[0], p[1].ifEmpty { null }, p[2].toLongOrNull() ?: return@mapNotNull null, p[3], p[4].toLongOrNull() ?: 0L)
    }

    private fun detail(l: List<DoseLog>): String = l.joinToString(";") { it.id + "|" + (it.medicineId ?: "") + "|" + it.scheduledAt + "|" + it.status + "|" + it.createdAt }

    private fun zone() = ZoneId.systemDefault()

    fun medicines(): Flow<List<Medicine>> = dao.medicines().map { l -> l.map { it.toModel() } }

    suspend fun saveMedicine(m: Medicine) {
        require(m.name.isNotBlank()) { "Medicine name is required" }
        val times = m.times.map { it.trim() }.filter { it.isNotEmpty() }
        dao.upsertMedicine(MedicineEntity(m.id, m.name.trim(), m.dose.trim(), times.joinToString(","), m.stock, m.active, System.currentTimeMillis()))
        repo.enqueue(
            "upsert", "medicines",
            jo("id" to m.id, "name" to m.name.trim(), "dose" to m.dose.trim(), "times" to JsonArray(times.map { JsonPrimitive(it) }), "stock" to m.stock, "active" to m.active),
        )
        repo.audit("medicine_saved", jo("id" to m.id))
    }

    suspend fun deleteMedicine(id: String) {
        dao.deleteMedicine(id)
        repo.enqueue("delete", "medicines", jo("id" to id))
        repo.audit("medicine_deleted", jo("id" to id))
    }

    private suspend fun edit(day: LocalDate, f: (Sig, List<DoseLog>) -> Pair<Sig, List<DoseLog>>) {
        val added = gate.withLock {
            val key = day.toString()
            val row = dao.signal(key)
            val old = row?.let { Signal.decode(it.t) } ?: Sig()
            val (sig, dl) = f(old, row?.let { detail(it.doses) } ?: emptyList())
            dao.upsertSignal(DailySignalEntity(key, Signal.encode(sig), detail(dl), false, System.currentTimeMillis()))
            sig.exc - old.exc
        }
        if (added.isNotEmpty()) {
            added.forEach { repo.audit("signal_exception", jo("code" to it)) }
            pushDay(day)
        }
    }

    private suspend fun pushDay(day: LocalDate) {
        val r = dao.signal(day.toString()) ?: return
        repo.enqueue("upsert", "daily_signal", jo("day" to r.day, "t" to r.t, "updated_at" to Instant.ofEpochMilli(r.updatedAt).toString()))
        dao.markSynced(r.day, r.updatedAt)
    }

    suspend fun logDose(medicineId: String?, scheduledAt: Long, status: String) {
        require(status in DOSE_STATUS) { "Invalid dose status $status" }
        val now = System.currentTimeMillis()
        val day = Instant.ofEpochMilli(scheduledAt).atZone(zone()).toLocalDate()
        val due = dao.medicinesNow().filter { it.active }.sumOf { m -> maxOf(m.times.split(",").count { it.isNotBlank() }, 1) }
        edit(day) { s, d ->
            val l = d.filter { !(it.medicineId == medicineId && it.scheduledAt == scheduledAt) } + DoseLog("$medicineId@$scheduledAt", medicineId, scheduledAt, status, now)
            s.copy(
                taken = l.count { it.status == "taken" },
                due = maxOf(due, l.size),
                exc = if (status == "missed") s.exc + "missed_dose" else s.exc,
            ) to l
        }
        repo.audit("dose_logged", jo("status" to status, "medicine_id" to medicineId))
    }

    fun doses(sinceMillis: Long): Flow<List<DoseLog>> =
        dao.signalsFlow(Instant.ofEpochMilli(sinceMillis).atZone(zone()).toLocalDate().toString()).map { l ->
            l.flatMap { detail(it.doses) }.filter { it.scheduledAt >= sinceMillis }.sortedByDescending { it.scheduledAt }
        }

    suspend fun diaryFor(day: LocalDate): Diary? = dao.signal(day.toString())?.toDiary()

    suspend fun signalLine(day: LocalDate): String = dao.signal(day.toString())?.t.orEmpty()

    suspend fun upsertDiary(day: LocalDate, update: Sig) {
        edit(day) { s, d -> Signal.merge(s, update) to d }
        repo.audit("diary_saved", jo("day" to day.toString()))
    }

    suspend fun addException(code: String) {
        require(code in Signal.EXC) { "Invalid exception $code" }
        edit(LocalDate.now()) { s, d -> s.copy(exc = s.exc + code) to d }
    }

    suspend fun onEvent(type: String) {
        val ok = type == "checkin_ok"
        val miss = type == "checkin_missed"
        val code = when (type) {
            "fall_suspected" -> "fall"
            "help" -> "redflag"
            "alert" -> "esc"
            "checkin_missed" -> "missed_ci"
            else -> null
        }
        if (!ok && code == null) return
        edit(LocalDate.now()) { s, d ->
            s.copy(
                ciOk = if (ok) (s.ciOk ?: 0) + 1 else s.ciOk,
                ciMiss = if (miss) (s.ciMiss ?: 0) + 1 else s.ciMiss,
                exc = if (code != null) s.exc + code else s.exc,
            ) to d
        }
    }

    suspend fun recentDiary(days: Int): List<Diary> =
        dao.signalsSince(LocalDate.now().minusDays(days.coerceAtLeast(1).toLong() - 1).toString()).map { it.toDiary() }

    suspend fun addMemory(kind: String, text: String): Boolean {
        require(kind in MEMORY_KINDS) { "Invalid memory kind $kind" }
        val t = text.trim()
        require(t.isNotEmpty()) { "Memory text is empty" }
        val k = Facts.keyFor(kind, t)
        val now = System.currentTimeMillis()
        val r = gate.withLock {
            val res = Facts.upsert(dao.facts().map { Fact(it.k, it.v, it.weight, it.updatedAt) }, k, Facts.valueFor(t), Facts.weightFor(kind, k), now)
            if (res.changed) {
                res.facts.firstOrNull { it.k == k }?.let { dao.upsertFact(FactEntity(it.k, it.v, it.w, it.at)) }
                res.evicted.forEach { dao.deleteFact(it) }
            }
            res
        }
        if (!r.changed) return false
        r.facts.firstOrNull { it.k == k }?.let {
            repo.enqueue("upsert", "facts", jo("k" to it.k, "v" to it.v, "weight" to it.w, "updated_at" to Instant.ofEpochMilli(it.at).toString()))
        }
        r.evicted.forEach { repo.enqueue("delete", "facts", jo("k" to it)) }
        repo.audit("memory_added", jo("kind" to kind))
        return true
    }

    suspend fun memories(limit: Int): List<Memory> =
        dao.facts().take(limit).map { Memory(it.k, it.k.substringBefore(':'), if (it.k.startsWith("asked:")) it.k else it.v, it.updatedAt) }

    suspend fun maintain() {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        dao.unsynced().forEach { r ->
            val d = LocalDate.parse(r.day)
            if (d < today || now.hour >= EVENING_HOUR) pushDay(d)
        }
        val monday = today.with(DayOfWeek.MONDAY).minusWeeks(1)
        val wk = Signal.weekKey(monday)
        if (dao.weekly(wk) == null) {
            val lines = dao.signalsBetween(monday.toString(), monday.plusDays(6).toString()).map { Signal.decode(it.t) }
            if (lines.isNotEmpty()) {
                val t = Signal.rollup(lines)
                dao.upsertWeekly(WeeklySignalEntity(wk, t))
                repo.enqueue("upsert", "weekly_signal", jo("week" to wk, "t" to t))
            }
        }
        val keepDay = today.minusDays(DAILY_KEEP - 1L).toString()
        val keepWeek = Signal.weekKey(today.minusWeeks(WEEKLY_KEEP - 1L))
        dao.clearDosesBefore(today.toString())
        val n = dao.deleteSignalsBefore(keepDay) + dao.deleteWeeklyBefore(keepWeek)
        if (n > 0 || cleaned != today) {
            cleaned = today
            val uid = runCatching { remote.uid() }.getOrNull() ?: return
            runCatching { remote.sb.from("daily_signal").delete { filter { eq("user_id", uid); lt("day", keepDay) } } }
            runCatching { remote.sb.from("weekly_signal").delete { filter { eq("user_id", uid); lt("week", keepWeek) } } }
            val cut = Instant.now().minus(AUDIT_DAYS, ChronoUnit.DAYS).toString()
            runCatching {
                remote.sb.from("audit_log").delete {
                    filter {
                        eq("user_id", uid)
                        lt("created_at", cut)
                        KEEP_AUDIT.forEach { neq("action", it) }
                    }
                }
            }
        }
    }

    suspend fun fetchFamilyMessages(): Result<List<FamilyMessage>> = safe {
        val uid = remote.uid()
        remote.sb.from("family_messages").select {
            filter { eq("user_id", uid) }
            order("created_at", Order.DESCENDING)
            limit(50)
        }.decodeList<FamilyMessage>()
    }

    suspend fun downloadFamilyMessage(ref: String): Result<ByteArray> = safe {
        remote.uid()
        remote.sb.storage.from("family_messages").downloadAuthenticated(ref)
    }

    suspend fun markPlayed(id: String): Result<Unit> = safe {
        remote.uid()
        remote.sb.from("family_messages").update({ set("played", true) }) { filter { eq("id", id) } }
        repo.audit("family_message_played", jo("id" to id))
    }

    suspend fun sendFamilyMessage(personUserId: String, fromName: String, audio: ByteArray): Result<Unit> = safe {
        val uid = remote.uid()
        val path = personUserId + "/" + UUID.randomUUID() + ".m4a"
        remote.sb.storage.from("family_messages").upload(path, audio)
        remote.sb.from("family_messages").insert(jo("user_id" to personUserId, "from_id" to uid, "from_name" to fromName, "storage_ref" to path))
        repo.audit("family_message_sent", jo("person" to personUserId))
    }

    suspend fun diaryFor(personUserId: String, days: Int): Result<List<Diary>> = safe {
        remote.uid()
        remote.sb.from("daily_signal").select {
            filter {
                eq("user_id", personUserId)
                gte("day", LocalDate.now().minusDays(days.coerceAtLeast(1).toLong() - 1).toString())
            }
            order("day", Order.DESCENDING)
        }.decodeList<DailySignalRow>().map { it.toDiary() }
    }

    suspend fun weeklyFor(personUserId: String): Result<List<Weekly>> = safe {
        remote.uid()
        remote.sb.from("weekly_signal").select {
            filter { eq("user_id", personUserId) }
            order("week", Order.DESCENDING)
            limit(WEEKLY_KEEP.toLong())
        }.decodeList<WeeklySignalRow>().map { Weekly(it.week, Signal.parseRollup(it.t)) }
    }

    suspend fun sentFamilyMessages(personUserId: String): Result<List<FamilyMessage>> = safe {
        val uid = remote.uid()
        remote.sb.from("family_messages").select {
            filter {
                eq("user_id", personUserId)
                eq("from_id", uid)
            }
            order("created_at", Order.DESCENDING)
            limit(50)
        }.decodeList<FamilyMessage>()
    }

    companion object {
        val DOSE_STATUS = setOf("taken", "missed", "skipped")
        val MEMORY_KINDS = setOf("fact", "person", "event")
        const val DAILY_KEEP = 14
        const val WEEKLY_KEEP = 8
        const val EVENING_HOUR = 20
        const val AUDIT_DAYS = 90L
        val KEEP_AUDIT = listOf("signal_exception", "emergency_dialer", "call_started", "call_failed", "dialer_opened")
    }
}

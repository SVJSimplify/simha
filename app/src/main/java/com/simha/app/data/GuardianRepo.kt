package com.simha.app.data

import com.simha.app.logic.isDuplicateMemory
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class GuardianRepo(
    private val dao: GuardianDao,
    private val repo: Repo,
    private val remote: Remote,
) {
    private fun MedicineEntity.toModel() = Medicine(id, name, dose, times.split(",").filter { it.isNotBlank() }, stock, active)

    private fun DoseLogEntity.toModel() = DoseLog(id, medicineId, scheduledAt, status, createdAt)

    private fun DoseRow.toModel() = DoseLog(
        id, medicineId, OffsetDateTime.parse(scheduledAt).toInstant().toEpochMilli(), status,
        if (createdAt.isBlank()) 0L else OffsetDateTime.parse(createdAt).toInstant().toEpochMilli(),
    )

    private fun DiaryEntity.toModel() = Diary(LocalDate.parse(day), Json.parseToJsonElement(data).jsonObject, summary, updatedAt)

    private fun DiaryRow.toModel() = Diary(
        LocalDate.parse(day), data, summary,
        if (updatedAt.isBlank()) 0L else OffsetDateTime.parse(updatedAt).toInstant().toEpochMilli(),
    )

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

    suspend fun logDose(medicineId: String?, scheduledAt: Long, status: String) {
        require(status in DOSE_STATUS) { "Invalid dose status $status" }
        val d = DoseLogEntity(UUID.randomUUID().toString(), medicineId, scheduledAt, status, System.currentTimeMillis())
        dao.insertDose(d)
        repo.enqueue(
            "insert", "dose_log",
            jo("id" to d.id, "medicine_id" to medicineId, "scheduled_at" to Instant.ofEpochMilli(scheduledAt).toString(), "status" to status),
        )
        repo.audit("dose_logged", jo("status" to status, "medicine_id" to medicineId))
    }

    fun doses(sinceMillis: Long): Flow<List<DoseLog>> = dao.doses(sinceMillis).map { l -> l.map { it.toModel() } }

    suspend fun diaryFor(day: LocalDate): Diary? = dao.diary(day.toString())?.toModel()

    suspend fun upsertDiary(day: LocalDate, data: JsonObject, summary: String) {
        val now = System.currentTimeMillis()
        dao.upsertDiary(DiaryEntity(day.toString(), data.toString(), summary, now))
        repo.enqueue("upsert", "diary", jo("day" to day.toString(), "data" to data, "summary" to summary, "updated_at" to Instant.ofEpochMilli(now).toString()))
        repo.audit("diary_saved", jo("day" to day.toString()))
    }

    suspend fun recentDiary(days: Int): List<Diary> =
        dao.diarySince(LocalDate.now().minusDays(days.coerceAtLeast(1).toLong() - 1).toString()).map { it.toModel() }

    suspend fun addMemory(kind: String, text: String): Boolean {
        require(kind in MEMORY_KINDS) { "Invalid memory kind $kind" }
        val t = text.trim()
        require(t.isNotEmpty()) { "Memory text is empty" }
        if (isDuplicateMemory(dao.memoryTexts(), t)) return false
        val m = MemoryEntity(UUID.randomUUID().toString(), kind, t, System.currentTimeMillis())
        dao.insertMemory(m)
        repo.enqueue("insert", "memories", jo("id" to m.id, "kind" to kind, "text" to t))
        repo.audit("memory_added", jo("kind" to kind))
        return true
    }

    suspend fun memories(limit: Int): List<Memory> = dao.memories(limit).map { Memory(it.id, it.kind, it.text, it.createdAt) }

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

    suspend fun dosesFor(personUserId: String, sinceMillis: Long): Result<List<DoseLog>> = safe {
        remote.uid()
        remote.sb.from("dose_log").select {
            filter {
                eq("user_id", personUserId)
                gte("scheduled_at", Instant.ofEpochMilli(sinceMillis).toString())
            }
            order("scheduled_at", Order.DESCENDING)
        }.decodeList<DoseRow>().map { it.toModel() }
    }

    suspend fun diaryFor(personUserId: String, days: Int): Result<List<Diary>> = safe {
        remote.uid()
        remote.sb.from("diary").select {
            filter {
                eq("user_id", personUserId)
                gte("day", LocalDate.now().minusDays(days.coerceAtLeast(1).toLong() - 1).toString())
            }
            order("day", Order.DESCENDING)
        }.decodeList<DiaryRow>().map { it.toModel() }
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
        val MEMORY_KINDS = setOf("fact", "person", "event", "summary")
    }
}

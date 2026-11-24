package com.simha.app.data

import android.content.Context
import com.simha.app.Actions
import com.simha.app.monitor.Work
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.time.LocalDate
import java.util.UUID

suspend inline fun <T> safe(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

data class PersonView(
    val name: String,
    val events: List<EventRow>,
    val calls: List<CallRow>,
    val recordings: List<RecordingRow>,
    val canViewRecordings: Boolean,
)

enum class CallResult { Called, Dialer, Failed }

class Repo(
    private val ctx: Context,
    private val dao: SimhaDao,
    val prefs: Prefs,
    val remote: Remote,
) {
    val contacts: Flow<List<ContactEntity>> = dao.contacts()
    val profile: Flow<ProfileEntity> = dao.profile().map { it ?: ProfileEntity() }
    val syncError = MutableStateFlow<String?>(null)
    private val lock = Mutex()

    suspend fun contactsNow(): List<ContactEntity> = dao.contactsNow()

    suspend fun profileNow(): ProfileEntity = dao.profileNow() ?: ProfileEntity()

    suspend fun enqueue(kind: String, tbl: String, payload: JsonObject) {
        dao.addOp(OutboxEntity(kind = kind, tbl = tbl, payload = payload.toString(), createdAt = System.currentTimeMillis()))
        flush()
    }

    suspend fun flush(): Result<Unit> = lock.withLock {
        val r = safe {
            val uid = remote.uid()
            while (true) {
                val ops = dao.ops()
                if (ops.isEmpty()) break
                for (op in ops) {
                    remote.apply(op, uid)
                    dao.delOp(op.id)
                }
            }
        }
        syncError.value = r.exceptionOrNull()?.let { it.message ?: it.javaClass.simpleName }
        if (r.isFailure) Work.scheduleSync(ctx)
        r
    }

    var signalHook: (suspend (String) -> Unit)? = null

    suspend fun logEvent(type: String, data: JsonObject = JsonObject(emptyMap())) {
        enqueue("insert", "events", jo("type" to type, "data" to data))
        runCatching { signalHook?.invoke(type) }
    }

    suspend fun audit(action: String, detail: JsonObject = JsonObject(emptyMap())) =
        enqueue("insert", "audit_log", jo("action" to action, "detail" to detail))

    suspend fun logCall(type: String, number: String, reason: String, outcome: String) =
        enqueue("insert", "calls", jo("type" to type, "number" to number, "reason" to reason, "outcome" to outcome))

    suspend fun syncUser() {
        val s = prefs.snap()
        enqueue(
            "upsert", "users",
            jo("name" to s.name, "language" to s.lang, "consent" to jo("call_family" to s.callFamily, "call_hospital" to s.callHospital, "record" to s.record)),
        )
        enqueue("upsert", "rules", jo("inactivity_minutes" to s.inactivity, "checkin_wait_minutes" to s.wait))
    }

    suspend fun saveRules(inactivity: Int, wait: Int) {
        prefs.edit {
            this[K.INACTIVITY] = inactivity.coerceIn(15, 1440)
            this[K.WAIT] = wait.coerceIn(1, 60)
        }
        audit("rules_changed", jo("inactivity" to inactivity.coerceIn(15, 1440), "wait" to wait.coerceIn(1, 60)))
        syncUser()
    }

    private suspend fun pushContact(c: ContactEntity) =
        enqueue("upsert", "emergency_contacts", jo("id" to c.id, "name" to c.name, "phone" to c.phone, "role" to c.role, "approved_to_call" to c.approved))

    suspend fun addContact(name: String, phone: String, role: String) {
        val s = prefs.snap()
        val approved = if (role in FAMILY_ROLES) s.callFamily else s.callHospital
        val c = ContactEntity(UUID.randomUUID().toString(), name.trim(), phone.trim(), role, approved)
        dao.upsertContact(c)
        pushContact(c)
        if (role == "doctor") {
            val p = profileNow()
            if (p.doctorName.isBlank() && p.doctorPhone.isBlank()) saveProfile(p.copy(doctorName = c.name, doctorPhone = c.phone))
        }
        audit("contact_added", jo("role" to role))
    }

    suspend fun removeContact(id: String) {
        dao.deleteContact(id)
        enqueue("delete", "emergency_contacts", jo("id" to id))
        audit("contact_removed")
    }

    suspend fun setApproval(kind: String, on: Boolean) {
        val key = when (kind) {
            "call_family" -> K.CALL_FAMILY
            "call_hospital" -> K.CALL_HOSPITAL
            else -> K.RECORD
        }
        prefs.edit { this[key] = on }
        if (kind != "record") {
            val roles = if (kind == "call_family") FAMILY_ROLES else CLINIC_ROLES
            dao.setApproved(roles, on)
            dao.contactsNow().filter { it.role in roles }.forEach { pushContact(it) }
        }
        audit("approval_" + kind, jo("granted" to on))
        syncUser()
    }

    suspend fun saveProfile(p: ProfileEntity) {
        dao.upsertProfile(p.copy(id = 1))
        enqueue(
            "upsert", "medical_profile",
            jo("conditions" to p.conditions, "medicines" to p.medicines, "allergies" to p.allergies, "doctor_name" to p.doctorName, "doctor_phone" to p.doctorPhone),
        )
        audit("profile_updated")
    }

    suspend fun checkinOk() {
        val now = System.currentTimeMillis()
        prefs.edit {
            this[K.LAST_ACTIVITY] = now
            this[K.LAST_CHECKIN_OK] = now
        }
        logEvent("checkin_ok")
    }

    suspend fun placeCall(number: String, approvedContact: Boolean, reason: String): CallResult {
        val auto = approvedContact && Actions.has(ctx, android.Manifest.permission.CALL_PHONE)
        val started = if (auto) Actions.call(ctx, number) else Actions.dial(ctx, number)
        if (!started) {
            audit("call_failed", jo("reason" to reason))
            return CallResult.Failed
        }
        return if (auto) {
            logCall("approved", number, reason, "started")
            audit("call_started", jo("reason" to reason))
            CallResult.Called
        } else {
            audit("dialer_opened", jo("reason" to reason))
            CallResult.Dialer
        }
    }

    suspend fun dialEmergency(reason: String): Boolean {
        val ok = Actions.dial(ctx, "112")
        logCall("emergency", "112", reason, if (ok) "dialer_opened" else "failed")
        audit("emergency_dialer", jo("reason" to reason, "opened" to ok))
        return ok
    }

    suspend fun uploadRecording(f: File): Result<String> = safe {
        val uid = remote.uid()
        val path = uid + "/" + f.name
        val bytes = withContext(Dispatchers.IO) { f.readBytes() }
        remote.sb.storage.from("recordings").upload(path, bytes)
        enqueue(
            "insert", "recordings",
            jo("storage_ref" to path, "retain_until" to LocalDate.now().plusDays(30).toString(), "consent" to jo("notified" to true, "approved" to true)),
        )
        audit("recording_saved", jo("ref" to path))
        f.delete()
        path
    }

    private suspend inline fun <reified T : Any> list(tbl: String, userId: String, limit: Long): List<T> =
        remote.sb.from(tbl).select {
            filter { eq("user_id", userId) }
            order("created_at", Order.DESCENDING)
            limit(limit)
        }.decodeList<T>()

    suspend fun recentEvents(limit: Long = 30): Result<List<EventRow>> = safe { list<EventRow>("events", remote.uid(), limit) }

    suspend fun auditLog(): Result<List<AuditRow>> = safe { list<AuditRow>("audit_log", remote.uid(), 100) }

    suspend fun createInvite(): Result<CaregiverRow> = safe {
        val uid = remote.uid()
        val row = remote.sb.from("caregivers").insert(jo("user_id" to uid, "status" to "pending")) { select() }.decodeSingle<CaregiverRow>()
        audit("caregiver_invite_created")
        row
    }

    suspend fun caregivers(): Result<List<CaregiverRow>> = safe {
        val uid = remote.uid()
        remote.sb.from("caregivers").select {
            filter { eq("user_id", uid) }
            order("created_at", Order.DESCENDING)
        }.decodeList<CaregiverRow>()
    }

    suspend fun removeCaregiver(id: String): Result<Unit> = safe {
        remote.uid()
        remote.sb.from("caregivers").update({ set("status", "removed") }) { filter { eq("id", id) } }
        audit("caregiver_removed", jo("id" to id))
    }

    suspend fun setCaregiverRecordings(id: String, on: Boolean): Result<Unit> = safe {
        remote.uid()
        remote.sb.from("caregivers").update({ set("can_view_recordings", on) }) { filter { eq("id", id) } }
        audit("caregiver_recordings_" + (if (on) "allowed" else "denied"), jo("id" to id))
    }

    suspend fun acceptInvite(code: String): Result<String> = safe {
        remote.uid()
        remote.sb.postgrest.rpc("accept_invite", buildJsonObject { put("code", code.trim().uppercase()) }).decodeAs<String>()
    }

    suspend fun personView(personId: String): Result<PersonView> = safe {
        val uid = remote.uid()
        val name = remote.sb.from("users").select { filter { eq("id", personId) } }.decodeList<UserRow>().firstOrNull()?.name ?: ""
        val events = list<EventRow>("events", personId, 50)
        val calls = list<CallRow>("calls", personId, 30)
        val cg = remote.sb.from("caregivers").select {
            filter {
                eq("user_id", personId)
                eq("caregiver_id", uid)
            }
        }.decodeList<CaregiverRow>().firstOrNull()
        val can = cg != null && cg.status == "active" && cg.canViewRecordings
        val recs = if (can) list<RecordingRow>("recordings", personId, 20) else emptyList()
        PersonView(name, events, calls, recs, can)
    }

    companion object {
        val FAMILY_ROLES = listOf("family", "neighbor")
        val CLINIC_ROLES = listOf("doctor", "hospital")
    }
}

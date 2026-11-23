package com.simha.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class CaregiverRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("caregiver_id") val caregiverId: String? = null,
    @SerialName("invite_code") val inviteCode: String = "",
    val status: String = "pending",
    @SerialName("can_view_recordings") val canViewRecordings: Boolean = false,
)

@Serializable
data class UserRow(val id: String, val name: String = "", val language: String = "en")

@Serializable
data class EventRow(
    val type: String,
    val data: JsonObject? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class CallRow(
    val type: String,
    val number: String,
    val reason: String = "",
    val outcome: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class AuditRow(
    val action: String,
    val detail: JsonObject? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class RecordingRow(
    val id: String,
    @SerialName("storage_ref") val storageRef: String,
    @SerialName("created_at") val createdAt: String = "",
)

fun jo(vararg p: Pair<String, Any?>): JsonObject = JsonObject(
    p.associate { (k, v) ->
        k to when (v) {
            null -> JsonNull
            is JsonElement -> v
            is Boolean -> JsonPrimitive(v)
            is Number -> JsonPrimitive(v)
            else -> JsonPrimitive(v.toString())
        }
    }
)

data class Medicine(
    val id: String,
    val name: String,
    val dose: String = "",
    val times: List<String> = emptyList(),
    val stock: Int? = null,
    val active: Boolean = true,
)

data class DoseLog(
    val id: String,
    val medicineId: String?,
    val scheduledAt: Long,
    val status: String,
    val createdAt: Long,
)

data class Diary(
    val day: java.time.LocalDate,
    val data: JsonObject,
    val summary: String,
    val updatedAt: Long,
)

data class Memory(
    val id: String,
    val kind: String,
    val text: String,
    val createdAt: Long,
)

@Serializable
data class FamilyMessage(
    val id: String,
    @SerialName("from_id") val fromId: String,
    @SerialName("from_name") val fromName: String = "",
    @SerialName("storage_ref") val storageRef: String,
    val played: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class DoseRow(
    val id: String,
    @SerialName("medicine_id") val medicineId: String? = null,
    @SerialName("scheduled_at") val scheduledAt: String,
    val status: String,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class DiaryRow(
    val day: String,
    val data: JsonObject = JsonObject(emptyMap()),
    val summary: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

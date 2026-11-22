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

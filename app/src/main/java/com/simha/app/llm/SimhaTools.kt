package com.simha.app.llm

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

data class ContactInfo(val name: String, val phone: String, val role: String, val approved: Boolean)

interface ToolHost {
    suspend fun getProfile(): JsonObject
    suspend fun getContacts(): JsonArray
    suspend fun getStatus(): JsonObject
    suspend fun logSymptom(text: String): String
    suspend fun markCheckinOk(): String
    suspend fun openEmergencyScreen(): String
    suspend fun dialEmergency(): String
    suspend fun contactByName(name: String): ContactInfo?
    suspend fun callContact(c: ContactInfo): String
    suspend fun sendFamilyAlert(message: String): String
    suspend fun recordingApproved(): Boolean
    suspend fun startRecording(): String
    suspend fun lookWithCamera(question: String): String
    suspend fun setReminder(text: String, minutes: Int): String
    suspend fun updateProfileField(field: String, value: String): String
    suspend fun rememberHealthDetail(kind: String, text: String): String
    suspend fun audit(tool: String, args: String, result: String)
}

object SimhaTools {
    val profileFields = setOf("conditions", "medicines", "allergies", "doctor_name", "doctor_phone")

    private fun tool(name: String, desc: String, props: Map<String, Pair<String, String>> = emptyMap()) = buildJsonObject {
        put("type", "function")
        putJsonObject("function") {
            put("name", name)
            put("description", desc)
            putJsonObject("parameters") {
                put("type", "object")
                putJsonObject("properties") {
                    props.forEach { (k, v) ->
                        putJsonObject(k) {
                            put("type", v.first)
                            put("description", v.second)
                        }
                    }
                }
                putJsonArray("required") { props.keys.forEach { add(it) } }
            }
        }
    }

    val definitions: JsonArray = JsonArray(
        listOf(
            tool("get_profile", "Get the person's medical profile and name."),
            tool("get_contacts", "List the person's emergency contacts with role and whether calls are approved."),
            tool("get_status", "Get last check-in time and recent activity."),
            tool("log_symptom", "Save a symptom the person describes, for the doctor and family.", mapOf("text" to ("string" to "What the person said"))),
            tool("mark_checkin_ok", "Record that the person said they are fine during a check-in."),
            tool("open_emergency_screen", "Open the step-by-step emergency screen."),
            tool("dial_emergency", "Open the phone dialer with 112. The person must press call."),
            tool("call_approved_contact", "Call an emergency contact by name. Only works for contacts the person approved.", mapOf("name" to ("string" to "Contact name"))),
            tool("send_family_alert", "Prepare a text message alert to family. The person presses send.", mapOf("message" to ("string" to "Message text"))),
            tool("start_recording", "Record a voice message. Only works if the person approved recording."),
            tool("look_with_camera", "Take a photo with the camera and describe it.", mapOf("question" to ("string" to "What to look for"))),
            tool("set_reminder", "Remind the person later.", mapOf("text" to ("string" to "Reminder text"), "minutes" to ("integer" to "Minutes from now, 1 to 1440"))),
            tool(
                "remember_health_detail",
                "Save a health detail the person mentions: a condition, a medicine or an allergy. Duplicates are ignored.",
                mapOf("kind" to ("string" to "One of condition, medicine, allergy"), "text" to ("string" to "The detail, in a few words")),
            ),
            tool(
                "update_profile_field",
                "Add a note to a profile field. Existing text is kept.",
                mapOf("field" to ("string" to "One of conditions, medicines, allergies, doctor_name, doctor_phone"), "value" to ("string" to "Text to add")),
            ),
        )
    )

    private fun err(m: String) = buildJsonObject { put("error", m) }.toString()
    private fun refuse(m: String) = buildJsonObject { put("refused", m) }.toString()
    private fun ok(m: String) = buildJsonObject { put("result", m) }.toString()

    private fun JsonObject.str(k: String): String? = this[k]?.jsonPrimitive?.contentOrNull?.trim()?.ifEmpty { null }

    suspend fun dispatch(name: String, argsJson: String, host: ToolHost): String {
        val args = try {
            Json.parseToJsonElement(argsJson.ifBlank { "{}" }).jsonObject
        } catch (e: Exception) {
            val r = err("Arguments were not valid JSON")
            host.audit(name, argsJson, r)
            return r
        }
        val result = try {
            execute(name, args, host)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            err(e.message ?: "Tool failed")
        }
        host.audit(name, argsJson, result)
        return result
    }

    private suspend fun execute(name: String, a: JsonObject, host: ToolHost): String {
        when (name) {
            "get_profile" -> return host.getProfile().toString()
            "get_contacts" -> return host.getContacts().toString()
            "get_status" -> return host.getStatus().toString()
            "log_symptom" -> {
                val t = a.str("text") ?: return err("text is required")
                return ok(host.logSymptom(t))
            }
            "mark_checkin_ok" -> return ok(host.markCheckinOk())
            "open_emergency_screen" -> return ok(host.openEmergencyScreen())
            "dial_emergency" -> return ok(host.dialEmergency())
            "call_approved_contact" -> {
                val n = a.str("name") ?: return err("name is required")
                val c = host.contactByName(n) ?: return err("No contact with that name")
                if (!c.approved) return refuse("This contact is not approved for calls. The person can approve calls in Settings.")
                return ok(host.callContact(c))
            }
            "send_family_alert" -> {
                val m = a.str("message") ?: return err("message is required")
                return ok(host.sendFamilyAlert(m))
            }
            "start_recording" -> {
                if (!host.recordingApproved()) return refuse("Recording is not approved. The person can approve it in Settings.")
                return ok(host.startRecording())
            }
            "look_with_camera" -> return ok(host.lookWithCamera(a.str("question") ?: "Describe what you see."))
            "set_reminder" -> {
                val t = a.str("text") ?: return err("text is required")
                val m = a["minutes"]?.jsonPrimitive?.intOrNull ?: return err("minutes is required")
                if (m !in 1..1440) return err("minutes must be 1 to 1440")
                return ok(host.setReminder(t, m))
            }
            "remember_health_detail" -> {
                val k = a.str("kind")?.lowercase() ?: return err("kind is required")
                if (k !in HealthMemory.kinds) return err("kind must be condition, medicine or allergy")
                val t = a.str("text") ?: return err("text is required")
                return ok(host.rememberHealthDetail(k, t.take(200)))
            }
            "update_profile_field" -> {
                val f = a.str("field") ?: return err("field is required")
                if (f !in profileFields) return err("Unknown field")
                val v = a.str("value") ?: return err("value is required")
                return ok(host.updateProfileField(f, v))
            }
            else -> return err("Unknown tool")
        }
    }
}

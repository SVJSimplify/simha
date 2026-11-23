package com.simha.app.llm

import com.simha.app.logic.FirstAid
import com.simha.app.logic.Guide
import com.simha.app.logic.MedicalProfile
import com.simha.app.logic.Situation
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
    suspend fun medicalProfile(): MedicalProfile
    suspend fun logDiary(update: JsonObject): String
    suspend fun remember(kind: String, text: String): String
    suspend fun takeMedicine(name: String, status: String): String
    suspend fun listMedicines(): JsonArray
    suspend fun familyContact(label: String?): ContactInfo?
    suspend fun playFamilyMessages(): String
    suspend fun addMedicine(name: String, dose: String, times: List<String>): String
    suspend fun audit(tool: String, args: String, result: String)
}

object SimhaTools {
    val profileFields = setOf("conditions", "medicines", "allergies", "doctor_name", "doctor_phone")

    val memoryKinds = setOf("fact", "person", "event")
    val doseStatuses = setOf("taken", "skipped")

    private fun tool(name: String, desc: String, props: Map<String, Pair<String, String>> = emptyMap(), required: Collection<String> = props.keys) = buildJsonObject {
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
                            if (v.first == "array") putJsonObject("items") { put("type", "string") }
                            put("description", v.second)
                        }
                    }
                }
                putJsonArray("required") { required.forEach { add(it) } }
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
            tool(
                "get_first_aid",
                "Get the fixed step-by-step first-aid guide for a health situation. Use it whenever the person describes a fall, pain, dizziness, breathlessness, bleeding, a burn, fever, sugar problems, an allergic reaction or a medicine mistake. Never answer such situations without it.",
                mapOf("situation" to ("string" to "Short description in English of only the signs that are present. Start with one of: ${Situation.entries.joinToString(", ") { it.key }}")),
            ),
            tool(
                "log_diary",
                "Save what the person tells you about their day into today's diary. Only send what they said. Earlier entries of the day are kept.",
                mapOf(
                    "meals" to ("string" to "Meals eaten, comma separated"),
                    "water" to ("string" to "How much water, for example 4 glasses"),
                    "mood" to ("string" to "Mood in a few words"),
                    "pain" to ("string" to "Any pain and where"),
                    "sleep" to ("string" to "How they slept"),
                    "activities" to ("string" to "What they did, comma separated"),
                    "visitors" to ("string" to "Who visited or called, comma separated"),
                    "notes" to ("string" to "Anything else worth remembering"),
                ),
                required = emptyList(),
            ),
            tool(
                "remember",
                "Remember something about the person's life for later conversations.",
                mapOf("kind" to ("string" to "One of fact, person, event"), "text" to ("string" to "What to remember, in one short sentence, for example: Priya is the daughter who lives in Pune")),
            ),
            tool(
                "take_medicine",
                "Record that the person took or skipped a medicine.",
                mapOf("name" to ("string" to "Medicine name"), "status" to ("string" to "taken or skipped")),
            ),
            tool("list_medicines", "List the person's medicines with times and today's dose status."),
            tool(
                "scam_check",
                "Check a suspicious call, message or request for scam signs. Use it whenever the person mentions a stranger asking for money, OTP, PIN, KYC, a link or an app.",
                mapOf("description" to ("string" to "What happened, in English")),
            ),
            tool(
                "call_family",
                "Call a family member. Only works for family contacts the person approved. Leave the label out to call the first approved family contact.",
                mapOf("label" to ("string" to "Optional name or relation, for example daughter or Priya")),
                required = emptyList(),
            ),
            tool("play_family_messages", "Play the person's unplayed voice messages from their family."),
            tool(
                "add_medicine",
                "Save a medicine the person takes, with reminders. Use it only after you know the name, the dose and the times of day. Saving a medicine with the same name updates it.",
                mapOf(
                    "name" to ("string" to "Medicine name"),
                    "dose" to ("string" to "Dose, for example 500mg or 1 tablet. Empty if unknown"),
                    "times" to ("array" to "Times of day in 24 hour HH:MM, for example 08:00 and 20:00"),
                ),
                required = listOf("name", "times"),
            ),
        )
    )

    private fun err(m: String) = buildJsonObject { put("error", m) }.toString()
    private fun refuse(m: String) = buildJsonObject { put("refused", m) }.toString()
    private fun ok(m: String) = buildJsonObject { put("result", m) }.toString()

    fun parseTimes(raw: List<String>): List<String> = raw.mapNotNull { t ->
        val m = Regex("^\\s*(\\d{1,2})[:.](\\d{2})\\s*$").matchEntire(t) ?: return@mapNotNull null
        val h = m.groupValues[1].toInt()
        val mi = m.groupValues[2].toInt()
        if (h in 0..23 && mi in 0..59) "%02d:%02d".format(h, mi) else null
    }.distinct().sorted()

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
            "get_first_aid" -> {
                val t = a.str("situation") ?: return err("situation is required")
                val sit = FirstAid.match(t) ?: return err("Unknown situation. Use one of: " + Situation.entries.joinToString(", ") { it.key })
                val g = FirstAid.guide(sit, host.medicalProfile())
                val urgent = FirstAid.dangerous(sit, t)
                val note = if (urgent) {
                    host.openEmergencyScreen()
                    "Danger signs are present. The emergency screen has been opened. Tell the person calmly to call 112 now, then keep guiding them through the steps while help comes."
                } else {
                    "No danger sign has been mentioned yet. Guide them step by step, ask about each danger sign one at a time, and if any is present call get_first_aid again describing it."
                }
                return guideJson(g, urgent, note)
            }
            "log_diary" -> {
                val u = DiaryMerge.update(DiaryMerge.allKeys.associateWith { a.str(it) })
                if (u.isEmpty()) return err("Nothing to save")
                return ok(host.logDiary(u))
            }
            "remember" -> {
                val k = a.str("kind")?.lowercase() ?: return err("kind is required")
                if (k !in memoryKinds) return err("kind must be fact, person or event")
                val t = a.str("text") ?: return err("text is required")
                return ok(host.remember(k, t.take(300)))
            }
            "take_medicine" -> {
                val n = a.str("name") ?: return err("name is required")
                val s = a.str("status")?.lowercase() ?: return err("status is required")
                if (s !in doseStatuses) return err("status must be taken or skipped")
                return ok(host.takeMedicine(n, s))
            }
            "list_medicines" -> return host.listMedicines().toString()
            "scam_check" -> {
                val d = a.str("description") ?: return err("description is required")
                return scamJson(Scam.check(d))
            }
            "call_family" -> {
                val c = host.familyContact(a.str("label")) ?: return err("No family contact found")
                if (!c.approved) return refuse("This family member is not approved for calls. The person can approve calls in Settings.")
                return ok(host.callContact(c))
            }
            "play_family_messages" -> return ok(host.playFamilyMessages())
            "add_medicine" -> {
                val n = a.str("name") ?: return err("name is required")
                val raw = when (val t = a["times"]) {
                    is JsonArray -> t.map { it.jsonPrimitive.content }
                    null -> emptyList()
                    else -> t.jsonPrimitive.content.split(",")
                }
                val times = parseTimes(raw)
                if (times.isEmpty()) return err("times must be like 08:00")
                return ok(host.addMedicine(n.take(80), (a.str("dose") ?: "").take(60), times.take(8)))
            }
            else -> return err("Unknown tool")
        }
    }

    fun guideJson(g: Guide, urgent: Boolean, note: String): String = buildJsonObject {
        put("situation", g.situation.key)
        put("title", g.situation.title)
        put("call_112_now", urgent)
        put("instruction", note)
        putJsonArray("steps") { g.steps.forEach { add(it) } }
        putJsonArray("danger_signs_that_mean_call_112") { g.danger.forEach { add(it) } }
        putJsonArray("personal_notes") { g.personal.forEach { add(it) } }
    }.toString()

    fun scamJson(r: ScamReport): String = buildJsonObject {
        put("risk", if (r.high) "high" else "unclear")
        putJsonArray("warning_signs") { r.signals.forEach { add(it) } }
        putJsonArray("rules") { r.rules.forEach { add(it) } }
        put(
            "instruction",
            if (r.high) "This looks like a scam. Calmly tell the person to hang up, share nothing and pay nothing, and offer to call their family."
            else "Not clearly a scam, but tell the person the rules in simple words and to check with family before acting.",
        )
    }.toString()
}

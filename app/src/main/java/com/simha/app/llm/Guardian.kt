package com.simha.app.llm

import android.content.Context
import com.simha.app.R
import com.simha.app.logic.MedicalProfile
import com.simha.app.voice.Langs
import com.simha.app.wake.WakeReason
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.LocalDate
import java.time.LocalTime

data class MedLine(val name: String, val dose: String, val times: List<String>, val status: String)

data class GuardianContext(
    val name: String,
    val language: String,
    val profile: MedicalProfile,
    val doctor: String = "",
    val medicines: List<MedLine> = emptyList(),
    val memories: List<String> = emptyList(),
    val todayDiary: String = "",
    val yesterdayDiary: String = "",
    val timeOfDay: String = "",
    val reason: WakeReason? = null,
    val extra: String? = null,
    val unwell: Boolean = false,
    val asked: Set<String> = emptySet(),
    val hook: String? = null,
) {
    val hasMedicines: Boolean get() = medicines.isNotEmpty() || profile.medicines.isNotBlank()

    fun known(): Known = Known(
        medicines = hasMedicines,
        newPerson = "intro" !in asked && !hasMedicines && profile.conditions.isBlank() && profile.allergies.isBlank() &&
            doctor.isBlank() && memories.isEmpty() && todayDiary.isBlank() && yesterdayDiary.isBlank(),
        asked = asked,
    )
}

data class Known(val medicines: Boolean = true, val newPerson: Boolean = false, val asked: Set<String> = emptySet())

enum class Open { Checkin, Medicine, Morning, Evening, Fall, Night, Family, FamilyGeneric, Heat, Intro, AskMedicines, None }

object Guardian {
    fun timeOfDay(hour: Int, minute: Int): String {
        val part = when (hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..20 -> "evening"
            else -> "night"
        }
        return part + ", " + "%02d:%02d".format(hour, minute)
    }

    fun variant(seed: Int, n: Int): Int = Math.floorMod(seed, n)

    fun seed(): Int = LocalDate.now().dayOfYear * 24 + LocalTime.now().hour

    fun hookFrom(data: JsonObject?): String? =
        (data?.get("pain") as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() && !it.equals("none", true) && !it.equals("no", true) }?.take(60)

    fun openingKey(reason: WakeReason?, k: Known = Known(), medicine: String? = null, sender: String? = null): Open {
        val ask = !k.medicines && "medicines" !in k.asked
        return when (reason) {
            null -> if (k.newPerson) Open.Intro else if (ask) Open.AskMedicines else Open.None
            WakeReason.Checkin -> Open.Checkin
            WakeReason.Medicine -> if (medicine.isNullOrBlank()) Open.Checkin else Open.Medicine
            WakeReason.Morning -> if (k.newPerson) Open.Intro else if (ask) Open.AskMedicines else Open.Morning
            WakeReason.Evening -> Open.Evening
            WakeReason.Fall -> Open.Fall
            WakeReason.NightWatch -> Open.Night
            WakeReason.FamilyMessage -> if (sender.isNullOrBlank()) Open.FamilyGeneric else Open.Family
            WakeReason.Heat -> Open.Heat
        }
    }

    private fun reasonText(r: WakeReason, extra: String?): String = when (r) {
        WakeReason.Checkin -> "This is a safety check-in. You just asked how they are feeling. If they say they are fine, call mark_checkin_ok and reply with one warm short sentence. If they are not fine, gently ask what is wrong, save it with log_symptom, and help them."
        WakeReason.Medicine -> "You just reminded them that it is time for ${extra ?: "a medicine"}. If they took it, call take_medicine with taken. If they choose not to, call take_medicine with skipped. If they forgot, gently encourage them to take it now, then record it."
        WakeReason.Morning -> "It is the morning greeting. You greeted them and told them their plan for the day. Ask how they slept, and about water and breakfast.${if (extra.isNullOrBlank()) "" else " Context for you: $extra"}"
        WakeReason.Evening -> "It is the evening. You asked how their day was. Gently collect the day's diary through chat: meals, water, mood, pain, sleep, what they did, who visited. Save with log_diary as they tell you. Skip what today's diary already has."
        WakeReason.Fall -> "The phone sensed a possible fall. You asked whether they fell and are hurt. If they are hurt or unsure, call get_first_aid with fall plus what they describe. If they are fine, call mark_checkin_ok, reassure them and ask them to sit and rest a moment."
        WakeReason.NightWatch -> "It is night and the phone noticed they are up. You asked if everything is alright. Be soft and quiet. If they need something, help. If they feel unwell, use get_first_aid."
        WakeReason.FamilyMessage -> "A voice message from ${extra ?: "their family"} is waiting. You offered to play it. If they say yes, call play_family_messages."
        WakeReason.Heat -> "It is very hot${if (extra.isNullOrBlank()) "" else " ($extra)"}. You asked them to drink water. Encourage small sips of water, staying in the shade or under a fan. If they feel dizzy, weak or confused, use get_first_aid with heat."
    }

    fun systemPrompt(c: GuardianContext): String {
        val lang = Langs.name(c.language)
        val sb = StringBuilder()
        sb.append("You are Simha, a warm, caring companion who lives with ").append(c.name).append(", like a loving grandchild or a kind nurse. ").append(c.name).append(" may be elderly and alone. Talk like a real person, never like a machine.\n")
        sb.append("Style:\n")
        sb.append("- Reply only in ").append(lang).append(" (in its own script), even if they speak or type another language, unless they ask to switch.\n")
        sb.append("- Short natural spoken sentences. No lists, no markdown, no emojis. Keep every reply brief.\n")
        sb.append("- Use ").append(c.name).append("'s name now and then. Ask only one thing at a time. Remember everything said in this conversation.\n")
        sb.append("- Never mention tools, functions, JSON or anything technical.\n")
        sb.append("Health:\n")
        sb.append("- Never say you do not know what to do. For any health situation (fall, pain, dizziness, breathlessness, bleeding, burn, fever, sugar problem, allergic reaction, choking, a medicine mistake, feeling faint or hot) call get_first_aid straight away and guide them through the steps in order, one short step at a time, waiting for them to say it is done before the next. Name the danger signs to watch for.\n")
        sb.append("- If the guide says danger signs are present, tell them calmly to call 112 now, and stay with them guiding them while help comes. You never decide escalation yourself: the app does. Do not promise that someone is coming or called unless a tool result confirms it.\n")
        sb.append("- For anything not urgent, recommend seeing their doctor soon").append(if (c.doctor.isBlank()) "" else " (their doctor is ${c.doctor})").append(", and offer to tell their family.\n")
        sb.append("- You are not a doctor. Do not diagnose. Never advise doses or new medicines beyond what the guide says.\n")
        sb.append("- When they mention a health condition, a medicine or an allergy, save it with remember_health_detail, then confirm briefly. When they describe a symptom, save it with log_symptom.\n")
        sb.append("Daily care:\n")
        sb.append("- Ask about medicines that are due and not yet logged. When they say they took or skipped one, call take_medicine.\n")
        sb.append("- Gently learn how their day is going, one topic at a time, never like a questionnaire: meals, water, mood, pain, sleep, what they did, who visited. Save what they tell you with log_diary. Do not ask again what today's diary already has.\n")
        sb.append("- When they mention a person, an event or a fact about their life, save it with remember. Bring up recent memories and yesterday's diary naturally, for example: Yesterday your knee was sore, how is it today? Never read them out as a list.\n")
        sb.append("- If someone asked them for money, an OTP, a PIN, KYC details, a link or to install an app, call scam_check and explain simply: never share an OTP or PIN, banks never ask for KYC by phone, hang up and call the official number, and offer to call family.\n")
        sb.append("- When they ask to call family, use call_family. When they want to hear family messages, use play_family_messages. Other abilities: call_approved_contact, send_family_alert, start_recording, look_with_camera, set_reminder.\n")
        sb.append("- After acting, say in plain words what you did, and only say you did it if the tool result confirms. If a tool says refused or error, say so simply and kindly.\n")
        sb.append("Now: ").append(c.timeOfDay.ifBlank { "unknown time" }).append(".\n")
        sb.append("Person: ").append(c.name).append(". Conditions: ").append(c.profile.conditions.ifBlank { "none given" })
        sb.append(". Allergies: ").append(c.profile.allergies.ifBlank { "none given" })
        sb.append(". Medicines noted: ").append(c.profile.medicines.ifBlank { "none given" })
        sb.append(". Doctor: ").append(c.doctor.ifBlank { "none given" }).append(".\n")
        if (c.medicines.isNotEmpty()) {
            sb.append("Medicine schedule today:\n")
            c.medicines.forEach { m ->
                sb.append("- ").append(m.name)
                if (m.dose.isNotBlank()) sb.append(" ").append(m.dose)
                sb.append(": ").append(m.status).append("\n")
            }
        }
        if (c.memories.isNotEmpty()) {
            sb.append("What you remember:\n")
            c.memories.take(15).forEach { sb.append("- ").append(it).append("\n") }
        }
        if (c.todayDiary.isNotBlank()) sb.append("Today's diary so far: ").append(c.todayDiary).append("\n")
        if (c.yesterdayDiary.isNotBlank()) sb.append("Yesterday's diary: ").append(c.yesterdayDiary).append("\n")
        c.reason?.let { sb.append("Why this conversation started: ").append(reasonText(it, c.extra)).append("\n") }
        if (c.unwell) sb.append("Why this conversation started: they opened the app because they feel unwell. You asked what is wrong. Listen, then use get_first_aid and guide them step by step.\n")
        return sb.toString()
    }

    fun openingLine(
        lc: Context, reason: WakeReason?, name: String, medicine: String? = null, plan: String? = null, unplayed: Int = 0, sender: String? = null,
        known: Known = Known(), hook: String? = null, seed: Int = seed(),
    ): String {
        val r = lc.resources
        fun pick(id: Int, vararg a: Any): String {
            val v = r.getStringArray(id)
            return v[variant(seed, v.size)].format(*a)
        }
        return when (openingKey(reason, known, medicine, sender)) {
            Open.Checkin -> pick(R.array.r4b_o_checkin, name)
            Open.Medicine -> pick(R.array.r4b_o_medicine, name, medicine.orEmpty())
            Open.Morning -> {
                val sb = StringBuilder(pick(R.array.r4b_o_morning, name))
                if (!plan.isNullOrBlank()) sb.append(" ").append(lc.getString(R.string.gd_open_plan, plan))
                if (unplayed > 0) sb.append(" ").append(r.getQuantityString(R.plurals.gd_open_msgs, unplayed, unplayed))
                else if (!hook.isNullOrBlank()) sb.append(" ").append(lc.getString(R.string.r4b_hook_pain, hook))
                sb.toString()
            }
            Open.Evening -> pick(R.array.r4b_o_evening, name)
            Open.Fall -> lc.getString(R.string.gd_open_fall, name)
            Open.Night -> lc.getString(R.string.gd_open_night, name)
            Open.Family -> lc.getString(R.string.gd_open_family, name, sender)
            Open.FamilyGeneric -> lc.getString(R.string.gd_open_family_generic, name)
            Open.Heat -> lc.getString(R.string.gd_open_heat, name)
            Open.Intro -> lc.getString(R.string.r4b_o_intro, name)
            Open.AskMedicines -> if (reason == WakeReason.Morning) lc.getString(R.string.r4b_gm, name) + " " + lc.getString(R.string.r4b_o_ask_med) else lc.getString(R.string.r4b_o_ask_med)
            Open.None -> ""
        }
    }

    fun summaryMessages(transcript: String): JsonArray = JsonArray(
        listOf(
            Msg.system("You write memory notes for a caring companion. Summarise the conversation below in at most two short plain English sentences: how the person felt, any health issue or medicine, and anything to follow up. No names of tools. No greeting."),
            Msg.user(transcript),
        )
    )
}

package com.simha.app.logic

import java.time.OffsetDateTime

enum class StepId { Calm, Call112, Unlock, Heart, Diabetes, Breath, Meds, Allergy, Family, Record }

data class Step(val id: StepId, val arg: String? = null)

object EmergencyPlan {
    private val heart = listOf("heart", "cardiac", "chest", "blood pressure", "bp", "हृदय", "दिल", "గుండె", "ಹೃದಯ", "இதய")
    private val diabetes = listOf("diabet", "sugar", "मधुमेह", "शुगर", "మధుమేహ", "షుగర్", "ಮಧುಮೇಹ", "ಸಕ್ಕರೆ", "சர்க்கரை", "நீரிழிவு")
    private val breath = listOf("asthma", "copd", "breath", "दमा", "सांस", "ఆస్తమా", "ಆಸ್ತಮಾ", "ஆஸ்துமா")

    fun build(conditions: String, medicines: String, allergies: String, familyName: String?, canRecord: Boolean): List<Step> {
        val c = conditions.lowercase()
        val steps = mutableListOf(Step(StepId.Calm), Step(StepId.Call112), Step(StepId.Unlock))
        if (heart.any { c.contains(it) }) steps += Step(StepId.Heart)
        if (diabetes.any { c.contains(it) }) steps += Step(StepId.Diabetes)
        if (breath.any { c.contains(it) }) steps += Step(StepId.Breath)
        if (medicines.isNotBlank()) steps += Step(StepId.Meds, medicines.trim())
        if (allergies.isNotBlank()) steps += Step(StepId.Allergy, allergies.trim())
        if (!familyName.isNullOrBlank()) steps += Step(StepId.Family, familyName)
        if (canRecord) steps += Step(StepId.Record)
        return steps
    }
}

enum class Answer { Fine, Help, Unknown }

object Reply {
    private val help = listOf(
        "help", "not fine", "not okay", "not ok", "no", "pain", "hurt", "bachao", "madad", "nahi",
        "मदद", "बचाओ", "दर्द", "नहीं", "సహాయం", "సాయం", "నొప్పి", "లేదు", "ಸಹಾಯ", "ನೋವು", "ಇಲ್ಲ", "உதவி", "வலி", "இல்லை",
    )
    private val fine = listOf(
        "fine", "okay", "ok", "good", "well", "yes", "all right", "alright", "theek", "thik", "haan",
        "ठीक", "हाँ", "हां", "अच्छा", "బాగున్నాను", "బాగుంది", "అవును", "ಚೆನ್ನಾಗಿ", "ಹೌದು", "நலம்", "நன்றாக", "ஆமாம்",
    )
    private val stop = setOf(
        "stop", "bye", "goodbye", "quit", "exit", "अलविदा", "रुको", "बंद", "ఆపు", "బై", "ನಿಲ್ಲಿಸು", "ಬಾಯ್", "நிறுத்து", "பை",
    )

    private fun tokens(s: String): Set<String> =
        s.lowercase().split(Regex("[^\\p{L}\\p{M}]+")).filter { it.isNotEmpty() }.toSet()

    private fun has(s: String, t: Set<String>, w: String): Boolean =
        if (w.contains(' ')) s.contains(w) else t.contains(w)

    fun classify(text: String): Answer {
        val s = text.lowercase().trim()
        val t = tokens(s)
        if (help.any { has(s, t, it) }) return Answer.Help
        if (fine.any { has(s, t, it) }) return Answer.Fine
        return Answer.Unknown
    }

    fun isStop(text: String): Boolean = tokens(text).any { it in stop }
}

sealed interface Ago {
    data object Never : Ago
    data object Now : Ago
    data class Minutes(val n: Int) : Ago
    data class Hours(val n: Int) : Ago
    data class Days(val n: Int) : Ago
}

object TimeUtil {
    fun ago(now: Long, then: Long?): Ago {
        if (then == null || then <= 0L) return Ago.Never
        val m = ((now - then) / 60_000L).coerceAtLeast(0L)
        return when {
            m < 1 -> Ago.Now
            m < 60 -> Ago.Minutes(m.toInt())
            m < 60 * 24 -> Ago.Hours((m / 60).toInt())
            else -> Ago.Days((m / (60 * 24)).toInt())
        }
    }

    fun isoMillis(s: String): Long? = try {
        OffsetDateTime.parse(s).toInstant().toEpochMilli()
    } catch (e: Exception) {
        null
    }
}

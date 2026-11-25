package com.simha.app.care

enum class Flag { WaterLow, WaterHigh, SleepShort, PainUp, MoodLow, MealSkipped }

data class CareFacts(
    val conditions: String = "",
    val medicines: String = "",
    val heatDay: Boolean = false,
    val hour: Int = 12,
    val yesterdayPain: Int? = null,
)

object Checks {
    private val fluidLimit = listOf("heart failure", "cardiac failure", "kidney", "renal", "dialysis", "ckd", "fluid restriction", "गुर्दे", "किडनी")
    private val liters = Regex("""(\d+(?:[.,]\d+)?)\s*(l|litre|liter|litres|liters|लीटर)(?![a-z])""", RegexOption.IGNORE_CASE)
    private val ml = Regex("""(\d+)\s*(ml|millilit)""", RegexOption.IGNORE_CASE)
    private val glasses = Regex("""(\d+)\s*(glass|glasses|cup|cups|गिलास|ग्लास)""", RegexOption.IGNORE_CASE)
    private val hours = Regex("""(\d+(?:[.,]\d+)?)\s*(h|hr|hrs|hour|hours|घंटे|घंटा)(?![a-z])""", RegexOption.IGNORE_CASE)
    private val num = Regex("""\d+(?:[.,]\d+)?""")

    fun fluidRestricted(f: CareFacts): Boolean {
        val t = (f.conditions + " " + f.medicines).lowercase()
        return fluidLimit.any { t.contains(it) }
    }

    fun waterLiters(raw: String): Double? {
        val t = raw.lowercase()
        liters.find(t)?.let { return it.groupValues[1].replace(',', '.').toDoubleOrNull() }
        ml.find(t)?.let { return it.groupValues[1].toDouble() / 1000 }
        glasses.find(t)?.let { return it.groupValues[1].toDouble() * 0.25 }
        return null
    }

    fun sleepHours(raw: String): Double? {
        val t = raw.lowercase()
        hours.find(t)?.let { return it.groupValues[1].replace(',', '.').toDoubleOrNull() }
        return num.find(t)?.value?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it in 0.0..16.0 }
    }

    fun level(raw: String): Int? {
        val t = raw.lowercase()
        num.find(t)?.value?.replace(',', '.')?.toDoubleOrNull()?.let { return it.toInt().coerceIn(0, 10) }
        return when {
            listOf("severe", "very bad", "unbearable", "terrible", "बहुत").any { t.contains(it) } -> 8
            listOf("bad", "strong", "worse", "ज़्यादा", "ज्यादा").any { t.contains(it) } -> 6
            listOf("mild", "little", "slight", "थोड़ा", "हल्का").any { t.contains(it) } -> 3
            listOf("none", "no pain", "fine", "नहीं").any { t.contains(it) } -> 0
            else -> null
        }
    }

    private fun has(t: String, words: List<String>) = words.any { t.lowercase().contains(it) }

    fun intake(kind: String, amount: String, f: CareFacts): Flag? = when (kind.lowercase()) {
        "water" -> {
            val l = waterLiters(amount)
            when {
                l == null -> null
                fluidRestricted(f) -> if (l > 1.5) Flag.WaterHigh else null
                f.hour >= 17 && l < (if (f.heatDay) 2.0 else 1.5) -> Flag.WaterLow
                f.hour in 12..16 && l < (if (f.heatDay) 1.0 else 0.75) -> Flag.WaterLow
                l > 5.0 -> Flag.WaterHigh
                else -> null
            }
        }
        "sleep" -> sleepHours(amount)?.let { if (it < 5.0) Flag.SleepShort else null }
        "pain" -> level(amount)?.let { p -> if (f.yesterdayPain != null && p > f.yesterdayPain) Flag.PainUp else null }
        "mood" -> if (has(amount, listOf("sad", "low", "lonely", "down", "unhappy", "उदास", "अकेला"))) Flag.MoodLow else null
        "meal" -> if (has(amount, listOf("skipped", "nothing", "not eaten", "didn't eat", "no food", "नहीं खाया"))) Flag.MealSkipped else null
        else -> null
    }
}

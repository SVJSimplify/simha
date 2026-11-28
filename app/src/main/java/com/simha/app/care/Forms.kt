package com.simha.app.care

import java.time.LocalTime

enum class TaskType(val key: String, val required: List<String>) {
    Medicine("medicine", listOf("name", "time")),
    Reminder("reminder", listOf("what", "time")),
    Symptom("symptom", listOf("what")),
    Intake("intake", listOf("kind", "amount"));

    companion object {
        fun of(k: String): TaskType? = entries.firstOrNull { it.key == k }
    }
}

data class Form(val type: TaskType, val slots: MutableMap<String, String> = mutableMapOf(), val createdAt: Long = System.currentTimeMillis()) {
    fun missing(): String? = type.required.firstOrNull { slots[it].isNullOrBlank() }
    val complete: Boolean get() = missing() == null
}

sealed interface Step {
    data class Ask(val form: Form, val slot: String) : Step
    data class Done(val form: Form) : Step
}

class FormStack(private val ttlMs: Long = 20 * 60_000L, private val clock: () -> Long = { System.currentTimeMillis() }) {
    private val open = mutableListOf<Form>()

    val forms: List<Form>
        get() {
            prune()
            return open.toList()
        }

    fun clear() = open.clear()

    private fun prune() {
        val now = clock()
        open.removeAll { now - it.createdAt > ttlMs }
    }

    fun offer(type: TaskType, slots: Map<String, String>): Form {
        prune()
        val clean = slots.filterValues { it.isNotBlank() }.mapValues { it.value.trim() }
        val target = open.lastOrNull { it.type == type && !it.complete && compatible(it, clean) }
        val f = target ?: Form(type, createdAt = clock()).also { open += it }
        f.slots.putAll(clean)
        return f
    }

    fun drain(): List<Step> {
        prune()
        val out = mutableListOf<Step>()
        while (true) {
            val done = open.firstOrNull { it.complete }
            if (done != null) {
                open.remove(done)
                out += Step.Done(done)
                continue
            }
            val f = open.firstOrNull() ?: break
            out += Step.Ask(f, f.missing()!!)
            break
        }
        return out
    }

    fun pending(): Pair<TaskType, String>? {
        prune()
        return open.firstOrNull()?.let { f -> f.missing()?.let { f.type to it } }
    }

    private fun compatible(f: Form, s: Map<String, String>): Boolean =
        s.all { (k, v) -> f.slots[k].isNullOrBlank() || f.slots[k].equals(v, true) }
}

object Clock {
    private const val NL = "(?<![\\p{L}\\p{M}])"
    private const val NR = "(?![\\p{L}\\p{M}])"
    private val num = Regex("""(\d{1,2})(?:\s*[:.]\s*(\d{2}))?""")
    private val words = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6, "seven" to 7, "eight" to 8,
        "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12,
        "एक" to 1, "दो" to 2, "तीन" to 3, "चार" to 4, "पांच" to 5, "पाँच" to 5, "छह" to 6, "छः" to 6, "सात" to 7, "आठ" to 8, "नौ" to 9, "दस" to 10, "ग्यारह" to 11, "बारह" to 12,
    )
    private val amWords = listOf("morning", "subah", "सुबह", "सवेरे", "ఉదయం", "ಬೆಳಿಗ್ಗೆ", "ಬೆಳಗ್ಗೆ", "காலை")
    private val pmWords = listOf("afternoon", "evening", "night", "tonight", "shaam", "sham", "raat", "dopahar", "दोपहर", "शाम", "रात", "మధ్యాహ్నం", "సాయంత్రం", "రాత్రి", "ಮಧ್ಯಾಹ್ನ", "ಸಂಜೆ", "ರಾತ್ರಿ", "மதியம்", "மாலை", "இரவு")
    private val nightWords = listOf("night", "tonight", "raat", "रात", "రాత్రి", "ರಾತ್ರಿ", "இரவு")
    private val relCue = listOf("in ", " later", "after ", "बाद", "में", "తర్వాత", "లో", "ನಂತರ", "ದಲ್ಲಿ", "பிறகு", "இல்")
    private val minUnit = Regex("""(\d{1,3})\s*(minutes?|mins?|मिनट|నిమిష\S*|ನಿಮಿಷ\S*|நிமிட\S*)""")
    private val hourUnit = Regex("""(\d{1,2})\s*(hours?|hrs?|घंटे|घंटा|గంటల|ಗಂಟೆಗಳ|மணி\s*நேர\S*)""")

    fun normalize(raw: String): String {
        val sb = StringBuilder()
        raw.lowercase().forEach { ch ->
            val c = ch.code
            sb.append(
                when (c) {
                    in 0x0966..0x096F -> '0' + (c - 0x0966)
                    in 0x0C66..0x0C6F -> '0' + (c - 0x0C66)
                    in 0x0CE6..0x0CEF -> '0' + (c - 0x0CE6)
                    in 0x0BE6..0x0BEF -> '0' + (c - 0x0BE6)
                    else -> ch
                }
            )
        }
        var s = sb.toString().replace("a.m.", "am").replace("p.m.", "pm").replace("a.m", "am").replace("p.m", "pm")
        words.forEach { (w, n) ->
            val tail = if (w.all { it.code < 0x80 }) NR else "(?=\\s*(बजे|baje))"
            s = s.replace(Regex(NL + Regex.escape(w) + tail), n.toString())
        }
        return s
    }

    fun resolve(raw: String, now: LocalTime): String? = resolveAll(raw, now).firstOrNull()

    fun resolveAll(raw: String, now: LocalTime): List<String> {
        val s = normalize(raw)
        relative(s, now)?.let { return listOf(it) }
        val parts = s.split(Regex("\\s*(,|;|&|/|\\band\\b|और|మరియు|ಮತ್ತು|மற்றும்)\\s*")).filter { it.isNotBlank() }
        val out = mutableListOf<String>()
        parts.forEach { p -> one(p, now)?.let { t -> out += if (t in out) flip(t) else t } }
        return out.distinct()
    }

    private fun flip(t: String): String {
        val h = t.take(2).toInt()
        return "%02d:%s".format(if (h < 12) h + 12 else h - 12, t.drop(3))
    }

    private fun relative(s: String, now: LocalTime): String? {
        if (relCue.none { s.contains(it) }) return null
        minUnit.find(s)?.let { m -> val n = m.groupValues[1].toInt(); if (n in 1..600) return fmt(now.plusMinutes(n.toLong())) }
        hourUnit.find(s)?.let { m -> val n = m.groupValues[1].toInt(); if (n in 1..23) return fmt(now.plusHours(n.toLong())) }
        if (Regex("half an hour|आधे घंटे|అరగంట|ಅರ್ಧ ಗಂಟೆ|அரை மணி").containsMatchIn(s)) return fmt(now.plusMinutes(30))
        return null
    }

    private fun fmt(t: LocalTime) = "%02d:%02d".format(t.hour, t.minute)

    private fun one(p: String, now: LocalTime): String? {
        if (p.contains("noon")) return "12:00"
        if (p.contains("midnight")) return "00:00"
        val m = num.find(p) ?: return when {
            p.contains("डेढ़") -> ambiguous(1, 30, p, now)
            p.contains("ढाई") -> ambiguous(2, 30, p, now)
            else -> null
        }
        var h = m.groupValues[1].toInt()
        var mi = m.groupValues[2].ifEmpty { "0" }.toInt()
        if (mi !in 0..59 || h > 23) return null
        when {
            p.contains("half past") || p.contains("साढ़े") || p.contains("sadhe") || p.contains("saade") -> mi = 30
            p.contains("quarter past") || p.contains("सवा") || p.contains("sawa") -> mi = 15
            p.contains("quarter to") || p.contains("पौने") || p.contains("paune") -> {
                mi = 45
                h = if (h == 1) 12 else h - 1
            }
        }
        return ambiguous(h, mi, p, now)
    }

    private fun ambiguous(h0: Int, mi: Int, p: String, now: LocalTime): String? {
        var h = h0
        val am = Regex("\\bam\\b").containsMatchIn(p) || amWords.any { p.contains(it) }
        val pm = Regex("\\bpm\\b").containsMatchIn(p) || pmWords.any { p.contains(it) }
        when {
            h == 0 || h in 13..23 -> Unit
            pm -> {
                if (h !in 1..12) return null
                val night = nightWords.any { p.contains(it) }
                h = when {
                    h == 12 && night -> 0
                    h == 12 -> 12
                    night && h <= 3 -> h
                    else -> h + 12
                }
            }
            am -> {
                if (h !in 1..12) return null
                if (h == 12) h = 0
            }
            h in 1..12 -> {
                val a = LocalTime.of(h % 12, mi)
                val b = LocalTime.of(h % 12 + 12, mi)
                h = when {
                    h in 1..5 -> b.hour
                    !a.isBefore(now) -> a.hour
                    !b.isBefore(now) -> b.hour
                    else -> a.hour
                }
            }
            else -> return null
        }
        return "%02d:%02d".format(h, mi)
    }
}

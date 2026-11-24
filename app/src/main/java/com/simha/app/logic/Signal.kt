package com.simha.app.logic

import java.time.LocalDate
import java.time.temporal.IsoFields
import java.util.Locale

data class Sig(
    val mood: Int? = null,
    val taken: Int? = null,
    val due: Int? = null,
    val pain: Map<String, Int> = emptyMap(),
    val sleep: String? = null,
    val water: String? = null,
    val activity: String? = null,
    val visitors: Int? = null,
    val ciOk: Int? = null,
    val ciMiss: Int? = null,
    val exc: Set<String> = emptySet(),
    val note: String? = null,
)

data class Rollup(val mdPct: Int?, val mood: Double?, val ciMiss: Int, val falls: Int, val pain: List<String>)

object Signal {
    val EXC = listOf("fall", "missed_ci", "missed_dose", "redflag", "esc")
    private val SLEEP = setOf("g", "ok", "p")
    private val WATER = setOf("lo", "ok")
    private val ACT = setOf("lo", "ok", "hi")
    const val NOTE_MAX = 40

    fun cleanNote(s: String?): String? =
        s?.replace('"', '\'')?.replace(Regex("\\s+"), " ")?.trim()?.take(NOTE_MAX)?.trim()?.ifEmpty { null }

    fun cleanArea(s: String): String = s.lowercase(Locale.ROOT).filter { it in 'a'..'z' }.take(12)

    fun encode(s: Sig): String {
        val p = ArrayList<String>()
        s.mood?.let { p += "m" + it.coerceIn(1, 5) }
        if (s.taken != null || s.due != null) p += "md" + (s.taken ?: 0) + "/" + (s.due ?: 0)
        val pain = s.pain.entries.mapNotNull { (k, v) -> cleanArea(k).takeIf { it.isNotEmpty() }?.let { it + v.coerceIn(1, 5) } }
        if (pain.isNotEmpty()) p += "p:" + pain.joinToString(",")
        s.sleep?.takeIf { it in SLEEP }?.let { p += "s:$it" }
        s.water?.takeIf { it in WATER }?.let { p += "w:$it" }
        s.activity?.takeIf { it in ACT }?.let { p += "a:$it" }
        s.visitors?.let { p += "v:" + it.coerceAtLeast(0) }
        if (s.ciOk != null || s.ciMiss != null) p += "ci:" + (s.ciOk ?: 0) + "/" + (s.ciMiss ?: 0)
        val x = EXC.filter { it in s.exc }
        if (x.isNotEmpty()) p += "x:" + x.joinToString(",")
        cleanNote(s.note)?.let { p += "n:\"$it\"" }
        return p.joinToString(" ")
    }

    fun decode(line: String): Sig {
        var rest = line.trim()
        var note: String? = null
        val ni = Regex("(^|\\s)n:\"").find(rest)
        if (ni != null) {
            val start = ni.range.last + 1
            val end = rest.lastIndexOf('"')
            note = if (end >= start) rest.substring(start, end) else rest.substring(start)
            rest = rest.substring(0, ni.range.first).trim()
        }
        var s = Sig(note = cleanNote(note))
        for (t in rest.split(Regex("\\s+")).filter { it.isNotEmpty() }) {
            when {
                t.startsWith("md") -> Regex("^md(\\d{1,3})/(\\d{1,3})$").find(t)?.let { s = s.copy(taken = it.groupValues[1].toInt(), due = it.groupValues[2].toInt()) }
                t.startsWith("ci:") -> Regex("^ci:(\\d{1,3})/(\\d{1,3})$").find(t)?.let { s = s.copy(ciOk = it.groupValues[1].toInt(), ciMiss = it.groupValues[2].toInt()) }
                t.startsWith("p:") -> {
                    val m = LinkedHashMap<String, Int>()
                    t.substring(2).split(',').forEach { e ->
                        Regex("^([a-z]+)([1-5])$").find(e)?.let { m[it.groupValues[1]] = it.groupValues[2].toInt() }
                    }
                    s = s.copy(pain = m)
                }
                t.startsWith("s:") -> s = s.copy(sleep = t.substring(2).takeIf { it in SLEEP })
                t.startsWith("w:") -> s = s.copy(water = t.substring(2).takeIf { it in WATER })
                t.startsWith("a:") -> s = s.copy(activity = t.substring(2).takeIf { it in ACT })
                t.startsWith("v:") -> s = s.copy(visitors = t.substring(2).toIntOrNull()?.takeIf { it in 0..999 })
                t.startsWith("x:") -> s = s.copy(exc = t.substring(2).split(',').filter { it in EXC }.toSet())
                t.startsWith("m") -> s = s.copy(mood = t.substring(1).toIntOrNull()?.takeIf { it in 1..5 })
            }
        }
        return s
    }

    fun merge(a: Sig, b: Sig): Sig = Sig(
        mood = b.mood ?: a.mood,
        taken = b.taken ?: a.taken,
        due = b.due ?: a.due,
        pain = a.pain + b.pain,
        sleep = b.sleep ?: a.sleep,
        water = b.water ?: a.water,
        activity = b.activity ?: a.activity,
        visitors = b.visitors ?: a.visitors,
        ciOk = b.ciOk ?: a.ciOk,
        ciMiss = b.ciMiss ?: a.ciMiss,
        exc = a.exc + b.exc,
        note = b.note ?: a.note,
    )

    fun summary(s: Sig): String {
        val p = ArrayList<String>()
        s.mood?.let { p += "mood $it/5" }
        if (s.taken != null || s.due != null) p += "medicines " + (s.taken ?: 0) + "/" + (s.due ?: 0)
        if (s.pain.isNotEmpty()) p += "pain " + s.pain.entries.joinToString(", ") { it.key + " " + it.value }
        s.sleep?.let { p += "sleep " + mapOf("g" to "good", "ok" to "ok", "p" to "poor")[it] }
        s.water?.let { p += "water " + mapOf("lo" to "low", "ok" to "ok")[it] }
        s.activity?.let { p += "activity " + mapOf("lo" to "low", "ok" to "ok", "hi" to "high")[it] }
        s.visitors?.let { p += "visitors $it" }
        if (s.ciOk != null || s.ciMiss != null) p += "check-ins " + (s.ciOk ?: 0) + " ok, " + (s.ciMiss ?: 0) + " missed"
        if (s.exc.isNotEmpty()) p += "alerts " + EXC.filter { it in s.exc }.joinToString(", ")
        s.note?.let { p += it }
        return p.joinToString("; ")
    }

    fun rollup(days: List<Sig>): String {
        val taken = days.sumOf { it.taken ?: 0 }
        val due = days.sumOf { it.due ?: 0 }
        val moods = days.mapNotNull { it.mood }
        val pain = days.flatMap { it.pain.keys }.distinct().take(3)
        val p = ArrayList<String>()
        if (due > 0) p += "md:" + (taken * 100 / due).coerceAtMost(100) + "%"
        if (moods.isNotEmpty()) p += "m:" + String.format(Locale.US, "%.1f", moods.average())
        p += "ci_miss:" + days.sumOf { it.ciMiss ?: 0 }
        p += "falls:" + days.count { "fall" in it.exc }
        if (pain.isNotEmpty()) p += "pain:" + pain.joinToString(",")
        return p.joinToString(" ")
    }

    fun parseRollup(t: String): Rollup {
        var md: Int? = null
        var m: Double? = null
        var cm = 0
        var f = 0
        var pain = emptyList<String>()
        for (tok in t.trim().split(Regex("\\s+"))) {
            val i = tok.indexOf(':')
            if (i < 0) continue
            val v = tok.substring(i + 1)
            when (tok.substring(0, i)) {
                "md" -> md = v.removeSuffix("%").toIntOrNull()
                "m" -> m = v.toDoubleOrNull()
                "ci_miss" -> cm = v.toIntOrNull() ?: 0
                "falls" -> f = v.toIntOrNull() ?: 0
                "pain" -> pain = v.split(',').filter { it.isNotEmpty() }
            }
        }
        return Rollup(md, m, cm, f, pain)
    }

    fun weekKey(d: LocalDate): String =
        String.format(Locale.US, "%d-W%02d", d.get(IsoFields.WEEK_BASED_YEAR), d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
}

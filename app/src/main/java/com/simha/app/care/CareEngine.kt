package com.simha.app.care

import android.content.Context
import com.simha.app.R
import com.simha.app.ServiceLocator
import com.simha.app.data.Medicine
import com.simha.app.data.Weather
import com.simha.app.data.jo
import com.simha.app.llm.DiaryMerge
import com.simha.app.logic.FirstAid
import com.simha.app.wake.Wake
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID

class CareEngine(private val app: Context, private val sl: ServiceLocator) {
    val forms = FormStack()
    private val said = mutableListOf<String>()
    private val flags = mutableSetOf<Flag>()
    var urgent = false
        private set

    fun newTurn() {
        said.clear()
        urgent = false
    }

    fun reset() {
        forms.clear()
        said.clear()
        flags.clear()
        urgent = false
    }

    fun reply(): String? = said.joinToString(" ").ifBlank { null }

    fun openFormTypes(): Set<TaskType> = forms.forms.map { it.type }.toSet()

    private fun openFormLines(): List<String> = forms.forms.map { f ->
        f.type.key + " " + f.slots.entries.joinToString(", ") { it.key + "=" + it.value } + " (still needs " + (f.missing() ?: "nothing") + ")"
    }

    private fun JsonObject.s(k: String): String? = this[k]?.jsonPrimitive?.contentOrNull?.trim()?.ifEmpty { null }

    suspend fun note(args: JsonObject, lc: Context): String {
        val type = TaskType.of(args.s("type")?.lowercase() ?: "") ?: return err("type must be medicine, reminder, symptom or intake")
        val now = LocalTime.now()
        val slots = mutableMapOf<String, String>()
        listOf("name", "dose", "what", "since", "severity", "kind", "amount", "repeat").forEach { k -> args.s(k)?.let { slots[k] = it.take(120) } }
        args.s("time")?.let { raw -> Clock.resolve(raw, now)?.let { slots["time"] = it } }
        slots["kind"]?.let { slots["kind"] = it.lowercase() }
        forms.offer(type, slots)
        val done = mutableListOf<String>()
        var ask: String? = null
        for (step in forms.drain()) {
            when (step) {
                is Step.Done -> done += execute(step.form, lc)
                is Step.Ask -> ask = question(step.form.type, step.slot, lc).also { said += it }
            }
        }
        return buildJsonObject {
            put("status", if (ask == null) "saved" else "need_more")
            if (done.isNotEmpty()) put("done", done.joinToString(" "))
            ask?.let { put("ask", it) }
            put("instruction", if (ask == null) "Saved. Confirm it in a few words only if you are replying." else "If you reply, ask exactly this question and nothing else.")
        }.toString()
    }

    private fun err(m: String) = buildJsonObject { put("error", m) }.toString()

    private fun question(t: TaskType, slot: String, lc: Context): String = lc.getString(
        when (t to slot) {
            TaskType.Medicine to "name" -> R.string.cf_ask_med_name
            TaskType.Medicine to "time" -> R.string.cf_ask_med_time
            TaskType.Reminder to "what" -> R.string.cf_ask_rem_what
            TaskType.Reminder to "time" -> R.string.cf_ask_rem_time
            TaskType.Symptom to "since" -> R.string.cf_ask_sym_since
            TaskType.Symptom to "severity" -> R.string.cf_ask_sym_sev
            else -> R.string.cf_ask_amount
        }
    )

    private fun show(hhmm: String, lang: String): String {
        val t = LocalTime.parse(hhmm)
        return runCatching { t.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.forLanguageTag(lang))) }.getOrDefault(hhmm)
    }

    private suspend fun facts(): CareFacts {
        val p = sl.repo.profileNow()
        val meds = sl.guardian.medicines().first().joinToString(", ") { it.name }
        val yPain = sl.guardian.diaryFor(LocalDate.now().minusDays(1))?.sig?.pain?.values?.maxOrNull()
        val heat = runCatching { Weather.isHeatDay(app) }.getOrDefault(false)
        return CareFacts(p.conditions, p.medicines + ", " + meds, heat, LocalTime.now().hour, yPain?.let { it * 2 })
    }

    private suspend fun execute(f: Form, lc: Context): String {
        val lang = sl.prefs.snap().lang
        val s = f.slots
        return when (f.type) {
            TaskType.Medicine -> {
                val name = s["name"]!!.trim()
                val time = s["time"]!!
                val g = sl.guardian
                val old = g.medicines().first().firstOrNull { it.name.equals(name, true) }
                val times = ((old?.times ?: emptyList()) + time).distinct().sorted()
                g.saveMedicine(Medicine(old?.id ?: UUID.randomUUID().toString(), old?.name ?: name, s["dose"] ?: old?.dose ?: "", times, old?.stock, true))
                Wake.scheduleAll(app)
                sl.graph.upsert(NodeType.Medicine, name, 2)
                said += lc.getString(R.string.cf_done_med, name, show(time, lang))
                allergyClash(name)?.let { said += lc.getString(R.string.cf_allergy, name, it) }
                "Medicine $name saved at $time with a reminder."
            }
            TaskType.Reminder -> {
                val what = s["what"]!!
                val time = s["time"]!!
                val now = LocalDateTime.now()
                var at = now.toLocalDate().atTime(LocalTime.parse(time))
                if (!at.isAfter(now)) at = at.plusDays(1)
                Wake.scheduleFollowUp(app, at.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), what)
                sl.repo.audit("reminder_set", jo("at" to time))
                said += lc.getString(R.string.cf_done_rem, what, show(time, lang))
                "Reminder saved for $time."
            }
            TaskType.Symptom -> {
                val what = s["what"]!!
                val detail = what + " " + (s["severity"] ?: "")
                sl.repo.logEvent("help", jo("kind" to "symptom", "text" to what, "since" to s["since"], "severity" to s["severity"]))
                val c = sl.graph.upsert(NodeType.Concern, what, 3)
                val n = sl.graph.upsert(NodeType.Event, "felt $what on " + LocalDate.now(), 1)
                sl.graph.link(c, "noted_on", n)
                val sit = FirstAid.match(detail)
                if (sit != null && FirstAid.dangerous(sit, detail)) urgent = true
                Wake.scheduleFollowUp(app, System.currentTimeMillis() + 3 * 3_600_000L, lc.getString(R.string.cf_followup, what))
                said += lc.getString(R.string.cf_done_sym)
                "Symptom noted. A follow-up is scheduled in 3 hours."
            }
            TaskType.Intake -> {
                val kind = s["kind"]!!
                val amount = s["amount"]!!
                val key = when (kind) { "meal", "food" -> "meals"; "activity", "walk" -> "activities"; else -> kind }
                runCatching { sl.guardian.upsertDiary(LocalDate.now(), DiaryMerge.toSig(jo(key to amount))) }
                val flag = Checks.intake(kind, amount, facts())
                if (flag != null) {
                    flags += flag
                    sl.graph.upsert(NodeType.Concern, flag.name.lowercase(), 2)
                }
                said += lc.getString(
                    when (flag) {
                        Flag.WaterLow -> R.string.cf_water_low
                        Flag.WaterHigh -> R.string.cf_water_high
                        Flag.SleepShort -> R.string.cf_sleep
                        Flag.PainUp -> R.string.cf_pain
                        Flag.MoodLow -> R.string.cf_mood
                        Flag.MealSkipped -> R.string.cf_meal
                        null -> R.string.cf_done_intake
                    }
                )
                if (flag == Flag.WaterLow) Wake.scheduleFollowUp(app, System.currentTimeMillis() + 90 * 60_000L, lc.getString(R.string.cf_water_low))
                "Saved $kind: $amount." + (flag?.let { " Flag: ${it.name}." } ?: "")
            }
        }
    }

    private suspend fun allergyClash(name: String): String? {
        val allergies = sl.repo.profileNow().allergies.split(',', ';').map { it.trim() }.filter { it.length >= 4 }
        val n = name.lowercase()
        return allergies.firstOrNull { a -> val x = a.lowercase(); n.contains(x) || x.contains(n) || (x.length >= 5 && n.contains(x.take(5))) }
    }

    suspend fun brief(now: LocalDateTime = LocalDateTime.now()): List<String> {
        val snap = sl.prefs.snap()
        val ms = System.currentTimeMillis()
        fun ago(t: Long): String {
            if (t <= 0) return "never"
            val m = (ms - t) / 60_000
            return when {
                m < 1 -> "just now"
                m < 60 -> "$m minutes ago"
                m < 1440 -> "${m / 60} h ${m % 60} min ago"
                else -> "${m / 1440} days ago"
            }
        }
        val out = mutableListOf<String>()
        out += "Now: " + now.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy, HH:mm", Locale.ENGLISH))
        out += "Last check-in answered fine: " + ago(snap.lastCheckinOk)
        out += "Phone last used: " + ago(snap.lastActivity)
        out += "Last talked with Simha: " + ago(CareStore.lastTalk(app))
        if (runCatching { Weather.isHeatDay(app) }.getOrDefault(false)) out += "Weather: heat day, risk of dehydration"
        if (flags.isNotEmpty()) out += "Watch today: " + flags.joinToString(", ") { it.name }
        if (forms.forms.isNotEmpty()) out += "Unfinished tasks: " + openFormLines().joinToString("; ")
        return out
    }
}

object CareStore {
    private fun p(ctx: Context) = ctx.getSharedPreferences("care", Context.MODE_PRIVATE)
    fun lastTalk(ctx: Context): Long = p(ctx).getLong("last_talk", 0L)
    fun talked(ctx: Context) = p(ctx).edit().putLong("last_talk", System.currentTimeMillis()).apply()
    fun lastMaint(ctx: Context): Long = p(ctx).getLong("last_graph_maint", 0L)
    fun maintained(ctx: Context) = p(ctx).edit().putLong("last_graph_maint", System.currentTimeMillis()).apply()
}

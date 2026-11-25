package com.simha.app.care

import java.time.LocalTime

enum class TaskType(val key: String, val required: List<String>) {
    Medicine("medicine", listOf("name", "time")),
    Reminder("reminder", listOf("what", "time")),
    Symptom("symptom", listOf("what", "since", "severity")),
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

class FormStack {
    private val open = mutableListOf<Form>()

    val forms: List<Form> get() = open.toList()

    fun clear() = open.clear()

    fun offer(type: TaskType, slots: Map<String, String>): Form {
        val clean = slots.filterValues { it.isNotBlank() }.mapValues { it.value.trim() }
        val target = open.lastOrNull { it.type == type && !it.complete && compatible(it, clean) }
        val f = target ?: Form(type).also { open += it }
        f.slots.putAll(clean)
        return f
    }

    fun drain(): List<Step> {
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

    fun pending(): Pair<TaskType, String>? = open.firstOrNull()?.let { f -> f.missing()?.let { f.type to it } }

    private fun compatible(f: Form, s: Map<String, String>): Boolean =
        s.all { (k, v) -> f.slots[k].isNullOrBlank() || f.slots[k].equals(v, true) }
}

object Clock {
    private val re = Regex("""^\s*(\d{1,2})(?:[:.](\d{2}))?\s*(am|pm|a\.m\.|p\.m\.)?\s*$""", RegexOption.IGNORE_CASE)

    fun resolve(raw: String, now: LocalTime): String? {
        val m = re.matchEntire(raw) ?: return null
        var h = m.groupValues[1].toInt()
        val mi = m.groupValues[2].ifEmpty { "0" }.toInt()
        if (mi !in 0..59) return null
        val ap = m.groupValues[3].lowercase().replace(".", "")
        when {
            ap == "am" -> { if (h !in 1..12) return null; if (h == 12) h = 0 }
            ap == "pm" -> { if (h !in 1..12) return null; if (h != 12) h += 12 }
            h == 0 || h in 13..23 -> Unit
            h in 1..12 -> {
                val am = LocalTime.of(h % 12, mi)
                val pm = LocalTime.of(h % 12 + 12, mi)
                h = when {
                    h in 1..5 -> pm.hour
                    !am.isBefore(now) -> am.hour
                    !pm.isBefore(now) -> pm.hour
                    else -> am.hour
                }
            }
            else -> return null
        }
        return "%02d:%02d".format(h, mi)
    }
}

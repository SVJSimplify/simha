package com.simha.app.llm

import com.simha.app.logic.Sig
import com.simha.app.logic.Signal
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

object DiaryMerge {
    val listKeys = listOf("meals", "activities", "visitors", "notes")
    val scalarKeys = listOf("water", "mood", "pain", "sleep")
    val allKeys = listKeys + scalarKeys

    private fun items(e: JsonElement?): List<String> = when (e) {
        null -> emptyList()
        is JsonArray -> e.mapNotNull { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull()?.trim()?.ifEmpty { null } }
        else -> runCatching { e.jsonPrimitive.contentOrNull.orEmpty() }.getOrDefault("").split(',', ';', '\n').map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun update(args: Map<String, String?>): JsonObject {
        val m = linkedMapOf<String, JsonElement>()
        for (k in listKeys) {
            val v = args[k]?.trim().orEmpty()
            if (v.isNotEmpty()) m[k] = JsonArray(items(JsonPrimitive(v)).map { JsonPrimitive(it) })
        }
        for (k in scalarKeys) {
            val v = args[k]?.trim().orEmpty()
            if (v.isNotEmpty()) m[k] = JsonPrimitive(v.take(200))
        }
        return JsonObject(m)
    }

    private val painStop = setOf(
        "in", "my", "the", "a", "an", "some", "mild", "slight", "slightly", "bad", "severe", "very", "pain", "sore", "hurts", "hurt", "ache",
        "aching", "little", "is", "at", "and", "on", "of", "has", "have", "been", "terrible", "unbearable", "no", "none", "feels", "feeling",
    )

    private fun has(t: String, vararg w: String) = w.any { t.contains(it) }

    fun toSig(u: JsonObject): Sig {
        fun txt(k: String) = items(u[k]).joinToString(" ").lowercase()
        val mt = txt("mood")
        val mood = Regex("[1-5]").find(mt)?.value?.toInt() ?: when {
            mt.isBlank() -> null
            has(mt, "great", "excellent", "very happy", "joy") -> 5
            has(mt, "awful", "terrible", "depress", "very bad") -> 1
            has(mt, "happy", "good", "fine", "well", "better", "calm", "cheerful") -> 4
            has(mt, "sad", "tired", "low", "worried", "lonely", "bad", "upset") -> 2
            else -> 3
        }
        val pt = txt("pain")
        val pain = if (pt.isBlank() || pt == "no" || has(pt, "none", "no pain")) emptyMap() else {
            val area = Regex("[a-z]+").findAll(pt).map { it.value }.firstOrNull { it !in painStop }.orEmpty().ifEmpty { "pain" }
            val lvl = Regex("[1-5]").find(pt)?.value?.toInt() ?: when {
                has(pt, "severe", "very", "bad", "terrible", "unbearable") -> 3
                has(pt, "mild", "slight", "little") -> 1
                else -> 2
            }
            mapOf(Signal.cleanArea(area).ifEmpty { "pain" } to lvl)
        }
        val st = txt("sleep")
        val sleep = when {
            st.isBlank() -> null
            has(st, "poor", "bad", "restless", "little", "not", "insomnia", "awake", "less") -> "p"
            has(st, "good", "well", "fine", "great", "deep", "sound") -> "g"
            else -> "ok"
        }
        val wt = txt("water")
        val water = when {
            wt.isBlank() -> null
            Regex("[0-9]+").find(wt) != null -> if (Regex("[0-9]+").find(wt)!!.value.toInt() < 5) "lo" else "ok"
            has(wt, "low", "less", "little", "not", "few", "barely") -> "lo"
            else -> "ok"
        }
        val at = txt("activities")
        val act = when {
            at.isBlank() -> null
            has(at, "tv", "rest", "sit", "bed", "nothing", "sleep") -> "lo"
            has(at, "walk", "exercise", "yoga", "garden", "gym", "run", "dance", "jog", "cycl") -> "hi"
            else -> "ok"
        }
        val visitors = items(u["visitors"]).size.takeIf { it > 0 }
        val note = items(u["notes"]).joinToString("; ").ifBlank { null }
        return Sig(mood = mood, pain = pain, sleep = sleep, water = water, activity = act, visitors = visitors, note = note)
    }
}

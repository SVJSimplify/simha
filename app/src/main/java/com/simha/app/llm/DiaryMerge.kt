package com.simha.app.llm

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

    fun merge(existing: JsonObject?, update: JsonObject): JsonObject {
        val m = linkedMapOf<String, JsonElement>()
        existing?.forEach { (k, v) -> m[k] = v }
        for ((k, v) in update) {
            if (k in listKeys) {
                val old = items(m[k])
                val seen = old.map { it.lowercase() }.toMutableSet()
                val add = items(v).filter { seen.add(it.lowercase()) }
                m[k] = JsonArray((old + add).map { JsonPrimitive(it) })
            } else {
                m[k] = v
            }
        }
        return JsonObject(m)
    }

    fun withDerived(d: JsonObject): JsonObject {
        val m = LinkedHashMap<String, JsonElement>(d)
        m.remove("activity")
        m.remove("summary")
        val act = items(d["activities"]).joinToString(", ")
        if (act.isNotEmpty()) m["activity"] = JsonPrimitive(act)
        val s = summary(JsonObject(m))
        if (s.isNotEmpty()) m["summary"] = JsonPrimitive(s)
        return JsonObject(m)
    }

    fun summary(d: JsonObject): String {
        val parts = mutableListOf<String>()
        fun add(label: String, k: String) {
            val v = items(d[k]).joinToString(", ")
            if (v.isNotBlank()) parts += "$label: $v"
        }
        add("Mood", "mood")
        add("Pain", "pain")
        add("Sleep", "sleep")
        add("Water", "water")
        add("Meals", "meals")
        add("Activities", "activities")
        add("Visitors", "visitors")
        add("Notes", "notes")
        return parts.joinToString(". ")
    }
}

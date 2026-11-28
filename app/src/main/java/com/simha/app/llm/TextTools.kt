package com.simha.app.llm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object Speakable {
    private val markers = listOf("<tool", "<|", "```", "[tool", "{\"name\"", "{\"tool\"", "{ \"name\"", "{ \"tool\"", "tool_code", "print(")

    fun of(raw: String): String {
        var s = raw.replace(Regex("(?s)<think>.*?</think>"), " ").replace(Regex("(?s)<thinking>.*?</thinking>"), " ")
        listOf("<think>", "<thinking>").forEach { t -> s.indexOf(t).takeIf { it >= 0 }?.let { s = s.substring(0, it) } }
        val cut = markers.mapNotNull { m -> s.indexOf(m).takeIf { it >= 0 } }.minOrNull()
        if (cut != null) s = s.substring(0, cut)
        val t = s.trimStart()
        if (t.startsWith("{") || t.startsWith("[{")) return ""
        return s
    }

    fun clean(s: String): String = s
        .replace(Regex("\\*\\*|__|`"), "")
        .replace(Regex("(?m)^\\s*(#{1,6}|[-*•]|\\d+[.)])\\s+"), "")
        .replace(Regex("[*_~]"), "")
        .replace(Regex("[\\x{1F300}-\\x{1FAFF}\\x{2600}-\\x{27BF}\\x{FE0F}]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

object TextTools {
    private val blocks = listOf(
        Regex("(?s)<tool_call>(.*?)</tool_call>"),
        Regex("(?s)<tool>(.*?)</tool>"),
        Regex("(?s)```(?:json|tool_code|tool|javascript)?\\s*(.*?)```"),
    )

    fun parse(text: String, known: Set<String>): List<ToolCall> {
        val found = mutableListOf<ToolCall>()
        val chunks = blocks.flatMap { r -> r.findAll(text).map { it.groupValues[1] } }.ifEmpty { listOf(text) }
        chunks.forEach { c -> objects(c).forEach { o -> toCall(o, known, found.size)?.let { found += it } } }
        if (found.isEmpty()) {
            chunks.forEach { c ->
                Regex("""\b([a-z_]{3,40})\s*\((.*?)\)""").findAll(c).forEach { m ->
                    val n = m.groupValues[1]
                    if (n in known) found += ToolCall("call_t" + found.size, n, pyArgs(m.groupValues[2]))
                }
            }
        }
        return found
    }

    private fun toCall(o: JsonObject, known: Set<String>, i: Int): ToolCall? {
        val inner = (o["function"] as? JsonObject) ?: o
        val name = (inner["name"] ?: inner["tool"] ?: inner["tool_name"])?.let { (it as? JsonPrimitive)?.contentOrNull } ?: return null
        if (name !in known) return null
        val args: JsonElement? = inner["arguments"] ?: inner["parameters"] ?: inner["args"] ?: inner["input"]
        val a = when (args) {
            null -> "{}"
            is JsonPrimitive -> args.contentOrNull ?: "{}"
            else -> args.toString()
        }
        return ToolCall("call_t$i", name, a)
    }

    fun objects(s: String): List<JsonObject> {
        val out = mutableListOf<JsonObject>()
        var i = 0
        while (i < s.length) {
            val st = s.indexOf('{', i)
            if (st < 0) break
            var depth = 0
            var inStr = false
            var esc = false
            var end = -1
            for (j in st until s.length) {
                val ch = s[j]
                if (inStr) {
                    if (esc) esc = false else if (ch == '\\') esc = true else if (ch == '"') inStr = false
                    continue
                }
                when (ch) {
                    '"' -> inStr = true
                    '{' -> depth++
                    '}' -> { depth--; if (depth == 0) { end = j; break } }
                }
            }
            if (end < 0) break
            val el = runCatching { Json.parseToJsonElement(s.substring(st, end + 1)) }.getOrNull()
            if (el is JsonObject) out += el
            i = end + 1
        }
        return out
    }

    private fun pyArgs(s: String): String {
        val m = Regex("""(\w+)\s*=\s*("(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*'|[\w.\-]+)""").findAll(s)
        val parts = m.map { r ->
            val v = r.groupValues[2]
            val js = when {
                v.startsWith("'") -> "\"" + v.substring(1, v.length - 1).replace("\"", "\\\"") + "\""
                v.startsWith("\"") -> v
                v == "True" || v == "true" -> "true"
                v == "False" || v == "false" -> "false"
                v.toDoubleOrNull() != null -> v
                else -> "\"" + v + "\""
            }
            "\"" + r.groupValues[1] + "\":" + js
        }.toList()
        return "{" + parts.joinToString(",") + "}"
    }

    fun describe(tools: JsonArray): String = tools.joinToString("\n") { t ->
        val f = t.jsonObject["function"]!!.jsonObject
        val props = f["parameters"]?.jsonObject?.get("properties")?.jsonObject
        val ps = props?.entries?.joinToString(", ") { (k, v) -> k + ": " + (v.jsonObject["description"]?.jsonPrimitive?.contentOrNull ?: "") }.orEmpty()
        "- " + f["name"]!!.jsonPrimitive.content + "(" + ps + "): " + (f["description"]?.jsonPrimitive?.contentOrNull ?: "")
    }

    const val PROTOCOL = "To use an ability, reply with only this and nothing else: <tool>{\"name\": \"ability_name\", \"arguments\": {...}}</tool>. You will then get the result and can answer the person. When you are not using an ability, reply normally in plain spoken sentences."
}

object Compat {
    private fun role(o: JsonObject) = o["role"]?.jsonPrimitive?.contentOrNull ?: ""
    private fun text(o: JsonObject): String = when (val c = o["content"]) {
        is JsonPrimitive -> c.contentOrNull ?: ""
        is JsonArray -> c.mapNotNull { (it as? JsonObject)?.get("text")?.let { t -> (t as? JsonPrimitive)?.contentOrNull } }.joinToString(" ")
        else -> ""
    }

    private fun msg(r: String, t: String) = kotlinx.serialization.json.buildJsonObject {
        put("role", JsonPrimitive(r))
        put("content", JsonPrimitive(t))
    }

    fun noTools(messages: JsonArray, tools: JsonArray): JsonArray {
        val names = mutableMapOf<String, String>()
        val out = mutableListOf<JsonObject>()
        val guide = "\n\nAbilities you can use:\n" + TextTools.describe(tools) + "\n" + TextTools.PROTOCOL
        var sysDone = false
        messages.forEach { e ->
            val o = e.jsonObject
            when (role(o)) {
                "system" -> { out += msg("system", text(o) + guide); sysDone = true }
                "assistant" -> {
                    val calls = o["tool_calls"] as? JsonArray
                    if (calls == null || calls.isEmpty()) out += msg("assistant", text(o)) else {
                        val parts = calls.map { c ->
                            val co = c.jsonObject
                            val f = co["function"]!!.jsonObject
                            val n = f["name"]!!.jsonPrimitive.content
                            co["id"]?.jsonPrimitive?.contentOrNull?.let { names[it] = n }
                            val a = f["arguments"]?.let { if (it is JsonPrimitive) it.contentOrNull ?: "{}" else it.toString() } ?: "{}"
                            "<tool>{\"name\": \"$n\", \"arguments\": $a}</tool>"
                        }
                        out += msg("assistant", (text(o) + " " + parts.joinToString(" ")).trim())
                    }
                }
                "tool" -> {
                    val n = o["tool_call_id"]?.jsonPrimitive?.contentOrNull?.let { names[it] } ?: "the ability"
                    out += msg("user", "Result of $n: " + text(o) + "\nNow answer the person in plain spoken words, or use another ability.")
                }
                else -> out += o
            }
        }
        if (!sysDone) out.add(0, msg("system", guide.trim()))
        return JsonArray(out)
    }

    fun noSystem(messages: JsonArray): JsonArray {
        val sys = messages.map { it.jsonObject }.filter { role(it) == "system" }.joinToString("\n\n") { text(it) }
        val rest = messages.map { it.jsonObject }.filter { role(it) != "system" }.toMutableList()
        if (sys.isBlank()) return JsonArray(rest)
        val i = rest.indexOfFirst { role(it) == "user" }
        if (i < 0) rest.add(0, msg("user", "Instructions:\n$sys")) else rest[i] = msg("user", "Instructions:\n$sys\n\n" + text(rest[i]))
        return JsonArray(rest)
    }

    fun alternate(messages: JsonArray): JsonArray {
        val out = mutableListOf<JsonObject>()
        messages.forEach { e ->
            val o = e.jsonObject
            val last = out.lastOrNull()
            if (last != null && role(last) == role(o) && o["tool_calls"] == null && last["tool_calls"] == null && o["content"] is JsonPrimitive && last["content"] is JsonPrimitive) {
                out[out.lastIndex] = msg(role(o), text(last) + "\n" + text(o))
            } else out += o
        }
        if (out.firstOrNull()?.let { role(it) } == "assistant") out.add(0, msg("user", "Hello"))
        val sysFirst = out.firstOrNull()?.let { role(it) } == "system"
        if (sysFirst && out.getOrNull(1)?.let { role(it) } == "assistant") out.add(1, msg("user", "Hello"))
        return JsonArray(out)
    }
}

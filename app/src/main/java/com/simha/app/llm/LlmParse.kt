package com.simha.app.llm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class LlmException(message: String) : Exception(message)

data class ToolCall(val id: String, val name: String, val arguments: String)

data class ParsedReply(val content: String?, val toolCalls: List<ToolCall>)

object LlmParse {
    fun errorText(e: JsonElement): String = when (e) {
        is JsonObject -> e["message"]?.jsonPrimitive?.contentOrNull ?: e.toString()
        is JsonPrimitive -> e.contentOrNull ?: "unknown error"
        else -> e.toString()
    }

    fun httpError(status: Int, body: String): String {
        val detail = try {
            val o = Json.parseToJsonElement(body).jsonObject
            o["error"]?.let { errorText(it) } ?: o["detail"]?.toString() ?: o["message"]?.jsonPrimitive?.contentOrNull
        } catch (e: Exception) {
            null
        }
        return "HTTP " + status + ": " + (detail ?: body.take(200)).take(300)
    }

    private fun contentText(c: JsonElement?): String? = when (c) {
        null, JsonNull -> null
        is JsonPrimitive -> c.contentOrNull
        is JsonArray -> c.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }.joinToString("")
        else -> null
    }

    fun parse(body: String): ParsedReply {
        val root = try {
            Json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            throw LlmException("Reply was not valid JSON")
        }
        root["error"]?.let { if (it !is JsonNull) throw LlmException(errorText(it)) }
        val msg = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject
            ?: throw LlmException("Reply had no message")
        val calls = msg["tool_calls"]?.takeIf { it !is JsonNull }?.jsonArray?.mapIndexed { i, tc ->
            val t = tc.jsonObject
            val f = t["function"]?.jsonObject ?: throw LlmException("Tool call had no function")
            val args = f["arguments"]
            ToolCall(
                id = t["id"]?.jsonPrimitive?.contentOrNull?.ifEmpty { null } ?: ("call_" + i),
                name = f["name"]?.jsonPrimitive?.contentOrNull ?: throw LlmException("Tool call had no name"),
                arguments = when (args) {
                    null, JsonNull -> "{}"
                    is JsonPrimitive -> args.contentOrNull ?: "{}"
                    else -> args.toString()
                },
            )
        } ?: emptyList()
        return ParsedReply(contentText(msg["content"]), calls)
    }
}

class StreamAcc {
    private class CallAcc {
        var id: String = ""
        var name: String = ""
        val args = StringBuilder()
    }

    var done = false
        private set
    private val text = StringBuilder()
    private val calls = sortedMapOf<Int, CallAcc>()

    fun feed(line: String): String? {
        val l = line.trim()
        if (!l.startsWith("data:")) return null
        val d = l.removePrefix("data:").trim()
        if (d == "[DONE]") {
            done = true
            return null
        }
        if (d.isEmpty()) return null
        val o = try {
            Json.parseToJsonElement(d).jsonObject
        } catch (e: Exception) {
            throw LlmException("Stream chunk was not valid JSON")
        }
        o["error"]?.let { if (it !is JsonNull) throw LlmException(LlmParse.errorText(it)) }
        val delta = o["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("delta")?.takeIf { it !is JsonNull }?.jsonObject
            ?: return null
        delta["tool_calls"]?.takeIf { it !is JsonNull }?.jsonArray?.forEach { tc ->
            val t = tc.jsonObject
            val idx = t["index"]?.jsonPrimitive?.intOrNull ?: 0
            val e = calls.getOrPut(idx) { CallAcc() }
            t["id"]?.jsonPrimitive?.contentOrNull?.let { if (it.isNotEmpty()) e.id = it }
            t["function"]?.jsonObject?.let { f ->
                f["name"]?.jsonPrimitive?.contentOrNull?.let { if (it.isNotEmpty()) e.name = it }
                f["arguments"]?.jsonPrimitive?.contentOrNull?.let { e.args.append(it) }
            }
        }
        val c = delta["content"]?.jsonPrimitive?.contentOrNull
        if (c != null) text.append(c)
        return c
    }

    fun result(): ParsedReply = ParsedReply(
        text.toString().ifEmpty { null },
        calls.entries.map { (i, c) ->
            ToolCall(c.id.ifEmpty { "call_" + i }, c.name, c.args.toString().ifBlank { "{}" })
        },
    )
}

object SentenceSplitter {
    fun split(buf: String): Pair<List<String>, String> {
        val out = mutableListOf<String>()
        var start = 0
        for (i in buf.indices) {
            val c = buf[i]
            val boundary = c == '\n' ||
                ((c == '.' || c == '!' || c == '?' || c == '।') && i + 1 < buf.length && buf[i + 1].isWhitespace())
            if (boundary) {
                val s = buf.substring(start, i + 1).trim()
                if (s.length >= 2) out += s
                start = i + 1
            }
        }
        return out to buf.substring(start)
    }
}

object History {
    fun trim(h: List<JsonObject>, maxUserTurns: Int = 20): List<JsonObject> {
        val users = h.indices.filter { h[it]["role"]?.jsonPrimitive?.contentOrNull == "user" }
        if (users.size <= maxUserTurns) return h
        return h.subList(users[users.size - maxUserTurns], h.size)
    }
}

object Msg {
    fun system(t: String): JsonObject = buildJsonObject {
        put("role", "system")
        put("content", t)
    }

    fun user(t: String): JsonObject = buildJsonObject {
        put("role", "user")
        put("content", t)
    }

    fun assistant(t: String): JsonObject = buildJsonObject {
        put("role", "assistant")
        put("content", t)
    }

    fun tool(id: String, t: String): JsonObject = buildJsonObject {
        put("role", "tool")
        put("tool_call_id", id)
        put("content", t)
    }

    fun assistantCalls(content: String?, calls: List<ToolCall>): JsonObject = buildJsonObject {
        put("role", "assistant")
        put("content", content)
        put("tool_calls", JsonArray(calls.map {
            buildJsonObject {
                put("id", it.id)
                put("type", "function")
                put("function", buildJsonObject {
                    put("name", it.name)
                    put("arguments", it.arguments)
                })
            }
        }))
    }

    fun userWithImage(text: String, jpegBase64: String): JsonObject = buildJsonObject {
        put("role", "user")
        put("content", JsonArray(listOf(
            buildJsonObject {
                put("type", "text")
                put("text", text)
            },
            buildJsonObject {
                put("type", "image_url")
                put("image_url", buildJsonObject { put("url", "data:image/jpeg;base64," + jpegBase64) })
            },
        )))
    }
}

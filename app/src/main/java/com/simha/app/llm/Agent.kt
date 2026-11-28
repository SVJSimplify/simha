package com.simha.app.llm

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface ChatApi {
    suspend fun chat(messages: JsonArray, tools: JsonArray, onDelta: (String) -> Unit): ParsedReply
}

object Agent {
    const val MAX_STEPS = 5

    suspend fun run(
        api: ChatApi,
        system: suspend () -> String,
        history: MutableList<JsonObject>,
        host: ToolHost,
        onSentence: (String) -> Unit,
        onToolStart: () -> Unit,
        slowMs: Long = 2500,
        onSlow: () -> Unit = {},
        tools: List<String>? = null,
        codeReply: () -> String? = { null },
        codeAsks: () -> Boolean = { false },
    ): String {
        val active = tools?.toMutableSet()
        var spokeAny = false
        val say: (String) -> Unit = { s ->
            val c = Speakable.clean(s)
            if (c.length >= 2) {
                spokeAny = true
                onSentence(c)
            }
        }
        for (step in 0..MAX_STEPS) {
            val last = step == MAX_STEPS
            val sb = StringBuilder()
            var used = 0
            var buf = ""
            val msgs = JsonArray(listOf(Msg.system(system())) + History.trim(history))
            val defs = if (last) JsonArray(emptyList()) else active?.let { SimhaTools.definitionsFor(it) } ?: SimhaTools.definitions
            val r = api.chat(msgs, defs) { d ->
                sb.append(d)
                val sp = Speakable.of(sb.toString())
                if (sp.length > used) {
                    val (done, rest) = SentenceSplitter.split(buf + sp.substring(used))
                    used = sp.length
                    buf = rest
                    done.forEach(say)
                }
            }
            val raw = r.content ?: sb.toString()
            val calls = if (last) emptyList() else r.toolCalls.ifEmpty { TextTools.parse(raw, SimhaTools.allNames) }
            val spoken = Speakable.of(raw)
            if (spoken.length > used) buf += spoken.substring(used)
            if (buf.isNotBlank()) say(buf.trim())
            val text = Speakable.clean(spoken)
            if (calls.isEmpty()) {
                val t = text.ifBlank { codeReply() ?: "" }
                if (t.isBlank() && last) throw LlmException("No answer")
                if (text.isBlank() && t.isNotBlank()) say(t)
                history += Msg.assistant(t)
                return t
            }
            history += Msg.assistantCalls(text.ifBlank { null }, calls)
            onToolStart()
            calls.forEach { c ->
                val out = timed(slowMs, onSlow) { SimhaTools.dispatch(c.name, c.arguments, host) }
                history += Msg.tool(c.id, out)
                if (c.name == "find_tool" && active != null) {
                    runCatching { Json.parseToJsonElement(out).jsonObject["loaded"]?.jsonArray?.forEach { active += it.jsonPrimitive.content } }
                }
            }
            if (calls.all { it.name == "note_task" }) {
                val t = codeReply()
                if (t != null) {
                    if (codeAsks() || !spokeAny) say(t)
                    history += Msg.assistant(t)
                    return t
                }
            }
        }
        throw LlmException("No answer")
    }

    private suspend fun <T> timed(ms: Long, onSlow: () -> Unit, f: suspend () -> T): T = coroutineScope {
        val t = launch {
            delay(ms)
            onSlow()
        }
        try {
            f()
        } finally {
            t.cancel()
        }
    }
}

package com.simha.app.llm

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

interface ChatApi {
    suspend fun chat(messages: JsonArray, tools: JsonArray, onDelta: (String) -> Unit): ParsedReply
}

object Agent {
    const val MAX_STEPS = 5

    suspend fun run(
        api: ChatApi,
        system: String,
        history: MutableList<JsonObject>,
        host: ToolHost,
        onSentence: (String) -> Unit,
        onToolStart: () -> Unit,
    ): String {
        repeat(MAX_STEPS) {
            var buf = ""
            val sb = StringBuilder()
            val msgs = JsonArray(listOf(Msg.system(system)) + History.trim(history))
            val r = api.chat(msgs, SimhaTools.definitions) { d ->
                sb.append(d)
                val (done, rest) = SentenceSplitter.split(buf + d)
                buf = rest
                done.forEach(onSentence)
            }
            if (buf.isNotBlank()) onSentence(buf.trim())
            if (r.toolCalls.isEmpty()) {
                val t = (r.content ?: sb.toString()).trim()
                history += Msg.assistant(t)
                return t
            }
            history += Msg.assistantCalls(r.content, r.toolCalls)
            onToolStart()
            r.toolCalls.forEach { history += Msg.tool(it.id, SimhaTools.dispatch(it.name, it.arguments, host)) }
        }
        throw LlmException("Too many tool steps without an answer")
    }
}

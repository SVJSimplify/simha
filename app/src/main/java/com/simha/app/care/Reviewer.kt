package com.simha.app.care

import android.content.Context
import android.util.Log
import com.simha.app.ServiceLocator
import com.simha.app.data.jo
import com.simha.app.voice.Langs
import com.simha.app.llm.Msg
import com.simha.app.wake.Wake
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ReviewNode(val type: NodeType, val label: String)
data class ReviewLink(val a: ReviewNode, val rel: String, val b: ReviewNode)
data class ReviewConcern(val text: String, val minutes: Int, val question: String)
data class Review(val nodes: List<ReviewNode>, val links: List<ReviewLink>, val concerns: List<ReviewConcern>, val resolved: List<String>)

object ReviewParse {
    private fun JsonObject.s(k: String) = this[k]?.jsonPrimitive?.contentOrNull?.trim()?.ifEmpty { null }

    private fun node(o: JsonObject?): ReviewNode? {
        o ?: return null
        val t = NodeType.of(o.s("type") ?: return null) ?: return null
        val l = o.s("label")?.take(60) ?: return null
        return ReviewNode(t, l)
    }

    private fun arr(o: JsonObject, k: String): JsonArray = runCatching { o[k]?.jsonArray }.getOrNull() ?: JsonArray(emptyList())

    fun parse(raw: String): Review? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val o = runCatching { Json.parseToJsonElement(raw.substring(start, end + 1)).jsonObject }.getOrNull() ?: return null
        val nodes = arr(o, "nodes").mapNotNull { runCatching { node(it.jsonObject) }.getOrNull() }
        val links = arr(o, "links").mapNotNull { e ->
            runCatching {
                val j = e.jsonObject
                val a = node(j["a"]?.jsonObject)
                val b = node(j["b"]?.jsonObject)
                if (a == null || b == null) null else ReviewLink(a, j.s("rel") ?: "related_to", b)
            }.getOrNull()
        }
        val concerns = arr(o, "concerns").mapNotNull { e ->
            runCatching {
                val j = e.jsonObject
                val t = j.s("text")
                val q = j.s("question")
                if (t == null || q == null) null else ReviewConcern(t.take(80), (j["check_in_minutes"]?.jsonPrimitive?.intOrNull ?: 180).coerceIn(30, 1440), q.take(200))
            }.getOrNull()
        }
        val resolved = arr(o, "resolved").mapNotNull { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull()?.take(80) }
        return Review(nodes.take(20), links.take(20), concerns.take(3), resolved.take(5))
    }
}

object Reviewer {
    private fun prompt(lang: String) = """
You review one conversation between Simha, a caring voice companion, and the elderly person it looks after.
Return only JSON, no other text, with this shape:
{"nodes":[{"type":"person|condition|allergy|medicine|routine|place|event|fact","label":"short"}],
 "links":[{"a":{"type":"...","label":"..."},"rel":"daughter_of|lives_in|takes|treats|causes|worse_when|likes|visits|has","b":{"type":"...","label":"..."}}],
 "concerns":[{"text":"short worry about the person","check_in_minutes":180,"question":"one short caring question to ask later, in ${Langs.name(lang)}"}],
 "resolved":["concern text that is clearly better now"]}
Only include facts the person actually said or clearly confirmed. Labels are a few words. Refer to the person themself as type person with label "me".
Add a concern only for something worth checking on later (pain, feeling unwell, low mood, not eating or drinking, a fall). At most 3. Leave arrays empty if nothing applies.
""".trimIndent()

    suspend fun review(ctx: Context, sl: ServiceLocator, transcript: List<Pair<Boolean, String>>) {
        if (transcript.none { it.first }) return
        val cfg = sl.llmStore.config()
        if (!cfg.configured) return
        val lang = sl.prefs.snap().lang
        val text = transcript.takeLast(30).joinToString("\n") { (u, t) -> (if (u) "Person: " else "Simha: ") + t.take(400) }
        val msgs = JsonArray(listOf(Msg.system(prompt(lang)), Msg.user(text)))
        val raw = try {
            sl.llm.complete(cfg, msgs, null).content.orEmpty()
        } catch (e: Exception) {
            Log.w("Simha", "review failed", e)
            return
        }
        val r = ReviewParse.parse(raw) ?: return
        apply(ctx, sl, r)
    }

    suspend fun apply(ctx: Context, sl: ServiceLocator, r: Review) {
        val g = sl.graph
        val ids = HashMap<ReviewNode, Long>()
        suspend fun id(n: ReviewNode): Long = ids[n] ?: g.upsert(n.type, n.label).also { ids[n] = it }
        r.nodes.forEach { id(it) }
        r.links.forEach { g.link(id(it.a), it.rel, id(it.b)) }
        val now = System.currentTimeMillis()
        r.concerns.forEach { c ->
            g.upsert(NodeType.Concern, c.text, 2)
            Wake.scheduleFollowUp(ctx, now + c.minutes * 60_000L, c.question)
        }
        if (r.resolved.isNotEmpty()) {
            val keys = r.resolved.map { GraphText.key(it) }
            g.all().filter { NodeType.of(it.type) == NodeType.Concern && keys.any { k -> it.k.contains(k) || k.contains(it.k) } }.forEach { g.delete(it.id) }
        }
        sl.repo.audit("memory_review", jo("nodes" to r.nodes.size, "links" to r.links.size, "concerns" to r.concerns.size))
    }
}

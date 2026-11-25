package com.simha.app.care

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

enum class NodeType(val code: Int, val word: String) {
    Person(1, "person"), Condition(2, "condition"), Medicine(3, "medicine"), Routine(4, "routine"),
    Place(5, "place"), Concern(6, "concern"), Event(7, "event"), Fact(8, "fact"), Allergy(9, "allergy");

    val pinned: Boolean get() = this == Condition || this == Allergy || this == Person

    companion object {
        fun of(c: Int) = entries.firstOrNull { it.code == c } ?: Fact
        fun of(w: String) = entries.firstOrNull { it.word == w.trim().lowercase() }
    }
}

@Entity(tableName = "g_node", indices = [Index(value = ["type", "k"], unique = true)])
data class GNode(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: Int,
    val k: String,
    val label: String,
    val weight: Int,
    val pinned: Boolean,
    val ts: Long,
)

@Entity(tableName = "g_edge", primaryKeys = ["src", "dst", "rel"])
data class GEdge(val src: Long, val dst: Long, val rel: String, val strength: Float, val ts: Long)

@Dao
interface GraphDao {
    @Query("SELECT * FROM g_node")
    suspend fun nodes(): List<GNode>

    @Query("SELECT * FROM g_edge")
    suspend fun edges(): List<GEdge>

    @Query("SELECT * FROM g_node WHERE type = :type AND k = :k")
    suspend fun node(type: Int, k: String): GNode?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNode(n: GNode): Long

    @Upsert
    suspend fun upsertNode(n: GNode)

    @Upsert
    suspend fun upsertEdge(e: GEdge)

    @Query("DELETE FROM g_node WHERE id = :id")
    suspend fun deleteNode(id: Long)

    @Query("DELETE FROM g_edge WHERE src = :id OR dst = :id")
    suspend fun deleteEdgesOf(id: Long)

    @Query("DELETE FROM g_edge WHERE strength < :min")
    suspend fun dropWeak(min: Float)

    @Query("UPDATE g_edge SET strength = strength * :f WHERE ts < :before AND src NOT IN (SELECT id FROM g_node WHERE pinned = 1) AND dst NOT IN (SELECT id FROM g_node WHERE pinned = 1)")
    suspend fun decay(f: Float, before: Long)
}

object GraphText {
    private val split = Regex("""[^\p{L}\p{N}]+""")
    private val stop = setOf(
        "the", "and", "for", "you", "your", "that", "this", "with", "have", "has", "was", "are", "but", "not", "can", "will",
        "what", "when", "how", "today", "very", "just", "some", "she", "her", "his", "him", "they", "them", "about", "from", "there",
    )

    fun key(label: String): String = label.trim().lowercase().replace(Regex("""\s+"""), " ").removePrefix("my ").removePrefix("the ").take(60)

    fun tokens(text: String): Set<String> = text.lowercase().split(split).filter { it.length >= 3 && it !in stop }.toSet()

    fun matches(k: String, toks: Set<String>): Boolean {
        val parts = tokens(k)
        return parts.isNotEmpty() && parts.any { p -> toks.any { t -> t == p || (p.length >= 4 && t.length >= 4 && t.take(4) == p.take(4)) } }
    }
}

object GraphRank {
    const val MAX_NODES = 200

    fun retrieve(nodes: List<GNode>, edges: List<GEdge>, text: String, extraSeeds: Collection<String>, now: Long, limit: Int = 16): List<String> {
        val byId = nodes.associateBy { it.id }
        val toks = GraphText.tokens(text + " " + extraSeeds.joinToString(" "))
        val seeds = nodes.filter { GraphText.matches(it.k, toks) }.map { it.id }.toMutableSet()
        nodes.filter { val t = NodeType.of(it.type); t == NodeType.Allergy || t == NodeType.Condition }.forEach { seeds += it.id }
        val out = edges.groupBy { it.src }
        val inc = edges.groupBy { it.dst }
        val dist = HashMap<Long, Int>()
        seeds.forEach { dist[it] = 0 }
        var frontier = seeds.toList()
        repeat(2) { d ->
            val next = mutableListOf<Long>()
            frontier.forEach { id ->
                val near = (out[id] ?: emptyList()).map { it.dst } + (inc[id] ?: emptyList()).map { it.src }
                near.forEach { n -> if (n !in dist) { dist[n] = d + 1; next += n } }
            }
            frontier = next
        }
        val lines = HashMap<String, Double>()
        edges.forEach { e ->
            val ds = dist[e.src] ?: return@forEach
            val dd = dist[e.dst] ?: return@forEach
            val a = byId[e.src] ?: return@forEach
            val b = byId[e.dst] ?: return@forEach
            val hop = minOf(ds, dd)
            val ageDays = (now - e.ts).coerceAtLeast(0) / 86_400_000.0
            val score = e.strength * (1.0 / (1 + hop)) * (a.weight + b.weight) / (1 + ageDays / 14)
            val line = a.label + " " + e.rel.replace('_', ' ') + " " + b.label
            if ((lines[line] ?: 0.0) < score) lines[line] = score
        }
        val linked = edges.flatMap { listOf(it.src, it.dst) }.toSet()
        nodes.filter { dist[it.id] == 0 && it.id !in linked }.forEach { lines[NodeType.of(it.type).word + ": " + it.label] = it.weight.toDouble() }
        return lines.entries.sortedByDescending { it.value }.take(limit).map { it.key }
    }

    fun prune(nodes: List<GNode>, edges: List<GEdge>, now: Long): List<Long> {
        val linked = edges.flatMap { listOf(it.src, it.dst) }.toSet()
        val stale = nodes.filter { !it.pinned && it.id !in linked && it.weight <= 1 && now - it.ts > 30L * 86_400_000 }.map { it.id }.toSet()
        val left = nodes.filter { it.id !in stale }
        val over = (left.size - MAX_NODES).coerceAtLeast(0)
        val extra = left.filter { !it.pinned }.sortedWith(compareBy<GNode>({ it.weight }, { it.ts })).take(over).map { it.id }
        return stale.toList() + extra
    }
}

class MemoryGraph(private val dao: GraphDao) {
    suspend fun upsert(type: NodeType, label: String, weight: Int = 1): Long {
        val k = GraphText.key(label)
        if (k.isBlank()) return -1
        val now = System.currentTimeMillis()
        val old = dao.node(type.code, k)
        if (old != null) {
            dao.upsertNode(old.copy(weight = (old.weight + weight).coerceAtMost(20), ts = now))
            return old.id
        }
        val id = dao.insertNode(GNode(type = type.code, k = k, label = label.trim().take(60), weight = weight, pinned = type.pinned, ts = now))
        return if (id > 0) id else dao.node(type.code, k)?.id ?: -1
    }

    suspend fun link(a: Long, rel: String, b: Long, strength: Float = 1f) {
        if (a <= 0 || b <= 0 || a == b) return
        val r = rel.trim().lowercase().replace(Regex("""[^a-z_ ]"""), "").trim().replace(' ', '_').take(24).ifBlank { "related_to" }
        dao.upsertEdge(GEdge(a, b, r, strength.coerceIn(0.1f, 3f), System.currentTimeMillis()))
    }

    suspend fun retrieve(text: String, extra: Collection<String> = emptyList(), limit: Int = 16): List<String> =
        GraphRank.retrieve(dao.nodes(), dao.edges(), text, extra, System.currentTimeMillis(), limit)

    suspend fun all(): List<GNode> = dao.nodes().sortedWith(compareByDescending<GNode> { it.pinned }.thenByDescending { it.weight })

    suspend fun types(): Set<NodeType> = dao.nodes().map { NodeType.of(it.type) }.toSet()

    suspend fun delete(id: Long) {
        dao.deleteEdgesOf(id)
        dao.deleteNode(id)
    }

    suspend fun maintain() {
        val now = System.currentTimeMillis()
        dao.decay(0.97f, now - 7L * 86_400_000)
        dao.dropWeak(0.15f)
        GraphRank.prune(dao.nodes(), dao.edges(), now).forEach { delete(it) }
    }
}

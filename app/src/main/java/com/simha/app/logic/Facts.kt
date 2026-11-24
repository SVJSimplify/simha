package com.simha.app.logic

import java.util.Locale

data class Fact(val k: String, val v: String, val w: Int, val at: Long)

data class FactsResult(val facts: List<Fact>, val changed: Boolean, val evicted: List<String>)

object Facts {
    const val MAX = 30

    fun normKey(k: String): String =
        k.trim().lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{M}\\p{N}:]+"), "_").trim('_').take(40).trim('_')

    fun keyFor(kind: String, text: String): String {
        val t = text.trim()
        if (t.startsWith("asked:")) return normKey(t)
        return normKey(kind + ":" + normKey(t).take(24))
    }

    fun valueFor(text: String): String = text.trim().let { if (it.startsWith("asked:")) "1" else it.take(80) }

    fun weightFor(kind: String, key: String): Int = when {
        key.startsWith("asked:") -> 1
        kind == "person" -> 3
        else -> 2
    }

    fun upsert(cur: List<Fact>, k: String, v: String, w: Int, now: Long): FactsResult {
        val key = normKey(k)
        val old = cur.firstOrNull { it.k == key }
        val weight = maxOf(w.coerceIn(1, 3), old?.w ?: 1)
        if (old != null && old.v == v && old.w == weight) return FactsResult(cur, false, emptyList())
        var list = cur.filter { it.k != key } + Fact(key, v, weight, now)
        val evicted = ArrayList<String>()
        while (list.size > MAX) {
            val victim = list.filter { it.k != key }.sortedWith(compareBy<Fact>({ it.w }, { it.at })).first()
            evicted += victim.k
            list = list.filter { it.k != victim.k }
        }
        return FactsResult(list, true, evicted)
    }
}

package com.simha.app.llm

object HealthMemory {
    val kinds = setOf("condition", "medicine", "allergy")

    fun split(s: String): List<String> = s.split(',', ';', '\n').map { it.trim() }.filter { it.isNotEmpty() }

    private fun norm(s: String) = s.lowercase().replace(Regex("\s+"), " ").trim()

    fun merge(existing: String, add: String): Pair<String, Boolean> {
        val items = split(existing).toMutableList()
        val seen = items.map(::norm).toMutableSet()
        var changed = false
        for (a in split(add.replace(" and ", ", ", ignoreCase = true))) {
            if (seen.add(norm(a))) {
                items += a
                changed = true
            }
        }
        return items.joinToString(", ") to changed
    }
}

package com.simha.app.llm

class FillerGate(private val variants: Int = 4, val delayMs: Long = 2500) {
    private var last = -1
    private var used = false

    fun newTurn() {
        used = false
    }

    fun pick(replied: Boolean, seed: Int): Int? {
        if (!replied || used || variants <= 0) return null
        used = true
        var i = Math.floorMod(seed, variants)
        if (variants > 1 && i == last) i = (i + 1) % variants
        last = i
        return i
    }
}

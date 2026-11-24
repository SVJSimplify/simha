package com.simha.app.llm

object ContactMatch {
    private val roleWords = mapOf(
        "family" to setOf("family", "relative", "relatives", "parivar", "kutumb", "परिवार", "कुटुंब", "కుటుంబం", "కుటుంబ", "ಕುಟುಂಬ", "குடும்பம்", "குடும்பத்தினர்"),
        "neighbor" to setOf("neighbor", "neighbors", "neighbour", "neighbours", "padosi", "पड़ोसी", "पड़ोसन", "పొరుగు", "పక్కింటి", "ನೆರೆಹೊರೆ", "ನೆರೆಯ", "ಪಕ್ಕದ ಮನೆ", "அண்டை", "பக்கத்து வீட்டு"),
        "doctor" to setOf("doctor", "doctors", "dr", "physician", "डॉक्टर", "डाक्टर", "वैद्य", "డాక్టర్", "వైద్యుడు", "వైద్యురాలు", "ಡಾಕ್ಟರ್", "ವೈದ್ಯ", "டாக்டர்", "மருத்துவர்"),
        "hospital" to setOf("hospital", "clinic", "अस्पताल", "క్లినిక్", "ఆసుపత్రి", "ఆస్పత్రి", "ಆಸ್ಪತ್ರೆ", "ಕ್ಲಿನಿಕ್", "மருத்துவமனை"),
    )

    private fun norm(s: String) = s.trim().lowercase().replace(Regex("[.,!?'\"]"), " ").replace(Regex("\\s+"), " ").trim()

    private fun roleHit(who: String, role: String): Boolean {
        val tokens = who.split(' ').toSet()
        return (roleWords[role] ?: emptySet()).any { w -> if (w.all { it.code < 128 }) w in tokens else who.contains(w) }
    }

    fun find(who: String, all: List<ContactInfo>): List<ContactInfo> {
        val w = norm(who)
        if (w.isEmpty()) return emptyList()
        all.filter { norm(it.name) == w }.let { if (it.isNotEmpty()) return it }
        all.filter { val n = norm(it.name); n.length >= 2 && (n.contains(w) || w.contains(n)) }.let { if (it.isNotEmpty()) return it }
        return all.filter { roleHit(w, it.role) }
    }

    fun normalizeIndian(raw: String): String? {
        if (raw.any { !(it.isDigit() || it in "+ -()") }) return null
        val d = raw.filter { it.isDigit() }
        val n = when {
            d.length == 12 && d.startsWith("91") -> d.substring(2)
            d.length == 11 && d.startsWith("0") -> d.substring(1)
            else -> d
        }
        return if (n.length == 10 && n[0] in '6'..'9') "+91$n" else null
    }
}

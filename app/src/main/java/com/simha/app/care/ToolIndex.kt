package com.simha.app.care

object ToolIndex {
    val core = listOf("note_task", "get_first_aid", "open_emergency_screen", "dial_emergency", "call_contact", "find_tool")

    private class Entry(val tools: List<String>, val words: List<String>, val types: Set<NodeType> = emptySet(), val forms: Set<TaskType> = emptySet())

    private val entries = listOf(
        Entry(
            listOf("add_medicine", "take_medicine", "list_medicines"),
            listOf("medicine", "medicines", "tablet", "tablets", "pill", "pills", "dose", "capsule", "syrup", "insulin", "दवा", "दवाई", "गोली", "మందు", "మాత్ర", "ಔಷಧ", "ಮಾತ್ರೆ", "மருந்து", "மாத்திரை"),
            setOf(NodeType.Medicine), setOf(TaskType.Medicine),
        ),
        Entry(
            listOf("log_symptom", "remember_health_detail"),
            listOf("pain", "hurt", "hurts", "ache", "dizzy", "fever", "fell", "fall", "chest", "breath", "vomit", "sugar", "pressure", "allergic", "allergy", "दर्द", "चक्कर", "बुखार", "सांस", "నొప్పి", "జ్వరం", "ನೋವು", "ಜ್ವರ", "வலி", "காய்ச்சல்", "மயக்கம்"),
            setOf(NodeType.Condition, NodeType.Allergy, NodeType.Concern), setOf(TaskType.Symptom),
        ),
        Entry(
            listOf("dial_contact", "list_contacts", "send_family_alert"),
            listOf("call", "phone", "ring", "family", "daughter", "son", "wife", "husband", "neighbour", "neighbor", "doctor", "text", "फोन", "कॉल", "बेटी", "बेटा", "पड़ोसी", "ఫోన్", "కాల్", "ಕರೆ", "ಫೋನ್", "அழை", "போன்"),
            setOf(NodeType.Person),
        ),
        Entry(listOf("add_contact", "set_call_approval"), listOf("add", "save", "number", "contact", "allow", "नंबर", "జోడించు", "ಸೇರಿಸು", "சேர்")),
        Entry(listOf("set_reminder"), listOf("remind", "reminder", "alarm", "later", "tomorrow", "याद", "గుర్తు", "ನೆನಪಿಸು", "நினைவூட்டு"), forms = setOf(TaskType.Reminder)),
        Entry(
            listOf("log_diary"),
            listOf("water", "drank", "ate", "eat", "food", "lunch", "dinner", "breakfast", "sleep", "slept", "walk", "visited", "mood", "पानी", "खाना", "नींद", "నీళ్లు", "నిద్ర", "ನೀರು", "ನಿದ್ರೆ", "தண்ணீர்", "தூக்கம்"),
            setOf(NodeType.Routine), setOf(TaskType.Intake),
        ),
        Entry(listOf("scam_check"), listOf("otp", "bank", "kyc", "pin", "link", "upi", "fraud", "scam", "money", "lottery", "prize", "पैसे", "बैंक", "ఓటీపీ", "ಬ್ಯಾಂಕ್", "வங்கி")),
        Entry(listOf("look_with_camera"), listOf("see", "look", "photo", "camera", "read", "देखो", "చూడు", "ನೋಡು", "பார்")),
        Entry(listOf("play_family_messages"), listOf("messages", "voice message", "संदेश", "సందేశం", "ಸಂದೇಶ", "செய்தி")),
        Entry(listOf("start_recording"), listOf("record", "recording", "रिकॉर्ड", "రికార్డ్", "ರೆಕಾರ್ಡ್", "பதிவு")),
        Entry(listOf("get_profile", "get_status"), listOf("profile", "status", "last", "कब")),
    )

    private fun hit(text: String, words: List<String>): Boolean {
        val t = text.lowercase()
        val toks = GraphText.tokens(t)
        return words.any { w -> if (w.contains(' ') || w.any { it.code > 0x7f }) t.contains(w) else w in toks }
    }

    fun select(text: String, openForms: Collection<TaskType>, extra: Collection<String> = emptyList()): List<String> {
        val out = LinkedHashSet(core)
        entries.forEach { e -> if (hit(text, e.words) || e.forms.any { it in openForms }) out += e.tools }
        out += extra
        return out.toList()
    }

    fun forGraph(lines: Collection<String>, types: Collection<NodeType>): List<String> =
        entries.filter { e -> e.types.any { it in types } && lines.any { l -> hit(l, e.words) } }.flatMap { it.tools }

    fun search(query: String): List<String> {
        val q = query.lowercase()
        val toks = GraphText.tokens(q.replace('_', ' '))
        return entries.filter { e -> hit(q, e.words) || e.tools.any { t -> q.contains(t) || GraphText.tokens(t.replace('_', ' ')).any { it in toks } } }
            .flatMap { it.tools }.distinct()
    }

    fun toolsFor(t: TaskType): List<String> = entries.filter { t in it.forms }.flatMap { it.tools }

    val all: List<String> get() = (core + entries.flatMap { it.tools }).distinct()
}

package com.simha.app.logic

data class MedicalProfile(val conditions: String = "", val medicines: String = "", val allergies: String = "")

enum class Situation(val key: String, val title: String, val alwaysUrgent: Boolean = false) {
    Allergy("allergy", "Mild allergic reaction"),
    Anaphylaxis("anaphylaxis", "Severe allergic reaction", true),
    Fall("fall", "Fall"),
    ChestPain("chest_pain", "Chest pain or possible heart attack", true),
    Stroke("stroke", "Possible stroke", true),
    LowSugar("low_sugar", "Low blood sugar"),
    HighSugar("high_sugar", "High blood sugar"),
    Choking("choking", "Choking", true),
    Breathless("breathless", "Breathlessness or asthma"),
    Fainting("fainting", "Fainting"),
    Fever("fever", "High fever"),
    Burn("burn", "Burn"),
    Bleeding("bleeding", "Bleeding"),
    MedicineError("medicine_error", "Wrong, missed or double medicine dose"),
    Dizziness("dizziness", "Dizziness"),
    Heat("heat", "Heat exhaustion"),
}

data class Guide(
    val situation: Situation,
    val steps: List<String>,
    val danger: List<String>,
    val call112Now: Boolean,
    val personal: List<String>,
)

object FirstAid {
    private val order = listOf(
        Situation.Anaphylaxis, Situation.ChestPain, Situation.Stroke, Situation.Choking, Situation.Breathless,
        Situation.Fainting, Situation.LowSugar, Situation.HighSugar, Situation.Bleeding, Situation.Burn,
        Situation.MedicineError, Situation.Fall, Situation.Allergy, Situation.Fever, Situation.Heat, Situation.Dizziness,
    )

    private val keywords: Map<Situation, List<String>> = mapOf(
        Situation.Anaphylaxis to listOf(
            "anaphyla", "epipen", "swelling of face", "swelling of the face", "swollen tongue", "tongue swelling", "swollen face", "face swelling",
            "throat closing", "throat swelling", "swollen lips", "lips swollen", "throat swell",
            "गला सूज", "जीभ सूज", "होंठ सूज", "चेहरा सूज", "चेहरे पर सूज",
            "గొంతు వాపు", "నాలుక వాపు", "ముఖం వాపు", "పెదవులు వాపు",
            "ಗಂಟಲು ಊತ", "ನಾಲಿಗೆ ಊತ", "ಮುಖ ಊತ", "ಮುಖ ಊದ", "ತುಟಿ ಊತ",
            "தொண்டை வீக்கம்", "நாக்கு வீங்க", "முகம் வீங்க", "உதடு வீங்க",
        ),
        Situation.ChestPain to listOf(
            "chest pain", "chest tight", "chest hurt", "chest heavy", "chest pressure", "pressure in my chest", "pain in my chest", "pain in the chest",
            "heart attack", "heart pain", "heartattack", "chest discomfort",
            "सीने में दर्द", "सीने में", "छाती में दर्द", "छाती में", "दिल का दौरा", "हार्ट अटैक", "सीने में जकड़न",
            "ఛాతీ నొప్పి", "ఛాతిలో నొప్పి", "ఛాతీలో నొప్పి", "గుండె నొప్పి", "గుండెపోటు", "గుండె నొప్పి",
            "ಎದೆ ನೋವು", "ಎದೆಯಲ್ಲಿ ನೋವು", "ಹೃದಯಾಘಾತ", "ಹೃದಯ ನೋವು",
            "நெஞ்சு வலி", "நெஞ்சில் வலி", "மாரடைப்பு", "இதய வலி", "நெஞ்சு இறுக்க",
        ),
        Situation.Stroke to listOf(
            "stroke", "face droop", "drooping", "droopy", "slurred", "cannot speak properly", "can't speak properly", "arm weak", "arm is weak", "one side weak", "weakness on one side", "mouth crooked",
            "लकवा", "पक्षाघात", "जुबान लड़खड़", "ज़ुबान लड़खड़", "मुंह टेढ़ा", "मुँह टेढ़ा", "चेहरा टेढ़ा", "एक तरफ कमजोर",
            "పక్షవాతం", "స్ట్రోక్", "మాట తడబడ", "ముఖం వంగి", "ఒక వైపు బలహీన",
            "ಪಾರ್ಶ್ವವಾಯು", "ಸ್ಟ್ರೋಕ್", "ಮಾತು ತೊದಲ", "ಮುಖ ಓರೆ", "ಒಂದು ಬದಿ ಬಲಹೀನ",
            "பக்கவாதம்", "ஸ்ட்ரோக்", "பேச்சு குழறு", "வாய் கோணல்", "ஒரு பக்கம் பலவீன",
        ),
        Situation.Choking to listOf(
            "chok", "stuck in throat", "stuck in my throat", "food stuck", "swallowed wrong", "went down the wrong",
            "गले में अटक", "दम घुट", "गला घुट", "गले में फंस",
            "గొంతులో ఇరుక్కు", "గొంతులో అడ్డు", "గొంతులో చిక్కు",
            "ಗಂಟಲಲ್ಲಿ ಸಿಕ್ಕ", "ಗಂಟಲಲ್ಲಿ ಅಂಟ", "ಉಸಿರುಗಟ್ಟ",
            "தொண்டையில் அடைத்து", "தொண்டையில் சிக்கி", "புரை ஏறி", "தொண்டையில் அடைப்பு",
        ),
        Situation.Breathless to listOf(
            "breathless", "short of breath", "shortness of breath", "can't breathe", "cannot breathe", "cant breathe", "difficulty breathing", "trouble breathing", "hard to breathe", "asthma", "wheez", "out of breath",
            "सांस फूल", "सांस लेने में", "दमा", "अस्थमा", "सांस की तकलीफ", "साँस फूल", "साँस लेने में",
            "ఊపిరి ఆడ", "శ్వాస", "ఆస్తమా", "ఆయాసం", "ఊపిరి తీసుకోవడం",
            "ಉಸಿರಾಟ", "ಉಸಿರು ಕಟ್ಟ", "ಆಸ್ತಮಾ", "ಏದುಸಿರು", "ಉಸಿರಾಡಲು",
            "மூச்சு திணறல்", "மூச்சு விட", "ஆஸ்துமா", "மூச்சிரைப்பு", "மூச்சுத் திணறல்",
        ),
        Situation.Fainting to listOf(
            "faint", "passed out", "pass out", "blacked out", "black out", "unconscious", "collapsed", "collapse",
            "बेहोश", "चक्कर खाकर गिर", "चक्कर आकर गिर",
            "స్పృహ తప్ప", "మూర్ఛ", "స్పృహ కోల్పో",
            "ಮೂರ್ಛೆ", "ಪ್ರಜ್ಞೆ ತಪ್ಪ", "ಪ್ರಜ್ಞೆ ಕಳೆ",
            "மயங்கி", "மயக்கம் போட", "மூர்ச்சை", "நினைவிழ", "நினைவு தப்ப",
        ),
        Situation.LowSugar to listOf(
            "low sugar", "low blood sugar", "hypoglyc", "sugar is low", "sugar low", "sugar dropped", "sugar went down",
            "शुगर कम", "शुगर लो", "ब्लड शुगर कम", "शुगर गिर", "चीनी कम",
            "షుగర్ తక్కువ", "షుగర్ లో", "షుగర్ తగ్గ", "చక్కెర తక్కువ",
            "ಸಕ್ಕರೆ ಕಡಿಮೆ", "ಶುಗರ್ ಕಡಿಮೆ", "ಶುಗರ್ ಇಳಿ", "ಸಕ್ಕರೆ ಇಳಿ",
            "சர்க்கரை குறைவு", "சுகர் குறைவு", "சர்க்கரை குறைந்", "சுகர் குறைந்",
        ),
        Situation.HighSugar to listOf(
            "high sugar", "high blood sugar", "hyperglyc", "sugar is high", "sugar high", "sugar went up", "sugar increased",
            "शुगर ज्यादा", "शुगर ज़्यादा", "शुगर हाई", "शुगर बढ़", "ब्लड शुगर ज्यादा",
            "షుగర్ ఎక్కువ", "షుగర్ హై", "షుగర్ పెరి", "చక్కెర ఎక్కువ",
            "ಸಕ್ಕರೆ ಹೆಚ್ಚು", "ಶುಗರ್ ಹೆಚ್ಚು", "ಶುಗರ್ ಏರಿ", "ಸಕ್ಕರೆ ಏರಿ",
            "சர்க்கரை அதிகம்", "சுகர் அதிகம்", "சர்க்கரை கூடு", "சுகர் கூடு", "சர்க்கரை ஏறி",
        ),
        Situation.Bleeding to listOf(
            "bleed", "deep cut", "cut myself", "cut my", "wound", "blood is coming", "blood coming", "blood loss", "gash", "nosebleed",
            "खून", "रक्तस्राव", "कट गया", "कट गई", "घाव",
            "రక్తం కారు", "రక్తస్రావ", "గాయం", "రక్తం వస్తోంది",
            "ರಕ್ತ ಸುರಿ", "ರಕ್ತಸ್ರಾವ", "ಗಾಯ", "ರಕ್ತ ಬರ",
            "ரத்தம் வழி", "இரத்தம் வழி", "ரத்தக் கசிவு", "இரத்தக் கசிவு", "காயம்", "ரத்தம் வருகிறது",
        ),
        Situation.Burn to listOf(
            "burn", "scald", "hot water spilled", "spilled hot", "caught fire", "hot oil",
            "जल गया", "जल गई", "जला", "झुलस", "जल गये",
            "కాలింది", "కాలిపోయ", "కాలిన", "మంటలు",
            "ಸುಟ್ಟ", "ಸುಟ್ಟು", "ಬೆಂಕಿ ತಗುಲ",
            "தீக்காயம்", "சுட்டு", "கொதிக்கும் நீர்", "வெந்நீர்", "தீப்புண்",
        ),
        Situation.MedicineError to listOf(
            "wrong medicine", "wrong tablet", "wrong pill", "wrong dose", "double dose", "took twice", "took two", "took it twice", "tablet twice", "tablets twice", "pill twice", "pills twice", "medicine twice", "dose twice", "missed dose", "missed my", "missed a dose", "forgot my medicine", "forgot my tablet", "forgot my pill", "forgot to take", "overdose", "took too many", "medicine error", "twice the",
            "गलत दवा", "दवा दो बार", "दोबारा दवा", "दवा भूल", "ज्यादा दवा", "ज़्यादा दवा", "दवा नहीं ली", "गोली भूल",
            "తప్పు మందు", "మందు రెండుసార్లు", "మందు మర్చిపో", "ఎక్కువ మందు", "మాత్ర మర్చిపో", "మందు వేసుకోలేదు",
            "ತಪ್ಪು ಔಷಧ", "ಔಷಧ ಎರಡು ಬಾರಿ", "ಮಾತ್ರೆ ಮರೆ", "ಹೆಚ್ಚು ಔಷಧ", "ಔಷಧ ಮರೆ", "ಮಾತ್ರೆ ತೆಗೆದುಕೊಳ್ಳಲಿಲ್ಲ",
            "தவறான மருந்து", "மருந்து இரண்டு முறை", "மாத்திரை மறந்து", "அதிக மருந்து", "மருந்து மறந்து", "மாத்திரை இரண்டு",
        ),
        Situation.Fall to listOf(
            "fell", "fall", "slipped", "tripped", "i slip", "collapsed on the floor",
            "गिर गया", "गिर गई", "गिर पड़", "फिसल", "गिर गये", "गिरा",
            "కింద పడ", "జారి పడ", "పడిపోయ", "పడ్డాను", "పడిపోయా",
            "ಬಿದ್ದೆ", "ಬಿದ್ದು", "ಜಾರಿ ಬಿದ್ದ", "ಬಿದ್ದಿದ್ದೇನೆ", "ಕೆಳಗೆ ಬಿ",
            "கீழே விழ", "விழுந்து", "வழுக்கி", "விழுந்தேன்", "தடுக்கி",
        ),
        Situation.Allergy to listOf(
            "allerg", "rash", "hives", "itch", "sneez",
            "एलर्जी", "खुजली", "दाने", "चकत्ते",
            "అలెర్జీ", "దురద", "దద్దుర్లు",
            "ಅಲರ್ಜಿ", "ತುರಿಕೆ", "ದದ್ದು",
            "ஒவ்வாமை", "அரிப்பு", "தடிப்பு",
        ),
        Situation.Fever to listOf(
            "fever", "temperature", "feverish", "high temperature",
            "बुखार", "तेज बुखार", "ज्वर",
            "జ్వరం", "జ్వర",
            "ಜ್ವರ",
            "காய்ச்சல்", "ஜுரம்", "காய்ச்சலா",
        ),
        Situation.Heat to listOf(
            "heat", "sunstroke", "heatstroke", "too hot", "dehydrat", "in the sun", "very hot",
            "लू लग", "धूप", "गर्मी", "पानी की कमी",
            "వడదెబ్బ", "ఎండ", "వేడి",
            "ಬಿಸಿಲಿನ ಹೊಡೆತ", "ಬಿಸಿಲು", "ಸೆಕೆ", "ಬಿಸಿಲಿನ",
            "வெயில்", "வெப்பம்", "சூடு", "நீரிழப்பு", "வெப்பத்தால்",
        ),
        Situation.Dizziness to listOf(
            "dizz", "giddy", "light-headed", "lightheaded", "light headed", "head spinning", "head is spinning", "spinning", "vertigo",
            "चक्कर", "सिर घूम", "सर घूम",
            "తల తిరుగ", "కళ్ళు తిరుగ", "తలతిరుగ", "కళ్లు తిరుగ",
            "ತಲೆ ಸುತ್ತ", "ತಲೆಸುತ್ತು", "ತಲೆ ತಿರುಗ",
            "தலை சுற்ற", "தலைசுற்ற", "மயக்கம்", "தலை சுத்த", "தலைச்சுற்று",
        ),
    )

    private val globalDanger = listOf(
        "unconscious", "not breathing", "cannot breathe", "can't breathe", "cant breathe", "not waking", "won't wake", "wont wake", "not responding", "seizure", "fits",
        "blue lips", "turning blue", "chest pain", "vomiting blood", "coughing blood", "head injury", "hit my head", "hit his head", "hit her head",
        "बेहोश", "सांस नहीं", "साँस नहीं", "सीने में दर्द", "दौरा",
        "స్పృహ లేదు", "ఊపిరి ఆడటం లేదు", "ఛాతీ నొప్పి", "ఫిట్స్",
        "ಪ್ರಜ್ಞೆ ಇಲ್ಲ", "ಉಸಿರಾಡಲು ಆಗುತ್ತಿಲ್ಲ", "ಎದೆ ನೋವು", "ಫಿಟ್ಸ್",
        "நினைவு இல்லை", "மூச்சு விட முடியவில்லை", "நெஞ்சு வலி", "வலிப்பு",
    )

    private val danger: Map<Situation, List<String>> = mapOf(
        Situation.Allergy to listOf(
            "swell", "swollen", "throat", "tongue", "lips", "face", "breath", "wheez", "dizz", "faint", "vomit", "cramp",
            "सूज", "గొంతు", "వాపు", "ಊತ", "ಊದ", "வீக்கம்", "வீங்க",
        ),
        Situation.Fall to listOf(
            "head", "can't get up", "cannot get up", "can't stand", "cannot stand", "can't move", "cannot move", "unable to", "hip", "broken", "fracture", "bleed", "confus", "drows", "vomit", "neck", "back pain", "severe pain", "blood thinner",
            "सिर में चोट", "सर पर चोट", "उठ नहीं", "कूल्हे", "खून",
            "తలకు దెబ్బ", "లేవలేక", "తలకు", "రక్తం",
            "ತಲೆಗೆ ಪೆಟ್ಟು", "ಏಳಲು ಆಗ", "ರಕ್ತ",
            "தலையில் அடி", "எழ முடியவில்லை", "ரத்தம்", "இரத்தம்",
        ),
        Situation.LowSugar to listOf(
            "unconscious", "drows", "confus", "can't swallow", "cannot swallow", "not better", "not improving", "can't stay awake", "very sleepy", "still low",
            "குழப்பம்", "గందరగోళం", "ಗೊಂದಲ", "उलझन",
        ),
        Situation.HighSugar to listOf(
            "vomit", "confus", "drows", "fast breathing", "breathing fast", "abdominal pain", "belly pain", "stomach pain", "fruity", "very thirsty and",
            "उल्टी", "వాంతి", "ವಾಂತಿ", "வாந்தி",
        ),
        Situation.Breathless to listOf(
            "can't speak", "cannot speak", "blue", "grey", "gray", "not helping", "no relief", "not better", "drows", "confus", "worse",
            "बोल नहीं", "మాట్లాడలేక", "ಮಾತನಾಡಲು ಆಗ", "பேச முடிய",
        ),
        Situation.Fainting to listOf(
            "not waking", "unconscious", "hit head", "head injury", "fits", "not breathing", "injur", "palpitation", "racing heart", "fell", "again",
            "మళ్ళీ", "ಮತ್ತೆ", "மீண்டும்",
        ),
        Situation.Fever to listOf(
            "confus", "stiff neck", "rash", "breath", "drows", "very weak", "cannot drink", "can't drink", "103", "104", "105", "not passing urine",
            "गर्दन अकड़", "అయోమయం", "ಗೊಂದಲ", "குழப்பம்",
        ),
        Situation.Burn to listOf(
            "large", "big", "face", "hand", "feet", "foot", "genital", "groin", "chemical", "electric", "deep", "white", "charred", "smoke", "inhal", "hoarse", "palm",
            "चेहरे", "ముఖం", "ಮುಖ", "முகம்",
        ),
        Situation.Bleeding to listOf(
            "spurt", "won't stop", "wont stop", "not stopping", "soaking", "heavy", "deep", "gaping", "dizz", "faint", "pale", "cold", "head", "chest", "belly", "stomach", "thinner", "warfarin",
            "बहुत खून", "खून नहीं रुक", "రక్తం ఆగ", "ರಕ್ತ ನಿಲ್ಲ", "ரத்தம் நிற்க",
        ),
        Situation.MedicineError to listOf(
            "drows", "confus", "sleepy", "breath", "chest pain", "faint", "seizure", "vomit", "large", "many", "bottle", "overdose", "too many", "whole strip", "lot of",
            "बहुत सारी", "ఎక్కువ మాత్ర", "ಹೆಚ್ಚು ಮಾತ್ರೆ", "அதிக மாத்திரை",
        ),
        Situation.Dizziness to listOf(
            "chest pain", "breath", "heartbeat", "palpitation", "droop", "slurred", "weak arm", "headache", "double vision", "faint", "head injury", "can't walk", "cannot walk",
            "सीने", "ఛాతీ", "ಎದೆ", "நெஞ்சு",
        ),
        Situation.Heat to listOf(
            "confus", "fits", "faint", "not sweating", "stopped sweating", "hot dry", "vomit", "drows",
            "उल्टी", "వాంతి", "ವಾಂತಿ", "வாந்தி",
        ),
    )

    private fun norm(s: String): String = s.lowercase().replace('’', '\'').replace('_', ' ').replace('-', ' ')

    private fun tokens(s: String): List<String> = s.split(Regex("[^\\p{L}\\p{M}\\p{N}']+")).filter { it.isNotEmpty() }

    private fun hit(text: String, toks: List<String>, k: String): Boolean =
        if (k.contains(' ') || k.any { it.code > 127 }) text.contains(k) else toks.any { it.startsWith(k) }

    private fun anyHit(text: String, ks: List<String>): Boolean {
        val t = norm(text)
        val toks = tokens(t)
        return ks.any { hit(t, toks, norm(it)) }
    }

    fun match(text: String): Situation? {
        if (text.isBlank()) return null
        val t = norm(text)
        val toks = tokens(t)
        return order.firstOrNull { s -> keywords[s].orEmpty().any { hit(t, toks, norm(it)) } }
    }

    fun dangerous(s: Situation, text: String): Boolean {
        if (s.alwaysUrgent) return true
        return anyHit(text, globalDanger) || anyHit(text, danger[s].orEmpty())
    }

    private fun has(s: String, ks: List<String>): Boolean {
        val t = s.lowercase()
        return ks.any { t.contains(it) }
    }

    private val epi = listOf("epipen", "epinephrine", "adrenaline", "auto-injector", "autoinjector", "anapen", "jext")
    private val inhaler = listOf("inhaler", "salbutamol", "albuterol", "asthalin", "ventolin", "levolin", "budesonide", "foracort", "seroflo", "puffer")
    private val insulin = listOf("insulin", "lantus", "mixtard", "humulin", "novorapid", "tresiba")
    private val sugarMeds = listOf("metformin", "glimepiride", "gliclazide", "glipizide", "sitagliptin", "vildagliptin")
    private val diabetesWords = listOf("diabet", "sugar", "मधुमेह", "शुगर", "మధుమేహ", "షుగర్", "ಮಧುಮೇಹ", "ಸಕ್ಕರೆ", "சர்க்கரை", "நீரிழிவு")
    private val thinner = listOf("warfarin", "acitrom", "clopidogrel", "apixaban", "rivaroxaban", "dabigatran", "ticagrelor", "ecosprin", "aspirin", "blood thinner")
    private val heartTab = listOf("nitroglycerin", "glyceryl", "sorbitrate", "isosorbide", "angispan", "nitrate", "gtn")
    private val heartWords = listOf("heart", "cardiac", "angina", "हृदय", "గుండె", "ಹೃದಯ", "இதய")
    private val asthmaWords = listOf("asthma", "copd", "दमा", "ఆస్తమా", "ಆಸ್ತಮಾ", "ஆஸ்துமா")
    private val bpWords = listOf("pressure", "hypertension", "bp", "रक्तचाप", "రక్తపోటు", "ರಕ್ತದೊತ್ತಡ", "இரத்த அழுத்தம்")

    fun guide(s: Situation, profile: MedicalProfile): Guide {
        val cond = profile.conditions
        val med = profile.medicines
        val alg = profile.allergies
        val diabetic = has(cond, diabetesWords) || has(med, insulin) || has(med, sugarMeds)
        val onInsulin = has(med, insulin)
        val onThinner = has(med, thinner)
        val p = mutableListOf<String>()
        if (alg.isNotBlank() && s in setOf(Situation.Allergy, Situation.Anaphylaxis)) p += "Known allergies on record: ${alg.trim()}. Keep away from these."
        when (s) {
            Situation.Anaphylaxis -> if (has(med, epi)) p += "They have an adrenaline auto-injector (EpiPen) listed. Use it now without waiting." else p += "No auto-injector is listed. Do not wait, call 112."
            Situation.Allergy -> if (has(med, epi)) p += "They carry an adrenaline auto-injector. Keep it close in case the reaction worsens."
            Situation.ChestPain -> {
                if (has(alg, listOf("aspirin"))) p += "Aspirin allergy is listed: do NOT take aspirin."
                if (has(med, heartTab)) p += "They have a heart tablet listed (${med.trim().take(80)}). Take it only exactly as the doctor prescribed."
                if (has(med, listOf("aspirin", "ecosprin"))) p += "They take daily aspirin. Tell the ambulance team."
                if (has(cond, heartWords)) p += "They have a heart condition listed. Say so when calling 112."
            }
            Situation.Stroke -> {
                if (has(cond, bpWords)) p += "They have high blood pressure listed. Say so when calling 112."
                if (onThinner) p += "They take a blood thinner. Tell the ambulance team."
            }
            Situation.LowSugar -> {
                if (diabetic) p += "They have diabetes listed, so low sugar is likely. Take fast sugar first, before anything else."
                if (onInsulin) p += "They take insulin: do not take more insulin until they have recovered and eaten, and tell the doctor."
            }
            Situation.HighSugar -> {
                if (diabetic) p += "They have diabetes listed. Take usual medicines as prescribed only."
                if (onInsulin) p += "They take insulin: use only the usual prescribed dose, never extra."
            }
            Situation.Breathless -> {
                if (has(med, inhaler)) p += "They have an inhaler listed. Use the reliever inhaler now, 1 puff every 30 to 60 seconds, up to 10 puffs."
                if (has(cond, asthmaWords)) p += "They have asthma or COPD listed. Sit upright and use the inhaler early."
                if (has(cond, heartWords)) p += "They have a heart condition listed: breathlessness can be a heart problem, so be quicker to call 112."
            }
            Situation.Fainting -> {
                if (diabetic) p += "They have diabetes listed: once awake and able to swallow, give fast sugar, as low sugar is a common cause."
                if (has(cond, heartWords)) p += "They have a heart condition listed. Tell the doctor about any faint."
            }
            Situation.Fall -> if (onThinner) p += "They take a blood thinner: any knock to the head or heavy bleeding means call 112 even if they feel fine."
            Situation.Bleeding -> if (onThinner) p += "They take a blood thinner, so bleeding can be heavier and slower to stop. Press longer and call 112 early."
            Situation.MedicineError -> {
                if (med.isNotBlank()) p += "Medicines on record: ${med.trim().take(200)}."
                if (onInsulin) p += "A double dose of insulin can cause very low sugar: take fast sugar and eat, and call 112 if drowsy or confused."
                if (onThinner) p += "A double dose of a blood thinner can cause bleeding: call the doctor now and watch for bleeding."
                if (has(med, heartTab) || has(cond, bpWords)) p += "A double dose of heart or blood pressure tablets can cause dizziness or fainting: sit or lie down and call the doctor."
            }
            Situation.Dizziness -> {
                if (diabetic) p += "They have diabetes listed: check sugar first, as low sugar causes dizziness."
                if (has(cond, bpWords)) p += "They have blood pressure listed: sit down, then tell the doctor, as blood pressure tablets can cause dizziness."
            }
            Situation.Heat -> {
                if (diabetic || has(cond, heartWords) || has(cond, bpWords)) p += "They have a long-term condition listed: heat is riskier for them, so be quicker to call 112 if not better soon."
            }
            Situation.Fever -> if (diabetic) p += "They have diabetes listed: infections raise sugar, so check sugar and call the doctor early."
            Situation.Burn, Situation.Choking -> Unit
        }
        return Guide(s, steps(s), dangerSigns(s), s.alwaysUrgent, p)
    }

    private fun steps(s: Situation): List<String> = when (s) {
        Situation.Allergy -> listOf(
            "Stay calm and sit down. Move away from what caused it, such as a food, insect or plant, if you know.",
            "Rinse a skin rash with cool water. A cool damp cloth eases itching. Try not to scratch.",
            "Take an antihistamine tablet only if your doctor has told you to take one.",
            "Remove tight rings or clothes near any swollen skin.",
            "Stay with someone and watch closely for the next 30 minutes. If any danger sign appears, call 112.",
        )
        Situation.Anaphylaxis -> listOf(
            "Call 112 now and say: severe allergic reaction.",
            "If there is an adrenaline auto-injector (EpiPen), use it now on the outer middle of the thigh, through clothes if needed. Hold it in place for 3 seconds.",
            "Lie down flat with legs raised. If breathing is hard, sit up instead. Do not stand or walk.",
            "If there is no improvement after 5 minutes and a second auto-injector is available, use it.",
            "Stay with the person until help arrives. If they stop responding and are not breathing normally, start CPR.",
        )
        Situation.Fall -> listOf(
            "Stay where you are for a moment. Do not get up quickly.",
            "Check for pain, bleeding, and whether you can move your arms and legs.",
            "If nothing hurts and you feel fine, roll onto your side, get onto hands and knees, crawl to a sturdy chair, and rise slowly. Then rest and drink some water.",
            "If you cannot get up, stay warm, keep calling for help or use your phone. Do not drag yourself if it hurts.",
            "Tell a family member about the fall even if you feel fine, and see a doctor within a day.",
        )
        Situation.ChestPain -> listOf(
            "Call 112 now and say: chest pain, possible heart attack.",
            "Stop everything and sit down to rest. Sitting half upright with knees bent is comfortable.",
            "Loosen tight clothes.",
            "If you are not allergic to aspirin and a doctor has not told you to avoid it, chew one adult aspirin (300 mg) slowly.",
            "Unlock the front door and ask someone to wait for the ambulance.",
            "If you stop responding and are not breathing normally, someone must start CPR: push hard and fast in the centre of the chest.",
        )
        Situation.Stroke -> listOf(
            "Face: ask them to smile. Does one side of the face droop?",
            "Arms: ask them to raise both arms. Does one arm drift down or feel weak?",
            "Speech: is the speech slurred, strange or confused?",
            "Time: if any of these is true, call 112 now. Note the time the signs began.",
            "Do not give food, drink or tablets, including aspirin.",
            "Keep them sitting or lying with head and shoulders slightly raised. If they become unresponsive but are breathing, lay them on their side.",
            "Unlock the front door and stay with them.",
        )
        Situation.LowSugar -> listOf(
            "Sit down. Take fast sugar now: 3 to 4 glucose tablets, or 3 teaspoons of sugar, or half a glass (150 ml) of fruit juice or a normal soft drink, or one tablespoon of honey.",
            "Wait 15 minutes. If you have a glucose meter, check again.",
            "If still shaky, sweaty or confused, take the same amount of sugar again.",
            "When you feel better, eat a snack such as biscuits or bread.",
            "Tell your family and doctor that your sugar was low.",
        )
        Situation.HighSugar -> listOf(
            "Sit calmly. Drink water, not sugary drinks.",
            "Check your sugar with your meter if you have one.",
            "Take your medicines only as prescribed. Do not take extra insulin or tablets on your own.",
            "Avoid sweets. Walk gently only if you feel well.",
            "Call your doctor today for advice.",
        )
        Situation.Choking -> listOf(
            "If they can cough, tell them to keep coughing. Do not slap the back and do not give water.",
            "If they cannot cough, speak or breathe, call 112 now and put the phone on speaker.",
            "Lean them forward and give up to 5 firm back blows between the shoulder blades with the heel of the hand.",
            "If that fails, give up to 5 abdominal thrusts: stand behind them, make a fist above the belly button, and pull hard inward and upward.",
            "Alternate 5 back blows and 5 abdominal thrusts until the object comes out or help arrives.",
            "If alone and choking, make a fist above your navel and thrust inward and upward, or press your belly firmly over the back of a chair.",
            "If they become unresponsive, start CPR.",
        )
        Situation.Breathless -> listOf(
            "Sit upright and lean slightly forward, resting your arms on a table or your knees. Do not lie flat.",
            "Loosen tight clothes. Open a window or use a fan for fresh air.",
            "Breathe slowly: in through the nose, out through pursed lips, as if blowing out a candle.",
            "If you have a reliever inhaler, use it: 1 puff every 30 to 60 seconds, up to 10 puffs. Use a spacer if you have one.",
            "Stay calm and ask someone to stay with you.",
            "If it is not getting better after the inhaler, call 112.",
        )
        Situation.Fainting -> listOf(
            "If you feel faint, sit or lie down at once and put your feet up.",
            "If someone has fainted, lay them flat on their back and raise their legs about 30 cm (12 inches) on a pillow.",
            "Loosen tight clothes and make sure there is fresh air.",
            "They usually wake within a minute. Check that they are breathing normally.",
            "When they wake, keep them lying for about 10 minutes. Sit up slowly, then stand slowly. Give water once fully awake.",
            "A first faint needs a doctor to check it, so call the doctor today.",
        )
        Situation.Fever -> listOf(
            "Rest and drink plenty of fluids.",
            "Wear light clothes and keep the room cool. Do not wrap up in blankets.",
            "Wipe the forehead and arms with a lukewarm, not cold, cloth.",
            "Paracetamol may be taken as the packet or your doctor says, if you are not allergic. Do not take more than the packet allows.",
            "Check the temperature every few hours.",
            "See a doctor today, especially if the fever lasts more than 2 days.",
        )
        Situation.Burn -> listOf(
            "Cool the burn at once under cool running water for at least 20 minutes. Do not use ice, butter, toothpaste or oil.",
            "Gently take off rings, watches and tight clothes near the burn before it swells, but not anything stuck to the skin.",
            "Cover the burn loosely with cling film or a clean, non-fluffy cloth.",
            "Do not burst blisters.",
            "Keep the rest of the body warm while cooling the burn.",
            "See a doctor for anything other than a small, mild burn.",
        )
        Situation.Bleeding -> listOf(
            "Sit or lie down. Press firmly on the wound with a clean cloth or pad. Use your hand if there is nothing else.",
            "Keep pressing for at least 10 minutes without lifting the cloth to look.",
            "If blood soaks through, put another pad on top. Do not remove the first one.",
            "Raise the injured arm or leg above the level of the heart if no bone seems broken.",
            "When the bleeding stops, bandage firmly, but not so tight that fingers or toes turn cold or blue.",
        )
        Situation.MedicineError -> listOf(
            "Missed dose: take it when you remember only if it is not nearly time for the next dose. Never take a double dose to catch up unless your doctor has said so.",
            "Took a double dose or the wrong medicine: do not take any more. Keep the packet or strip to show.",
            "Call your doctor or pharmacist now and tell them the medicine, the amount and the time.",
            "Do not make yourself vomit.",
            "Sit down and have someone stay with you. Watch how you feel over the next few hours.",
        )
        Situation.Dizziness -> listOf(
            "Sit or lie down at once so you do not fall.",
            "Stay still with your head slightly raised until it passes.",
            "Drink a glass of water. If you have not eaten, have a small snack.",
            "Stand up slowly later: sit on the edge of the bed for a minute first.",
            "Avoid stairs and walking alone until it has passed.",
            "If it happens again, see your doctor within a day or two.",
        )
        Situation.Heat -> listOf(
            "Move to a cool, shaded place or indoors, under a fan or air conditioning.",
            "Lie down and raise the legs. Loosen or remove extra clothes.",
            "Sip cool water, ORS or lemon water with a pinch of salt and sugar, in small sips.",
            "Cool the skin with a wet cloth on the neck, armpits and forehead, and fan them.",
            "Rest for the rest of the day. If not much better in 30 minutes, call 112.",
        )
    }

    private fun dangerSigns(s: Situation): List<String> = when (s) {
        Situation.Allergy -> listOf(
            "Swelling of the lips, tongue, face or throat",
            "Trouble breathing, wheezing or a tight throat",
            "Dizziness, feeling faint or a very fast heartbeat",
            "Spreading hives with vomiting or stomach cramps",
        )
        Situation.Anaphylaxis -> listOf("All of this is an emergency: call 112 now")
        Situation.Fall -> listOf(
            "Hit the head, or feel confused, sleepy or are vomiting",
            "Cannot get up or cannot move an arm or leg",
            "Severe pain in the hip, back, neck or any bone",
            "Heavy bleeding",
            "Takes a blood thinner and hit the head",
        )
        Situation.ChestPain -> listOf("All of this is an emergency: call 112 now")
        Situation.Stroke -> listOf("Any face droop, arm weakness or slurred speech is an emergency: call 112 now")
        Situation.LowSugar -> listOf(
            "Too drowsy or confused to swallow safely. Do not put food or drink in the mouth",
            "Unconscious or having a fit",
            "Not better after two rounds of sugar",
        )
        Situation.HighSugar -> listOf(
            "Vomiting and cannot keep fluids down",
            "Fruity breath, fast deep breathing or belly pain",
            "Very drowsy or confused",
            "Meter shows HI",
        )
        Situation.Choking -> listOf("Cannot cough, speak or breathe, or turning blue: call 112 now")
        Situation.Breathless -> listOf(
            "Cannot speak a full sentence",
            "Lips or face turning blue or grey",
            "Inhaler not helping after a few minutes",
            "Chest pain, confusion or very drowsy",
        )
        Situation.Fainting -> listOf(
            "Not waking within one minute",
            "Not breathing normally",
            "Hurt in the fall or hit the head",
            "Chest pain, racing or irregular heartbeat, or trouble speaking before the faint",
            "A fit",
        )
        Situation.Fever -> listOf(
            "Stiff neck, severe headache, new rash or confusion",
            "Trouble breathing or chest pain",
            "A fit",
            "Very weak, unable to drink, or passing very little urine",
            "Temperature above 103 F (39.4 C) that does not come down",
        )
        Situation.Burn -> listOf(
            "Burn larger than the palm of the hand",
            "Burn on the face, hands, feet, groin or a joint",
            "Deep, white or charred skin, or no pain in the burn",
            "Chemical or electrical burn",
            "Breathed in smoke or hot air, hoarse voice or coughing",
        )
        Situation.Bleeding -> listOf(
            "Blood spurting or soaking through pads, or not slowing after 10 minutes",
            "Deep or gaping wound, or something stuck in it",
            "Wound on the head, chest or belly",
            "Dizzy, pale, cold or faint",
            "Takes a blood thinner",
        )
        Situation.MedicineError -> listOf(
            "Very sleepy, confused or hard to wake",
            "Trouble breathing",
            "Chest pain, very fast or very slow heartbeat, or fainting",
            "A fit",
            "A large amount taken, or double insulin, blood thinner or heart medicine",
        )
        Situation.Dizziness -> listOf(
            "Chest pain, trouble breathing or a fast irregular heartbeat",
            "Face droop, weak arm, slurred speech or a sudden severe headache",
            "Faints or nearly faints repeatedly",
            "After a head injury",
            "Cannot walk straight or has double vision",
        )
        Situation.Heat -> listOf(
            "Confused, fitting or fainting",
            "Hot dry skin and no sweating",
            "Vomiting and cannot keep water down",
            "Not better within 30 minutes of cooling",
        )
    }
}

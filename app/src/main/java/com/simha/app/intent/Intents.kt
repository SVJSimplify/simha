package com.simha.app.intent

import com.simha.app.agenda.Rule
import com.simha.app.care.Clock
import java.time.DayOfWeek
import java.time.ZonedDateTime

sealed interface Cmd {
    data class Alarm(val times: List<String>, val rule: String, val date: String) : Cmd
    data class Remind(val what: String, val times: List<String>, val rule: String, val date: String) : Cmd
    data object AlarmList : Cmd
    data class AlarmCancel(val time: String?) : Cmd
    data class Snooze(val minutes: Int) : Cmd
    data class Call(val who: String) : Cmd
    data class WhatsApp(val who: String, val video: Boolean) : Cmd
    data object Call112 : Cmd
    data object Help : Cmd
    data class Volume(val dir: Int) : Cmd
    data class Torch(val on: Boolean) : Cmd
    data class Bright(val dir: Int) : Cmd
    data class Dnd(val on: Boolean) : Cmd
    data object TimeNow : Cmd
    data object DateNow : Cmd
    data object Battery : Cmd
    data class OpenApp(val name: String) : Cmd
    data class Music(val query: String) : Cmd
    data object Undo : Cmd
    data object MedTaken : Cmd
    data object MedSkipped : Cmd
    data object ReadScreen : Cmd
    data object Back : Cmd
    data object Home : Cmd
}

object Lex {
    val help = listOf("bachao", "emergency", "बचाओ", "आपातकाल", "కాపాడండి", "కాపాడు", "అత్యవసరం", "ಕಾಪಾಡಿ", "ತುರ್ತು", "காப்பாத்து", "காப்பாற்று", "அவசரம்")
    val helpShort = listOf("help", "help me", "मदद", "मदद करो", "सहायता", "సహాయం", "సాయం", "ಸಹಾಯ", "ಸಹಾಯ ಮಾಡಿ", "உதவி", "உதவுங்கள்")
    val ambulance = listOf("112", "108", "ambulance", "ambulence", "एम्बुलेंस", "एंबुलेंस", "అంబులెన్స్", "ಆಂಬುಲೆನ್ಸ್", "ஆம்புலன்ஸ்", "police", "पुलिस")
    val undo = listOf("undo", "wapas karo", "pehle jaisa", "wait no", "that's wrong", "change it back", "वापस करो", "पहले जैसा", "उल्टा करो", "వెనక్కి మార్చు", "ఇంతకు ముందులా", "ಹಿಂದಿನಂತೆ", "ಹಿಂದಕ್ಕೆ ಮಾಡು", "பழைய படி", "திரும்ப மாற்று")
    val timeQ = listOf("what time", "time kya", "kitne baje", "samay kya", "time batao", "time is it", "the time", "टाइम क्या", "कितने बजे", "समय क्या", "क्या टाइम", "టైం ఎంత", "సమయం ఎంత", "ఎన్ని గంటలు", "ಸಮಯ ಎಷ್ಟು", "ಟೈಮ್ ಎಷ್ಟು", "ಗಂಟೆ ಎಷ್ಟು", "மணி என்ன", "நேரம் என்ன", "மணி எவ்வளவு")
    val dateQ = listOf("what day", "what is the date", "date today", "kaun sa din", "aaj ki tarikh", "kya tarikh", "what date", "today's date", "which day", "आज कौन सा दिन", "आज क्या तारीख", "कौन सी तारीख", "आज तारीख", "ఈరోజు ఏ రోజు", "ఈరోజు తేదీ", "ఏ తేదీ", "ಇಂದು ಯಾವ ದಿನ", "ಇಂದಿನ ದಿನಾಂಕ", "ಯಾವ ತಾರೀಖು", "இன்று என்ன நாள்", "இன்று தேதி", "என்ன தேதி")
    val battery = listOf("battery", "बैटरी", "चार्ज कितना", "బ్యాటరీ", "ಬ್ಯಾಟರಿ", "பேட்டரி")
    val torch = listOf("torch", "flashlight", "flash light", "टॉर्च", "टार्च", "बत्ती", "టార్చ్", "ಟಾರ್ಚ್", "டார்ச்")
    val off = listOf("off", "band", "bujha", "बंद", "बुझा", "बुझाओ", "ऑफ", "ఆఫ్", "ఆపు", "ఆర్పు", "ಆಫ್", "ಆರಿಸು", "ஆஃப்", "அணை", "நிறுத்து")
    val volume = listOf("volume", "sound", "too loud", "turn it down", "turn down", "turn it up", "sunai nahi", "awaaz", "aawaz", "आवाज़", "आवाज", "वॉल्यूम", "louder", "softer", "can't hear", "cannot hear", "सुनाई नहीं", "శబ్దం", "వాల్యూమ్", "సౌండ్", "వినపడటం లేదు", "ಧ್ವನಿ", "ವಾಲ್ಯೂಮ್", "ಸೌಂಡ್", "ಕೇಳಿಸುತ್ತಿಲ್ಲ", "ஒலி", "சத்தம்", "வால்யூம்", "கேட்கவில்லை")
    val down = listOf("down", "decrease", "too loud", "dheema", "धीमी", "धीरे", "lower", "less", "softer", "reduce", "kam", "धीमा", "कम", "తగ్గించు", "తక్కువ", "ಕಡಿಮೆ", "ತಗ್ಗಿಸು", "குறை", "குறைவு")
    val max = listOf("max", "maximum", "full", "पूरा", "फुल", "पूरी", "పూర్తి", "ಪೂರ್ತಿ", "முழு")
    val mute = listOf("mute", "band", "बंद", "off", "silent", "chup", "चुप", "म्यूट", "మ్యూట్", "ಮ್ಯೂಟ್", "மியூட்")
    val bright = listOf("brightness", "bright", "brighter", "dim", "darker", "roshni", "रोशनी", "ब्राइटनेस", "चमक", "screen light", "ప్రకాశం", "బ్రైట్‌నెస్", "వెలుతురు", "ಪ್ರಕಾಶ", "ಬ್ರೈಟ್ನೆಸ್", "ಬೆಳಕು", "வெளிச்சம்", "பிரைட்னஸ்")
    val dark = listOf("dim", "darker", "less bright", "too bright", "कम", "తగ్గించు", "ಕಡಿಮೆ", "குறை")
    val dnd = listOf("do not disturb", "don't disturb", "dnd", "silent mode", "डू नॉट डिस्टर्ब", "साइलेंट मोड", "परेशान मत", "డిస్టర్బ్", "సైలెంట్ మోడ్", "ಡಿಸ್ಟರ್ಬ್", "ಸೈಲೆಂಟ್ ಮೋಡ್", "தொந்தரவு செய்யாதே", "சைலன்ட் மோட்")
    val alarm = listOf("alarm", "alarms", "alaram", "wake up", "jaga", "jaga dena", "utha dena", "अलार्म", "अलारम", "wake me", "जगा", "जगाना", "उठा देना", "उठाना", "అలారం", "అలారమ్", "లేపు", "ಅಲಾರಾಂ", "ಅಲಾರ್ಮ್", "ಎಬ್ಬಿಸು", "அலாரம்", "எழுப்பு")
    val remind = listOf("remind", "reminder", "reminders", "timer", "yaad dila", "याद दिला", "याद दिलाना", "याद दिलाओ", "रिमाइंडर", "గుర్తు చేయ", "గుర్తు చెయ్", "గుర్తుచేయ", "రిమైండర్", "ನೆನಪಿಸ", "ನೆನಪು ಮಾಡ", "ರಿಮೈಂಡರ್", "நினைவூட்ட", "ஞாபகப்படுத்த", "நினைவுபடுத்த", "ரிமைண்டர்")
    val list = listOf("list", "what", "which", "show", "tell me", "my alarms", "कौन", "क्या", "कितने", "बताओ", "ఏమి", "ఏవి", "చెప్పు", "ಯಾವ", "ಏನು", "ಹೇಳು", "என்ன", "எந்த", "சொல்லு")
    val cancel = listOf("cancel", "delete", "hata", "hatao", "band karo", "mita", "remove", "turn off", "stop", "clear", "हटा", "हटाओ", "रद्द", "बंद कर", "కాన్సిల్", "తీసేయ", "తీసివేయ", "రద్దు", "ಕ್ಯಾನ್ಸಲ್", "ತೆಗೆ", "ರದ್ದು", "ரத்து", "நீக்கு", "நிறுத்து")
    val snooze = listOf("snooze", "later", "baad mein", "बाद में", "थोड़ी देर", "తర్వాత", "కాసేపట్లో", "ಆಮೇಲೆ", "ಸ್ವಲ್ಪ ಹೊತ್ತಿನ", "அப்புறம்", "பிறகு")
    val call = listOf("call", "ring", "dial", "phone my", "phone to", "phone the", "phone karo", "call karo", "फोन कर", "फ़ोन कर", "कॉल कर", "फोन लगा", "फ़ोन लगा", "बात करा", "ఫోన్ చేయ", "ఫోన్ చెయ్", "కాల్ చేయ", "కాల్ చెయ్", "ಕರೆ ಮಾಡ", "ಫೋನ್ ಮಾಡ", "ಕಾಲ್ ಮಾಡ", "அழை", "போன் பண்ணு", "கால் பண்ணு", "போன் போடு")
    val whatsapp = listOf("whatsapp", "whats app", "व्हाट्सएप", "वॉट्सऐप", "వాట్సాప్", "ವಾಟ್ಸಾಪ್", "வாட்ஸ்அப்")
    val video = listOf("video", "वीडियो", "వీడియో", "ವಿಡಿಯೋ", "வீடியோ")
    val music = listOf("music", "song", "songs", "gaana", "gaane", "bhajan", "गाना", "गाने", "भजन", "संगीत", "పాట", "పాటలు", "సంగీతం", "భజన", "ಹಾಡು", "ಹಾಡುಗಳು", "ಸಂಗೀತ", "ಭಜನೆ", "பாட்டு", "பாடல்", "இசை", "பஜனை")
    val play = listOf("play", "listen", "put on", "sunao", "lagao", "सुनाओ", "chalao", "bajao", "चलाओ", "बजाओ", "लगाओ", "వినిపించు", "పెట్టు", "ಹಾಕು", "ಕೇಳಿಸು", "போடு", "ஒலிக்க")
    val open = listOf("open", "launch", "start", "kholo", "खोलो", "खोल", "ओपन", "తెరువు", "ఓపెన్", "ತೆರೆ", "ಓಪನ್", "திற", "ஓப்பன்")
    val medicine = listOf("medicine", "medicines", "tablet", "tablets", "pill", "pills", "dawa", "dawai", "goli", "दवा", "दवाई", "गोली", "మందు", "మాత్ర", "ಔಷಧ", "ಮಾತ್ರೆ", "மருந்து", "மாத்திரை")
    val took = listOf("took", "taken", "have taken", "had my", "le li", "kha li", "ले ली", "खा ली", "ले लिया", "खा लिया", "వేసుకున్నా", "తీసుకున్నా", "వేసుకున్నాను", "ತೆಗೆದುಕೊಂಡೆ", "ತಗೊಂಡೆ", "ತಗೊಂಡಿದ್ದೇನೆ", "எடுத்துக்கொண்டேன்", "சாப்பிட்டேன்", "போட்டுக்கிட்டேன்")
    val skipped = listOf("skipped", "did not take", "didn't take", "missed", "nahi li", "नहीं ली", "नहीं खाई", "छूट गई", "వేసుకోలేదు", "తీసుకోలేదు", "ತೆಗೆದುಕೊಂಡಿಲ್ಲ", "ತಗೊಂಡಿಲ್ಲ", "எடுக்கவில்லை", "சாப்பிடவில்லை")
    val readScreen = listOf("read the screen", "what's on the screen", "what is on the screen", "read this", "स्क्रीन पढ़", "यह पढ़ो", "ये पढ़ो", "స్క్రీన్ చదువు", "ఇది చదువు", "ಪರದೆ ಓದು", "ಇದನ್ನು ಓದು", "திரையை படி", "இதை படி")
    val back = listOf("go back", "back button", "पीछे जाओ", "वापस जाओ", "వెనక్కి వెళ్ళు", "ಹಿಂದೆ ಹೋಗು", "பின்னால் போ")
    val home = listOf("go home", "home screen", "होम स्क्रीन", "होम पर", "హోమ్ స్క్రీన్", "ಹೋಮ್ ಸ್ಕ್ರೀನ್", "முகப்புத் திரை")

    val daily = listOf("every day", "everyday", "daily", "each day", "every morning", "every night", "every evening", "roz", "rozana", "har din", "रोज़", "रोज", "रोजाना", "रोज़ाना", "हर दिन", "हर रोज", "प्रतिदिन", "ప్రతి రోజు", "ప్రతిరోజు", "రోజూ", "ಪ್ರತಿದಿನ", "ದಿನಾ", "ದಿನವೂ", "ಪ್ರತಿ ದಿನ", "தினமும்", "ஒவ்வொரு நாளும்")
    val weekdays = listOf("weekdays", "every weekday", "monday to friday")
    val every = listOf("every", "each", "har", "हर", "प्रति", "ప్రతి", "ಪ್ರತಿ", "ஒவ்வொரு")
    val tomorrow = listOf("tomorrow", "kal", "कल", "రేపు", "ನಾಳೆ", "நாளை")
    val month = listOf("every month", "monthly", "har mahine", "हर महीने", "महीने की", "ప్రతి నెల", "ಪ್ರತಿ ತಿಂಗಳು", "ஒவ்வொரு மாதமும்", "மாதந்தோறும்")
    val dayNames: Map<DayOfWeek, List<String>> = mapOf(
        DayOfWeek.MONDAY to listOf("monday", "somvar", "सोमवार", "సోమవారం", "ಸೋಮವಾರ", "திங்கள்"),
        DayOfWeek.TUESDAY to listOf("tuesday", "mangalvar", "मंगलवार", "మంగళవారం", "ಮಂಗಳವಾರ", "செவ்வாய்"),
        DayOfWeek.WEDNESDAY to listOf("wednesday", "budhvar", "बुधवार", "బుధవారం", "ಬುಧವಾರ", "புதன்"),
        DayOfWeek.THURSDAY to listOf("thursday", "guruvar", "गुरुवार", "గురువారం", "ಗುರುವಾರ", "வியாழன்"),
        DayOfWeek.FRIDAY to listOf("friday", "shukravar", "शुक्रवार", "శుక్రవారం", "ಶುಕ್ರವಾರ", "வெள்ளி"),
        DayOfWeek.SATURDAY to listOf("saturday", "shanivar", "शनिवार", "శనివారం", "ಶನಿವಾರ", "சனி"),
        DayOfWeek.SUNDAY to listOf("sunday", "ravivar", "रविवार", "ఆదివారం", "ಭಾನುವಾರ", "ಆದಿತ್ಯವಾರ", "ஞாயிறு"),
    )

    val drop = setOf(
        "um", "uh", "er", "hmm", "matlab", "dena", "dila", "dilana", "yaad", "mujhe", "subah", "shaam", "raat", "baje", "baad", "mein", "की", "का", "बाद", "में", "दिलाना", "me", "to", "about", "please", "at", "on", "for", "the", "a", "an", "and", "my", "set", "o'clock", "oclock", "clock", "in", "morning", "evening", "night", "afternoon", "am", "pm", "every", "day", "daily", "tomorrow", "today", "that", "i", "should", "need",
        "मुझे", "को", "कि", "के", "लिए", "बजे", "baje", "ka", "ki", "ko", "ke", "liye", "है", "सुबह", "शाम", "रात", "दोपहर", "हर", "रोज़", "रोज", "कल", "आज", "दिन", "देना", "दो", "करना",
        "నాకు", "కి", "గంటలకు", "ఉదయం", "సాయంత్రం", "రాత్రి", "రేపు", "ఈరోజు", "ప్రతి", "రోజు",
        "ನನಗೆ", "ಗೆ", "ಗಂಟೆಗೆ", "ಬೆಳಿಗ್ಗೆ", "ಸಂಜೆ", "ರಾತ್ರಿ", "ನಾಳೆ", "ಇಂದು", "ಪ್ರತಿ", "ದಿನ",
        "எனக்கு", "க்கு", "மணிக்கு", "காலை", "மாலை", "இரவு", "நாளை", "இன்று", "ஒவ்வொரு", "நாள்",
    )
}

data class When(val rule: String, val date: String, val rest: String)

object Parser {
    fun has(t: String, words: List<String>): Boolean = words.any { w -> hit(t, w) }

    private fun hit(t: String, w: String): Boolean =
        if (w.all { it.code < 0x80 }) Regex("(?<![a-z0-9])" + Regex.escape(w) + "(?![a-z0-9])").containsMatchIn(t) else t.contains(w)

    private fun mark(c: Char): Boolean = Character.getType(c).let { it == Character.NON_SPACING_MARK.toInt() || it == Character.COMBINING_SPACING_MARK.toInt() }

    private fun tidy(s: String): String = s.split(' ').filter { w ->
        w.isNotBlank() && !mark(w.first()) && !(w.length <= 2 && w.any { it.code > 0x7f })
    }.joinToString(" ").trim()

    private fun hasWord(t: String, words: List<String>): Boolean {
        val toks = t.split(' ').map { it.trim(',', '.', '?', '!', '।') }
        return words.any { w -> if (w.contains(' ')) hit(t, w) else w in toks }
    }

    private fun strip(t: String, words: List<String>): String {
        var s = t
        words.sortedByDescending { it.length }.forEach { w ->
            s = if (w.all { it.code < 0x80 }) s.replace(Regex("(?<![a-z0-9])" + Regex.escape(w) + "(?![a-z0-9])"), " ") else s.replace(w, " ")
        }
        return s.replace(Regex(" +"), " ").trim()
    }

    fun whenOf(t: String, now: ZonedDateTime): When {
        val today = now.toLocalDate()
        Regex("(?:every|har|हर|प्रति|ప్రతి|ಪ್ರತಿ|ஒவ்வொரு)\\s*(\\d{1,2})\\s*(?:hours?|hrs?|ghante|ghanta|घंटे|घंटा|గంటల\\S*|ಗಂಟೆ\\S*|மணி\\s*நேர\\S*)").find(t)?.let { m ->
            val n = m.groupValues[1].toInt()
            if (n in 1..24) return When(Rule.hours(n), "", t.replace(m.value, " "))
        }
        if (has(t, Lex.month)) {
            val d = Regex("(\\d{1,2})\\s*(st|nd|rd|th|tarikh|तारीख|వ|ನೇ|ஆம்|తేదీ|ತಾರೀಖು)").find(t)
            val dom = d?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..31 } ?: today.dayOfMonth
            val rest = if (d != null) t.replace(d.value, " ") else t
            return When(Rule.monthly(dom), "", strip(rest, Lex.month))
        }
        if (has(t, Lex.weekdays)) return When(Rule.WEEKDAYS, "", strip(t, Lex.weekdays))
        if (has(t, Lex.daily)) return When(Rule.DAILY, "", strip(t, Lex.daily))
        val all = Lex.dayNames.values.flatten()
        val named = Lex.dayNames.filter { (_, ws) -> ws.any { w -> hit(t, w) || hit(t, w + "s") } }.keys
        if (named.isNotEmpty()) {
            val rest = strip(t, all.flatMap { listOf(it + "s", it) })
            val recurring = has(t, Lex.every) || all.any { hit(t, it + "s") }
            if (recurring) return When(Rule.days(named), "", rest)
            val d = (1..7).map { today.plusDays(it.toLong()) }.first { it.dayOfWeek in named }
            return When(Rule.ONCE, d.toString(), rest)
        }
        if (has(t, Lex.tomorrow)) return When(Rule.ONCE, today.plusDays(1).toString(), strip(t, Lex.tomorrow))
        return When(Rule.ONCE, "", t)
    }

    private fun times(t: String, now: ZonedDateTime, alarm: Boolean = false, date: String = ""): List<String> {
        val n = Clock.normalize(t)
        if (!Regex("\\d").containsMatchIn(n) && !n.contains("noon") && !n.contains("midnight") && !n.contains("डेढ़") && !n.contains("ढाई")) return emptyList()
        val ref = if (date.isNotBlank() && date != now.toLocalDate().toString()) java.time.LocalTime.MIDNIGHT else now.toLocalTime()
        return Clock.resolveAll(t, ref, alarm)
    }

    private fun what(t: String): String {
        val noTrig = strip(t, Lex.remind + Lex.alarm)
        val toks = noTrig.split(' ').filter { w ->
            val x = w.trim(',', '.', '?', '!', '।')
            x.isNotEmpty() && x !in Lex.drop && !Regex("^\\d{1,2}([:.]\\d{2})?$").matches(x)
        }
        return tidy(toks.joinToString(" "))
    }

    fun minutes(t: String): Int? {
        Regex("(\\d{1,3})\\s*(minutes?|mins?|मिनट|నిమిష\\S*|ನಿಮಿಷ\\S*|நிமிட\\S*)").find(t)?.let { return it.groupValues[1].toInt().coerceIn(1, 240) }
        Regex("(\\d{1,2})\\s*(hours?|घंटे|घंटा|గంట\\S*|ಗಂಟೆ\\S*|மணி\\s*நேர\\S*)").find(t)?.let { return (it.groupValues[1].toInt() * 60).coerceIn(1, 720) }
        return null
    }

    private fun who(t: String): String = tidy(strip(t, Lex.call + Lex.whatsapp + Lex.video + listOf("phone", "voice", "on", "i want to", "i need to", "can you", "could you", "please", "my", "to", "now", "abhi", "ko", "lagao", "karo", "kar do", "को", "से", "अभी", "जरा", "ज़रा", "करो", "लगाओ", "కి", "కు", "చేయి", "ಗೆ", "ಮಾಡು", "க்கு", "பண்ணு")).split(' ').joinToString(" ") { w -> w.removeSuffix("ுக்கு").removeSuffix("ికి").removeSuffix("ಗೆ") })

    fun parse(raw: String, now: ZonedDateTime): Cmd? {
        val t = Clock.normalize(raw).trim().trimEnd('.', '?', '!', '।').trim()
        if (t.isEmpty()) return null
        val words = t.split(' ').count { it.isNotBlank() }

        if (has(t, Lex.help) || (words <= 4 && hasWord(t, Lex.helpShort) && !has(t, Lex.alarm + Lex.remind + Lex.call + Lex.volume))) return Cmd.Help
        if (has(t, Lex.ambulance)) return Cmd.Call112
        if (has(t, Lex.undo)) return Cmd.Undo
        if (has(t, Lex.readScreen)) return Cmd.ReadScreen
        if (words <= 4 && has(t, Lex.back)) return Cmd.Back
        if (words <= 4 && has(t, Lex.home)) return Cmd.Home
        if (has(t, Lex.timeQ) && !has(t, Lex.alarm + Lex.remind)) return Cmd.TimeNow
        if (has(t, Lex.dateQ)) return Cmd.DateNow
        if (has(t, Lex.battery)) return Cmd.Battery
        if (has(t, Lex.torch)) return Cmd.Torch(!has(t, Lex.off))
        if (has(t, Lex.dnd)) return Cmd.Dnd(!has(t, Lex.off))
        if (has(t, Lex.music) && has(t, Lex.play)) return Cmd.Music(tidy(strip(t, Lex.music + Lex.play + listOf("some", "a", "me", "to", "i want to", "kuch", "कुछ", "मुझे", "के"))))
        if (words <= 3 && hasWord(t, listOf("mute", "म्यूट"))) return Cmd.Volume(0)
        if (has(t, Lex.bright) && !has(t, Lex.volume)) return Cmd.Bright(if (has(t, Lex.max)) 2 else if (has(t, Lex.dark) || has(t, Lex.down)) -1 else 1)
        if (has(t, Lex.volume) && !has(t, Lex.alarm)) {
            val cantHear = t.contains("can't hear") || t.contains("cannot hear") || t.contains("सुनाई नहीं") || t.contains("వినపడటం లేదు") || t.contains("ಕೇಳಿಸುತ್ತಿಲ್ಲ") || t.contains("கேட்கவில்லை")
            return Cmd.Volume(
                when {
                    has(t, Lex.mute) || (!cantHear && has(t, Lex.off)) -> 0
                    has(t, Lex.max) -> 2
                    !cantHear && has(t, Lex.down) -> -1
                    else -> 1
                }
            )
        }

        if (has(t, Lex.alarm)) {
            if (has(t, Lex.cancel)) return Cmd.AlarmCancel(times(t, now, true).firstOrNull())
            val w = whenOf(t, now)
            val ts = times(w.rest, now, true, w.date)
            if (ts.isEmpty() && has(t, Lex.list)) return Cmd.AlarmList
            return Cmd.Alarm(ts, w.rule, w.date)
        }
        if (has(t, Lex.remind)) {
            val rel = minutes(t)
            if (rel != null && Regex("(\\bin\\b|\\bfor\\b|baad|mein|बाद|में|తర్వాత|లో|ನಂತರ|ದಲ್ಲಿ|பிறகு|இல்)").containsMatchIn(t)) {
                val at = now.plusMinutes(rel.toLong())
                val w = what(t.replace(Regex("\\d{1,3}\\s*\\S*"), " ")).ifBlank { if (t.contains("timer")) "timer" else "" }
                return Cmd.Remind(w, listOf("%02d:%02d".format(at.hour, at.minute)), Rule.ONCE, at.toLocalDate().toString())
            }
            val w = whenOf(t, now)
            return Cmd.Remind(what(w.rest), times(w.rest, now, false, w.date), w.rule, w.date)
        }
        if (has(t, listOf("snooze")) || (words <= 3 && has(t, Lex.snooze) && !has(t, Lex.call))) return Cmd.Snooze(minutes(t) ?: 10)

        if (has(t, Lex.medicine) || words <= 4) {
            if (has(t, Lex.skipped)) return Cmd.MedSkipped
            if (has(t, Lex.took)) return Cmd.MedTaken
        }

        if ((has(t, Lex.whatsapp) && (has(t, Lex.call) || has(t, Lex.video) || words <= 3)) || (has(t, Lex.video) && has(t, Lex.call))) return Cmd.WhatsApp(who(t), has(t, Lex.video))
        if (has(t, Lex.call) && !has(t, Lex.remind + Lex.alarm)) return Cmd.Call(who(t))
        if (has(t, Lex.music) && words <= 2) return Cmd.Music(tidy(strip(t, Lex.music)))
        if (has(t, Lex.open)) {
            val name = tidy(strip(t, Lex.open + listOf("app", "the", "please", "karo", "chey", "ऐप", "एप", "करो", "యాప్", "చేయి", "చెయ్", "ಆಪ್", "ಮಾಡು", "ஆப்", "பண்ணு")))
            if (name.isNotBlank() && name.split(' ').size <= 3) return Cmd.OpenApp(name)
        }
        return null
    }
}

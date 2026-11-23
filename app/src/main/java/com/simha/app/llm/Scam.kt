package com.simha.app.llm

data class ScamReport(val high: Boolean, val signals: List<String>, val rules: List<String>)

object Scam {
    private class Sig(val text: String, val words: List<String>, val severe: Boolean = false)

    private val sigs = listOf(
        Sig("They ask for an OTP, PIN, CVV, password or card number", listOf(" otp", "pin number", " pin ", "cvv", "password", "card number", "one time password"), true),
        Sig("They claim your bank account or KYC is blocked or must be updated", listOf("kyc", "account blocked", "account will be blocked", "account suspended", "account freeze", "update your account", "bank account")),
        Sig("They say you won a prize, lottery or gift", listOf("lottery", "prize", "you won", "won a", "lucky draw", "free gift", "reward")),
        Sig("They ask for gift cards, crypto or an urgent money transfer", listOf("gift card", "crypto", "bitcoin", "transfer money", "send money", " upi", "pay now", "processing fee", "advance payment"), true),
        Sig("They pressure you to act immediately or keep it secret", listOf("urgent", "immediately", "right now", "within an hour", "do not tell", "don't tell", "keep it secret", "last chance")),
        Sig("They pose as police, CBI, customs or a court and threaten arrest", listOf("police", "cbi", "customs", "arrest", "digital arrest", "court", "legal action", "narcotics"), true),
        Sig("They ask you to install an app or share your screen", listOf("anydesk", "teamviewer", "quicksupport", "screen share", "install an app", "install app", "remote access", "download an app", "apk"), true),
        Sig("They send a link to click", listOf("link", "click here", "click on", "sms link", "whatsapp link")),
        Sig("Someone claims to be a relative in trouble who needs money", listOf("grandson", "granddaughter", "your son is", "your daughter is", "relative in", "accident and need", "hospital and need")),
        Sig("They threaten to cut electricity, gas, SIM or a service", listOf("electricity", "power cut", "disconnect", "sim will be", "gas connection", "bill is pending")),
        Sig("A parcel, courier or customs fee you did not expect", listOf("parcel", "courier", "package")),
        Sig("They offer a refund, job or investment with big guaranteed returns", listOf("refund", "work from home", "guaranteed return", "double your money", "investment", "job offer")),
    )

    val rules = listOf(
        "Never share an OTP, PIN, CVV or password with anyone, even if they say they are from the bank.",
        "Banks never ask for KYC updates or card details over a phone call, SMS link or WhatsApp.",
        "Police, CBI or courts never arrest anyone over a phone or video call, and never ask for money.",
        "Hang up. Then call the official number printed on your card or on the bank's own website.",
        "Do not click links, install apps or share your screen at a stranger's request.",
        "Do not send money to anyone who rushes you. Talk to your family first.",
    )

    fun check(description: String): ScamReport {
        val t = " " + description.lowercase().replace('’', '\'') + " "
        val hits = sigs.filter { s -> s.words.any { t.contains(it) } }
        val high = hits.any { it.severe } || hits.size >= 2
        return ScamReport(high, hits.map { it.text }, rules)
    }
}

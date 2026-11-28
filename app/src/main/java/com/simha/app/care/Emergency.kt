package com.simha.app.care

import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.telephony.SmsManager
import com.simha.app.Actions
import com.simha.app.R
import com.simha.app.ServiceLocator
import com.simha.app.data.Repo
import com.simha.app.data.jo
import com.simha.app.forLang

object Emergency {
    suspend fun start(
        ctx: Context,
        sl: ServiceLocator,
        lang: String,
        say: suspend (String) -> Unit,
        listenStop: suspend (Int) -> Boolean,
    ) {
        val lc = ctx.forLang(lang)
        runCatching {
            val am = ctx.getSystemService(AudioManager::class.java)
            intArrayOf(AudioManager.STREAM_ALARM, AudioManager.STREAM_MUSIC).forEach { am.setStreamVolume(it, am.getStreamMaxVolume(it), 0) }
        }
        val family = runCatching { sl.repo.contactsNow().filter { it.role in Repo.FAMILY_ROLES } }.getOrDefault(emptyList())
        runCatching { sl.repo.audit("emergency_flow_start", jo("family" to family.size)) }
        if (family.isEmpty()) {
            runCatching { say(lc.getString(R.string.em_no_family)) }
            runCatching { sl.repo.dialEmergency("emergency_flow") }
            return
        }
        val first = family.firstOrNull { it.approved } ?: family.first()
        runCatching { say(lc.getString(R.string.em_countdown, first.name)) }
        val stopped = runCatching { listenStop(10) }.getOrDefault(false)
        if (stopped) {
            runCatching { sl.repo.audit("emergency_flow_stopped", jo("by" to "user_voice")) }
            runCatching { say(lc.getString(R.string.em_stopped)) }
            return
        }
        val numbers = family.map { it.phone }.filter { it.isNotBlank() }.distinct()
        val sent = runCatching { sendSms(ctx, lc, numbers, first.name) }.getOrDefault(false)
        runCatching { sl.repo.audit("emergency_flow_sms", jo("sent" to sent, "count" to numbers.size)) }
        if (sent) runCatching { say(lc.getString(R.string.em_sms_sent, first.name)) }
        runCatching { say(lc.getString(R.string.em_calling, first.name)) }
        val r = runCatching { sl.repo.placeCall(first.phone, first.approved, "emergency_flow") }.getOrNull()
        runCatching { sl.repo.audit("emergency_flow_call", jo("result" to (r?.name ?: "error"))) }
    }

    private fun sendSms(ctx: Context, lc: Context, numbers: List<String>, name: String): Boolean {
        if (numbers.isEmpty()) return false
        val loc = Actions.lastLocation(ctx)
        val link = if (loc != null) "https://maps.google.com/?q=" + loc.latitude + "," + loc.longitude else "not available"
        var body = lc.getString(R.string.em_sms, name, link)
        if (!body.contains(link)) body += " " + lc.getString(R.string.sms_loc, link)
        if (!Actions.has(ctx, Manifest.permission.SEND_SMS)) return Actions.sms(ctx, numbers, body)
        val sm = if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(SmsManager::class.java) else @Suppress("DEPRECATION") SmsManager.getDefault()
        val parts = sm.divideMessage(body)
        var ok = 0
        numbers.forEach { n -> if (runCatching { sm.sendMultipartTextMessage(n, null, parts, null, null) }.isSuccess) ok++ }
        return ok > 0
    }
}

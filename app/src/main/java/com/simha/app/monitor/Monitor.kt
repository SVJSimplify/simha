package com.simha.app.monitor

import android.content.Context
import com.simha.app.R
import com.simha.app.SimhaApp
import com.simha.app.data.K
import com.simha.app.data.Repo
import com.simha.app.data.Snap
import com.simha.app.data.jo
import com.simha.app.forLang
import com.simha.app.logic.Approvals
import com.simha.app.logic.Decision
import com.simha.app.logic.Rules
import com.simha.app.logic.RulesEngine
import com.simha.app.Actions
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object Monitor {
    private const val HEARTBEAT_MS = 15L * 60_000L
    private val mutex = Mutex()

    suspend fun tick(ctx: Context) {
        mutex.withLock { tickLocked(ctx) }
    }

    private suspend fun tickLocked(ctx: Context) {
        val sl = (ctx.applicationContext as SimhaApp).sl
        val p = sl.prefs.snap()
        if (p.role != "self" || !p.setup || !p.running) return
        val now = System.currentTimeMillis()
        val usageOk = UsageReader.granted(ctx)
        val usage = if (usageOk) UsageReader.lastUse(ctx, now) else null
        var last = maxOf(p.lastActivity, usage ?: 0L)
        if (last == 0L) last = now
        if (last != p.lastActivity) sl.prefs.edit { this[K.LAST_ACTIVITY] = last }
        if (now - p.lastHeartbeat >= HEARTBEAT_MS) {
            sl.prefs.edit { this[K.LAST_HEARTBEAT] = now }
            sl.repo.logEvent("heartbeat", jo("usage_access" to usageOk, "last_activity" to last))
        }
        val d = RulesEngine.decide(
            last,
            p.lastPrompt.takeIf { it > 0 },
            Rules(p.inactivity, p.wait),
            Approvals(p.callFamily, p.callHospital, p.record),
            now,
        )
        val lc = ctx.applicationContext.forLang(p.lang)
        when (d) {
            Decision.None -> Unit
            Decision.PromptCheckin -> {
                sl.prefs.edit { this[K.LAST_PROMPT] = now }
                sl.repo.logEvent("checkin", jo("usage_access" to usageOk))
                if (!Notifier.checkin(lc, p.name)) sl.repo.audit("notification_blocked", jo("kind" to "checkin"))
            }
            Decision.EscalateCallFamily, Decision.AlertFamily -> if (p.escalatedFor != p.lastPrompt) {
                sl.prefs.edit { this[K.ESCALATED_FOR] = p.lastPrompt }
                escalate(ctx, lc, sl.repo, p, d, now)
            }
        }
    }

    private suspend fun escalate(ctx: Context, lc: Context, repo: Repo, p: Snap, d: Decision, now: Long) {
        val fam = repo.contactsNow().filter { it.role in Repo.FAMILY_ROLES }
        val minutes = ((now - p.lastPrompt) / 60_000L).toInt()
        repo.logEvent("checkin_missed", jo("minutes" to minutes))
        val target = if (d == Decision.EscalateCallFamily) fam.firstOrNull { it.approved } else null
        if (target != null) {
            val ok = Notifier.escalate(lc, target.name, target.phone)
            repo.audit("escalation_call_prompted", jo("notified" to ok))
        }
        val loc = Actions.lastLocation(ctx)
        val link = loc?.let { " " + lc.getString(R.string.sms_loc, "https://maps.google.com/?q=" + it.latitude + "," + it.longitude) } ?: ""
        val body = lc.getString(R.string.sms_alert, p.name, minutes) + link
        val numbers = fam.map { it.phone }
        if (numbers.isEmpty()) {
            repo.logEvent("alert", jo("sent" to false, "reason" to "no_contacts"))
            repo.audit("alert_no_contacts")
            return
        }
        val ok = Notifier.smsAlert(lc, numbers, body)
        repo.logEvent("alert", jo("sms_prepared" to ok, "location_included" to (loc != null)))
        repo.audit("alert_sms_prepared", jo("notified" to ok, "location_included" to (loc != null), "recipients" to numbers.size))
    }
}

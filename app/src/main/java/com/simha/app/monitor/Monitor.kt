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
import com.simha.app.logic.NightState
import com.simha.app.logic.Routine
import com.simha.app.voice.Escalation
import com.simha.app.data.Weather
import com.simha.app.wake.RoutineStore
import com.simha.app.wake.Wake
import com.simha.app.wake.WakeReason
import com.simha.app.wake.WakeRequest
import com.simha.app.wake.WakeStore
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
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
                if (!Wake.fire(ctx, WakeRequest(WakeReason.Checkin))) sl.repo.audit("notification_blocked", jo("kind" to "checkin"))
                sl.repo.logEvent("checkin", jo("usage_access" to usageOk))
            }
            Decision.EscalateCallFamily, Decision.AlertFamily -> if (p.escalatedFor != p.lastPrompt) {
                sl.prefs.edit { this[K.ESCALATED_FOR] = p.lastPrompt }
                escalate(ctx, lc, sl.repo, p, d, now)
            }
        }
        aux(ctx, sl, usage, usageOk, now)
    }

    suspend fun markPrompt(ctx: Context, now: Long) {
        val sl = (ctx.applicationContext as SimhaApp).sl
        val p = sl.prefs.snap()
        if (p.lastPrompt > p.lastActivity) return
        sl.prefs.edit { this[K.LAST_PROMPT] = now }
    }

    private suspend fun step(name: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("Simha", "monitor step $name failed", e)
        }
    }

    private suspend fun aux(ctx: Context, sl: com.simha.app.ServiceLocator, usage: Long?, usageOk: Boolean, now: Long) {
        val zone = ZoneId.systemDefault()
        val pm = ctx.getSystemService(PowerManager::class.java)
        step("routine") {
            if (usage != null) RoutineStore.record(ctx, usage, now)
            if (pm?.isInteractive == true) RoutineStore.record(ctx, now, now)
        }
        step("early") { if (usageOk) earlyCheckin(ctx, sl, now, zone) }
        step("night") { nightWatch(ctx, pm?.isInteractive == true, now, zone) }
        step("family") { Wake.pollFamily(ctx) }
        step("weather") { Weather.refreshIfDue(ctx) }
        step("safe") { SafeArea.check(ctx) }
        step("wake") { Wake.refresh(ctx) }
    }

    private suspend fun earlyCheckin(ctx: Context, sl: com.simha.app.ServiceLocator, now: Long, zone: ZoneId) {
        val today = LocalDate.now(zone)
        val by = Routine.learn(RoutineStore.load(ctx), now, zone).expectedActiveBy(today, zone) ?: return
        val p = sl.prefs.snap()
        if (now < by || p.lastActivity >= Routine.dayStart(today, zone) || p.lastPrompt > p.lastActivity) return
        if (WakeStore.str(ctx, "early_day") == today.toString()) return
        WakeStore.put(ctx, "early_day" to today.toString())
        sl.prefs.edit { this[K.LAST_PROMPT] = now }
        if (!Wake.fire(ctx, WakeRequest(WakeReason.Checkin, "No phone activity yet today, well past the usual wake-up time."))) {
            sl.repo.audit("notification_blocked", jo("kind" to "early_checkin"))
        }
        sl.repo.logEvent("checkin", jo("early" to true))
    }

    private suspend fun nightWatch(ctx: Context, interactive: Boolean, now: Long, zone: ZoneId) {
        if (!Wake.ui(ctx).nightWatch) return
        val dt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), zone)
        val day = dt.toLocalDate().toString()
        val s = NightState(
            WakeStore.str(ctx, "night_day").orEmpty(),
            WakeStore.long(ctx, "night_ms"),
            WakeStore.bool(ctx, "night_fired"),
            WakeStore.long(ctx, "night_tick"),
        )
        val (n, fire) = Routine.nightStep(s, day, Routine.inNightWindow(dt.toLocalTime()), interactive, now)
        WakeStore.put(ctx, "night_day" to n.day, "night_ms" to n.ms, "night_fired" to n.fired, "night_tick" to n.lastTick)
        if (fire) Wake.fire(ctx, WakeRequest(WakeReason.NightWatch, "The phone has been in use for over five minutes in the middle of the night. Gently ask if the user cannot sleep or needs anything."))
    }

    private suspend fun escalate(ctx: Context, lc: Context, repo: Repo, p: Snap, d: Decision, now: Long) {
        val fam = repo.contactsNow().filter { it.role in Repo.FAMILY_ROLES }
        val minutes = ((now - p.lastPrompt) / 60_000L).toInt()
        repo.logEvent("checkin_missed", jo("minutes" to minutes))
        val target = if (d == Decision.EscalateCallFamily) fam.firstOrNull { it.approved } else null
        if (target != null) {
            if (!Escalation.callFamily(ctx, lc, p, target, minutes)) {
                val ok = Notifier.escalate(lc, target.name, target.phone)
                repo.audit("escalation_call_prompted", jo("notified" to ok))
            }
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

package com.simha.app.wake

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.simha.app.Actions
import com.simha.app.MainActivity
import com.simha.app.R
import com.simha.app.SimhaApp
import com.simha.app.data.Weather
import com.simha.app.data.jo
import com.simha.app.data.simhaStore
import com.simha.app.llm.Guardian
import com.simha.app.ui.home.UiSettings
import com.simha.app.ui.home.toUi
import com.simha.app.forLang
import com.simha.app.logic.Routine
import com.simha.app.logic.nextOccurrence
import com.simha.app.logic.parseTime
import com.simha.app.monitor.ActivityService
import com.simha.app.monitor.Monitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class WakeReason { Checkin, Medicine, Morning, Evening, Fall, NightWatch, FamilyMessage, Heat }

object WakeAudio {
    @Volatile
    var alarmMode = false

    fun begin(ctx: Context, attempt: Int) {
        try {
            val am = ctx.getSystemService(AudioManager::class.java) ?: return
            val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val cur = am.getStreamVolume(AudioManager.STREAM_ALARM)
            if (WakeStore.int(ctx, "vol_saved", -1) < 0) WakeStore.put(ctx, "vol_saved" to cur)
            val target = WakeMath.volumeTarget(max, cur, attempt)
            if (target != cur) am.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
            alarmMode = true
        } catch (e: Exception) {
            Log.w("Simha", "alarm volume failed", e)
        }
    }

    fun end(ctx: Context) {
        alarmMode = false
        try {
            val saved = WakeStore.int(ctx, "vol_saved", -1)
            if (saved >= 0) {
                ctx.getSystemService(AudioManager::class.java)?.setStreamVolume(AudioManager.STREAM_ALARM, saved, 0)
            }
        } catch (e: Exception) {
            Log.w("Simha", "alarm volume restore failed", e)
        } finally {
            WakeStore.put(ctx, "vol_saved" to null)
        }
    }
}

object Wake {
    const val X_KIND = "kind"
    const val X_TAG = "tag"
    const val X_AT = "at"
    const val X_REQ = "req"
    private const val ACTION = "com.simha.app.WAKE_ALARM"
    private const val CH_WAKE = "wake"
    private const val ID_WAKE = 21
    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    private const val POLL_MS = 15L * 60_000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    @Volatile
    private var replied = false

    fun noteReply() {
        replied = true
    }

    fun hasReplied(): Boolean = replied

    fun scheduleAll(ctx: Context) {
        val app = ctx.applicationContext
        scope.launch { guard("scheduleAll") { reschedule(app, true) } }
    }

    suspend fun scheduleNow(ctx: Context) = reschedule(ctx.applicationContext, true)

    suspend fun refresh(ctx: Context) = reschedule(ctx.applicationContext, false)

    fun wakeNow(ctx: Context, reason: WakeReason, extra: String? = null) {
        val app = ctx.applicationContext
        scope.launch { guard("wakeNow") { fire(app, WakeRequest(reason, extra)) } }
    }

    suspend fun ui(ctx: Context): UiSettings = ctx.applicationContext.simhaStore.data.first().toUi()

    private fun time(s: String, d: LocalTime): LocalTime = parseTime(s) ?: d

    fun inexactNote(ctx: Context): String? =
        if (WakeStore.bool(ctx, "exact", true)) null else ctx.getString(R.string.wake_note_inexact)

    fun audit(ctx: Context, action: String, detail: kotlinx.serialization.json.JsonObject = jo()) {
        val app = ctx.applicationContext
        scope.launch { guard("audit") { (app as SimhaApp).sl.repo.audit(action, detail) } }
    }

    fun restartService(ctx: Context) {
        val app = ctx.applicationContext
        scope.launch {
            guard("restartService") {
                val p = (app as SimhaApp).sl.prefs.snap()
                if (p.setup && p.role == "self" && p.running) ActivityService.start(app)
            }
        }
    }

    private suspend fun guard(name: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Simha", "wake $name failed", e)
        }
    }

    private fun alarmIntent(ctx: Context): Intent = Intent(ctx, AlarmReceiver::class.java).setAction(ACTION)

    private fun showIntent(ctx: Context): PendingIntent =
        PendingIntent.getActivity(ctx, 20, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), FLAGS)

    @SuppressLint("ScheduleExactAlarm")
    private fun setAt(ctx: Context, am: AlarmManager, code: Int, at: Long, fill: Intent.() -> Unit): Boolean {
        val pi = PendingIntent.getBroadcast(ctx, code, alarmIntent(ctx).apply(fill), FLAGS)
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(at, showIntent(ctx)), pi)
            return true
        } catch (e: SecurityException) {
            Log.w("Simha", "alarm clock denied", e)
        }
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            return true
        } catch (e: SecurityException) {
            Log.w("Simha", "exact alarm denied", e)
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        return false
    }

    private fun cancelCode(ctx: Context, am: AlarmManager, code: Int) {
        val pi = PendingIntent.getBroadcast(ctx, code, alarmIntent(ctx), FLAGS)
        am.cancel(pi)
        pi.cancel()
    }

    private suspend fun reschedule(ctx: Context, force: Boolean) = mutex.withLock {
        val sl = (ctx as SimhaApp).sl
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return@withLock
        val p = sl.prefs.snap()
        val oldMed = WakeStore.set(ctx, "med_codes")
        if (p.role != "self" || !p.setup || !p.running) {
            (listOf(WakeMath.CODE_CHECKIN, WakeMath.CODE_MORNING, WakeMath.CODE_EVENING, WakeMath.CODE_HEAT) + oldMed.map { it.toInt() })
                .forEach { cancelCode(ctx, am, it) }
            WakeStore.put(ctx, "sig" to null, "med_codes" to emptySet<String>())
            return@withLock
        }
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val meds = sl.guardian.medicines().first().filter { it.active }.flatMap { it.times }
        val routine = Routine.learn(RoutineStore.load(ctx), now, zone)
        val chk = WakeMath.checkinAt(if (p.lastActivity > 0) p.lastActivity else now, p.lastPrompt, p.inactivity, p.wait, p.escalatedFor, now)
        val ui = ui(ctx)
        val slots = WakeMath.plan(now, zone, chk, meds, routine.wake ?: time(ui.morning, WakeMath.MORNING), time(ui.evening, WakeMath.EVENING))
        val sig = WakeMath.signature(slots)
        if (!force && sig == WakeStore.str(ctx, "sig")) return@withLock
        var exact = true
        for (s in slots) {
            val ok = setAt(ctx, am, s.code, s.at) {
                putExtra(X_KIND, s.kind.name)
                putExtra(X_TAG, s.tag)
                putExtra(X_AT, s.at)
            }
            exact = exact && ok
        }
        val codes = slots.filter { it.kind == SlotKind.Medicine }.map { it.code.toString() }.toSet()
        (oldMed - codes).forEach { cancelCode(ctx, am, it.toInt()) }
        if (chk == null) cancelCode(ctx, am, WakeMath.CODE_CHECKIN)
        WakeStore.put(ctx, "sig" to sig, "med_codes" to codes, "exact" to exact)
    }

    suspend fun onAlarm(ctx: Context, kind: String, tag: String, at: Long, req: String?) {
        val sl = (ctx as SimhaApp).sl
        val p = sl.prefs.snap()
        if (p.role == "self" && p.setup && p.running) {
            when (kind) {
                "retry" -> WakeRequest.parse(req)?.let { onRetry(ctx, it) }
                SlotKind.Checkin.name -> Monitor.tick(ctx)
                SlotKind.Morning.name -> fire(ctx, WakeRequest(WakeReason.Morning, morningExtra(ctx)))
                SlotKind.Evening.name -> fire(ctx, WakeRequest(WakeReason.Evening))
                SlotKind.Heat.name -> Weather.note(ctx, true)?.let { fire(ctx, WakeRequest(WakeReason.Heat, it)) }
                SlotKind.Medicine.name -> {
                    val due = sl.guardian.medicines().first().filter { m ->
                        m.active && m.times.any { t -> parseTime(t)?.let { WakeMath.hhmm(it) } == tag }
                    }
                    if (due.isNotEmpty()) {
                        val names = due.joinToString(", ") { (it.name + " " + it.dose).trim() }
                        fire(ctx, WakeRequest(WakeReason.Medicine, names, medIds = due.map { it.id }, scheduledAt = at))
                    }
                }
            }
        }
        reschedule(ctx, true)
    }

    private suspend fun morningExtra(ctx: Context): String? {
        val parts = ArrayList<String>()
        Weather.note(ctx, true)?.let { parts.add(it) }
        val who = WakeStore.str(ctx, "fam_names").orEmpty()
        if (who.isNotBlank()) {
            parts.add("Unplayed voice messages from family are waiting (from: $who). Offer to play them.")
            WakeStore.put(ctx, "fam_announced" to (WakeStore.set(ctx, "fam_announced") + WakeStore.set(ctx, "fam_ids")))
        }
        return parts.joinToString(" ").ifBlank { null }
    }

    suspend fun pollFamily(ctx: Context) {
        val now = System.currentTimeMillis()
        if (now - WakeStore.long(ctx, "fam_poll") < POLL_MS) return
        WakeStore.put(ctx, "fam_poll" to now)
        val sl = (ctx.applicationContext as SimhaApp).sl
        val list = sl.guardian.fetchFamilyMessages().getOrElse {
            Log.w("Simha", "family poll failed: " + it.message)
            return
        }
        val un = list.filter { !it.played }
        WakeStore.put(ctx, "fam_ids" to un.map { it.id }.toSet(), "fam_names" to un.map { it.fromName }.distinct().joinToString(", "))
        val announced = WakeStore.set(ctx, "fam_announced")
        val fresh = un.filter { it.id !in announced }
        if (fresh.isEmpty()) return
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone).toString()
        val n = if (WakeStore.str(ctx, "fam_day") == today) WakeStore.int(ctx, "fam_n") else 0
        val hour = LocalTime.now(zone).hour
        val routine = Routine.learn(RoutineStore.load(ctx), now, zone)
        val nextMorning = nextOccurrence(listOf(WakeMath.hhmm(routine.wake ?: time(ui(ctx).morning, WakeMath.MORNING))), now, zone) ?: Long.MAX_VALUE
        if (hour !in 8..20 || n >= 2 || nextMorning - now < 3L * 3_600_000L) return
        WakeStore.put(ctx, "fam_day" to today, "fam_n" to n + 1, "fam_announced" to announced + fresh.map { it.id })
        fire(ctx, WakeRequest(WakeReason.FamilyMessage, "New voice message from " + fresh.map { it.fromName }.distinct().joinToString(", ")))
    }

    private fun ensureChannel(lc: Context) {
        val ch = NotificationChannel(CH_WAKE, lc.getString(R.string.ch_wake), NotificationManager.IMPORTANCE_HIGH)
        ch.setSound(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
        )
        ch.enableVibration(true)
        ch.lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        lc.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun title(r: WakeReason): Int = when (r) {
        WakeReason.Checkin -> R.string.wake_n_checkin
        WakeReason.Medicine -> R.string.wake_n_medicine
        WakeReason.Morning -> R.string.wake_n_morning
        WakeReason.Evening -> R.string.wake_n_evening
        WakeReason.Fall -> R.string.wake_n_fall
        WakeReason.NightWatch -> R.string.wake_n_night
        WakeReason.FamilyMessage -> R.string.wake_n_family
        WakeReason.Heat -> R.string.wake_n_heat
    }

    fun activityIntent(ctx: Context, r: WakeRequest): Intent =
        Intent(ctx, WakeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(X_REQ, r.toJson())

    @SuppressLint("MissingPermission")
    private fun notify(lc: Context, r: WakeRequest, i: Intent): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && !Actions.has(lc, Manifest.permission.POST_NOTIFICATIONS)) return false
        val pi = PendingIntent.getActivity(lc, 22, i, FLAGS)
        val n = NotificationCompat.Builder(lc, CH_WAKE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(lc.getString(title(r.reason)))
            .setContentText(lc.getString(R.string.wake_n_tap))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(10 * 60_000L)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .build()
        NotificationManagerCompat.from(lc).notify(ID_WAKE, n)
        return true
    }

    suspend fun fire(ctx: Context, req: WakeRequest): Boolean {
        val app = ctx.applicationContext
        val sl = (app as SimhaApp).sl
        val r = if (req.id == 0L) req.copy(id = System.currentTimeMillis()) else req
        WakeStore.put(app, "pending" to r.toJson())
        replied = false
        val lc = app.forLang(sl.prefs.snap().lang)
        ensureChannel(lc)
        WakeAudio.begin(app, r.attempt)
        val i = activityIntent(app, r)
        val posted = notify(lc, r, i)
        val canOverlay = Settings.canDrawOverlays(app)
        try {
            val snap = sl.prefs.snap()
            var text = Guardian.openingLine(lc, r.reason, snap.name, if (r.reason == WakeReason.Medicine) r.extra else null)
            if (!canOverlay) text += " " + lc.getString(R.string.wake_tap_say)
            sl.voice.speak(text, snap.lang)
        } catch (e: Exception) {
            Log.w("Simha", "wake speech failed", e)
        }
        try {
            app.startActivity(Intent(i))
        } catch (e: Exception) {
            Log.w("Simha", "direct wake start blocked", e)
        }
        scheduleRetry(app, r, WakeMath.FIRST_RETRY_MS)
        sl.repo.audit("wake", jo("reason" to r.reason.name, "attempt" to r.attempt, "notified" to posted))
        return posted
    }

    fun pending(ctx: Context): WakeRequest? = WakeRequest.parse(WakeStore.str(ctx, "pending"))

    fun scheduleRetry(ctx: Context, r: WakeRequest, delayMs: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val next = r.copy(attempt = r.attempt + 1)
        val at = System.currentTimeMillis() + delayMs
        setAt(ctx, am, WakeMath.CODE_RETRY, at) {
            putExtra(X_KIND, "retry")
            putExtra(X_AT, at)
            putExtra(X_REQ, next.toJson())
        }
    }

    fun afterSilence(ctx: Context) {
        val app = ctx.applicationContext
        val r = pending(app) ?: return
        scheduleRetry(app, r, WakeMath.RETRY_MS)
    }

    fun resolve(ctx: Context) {
        val app = ctx.applicationContext
        app.getSystemService(AlarmManager::class.java)?.let { cancelCode(app, it, WakeMath.CODE_RETRY) }
        WakeStore.put(app, "pending" to null)
        replied = false
        WakeAudio.end(app)
        NotificationManagerCompat.from(app).cancel(ID_WAKE)
    }

    private suspend fun answered(ctx: Context, r: WakeRequest): Boolean {
        val sl = (ctx as SimhaApp).sl
        return when (r.reason) {
            WakeReason.Checkin, WakeReason.Fall -> sl.prefs.snap().lastCheckinOk >= r.id
            WakeReason.Medicine -> sl.guardian.doses(r.id).first().any { it.medicineId in r.medIds && it.status != "missed" }
            else -> false
        }
    }

    private suspend fun onRetry(ctx: Context, r: WakeRequest) {
        val cur = pending(ctx)
        if (cur == null || cur.id != r.id) return
        if (answered(ctx, r)) {
            resolve(ctx)
            return
        }
        if (r.attempt <= WakeMath.MAX_ATTEMPTS) fire(ctx, r) else giveUp(ctx, r)
    }

    private suspend fun giveUp(ctx: Context, r: WakeRequest) {
        val sl = (ctx as SimhaApp).sl
        resolve(ctx)
        sl.repo.audit("wake_no_answer", jo("reason" to r.reason.name, "attempts" to WakeMath.MAX_ATTEMPTS))
        when (r.reason) {
            WakeReason.Medicine -> r.medIds.forEach { sl.guardian.logDose(it, r.scheduledAt, "missed") }
            WakeReason.Checkin -> Monitor.tick(ctx)
            WakeReason.Fall -> {
                Monitor.markPrompt(ctx, System.currentTimeMillis())
                Monitor.tick(ctx)
            }
            else -> Unit
        }
    }
}

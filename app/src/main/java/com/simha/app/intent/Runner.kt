package com.simha.app.intent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.simha.app.R
import com.simha.app.ServiceLocator
import com.simha.app.agenda.AgendaItem
import com.simha.app.agenda.AgendaStore
import com.simha.app.agenda.Recur
import com.simha.app.agenda.Rule
import com.simha.app.care.Clock
import com.simha.app.care.Slots
import com.simha.app.data.CallResult
import com.simha.app.llm.ContactInfo
import com.simha.app.llm.ContactMatch
import com.simha.app.phone.Journal
import com.simha.app.phone.Phone
import com.simha.app.phone.Res
import com.simha.app.phone.SimhaAccess
import com.simha.app.phone.Undoable
import com.simha.app.wake.Wake
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

class Runner(private val app: Context, private val sl: ServiceLocator) {
    class Countdown(val seconds: Int, val stoppedSay: String, val run: suspend () -> String)

    data class Out(val say: String, val emergency: Boolean = false, val countdown: Countdown? = null, val undo: Boolean = false)

    private var pending: Cmd? = null
    private var pendingAt = 0L

    fun clear() {
        pending = null
    }

    fun hasPending(): Boolean = pending != null && System.currentTimeMillis() - pendingAt < 120_000

    suspend fun handle(text: String, lc: Context, lang: String): Out? {
        val now = ZonedDateTime.now()
        val p = pending
        if (p != null && hasPending()) {
            if (Slots.isCancel(text)) {
                pending = null
                return Out(lc.getString(R.string.cf_cancelled))
            }
            val fresh = Parser.parse(text, now)
            val filled = if (fresh != null && fresh !is Cmd.Call) null else fill(p, text, now)
            if (filled != null) {
                pending = null
                return run(filled, lc, lang, now)
            }
        }
        pending = null
        val cmd = Parser.parse(text, now) ?: return null
        return run(cmd, lc, lang, now)
    }

    private fun short(t: String, max: Int): String? =
        t.trim().trimEnd('.', '!', '?', '।').trim().takeIf { it.isNotEmpty() && it.split(' ').count { w -> w.isNotBlank() } <= max }

    private fun fill(c: Cmd, text: String, now: ZonedDateTime): Cmd? = when (c) {
        is Cmd.Alarm -> Clock.resolveAll(text, now.toLocalTime()).takeIf { it.isNotEmpty() }?.let { c.copy(times = it) }
        is Cmd.Remind -> if (c.what.isBlank()) short(text, 12)?.let { c.copy(what = it) }
        else Clock.resolveAll(text, now.toLocalTime()).takeIf { it.isNotEmpty() }?.let { c.copy(times = it) }
        is Cmd.Call -> short(text, 5)?.let { Cmd.Call(it) }
        is Cmd.WhatsApp -> short(text, 5)?.let { c.copy(who = it) }
        else -> null
    }

    private fun ask(c: Cmd, s: String): Out {
        pending = c
        pendingAt = System.currentTimeMillis()
        return Out(s)
    }

    private fun locale(lang: String): Locale = Locale.forLanguageTag(lang)

    fun showTime(hhmm: String, lang: String): String =
        runCatching { LocalTime.parse(hhmm).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale(lang))) }.getOrDefault(hhmm)

    fun rulePhrase(rule: String, date: String, lc: Context, lang: String): String = when {
        rule == Rule.DAILY -> lc.getString(R.string.ix_r_daily)
        rule == Rule.WEEKDAYS -> lc.getString(R.string.ix_r_weekdays)
        rule.startsWith("days:") -> lc.getString(
            R.string.ix_r_days,
            rule.removePrefix("days:").split(",").mapNotNull { it.toIntOrNull() }.joinToString(", ") { DayOfWeek.of(it).getDisplayName(TextStyle.FULL, locale(lang)) },
        )
        rule.startsWith("hours:") -> lc.getString(R.string.ix_r_hours, rule.removePrefix("hours:").toIntOrNull() ?: 1)
        rule.startsWith("monthly:") -> lc.getString(R.string.ix_r_monthly, rule.removePrefix("monthly:").toIntOrNull() ?: 1)
        date.isNotBlank() && date != LocalDate.now().toString() -> lc.getString(
            R.string.ix_r_on,
            runCatching { LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale(lang))) }.getOrDefault(date),
        )
        else -> ""
    }

    private suspend fun save(kind: String, what: String, times: List<String>, rule: String, date: String, now: ZonedDateTime): List<AgendaItem> {
        val made = times.mapNotNull { t ->
            val lt = runCatching { LocalTime.parse(t) }.getOrNull() ?: return@mapNotNull null
            val d = if (rule == Rule.ONCE && date.isBlank()) {
                val today = now.toLocalDate()
                if (lt.isAfter(now.toLocalTime())) today.toString() else today.plusDays(1).toString()
            } else date
            val created = System.currentTimeMillis()
            val next = Recur.next(rule, t, d, created, now) ?: return@mapNotNull null
            AgendaItem(UUID.randomUUID().toString(), kind, what, t, rule, d, next.toInstant().toEpochMilli(), true, created)
        }
        made.forEach { sl.agenda.put(it) }
        if (made.isNotEmpty()) {
            Journal.push(Undoable(kind) {
                made.forEach { sl.agenda.delete(it.id) }
                Wake.scheduleAll(app)
                true
            })
            Wake.scheduleAll(app)
        }
        return made
    }

    private suspend fun contacts(): List<ContactInfo> = sl.repo.contactsNow().map { ContactInfo(it.name, it.phone, it.role, it.approved, it.id) }

    private fun openSetting(action: String) {
        val withPkg = runCatching { app.startActivity(Intent(action, Uri.parse("package:" + app.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        if (withPkg.isFailure) runCatching { app.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private suspend fun dueMeds(now: ZonedDateTime): List<Pair<String, Pair<String, Long>>> {
        val zone = now.zone
        val nowMs = now.toInstant().toEpochMilli()
        val dayStart = now.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val logs = sl.guardian.doses(dayStart).first()
        return sl.guardian.medicines().first().filter { it.active }.flatMap { m ->
            m.times.mapNotNull { t -> runCatching { LocalTime.parse(t.trim()) }.getOrNull() }
                .map { t -> m.id to (m.name to now.toLocalDate().atTime(t).atZone(zone).toInstant().toEpochMilli()) }
        }.filter { (id, p) -> kotlin.math.abs(p.second - nowMs) <= 4 * 3_600_000L && logs.none { it.medicineId == id && it.scheduledAt == p.second } }
    }

    suspend fun run(cmd: Cmd, lc: Context, lang: String, now: ZonedDateTime): Out = when (cmd) {
        is Cmd.Alarm -> {
            if (cmd.times.isEmpty()) ask(cmd, lc.getString(R.string.ix_ask_alarm_time))
            else {
                val made = save("alarm", "", cmd.times, cmd.rule, cmd.date, now)
                if (made.isEmpty()) ask(cmd.copy(times = emptyList()), lc.getString(R.string.ix_ask_alarm_time))
                else Out(lc.getString(R.string.ix_alarm_set, made.joinToString(", ") { showTime(it.time, lang) }, rulePhrase(cmd.rule, made.first().date, lc, lang)), undo = true)
            }
        }
        is Cmd.Remind -> when {
            cmd.what.isBlank() -> ask(cmd, lc.getString(R.string.ix_ask_rem_what))
            cmd.times.isEmpty() -> ask(cmd, lc.getString(R.string.ix_ask_rem_time))
            else -> {
                val made = save("reminder", cmd.what, cmd.times, cmd.rule, cmd.date, now)
                if (made.isEmpty()) ask(cmd.copy(times = emptyList()), lc.getString(R.string.ix_ask_rem_time))
                else Out(lc.getString(R.string.ix_rem_set, cmd.what, made.joinToString(", ") { showTime(it.time, lang) }, rulePhrase(cmd.rule, made.first().date, lc, lang)), undo = true)
            }
        }
        Cmd.AlarmList -> {
            val all = sl.agenda.active()
            if (all.isEmpty()) Out(lc.getString(R.string.ix_list_none))
            else Out(lc.getString(R.string.ix_list, all.take(8).joinToString("; ") { a ->
                (if (a.what.isBlank()) "" else a.what + ", ") + showTime(a.time, lang) + rulePhrase(a.rule, a.date, lc, lang)
            }))
        }
        is Cmd.AlarmCancel -> {
            val alarms = sl.agenda.active().filter { it.kind == "alarm" }
            val target = if (cmd.time != null) alarms.filter { it.time == cmd.time } else alarms
            if (target.isEmpty()) Out(lc.getString(R.string.ix_list_none))
            else {
                target.forEach { sl.agenda.put(it.copy(active = false)) }
                Journal.push(Undoable("alarm_cancel") {
                    target.forEach { sl.agenda.put(it) }
                    Wake.scheduleAll(app)
                    true
                })
                Wake.scheduleAll(app)
                if (target.size == 1) Out(lc.getString(R.string.ix_cancelled_one, showTime(target[0].time, lang)), undo = true)
                else Out(lc.getString(R.string.ix_cancelled_all), undo = true)
            }
        }
        is Cmd.Snooze -> {
            val at = now.plusMinutes(cmd.minutes.toLong())
            val what = AgendaStore.lastFired(app) ?: lc.getString(R.string.ix_snooze_default)
            save("reminder", what, listOf("%02d:%02d".format(at.hour, at.minute)), Rule.ONCE, at.toLocalDate().toString(), now)
            Out(lc.getString(R.string.ix_snoozed, cmd.minutes), undo = true)
        }
        is Cmd.Call -> {
            if (cmd.who.isBlank()) ask(cmd, lc.getString(R.string.ix_ask_who))
            else {
                val m = ContactMatch.find(cmd.who, contacts())
                when {
                    m.isEmpty() -> Out(lc.getString(R.string.ix_no_contact, cmd.who))
                    m.size > 1 -> ask(Cmd.Call(""), lc.getString(R.string.ix_which, m.joinToString(", ") { it.name }))
                    else -> {
                        val c = m[0]
                        Out(
                            lc.getString(R.string.ix_calling_in, c.name),
                            countdown = Countdown(5, lc.getString(R.string.ix_call_stopped)) {
                                when (sl.repo.placeCall(c.phone, c.approved, "voice_command")) {
                                    CallResult.Called -> ""
                                    CallResult.Dialer -> lc.getString(R.string.ix_dialer, c.name)
                                    CallResult.Failed -> lc.getString(R.string.ix_failed)
                                }
                            },
                        )
                    }
                }
            }
        }
        is Cmd.WhatsApp -> {
            if (cmd.who.isBlank()) ask(cmd, lc.getString(R.string.ix_ask_who))
            else {
                val m = ContactMatch.find(cmd.who, contacts())
                when {
                    m.isEmpty() -> Out(lc.getString(R.string.ix_no_contact, cmd.who))
                    m.size > 1 -> ask(cmd.copy(who = ""), lc.getString(R.string.ix_which, m.joinToString(", ") { it.name }))
                    else -> {
                        val c = m[0]
                        Out(
                            lc.getString(R.string.ix_wa_in, c.name),
                            countdown = Countdown(5, lc.getString(R.string.ix_call_stopped)) {
                                when (Phone.whatsappChat(app, c.phone)) {
                                    is Res.Ok -> {
                                        val pressed = SimhaAccess.instance != null &&
                                            SimhaAccess.clickWhenShown(if (cmd.video) SimhaAccess.videoCallLabels else SimhaAccess.voiceCallLabels)
                                        if (pressed) {
                                            Journal.push(Undoable("whatsapp") { SimhaAccess.back() })
                                            ""
                                        } else lc.getString(R.string.ix_wa_press)
                                    }
                                    Res.Missing -> lc.getString(R.string.ix_wa_missing)
                                    else -> lc.getString(R.string.ix_failed)
                                }
                            },
                        )
                    }
                }
            }
        }
        Cmd.Call112 -> {
            sl.repo.dialEmergency("voice_command")
            Out(lc.getString(R.string.ix_112), emergency = true)
        }
        Cmd.Help -> Out(lc.getString(R.string.ix_help_say), emergency = true)
        is Cmd.Volume -> when (val r = Phone.volume(app, cmd.dir)) {
            is Res.Ok -> Out(if (cmd.dir == 0) lc.getString(R.string.ix_mute) else lc.getString(R.string.ix_vol, r.value), undo = true)
            else -> Out(lc.getString(R.string.ix_failed))
        }
        is Cmd.Torch -> when (Phone.torch(app, cmd.on)) {
            is Res.Ok -> Out(lc.getString(if (cmd.on) R.string.ix_torch_on else R.string.ix_torch_off), undo = true)
            Res.Missing -> Out(lc.getString(R.string.ix_torch_missing))
            else -> Out(lc.getString(R.string.ix_failed))
        }
        is Cmd.Bright -> when (val r = Phone.brightness(app, cmd.dir)) {
            is Res.Ok -> Out(lc.getString(R.string.ix_bright, r.value), undo = true)
            Res.NeedPermission -> {
                openSetting(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                Out(lc.getString(R.string.ix_need_perm))
            }
            else -> Out(lc.getString(R.string.ix_failed))
        }
        is Cmd.Dnd -> when (Phone.dnd(app, cmd.on)) {
            is Res.Ok -> Out(lc.getString(if (cmd.on) R.string.ix_dnd_on else R.string.ix_dnd_off), undo = true)
            Res.NeedPermission -> {
                runCatching { app.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                Out(lc.getString(R.string.ix_need_perm))
            }
            else -> Out(lc.getString(R.string.ix_failed))
        }
        Cmd.TimeNow -> Out(lc.getString(R.string.ix_time, now.toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale(lang)))))
        Cmd.DateNow -> Out(lc.getString(R.string.ix_date, now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale(lang)))))
        Cmd.Battery -> {
            val (pct, charging) = Phone.battery(app)
            if (pct < 0) Out(lc.getString(R.string.ix_failed))
            else Out(lc.getString(if (charging) R.string.ix_battery_charging else R.string.ix_battery, pct))
        }
        is Cmd.OpenApp -> {
            val (r, label) = Phone.openApp(app, cmd.name)
            if (r is Res.Ok) {
                Journal.push(Undoable("open_app") { SimhaAccess.home() })
                Out(lc.getString(R.string.ix_open, label), undo = SimhaAccess.instance != null)
            } else Out(lc.getString(R.string.ix_app_missing, cmd.name))
        }
        is Cmd.Music -> {
            val q = cmd.query.ifBlank { lc.getString(R.string.ix_music_default) }
            if (Phone.music(app, q) is Res.Ok) Out(lc.getString(R.string.ix_music, q)) else Out(lc.getString(R.string.ix_failed))
        }
        Cmd.Undo -> Out(lc.getString(if (Journal.undo() != null) R.string.ix_undone else R.string.ix_nothing_undo))
        Cmd.MedTaken, Cmd.MedSkipped -> {
            val status = if (cmd == Cmd.MedTaken) "taken" else "skipped"
            val due = dueMeds(now)
            if (due.isEmpty()) Out(lc.getString(R.string.ix_med_none))
            else {
                due.forEach { (id, p) -> sl.guardian.logDose(id, p.second, status) }
                Out(lc.getString(if (status == "taken") R.string.ix_med_taken else R.string.ix_med_skipped, due.map { it.second.first }.distinct().joinToString(", ")))
            }
        }
        Cmd.ReadScreen -> {
            if (SimhaAccess.instance == null) Out(lc.getString(R.string.ix_access_needed))
            else {
                val t = SimhaAccess.readScreen(app.packageName)
                Out(if (t == null) lc.getString(R.string.ix_screen_none) else lc.getString(R.string.ix_screen, t))
            }
        }
        Cmd.Back -> Out(lc.getString(if (SimhaAccess.back()) R.string.ix_done else R.string.ix_access_needed))
        Cmd.Home -> Out(lc.getString(if (SimhaAccess.home()) R.string.ix_done else R.string.ix_access_needed))
    }
}

package com.simha.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.simha.app.Actions
import com.simha.app.data.Diary
import com.simha.app.data.Weekly
import com.simha.app.logic.Sig
import com.simha.app.data.FamilyMessage
import com.simha.app.ui.home.MsgRecorder
import com.simha.app.ui.home.dayStats
import com.simha.app.ui.home.weekStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.simha.app.MainViewModel
import com.simha.app.R
import com.simha.app.data.AuditRow
import com.simha.app.data.PersonView
import com.simha.app.data.Snap
import com.simha.app.logic.TimeUtil
import kotlinx.coroutines.launch

@Composable
private fun eventLabel(type: String): String = stringResource(
    when (type) {
        "heartbeat" -> R.string.ev_heartbeat
        "checkin" -> R.string.ev_checkin
        "checkin_ok" -> R.string.ev_checkin_ok
        "checkin_missed" -> R.string.ev_checkin_missed
        "alert" -> R.string.ev_alert
        else -> R.string.ev_help
    }
)

private fun shortTime(iso: String): String = iso.take(16).replace('T', ' ')

@Composable
fun CaregiverSetupScreen(vm: MainViewModel, s: Snap, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val title = stringResource(R.string.cg_setup_title)
    ScreenFrame(title, title, null) {
        Body(stringResource(R.string.cg_setup_body))
        LabeledField(stringResource(R.string.cg_code_label), code, { code = it.uppercase() }, singleLine = true)
        BigButton(stringResource(R.string.cg_join), {
            scope.launch {
                busy = true
                err = null
                vm.sl.repo.acceptInvite(code).fold(
                    { id ->
                        val name = vm.sl.repo.personView(id).getOrNull()?.name ?: ""
                        vm.bindPerson(id, name)
                        onDone()
                    },
                    { err = it.message ?: it.javaClass.simpleName },
                )
                busy = false
            }
        }, enabled = code.isNotBlank() && !busy)
        err?.let { Body(stringResource(R.string.cg_join_failed, it), color = MaterialTheme.colorScheme.error) }
    }
}

private enum class RecPhase { Idle, Recording, Ready }

@Composable
private fun VoiceMessageCard(vm: MainViewModel, s: Snap, sent: List<FamilyMessage>, onSent: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val rec = remember { MsgRecorder(ctx) }
    DisposableEffect(Unit) { onDispose { rec.discard() } }
    var phase by remember { mutableStateOf(RecPhase.Idle) }
    var secs by remember { mutableStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val micNeeded = stringResource(R.string.r3_cg_mic_needed)
    val family = stringResource(R.string.r3_cg_family_name)
    val sentOk = stringResource(R.string.r3_cg_rec_sent)
    val finish: () -> Unit = {
        phase = if (rec.stop()) RecPhase.Ready else RecPhase.Idle
    }
    val begin: () -> Unit = {
        rec.start { finish() }.fold(
            { phase = RecPhase.Recording; secs = 0; msg = null },
            { msg = ctx.getString(R.string.r3_cg_rec_error, it.message ?: it.javaClass.simpleName) },
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) begin() else msg = micNeeded
    }
    LaunchedEffect(phase) {
        while (phase == RecPhase.Recording) {
            delay(1000L)
            secs += 1
        }
    }
    Card {
        Body(stringResource(R.string.r3_cg_msg_title))
        when (phase) {
            RecPhase.Idle -> PrimaryButton(stringResource(R.string.r3_cg_rec), {
                if (Actions.has(ctx, Manifest.permission.RECORD_AUDIO)) begin() else launcher.launch(Manifest.permission.RECORD_AUDIO)
            })
            RecPhase.Recording -> {
                Caption(stringResource(R.string.r3_cg_rec_recording, secs))
                PrimaryButton(stringResource(R.string.r3_cg_rec_stop), finish)
            }
            RecPhase.Ready -> {
                SecondaryButton(
                    stringResource(if (playing) R.string.r3_cg_rec_stop else R.string.r3_cg_rec_preview),
                    {
                        if (playing) {
                            rec.stopPlay()
                            playing = false
                        } else {
                            playing = rec.play { playing = false }
                        }
                    },
                )
                PrimaryButton(stringResource(R.string.r3_cg_rec_send), {
                    val f = rec.file
                    val id = s.personId
                    if (f != null && id != null && !busy) {
                        busy = true
                        scope.launch {
                            val bytes = withContext(Dispatchers.IO) { runCatching { f.readBytes() } }
                            bytes.fold(
                                { b ->
                                    vm.sl.guardian.sendFamilyMessage(id, s.name.ifBlank { family }, b).fold(
                                        { rec.discard(); phase = RecPhase.Idle; playing = false; msg = sentOk; onSent() },
                                        { msg = ctx.getString(R.string.r3_cg_rec_failed, it.message ?: it.javaClass.simpleName) },
                                    )
                                },
                                { msg = ctx.getString(R.string.r3_cg_rec_failed, it.message ?: it.javaClass.simpleName) },
                            )
                            busy = false
                        }
                    }
                }, enabled = !busy)
                SecondaryButton(stringResource(R.string.r3_cg_rec_discard), { rec.discard(); phase = RecPhase.Idle; playing = false })
            }
        }
        msg?.let { Body(it) }
        Caption(stringResource(R.string.r3_cg_sent_title))
        if (sent.isEmpty()) Caption(stringResource(R.string.r3_cg_sent_none))
        sent.take(10).forEach {
            Caption(shortTime(it.createdAt) + "  " + stringResource(if (it.played) R.string.r3_cg_played else R.string.r3_cg_not_played))
        }
    }
}

@Composable
private fun SignalDetail(s: Sig) {
    fun lvl(v: String) = when (v) {
        "lo" -> R.string.sig_lo
        "hi" -> R.string.sig_hi
        "g" -> R.string.sig_good
        "p" -> R.string.sig_poor
        else -> R.string.sig_ok
    }
    fun xr(c: String) = when (c) {
        "fall" -> R.string.sig_x_fall
        "missed_ci" -> R.string.sig_x_missed_ci
        "missed_dose" -> R.string.sig_x_missed_dose
        "redflag" -> R.string.sig_x_redflag
        else -> R.string.sig_x_esc
    }
    if (s.pain.isNotEmpty()) Caption(stringResource(R.string.sig_pain, s.pain.entries.joinToString(", ") { it.key + " " + it.value }))
    s.sleep?.let { Caption(stringResource(R.string.sig_sleep, stringResource(lvl(it)))) }
    s.water?.let { Caption(stringResource(R.string.sig_water, stringResource(lvl(it)))) }
    s.activity?.let { Caption(stringResource(R.string.sig_activity, stringResource(lvl(it)))) }
    s.visitors?.let { Caption(stringResource(R.string.sig_visitors, it)) }
    if (s.exc.isNotEmpty()) Caption(stringResource(R.string.sig_exc, s.exc.joinToString(", ") { stringResource(xr(it)) }))
    s.note?.let { Caption(stringResource(R.string.sig_note, it)) }
}

@Composable
fun CaregiverHomeScreen(vm: MainViewModel, s: Snap, onSwitch: () -> Unit) {
    val scope = rememberCoroutineScope()
    var view by remember { mutableStateOf<PersonView?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var diaries by remember { mutableStateOf<List<Diary>>(emptyList()) }
    var weeks by remember { mutableStateOf<List<Weekly>>(emptyList()) }
    var sent by remember { mutableStateOf<List<FamilyMessage>>(emptyList()) }
    val reload: () -> Unit = {
        val id = s.personId
        if (id != null) {
            scope.launch {
                vm.sl.repo.personView(id).fold(
                    { view = it; err = null },
                    { err = it.message ?: it.javaClass.simpleName },
                )
            }
            scope.launch {
                vm.sl.guardian.diaryFor(id, 7).fold({ diaries = it }, { err = it.message ?: it.javaClass.simpleName })
                vm.sl.guardian.weeklyFor(id).fold({ weeks = it }, { err = it.message ?: it.javaClass.simpleName })
                vm.sl.guardian.sentFamilyMessages(id).fold({ sent = it }, { err = it.message ?: it.javaClass.simpleName })
            }
        }
    }
    LaunchedEffect(Unit) { reload() }
    val v = view
    val name = (v?.name ?: "").ifBlank { s.personName }
    val title = stringResource(R.string.cg_home_title, name)
    val lastOk = v?.events?.firstOrNull { it.type == "checkin_ok" }?.let { TimeUtil.isoMillis(it.createdAt) }
    val now = System.currentTimeMillis()
    ScreenFrame(title, title, null) {
        err?.let { Body(stringResource(R.string.sync_error, it), color = MaterialTheme.colorScheme.error) }
        Card { Body(stringResource(R.string.cg_last, agoText(TimeUtil.ago(now, lastOk)))) }
        if (v != null) {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val d = dayStats(v.events, diaries.firstOrNull { it.day == today }, today, zone)
            val w = weekStats(diaries, today)
            val noData = stringResource(R.string.r3_cg_none_data)
            Card {
                Body(stringResource(R.string.r3_cg_summary))
                Caption(stringResource(R.string.r3_cg_mood, d.mood ?: noData))
                Caption(stringResource(R.string.r3_cg_checkins, d.fine, d.missed))
                Caption(stringResource(R.string.r3_cg_doses, d.taken, d.dosesMissed))
                d.sig?.let { SignalDetail(it) }
            }
            Card {
                Body(stringResource(R.string.r3_cg_week))
                Caption(w.dosePct?.let { stringResource(R.string.r3_cg_week_doses, it) } ?: noData)
                Caption(stringResource(R.string.r3_cg_week_missed, w.missedCheckins))
                Caption(stringResource(R.string.r3_cg_week_mood, w.avgMood ?: noData))
                Caption(stringResource(R.string.r3_cg_week_activity))
                if (w.notes.isEmpty()) Caption(noData)
                w.notes.forEach { Body(it) }
                Caption(stringResource(R.string.sig_week_falls, w.falls))
                if (w.pain.isNotEmpty()) Caption(stringResource(R.string.sig_week_pain, w.pain.joinToString(", ")))
            }
            if (weeks.isNotEmpty()) {
                Card {
                    Body(stringResource(R.string.sig_older))
                    weeks.forEach { k ->
                        Caption(k.week)
                        Caption(k.roll.mdPct?.let { stringResource(R.string.r3_cg_week_doses, it) } ?: noData)
                        Caption(stringResource(R.string.r3_cg_week_missed, k.roll.ciMiss))
                        Caption(stringResource(R.string.r3_cg_week_mood, k.roll.mood?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: noData))
                        Caption(stringResource(R.string.sig_week_falls, k.roll.falls))
                        if (k.roll.pain.isNotEmpty()) Caption(stringResource(R.string.sig_week_pain, k.roll.pain.joinToString(", ")))
                    }
                }
            }
            VoiceMessageCard(vm, s, sent, reload)
            val alerts = v.events.filter { it.type == "alert" || it.type == "checkin_missed" || it.type == "help" }
            Card {
                Body(stringResource(R.string.cg_alerts))
                if (alerts.isEmpty()) Caption(stringResource(R.string.cg_empty))
                alerts.take(10).forEach { Caption(shortTime(it.createdAt) + "  " + eventLabel(it.type)) }
            }
            Card {
                Body(stringResource(R.string.cg_calls))
                if (v.calls.isEmpty()) Caption(stringResource(R.string.cg_empty))
                v.calls.take(10).forEach { Caption(shortTime(it.createdAt) + "  " + it.number + "  " + it.reason + "  " + it.outcome) }
            }
            Card {
                Body(stringResource(R.string.cg_recent))
                if (v.events.isEmpty()) Caption(stringResource(R.string.cg_empty))
                v.events.take(10).forEach { Caption(shortTime(it.createdAt) + "  " + eventLabel(it.type)) }
            }
            Card {
                if (v.canViewRecordings) {
                    Body(stringResource(R.string.cg_recordings, v.recordings.size))
                    v.recordings.forEach { Caption(shortTime(it.createdAt) + "  " + it.storageRef.substringAfterLast('/')) }
                } else {
                    Body(stringResource(R.string.cg_no_recordings))
                }
            }
        }
        BigButton(stringResource(R.string.cg_refresh), reload)
        BigButton(stringResource(R.string.cg_switch), onSwitch, kind = Kind.Secondary)
    }
}

@Composable
fun AuditScreen(vm: MainViewModel, onBack: () -> Unit) {
    var rows by remember { mutableStateOf<List<AuditRow>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val reload: () -> Unit = {
        scope.launch {
            vm.sl.repo.auditLog().fold(
                { rows = it; err = null },
                { err = it.message ?: it.javaClass.simpleName },
            )
        }
    }
    LaunchedEffect(Unit) { reload() }
    val title = stringResource(R.string.audit_title)
    ScreenFrame(title, title, onBack) {
        err?.let { Body(stringResource(R.string.sync_error, it), color = MaterialTheme.colorScheme.error) }
        val r = rows
        if (r != null && r.isEmpty()) Body(stringResource(R.string.audit_empty))
        r?.forEach { Card { Caption(shortTime(it.createdAt)); Body(it.action) } }
        BigButton(stringResource(R.string.cg_refresh), reload)
    }
}

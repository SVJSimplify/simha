package com.simha.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.simha.app.MainViewModel
import com.simha.app.Perm
import com.simha.app.Perms
import com.simha.app.R
import com.simha.app.TodayItem
import com.simha.app.WakeReady
import com.simha.app.data.CallResult
import com.simha.app.data.Repo
import com.simha.app.data.Snap
import com.simha.app.logic.TimeUtil
import com.simha.app.ui.home.OrbState
import com.simha.app.ui.home.SimhaOrb
import com.simha.app.ui.meds.nextDose
import com.simha.app.ui.meds.showMillis
import com.simha.app.ui.meds.todayDoses
import com.simha.app.ui.theme.LocalSimha
import com.simha.app.ui.theme.SimhaRadius
import com.simha.app.update.UpdateBanner
import com.simha.app.voice.WakeWord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RoleScreen(onSelf: () -> Unit, onCaregiver: () -> Unit) {
    val title = stringResource(R.string.role_title)
    ScreenFrame(title, title, null) {
        PrimaryButton(stringResource(R.string.role_self), onSelf)
        SecondaryButton(stringResource(R.string.role_caregiver), onCaregiver)
    }
}

@Composable
private fun ActionTile(icon: Int, text: String, onClick: () -> Unit, modifier: Modifier, tint: Color) {
    val c = LocalSimha.current
    val shape = RoundedCornerShape(SimhaRadius)
    Column(
        modifier
            .heightIn(min = 96.dp)
            .pressClick(onClick)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.border, shape)
            .padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Icon(painterResource(icon), null, Modifier.size(32.dp), tint = tint)
        Text(text, style = MaterialTheme.typography.labelLarge, color = c.text, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TodayRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Caption(label)
        Body(value)
    }
}

@Composable
internal fun todayText(i: TodayItem, who: String): String = when (i.kind) {
    "checkin_ok" -> stringResource(R.string.r3_tl_checkin_ok)
    "checkin_missed" -> stringResource(R.string.r3_tl_checkin_missed)
    "help" -> stringResource(R.string.r3_tl_help)
    "call" -> stringResource(R.string.r3_tl_call, who)
    else -> i.arg
}

@Composable
fun HomeScreen(
    vm: MainViewModel,
    s: Snap,
    onTalk: () -> Unit,
    onFamily: () -> Unit,
    onDoctor: () -> Unit,
    onHelp: () -> Unit,
    onMeds: () -> Unit,
    onFixWake: () -> Unit,
) {
    val ctx = LocalContext.current
    val c = LocalSimha.current
    val lang = LocalLang.current
    val scope = rememberCoroutineScope()
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000L)
            value = System.currentTimeMillis()
        }
    }
    val contacts by vm.contacts.collectAsState()
    val prof by vm.profile.collectAsState()
    val meds by vm.medicines.collectAsState()
    val doses by vm.doses.collectAsState()
    val ui by vm.ui.collectAsState()
    val tick by vm.resume.collectAsState()
    val level by vm.sl.voice.level.collectAsState()
    var failed by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()

    LaunchedEffect(tick) { vm.refreshToday() }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, ui.hey) {
        if (!ui.hey) {
            onDispose { }
        } else {
            val start = { if (Perms.granted(ctx, Perm.Mic)) WakeWord.startWakeWord(ctx) { onTalk() } }
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
            val obs = LifecycleEventObserver { _, e ->
                if (e == Lifecycle.Event.ON_RESUME) start()
                if (e == Lifecycle.Event.ON_PAUSE) WakeWord.stopWakeWord()
            }
            owner.lifecycle.addObserver(obs)
            onDispose {
                owner.lifecycle.removeObserver(obs)
                WakeWord.stopWakeWord()
            }
        }
    }

    val word = stringResource(
        when (LocalTime.now().hour) {
            in 5..11 -> R.string.r3_g_morning
            in 12..16 -> R.string.r3_g_afternoon
            in 17..20 -> R.string.r3_g_evening
            else -> R.string.r3_g_night
        }
    )
    val greet = if (s.name.isBlank()) word else word + ", " + s.name
    val date = remember(lang, now / 3_600_000L) {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag(lang)))
    }

    val hasMeds = meds.any { it.active }
    val due = remember(meds, doses, now / 60_000L) { todayDoses(meds, doses, now, zone) }
    val nd = nextDose(due, now)
    val todayState = vm.today

    val callFamily: () -> Unit = {
        val f = contacts.firstOrNull { it.role == "family" } ?: contacts.firstOrNull { it.role in Repo.FAMILY_ROLES }
        if (f == null) onFamily() else scope.launch {
            failed = vm.sl.repo.placeCall(f.phone, f.approved, "user_button") == CallResult.Failed
        }
    }
    val callDoctor: () -> Unit = {
        val d = contacts.firstOrNull { it.role == "doctor" }
        val phone = d?.phone ?: prof.doctorPhone.takeIf { it.isNotBlank() }
        if (phone == null) onDoctor() else scope.launch {
            failed = vm.sl.repo.placeCall(phone, d?.approved == true, "user_button") == CallResult.Failed
        }
    }
    val lastCheckin = stringResource(R.string.r3_last_checkin, agoText(TimeUtil.ago(now, s.lastCheckinOk.takeIf { it > 0 })))
    val moodLine = todayState.mood?.let { stringResource(R.string.r3_mood, it) }
    val wakeMissing = remember(tick) { WakeReady.missing(ctx) }

    TabFrame(
        top = { UpdateBanner() },
        bottom = { EmergencyButton(stringResource(R.string.m_get_help), onHelp) },
    ) {
        AnimatedEntrance(0) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(greet, style = MaterialTheme.typography.headlineSmall)
                Caption(date)
            }
        }
        AnimatedEntrance(1, Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SimhaOrb(
                    if (level > 0.05f) OrbState.Speaking else OrbState.Idle,
                    onTalk,
                    Modifier.size(200.dp),
                    level,
                )
                Body(stringResource(R.string.r4c_tap_talk), color = c.text2)
            }
        }
        var i = 2
        if (!s.running) {
            AnimatedEntrance(i++) {
                Card {
                    Body(stringResource(R.string.stopped_banner))
                    PrimaryButton(stringResource(R.string.set_start), { vm.startSimha() })
                }
            }
        }
        if (wakeMissing) {
            AnimatedEntrance(i++) {
                Card(Modifier.pressClick(onFixWake)) { Body(stringResource(R.string.wr_home), color = c.emergency) }
            }
        }
        AnimatedEntrance(i++) {
            Card {
                TodayRow(stringResource(R.string.r3_doing), if (moodLine == null) lastCheckin else lastCheckin + "\n" + moodLine)
                if (s.running) {
                    TodayRow(
                        stringResource(R.string.r3_next_checkin_label),
                        showMillis(maxOf(now, s.lastActivity + s.inactivity * 60_000L)),
                    )
                }
            }
        }
        AnimatedEntrance(i++) {
            if (hasMeds) {
                Card(Modifier.pressClick(onMeds)) {
                    TodayRow(
                        stringResource(R.string.r3_next_med),
                        if (nd == null) stringResource(R.string.r3_no_med)
                        else nd.medicine.name + (if (nd.medicine.dose.isBlank()) "" else " " + nd.medicine.dose) + "  " + showMillis(nd.at),
                    )
                    if (nd != null) SecondaryButton(stringResource(R.string.r3_mark_taken), { vm.logDose(nd.medicine.id, nd.at, "taken") })
                }
            } else {
                NavRow(stringResource(R.string.r4c_add_meds), onMeds)
            }
        }
        AnimatedEntrance(i++, Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionTile(R.drawable.ic_users, stringResource(R.string.m_call_family), callFamily, Modifier.weight(1f), c.primary)
                ActionTile(R.drawable.ic_phone, stringResource(R.string.m_call_doctor), callDoctor, Modifier.weight(1f), c.primary)
            }
        }
        if (failed) Body(stringResource(R.string.call_failed), color = c.emergency)
    }
}

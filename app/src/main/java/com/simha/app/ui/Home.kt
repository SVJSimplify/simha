package com.simha.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.simha.app.data.CallResult
import com.simha.app.data.DoseLog
import com.simha.app.data.Repo
import com.simha.app.data.Snap
import com.simha.app.logic.TimeUtil
import com.simha.app.ui.home.OrbState
import com.simha.app.ui.home.SimhaOrb
import com.simha.app.ui.meds.doseStatusText
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
    val l = LocalLayout.current
    val shape = RoundedCornerShape(SimhaRadius)
    Column(
        modifier
            .heightIn(min = l.primaryH)
            .pressClick(onClick)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.border, shape)
            .padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(icon), null, Modifier.size(28.dp), tint = tint)
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
private fun todayText(i: TodayItem, who: String): String = when (i.kind) {
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
    onUnwell: () -> Unit,
    onFamily: () -> Unit,
    onDoctor: () -> Unit,
    onHelp: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onMeds: () -> Unit,
) {
    val ctx = LocalContext.current
    val l = LocalLayout.current
    val c = LocalSimha.current
    val lang = LocalLang.current
    val scope = rememberCoroutineScope()
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000L)
            value = System.currentTimeMillis()
        }
    }
    val err by vm.syncError.collectAsState()
    val contacts by vm.contacts.collectAsState()
    val prof by vm.profile.collectAsState()
    val meds by vm.medicines.collectAsState()
    val doses by vm.doses.collectAsState()
    val ui by vm.ui.collectAsState()
    val tick by vm.resume.collectAsState()
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

    val due = remember(meds, doses, now / 60_000L) { todayDoses(meds, doses, now, zone) }
    val nd = nextDose(due, now)
    val dayStart = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
    val medName = { id: String? -> meds.firstOrNull { it.id == id }?.name ?: "" }
    val todayState = vm.today
    val tl = remember(todayState, doses) {
        val items = mutableListOf<Pair<Long, Any>>()
        todayState.items.forEach { items += it.at to it }
        doses.filter { it.scheduledAt >= dayStart }.forEach { items += (if (it.createdAt > 0) it.createdAt else it.scheduledAt) to it }
        items.sortedByDescending { it.first }.take(8)
    }

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
    val shape = RoundedCornerShape(SimhaRadius)
    val lastCheckin = stringResource(R.string.r3_last_checkin, agoText(TimeUtil.ago(now, s.lastCheckinOk.takeIf { it > 0 })))
    val moodLine = todayState.mood?.let { stringResource(R.string.r3_mood, it) }

    CompositionLocalProvider(LocalContentColor provides c.text) {
        Box(
            Modifier
                .fillMaxSize()
                .background(c.page)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                UpdateBanner()
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = l.pad, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(greet, style = MaterialTheme.typography.headlineSmall)
                        Caption(date)
                    }
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SimhaOrb(OrbState.Idle, onTalk, Modifier.fillMaxWidth(0.7f))
                        Body(
                            stringResource(if (ui.hey) R.string.r3_orb_caption else R.string.r3_orb_caption_tap),
                            color = c.text2,
                        )
                    }
                    if (!s.running) {
                        Card {
                            Body(stringResource(R.string.stopped_banner))
                            PrimaryButton(stringResource(R.string.set_start), { vm.startSimha() })
                        }
                    }
                    err?.let { Card { Body(stringResource(R.string.sync_error, it)) } }
                    Card {
                        Text(stringResource(R.string.r3_today), style = MaterialTheme.typography.titleMedium)
                        TodayRow(stringResource(R.string.r3_doing), if (moodLine == null) lastCheckin else lastCheckin + "\n" + moodLine)
                        TodayRow(
                            stringResource(R.string.r3_next_med),
                            if (nd == null) stringResource(R.string.r3_no_med)
                            else nd.medicine.name + (if (nd.medicine.dose.isBlank()) "" else " " + nd.medicine.dose) + "  " + showMillis(nd.at),
                            Modifier.pressClick(onMeds),
                        )
                        if (nd != null) SecondaryButton(stringResource(R.string.r3_mark_taken), { vm.logDose(nd.medicine.id, nd.at, "taken") })
                        if (s.running) {
                            TodayRow(
                                stringResource(R.string.r3_next_checkin_label),
                                showMillis(maxOf(now, s.lastActivity + s.inactivity * 60_000L)),
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionTile(R.drawable.ic_heart, stringResource(R.string.r3_unwell), onUnwell, Modifier.weight(1f), c.emergency)
                        ActionTile(R.drawable.ic_users, stringResource(R.string.m_call_family), callFamily, Modifier.weight(1f), c.primary)
                        ActionTile(R.drawable.ic_phone, stringResource(R.string.m_call_doctor), callDoctor, Modifier.weight(1f), c.primary)
                    }
                    if (failed) Body(stringResource(R.string.call_failed), color = c.emergency)
                    Column(
                        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.border, shape).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.r3_timeline), style = MaterialTheme.typography.titleMedium)
                        if (tl.isEmpty()) Caption(stringResource(R.string.r3_tl_empty))
                        tl.forEach { (at, any) ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Caption(showMillis(at), Modifier.widthIn(min = 72.dp))
                                Body(
                                    when (any) {
                                        is TodayItem -> todayText(any, contacts.firstOrNull { it.phone == any.arg }?.name ?: any.arg)
                                        is DoseLog -> doseStatusText(any.status) + " " + medName(any.medicineId)
                                        else -> ""
                                    },
                                    Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        TextLink(stringResource(R.string.m_link_profile), onProfile)
                        TextLink(stringResource(R.string.m_link_settings), onSettings)
                    }
                }
                Box(Modifier.fillMaxWidth().padding(horizontal = l.pad, vertical = 12.dp)) {
                    EmergencyButton(stringResource(R.string.m_get_help), onHelp)
                }
            }
        }
    }
}

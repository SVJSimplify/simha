package com.simha.app.ui

import android.Manifest
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.simha.app.BuildConfig
import com.simha.app.MainViewModel
import com.simha.app.Perm
import com.simha.app.Perms
import com.simha.app.R
import com.simha.app.WakeReady
import com.simha.app.WakeReq
import com.simha.app.data.CaregiverRow
import com.simha.app.data.Repo
import com.simha.app.data.Snap
import com.simha.app.ui.home.Oem
import com.simha.app.ui.meds.TimeDialog
import com.simha.app.ui.meds.showTime
import com.simha.app.ui.talk.AiSettingsSection
import com.simha.app.ui.theme.LocalSimha
import com.simha.app.update.UpdateBanner
import com.simha.app.update.UpdateState
import com.simha.app.wake.Wake
import com.simha.app.wake.WakeReason
import kotlinx.coroutines.launch

private val INACTIVITY_HOURS = listOf(1, 2, 3, 4, 6, 8)
private val WAIT_MINUTES = listOf(5, 10, 15)

private fun permRes(p: Perm): Pair<Int, Int> = when (p) {
    Perm.Notifications -> R.string.perm_notif to R.string.perm_notif_short
    Perm.Mic -> R.string.perm_mic to R.string.perm_mic_short
    Perm.Camera -> R.string.perm_cam to R.string.perm_cam_short
    Perm.Location -> R.string.perm_loc to R.string.perm_loc_short
    Perm.Usage -> R.string.perm_usage to R.string.perm_usage_short
    Perm.FullScreen -> R.string.perm_full to R.string.perm_full_short
    Perm.Battery -> R.string.perm_batt to R.string.perm_batt_short
    Perm.CallPhone -> R.string.perm_call to R.string.perm_call_short
    Perm.Overlay -> R.string.perm_overlay to R.string.perm_overlay_short
}

@Composable
private fun hoursText(h: Int): String = LocalContext.current.resources.getQuantityString(R.plurals.m_hours, h, h)

@Composable
private fun minutesText(m: Int): String = LocalContext.current.resources.getQuantityString(R.plurals.m_minutes, m, m)

@Composable
fun SettingsScreen(onPage: (String) -> Unit) {
    val rows = listOf(
        "me" to R.string.r4c_s_me,
        "people" to R.string.r4c_s_people,
        "checkins" to R.string.r4c_s_checkins,
        "ai" to R.string.m_set_ai,
        "privacy" to R.string.r4c_s_privacy,
        "about" to R.string.r4c_s_about,
    )
    TabFrame {
        AnimatedEntrance(0) { Text(stringResource(R.string.set_title), style = MaterialTheme.typography.headlineSmall) }
        rows.forEachIndexed { i, (page, label) ->
            AnimatedEntrance(i + 1, Modifier.fillMaxWidth()) { NavRow(stringResource(label), { onPage(page) }) }
        }
    }
}

@Composable
fun SettingsPage(
    vm: MainViewModel,
    s: Snap,
    page: String,
    onBack: () -> Unit,
    onAudit: () -> Unit,
    onContacts: (String) -> Unit,
) {
    val title = stringResource(
        when (page) {
            "me" -> R.string.r4c_s_me
            "people" -> R.string.r4c_s_people
            "checkins" -> R.string.r4c_s_checkins
            "ai" -> R.string.m_set_ai
            "privacy" -> R.string.r4c_s_privacy
            else -> R.string.r4c_s_about
        }
    )
    ScreenFrame(title, title, onBack) {
        when (page) {
            "me" -> MePage(vm, s)
            "people" -> PeoplePage(vm, onContacts)
            "checkins" -> CheckinsPage(vm, s)
            "ai" -> AiSettingsSection()
            "privacy" -> PrivacyPage(vm, s, onAudit)
            else -> AboutPage(vm, s)
        }
    }
}

@Composable
private fun MePage(vm: MainViewModel, s: Snap) {
    var name by remember(s.name) { mutableStateOf(s.name) }
    var saved by remember { mutableStateOf(false) }
    LabeledField(stringResource(R.string.o_name_label_self), name, { name = it; saved = false })
    PrimaryButton(stringResource(R.string.save), { vm.saveName(name.trim()); saved = true }, enabled = name.isNotBlank())
    if (saved) Body(stringResource(R.string.saved), color = LocalSimha.current.success)
    SectionTitle(stringResource(R.string.set_language))
    listOf("en" to R.string.lang_en, "hi" to R.string.lang_hi, "te" to R.string.lang_te, "kn" to R.string.lang_kn, "ta" to R.string.lang_ta)
        .forEach { (code, res) ->
            OptionRow(stringResource(res), s.lang == code, { if (s.lang != code) vm.setLanguage(code) })
        }
}

@Composable
private fun PeoplePage(vm: MainViewModel, onContacts: (String) -> Unit) {
    val sl = vm.sl
    val scope = rememberCoroutineScope()
    val contacts by vm.contacts.collectAsState()
    var cgs by remember { mutableStateOf<List<CaregiverRow>>(emptyList()) }
    var cgErr by remember { mutableStateOf<String?>(null) }
    val reload: () -> Unit = {
        scope.launch {
            sl.repo.caregivers().fold({ cgs = it.filter { c -> c.status != "removed" }; cgErr = null }, { cgErr = it.message ?: it.javaClass.simpleName })
        }
    }
    LaunchedEffect(Unit) { if (Repo.CAREGIVER_ENABLED) reload() }
    val noFamily = contacts.none { it.role in Repo.FAMILY_ROLES }
    if (noFamily) Body(stringResource(R.string.m_contacts_missing), color = LocalSimha.current.emergency)
    NavRow(stringResource(R.string.fam_title), { onContacts("family") })
    NavRow(stringResource(R.string.doc_title), { onContacts("doctor") })
    if (!Repo.CAREGIVER_ENABLED) return
    SectionTitle(stringResource(R.string.set_caregivers))
    cgErr?.let { Body(stringResource(R.string.sync_error, it), color = LocalSimha.current.emergency) }
    if (cgs.isEmpty()) Body(stringResource(R.string.cg_none))
    cgs.forEach { c ->
        Card {
            Body(stringResource(R.string.cg_code, c.inviteCode))
            Caption(
                stringResource(
                    R.string.cg_status,
                    stringResource(if (c.status == "active") R.string.cg_active else R.string.cg_pending),
                )
            )
            SwitchRow(stringResource(R.string.cg_view_rec), c.canViewRecordings, { on ->
                scope.launch {
                    sl.repo.setCaregiverRecordings(c.id, on).onFailure { cgErr = it.message ?: it.javaClass.simpleName }
                    reload()
                }
            })
            SecondaryButton(stringResource(R.string.cg_remove), {
                scope.launch {
                    sl.repo.removeCaregiver(c.id).onFailure { cgErr = it.message ?: it.javaClass.simpleName }
                    reload()
                }
            })
        }
    }
    PrimaryButton(stringResource(R.string.cg_invite), {
        scope.launch {
            sl.repo.createInvite().fold({ reload() }, { cgErr = it.message ?: it.javaClass.simpleName })
        }
    })
}

@Composable
private fun CheckinsPage(vm: MainViewModel, s: Snap) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val tick by vm.resume.collectAsState()
    val gs by vm.ui.collectAsState()
    var pickMorning by remember { mutableStateOf(false) }
    var pickEvening by remember { mutableStateOf(false) }
    var homeMsg by remember { mutableStateOf<Int?>(null) }
    var testMsg by remember { mutableStateOf(false) }
    val inactivityLabels = INACTIVITY_HOURS.map { hoursText(it) }
    val waitLabels = WAIT_MINUTES.map { minutesText(it) }
    val inactivityText = if (s.inactivity % 60 == 0) hoursText(s.inactivity / 60) else minutesText(s.inactivity)

    SectionTitle(stringResource(R.string.set_rules))
    Body(stringResource(R.string.m_ask_if))
    ChipGroup(
        inactivityLabels,
        INACTIVITY_HOURS.indexOfFirst { it * 60 == s.inactivity },
        { vm.saveRules(INACTIVITY_HOURS[it] * 60, s.wait) },
    )
    Body(stringResource(R.string.m_wait_before))
    ChipGroup(
        waitLabels,
        WAIT_MINUTES.indexOf(s.wait),
        { vm.saveRules(s.inactivity, WAIT_MINUTES[it]) },
    )
    Caption(stringResource(R.string.m_timing_note, inactivityText, minutesText(s.wait)))

    SectionTitle(stringResource(R.string.wr_title))
    Card {
        WakeReq.entries.filter { WakeReady.applies(it) }.forEach { r ->
            val ok = remember(tick, r) { WakeReady.ok(ctx, r) }
            Body((if (ok == true) "✓ " else if (ok == false) "✗ " else "• ") + stringResource(r.label))
            if (ok != true) SecondaryButton(stringResource(R.string.wr_fix), { WakeReady.fix(ctx, r) })
        }
        SecondaryButton(stringResource(R.string.wr_test), {
            Handler(Looper.getMainLooper()).postDelayed({ Wake.wakeNow(ctx, WakeReason.Checkin) }, 10_000L)
            testMsg = true
        })
        if (testMsg) Caption(stringResource(R.string.wr_test_note))
    }
    SwitchRow(stringResource(R.string.r3_hey), gs.hey, { vm.setHey(it) }, stringResource(R.string.r3_hey_hint))

    SectionTitle(stringResource(R.string.r3_set_guardian))
    SwitchRow(stringResource(R.string.r3_nightwatch), gs.nightWatch, { vm.setNightWatch(it) }, stringResource(R.string.r3_nightwatch_hint))
    SwitchRow(stringResource(R.string.r3_fall), gs.fall, { vm.setFall(it) }, stringResource(R.string.r3_fall_hint))
    SwitchRow(stringResource(R.string.r3_safe), gs.safe, { vm.setSafe(it) }, stringResource(R.string.r3_safe_hint))
    SecondaryButton(stringResource(R.string.r3_safe_set), {
        scope.launch { homeMsg = if (vm.setHomeHere()) R.string.r3_safe_done else R.string.r3_safe_fail }
    })
    homeMsg?.let { Caption(stringResource(it)) }

    SectionTitle(stringResource(R.string.r3_greet_times))
    SecondaryButton(stringResource(R.string.r3_morning) + "  " + showTime(gs.morning), { pickMorning = true })
    SecondaryButton(stringResource(R.string.r3_evening) + "  " + showTime(gs.evening), { pickEvening = true })
    val exactOk = remember(tick) { Oem.exactAllowed(ctx) }
    SwitchRow(
        stringResource(R.string.r3_exact), exactOk, { Oem.openExact(ctx) },
        stringResource(if (exactOk) R.string.r3_exact_on else R.string.r3_exact_off),
    )
    SecondaryButton(stringResource(R.string.r3_oem), { Oem.open(ctx) })
    Caption(stringResource(R.string.r3_oem_hint))
    if (pickMorning) TimeDialog(gs.morning, { vm.setMorning(it); pickMorning = false }, { pickMorning = false })
    if (pickEvening) TimeDialog(gs.evening, { vm.setEvening(it); pickEvening = false }, { pickEvening = false })
}

@Composable
private fun PrivacyPage(vm: MainViewModel, s: Snap, onAudit: () -> Unit) {
    val ctx = LocalContext.current
    val tick by vm.resume.collectAsState()
    val callLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.resume.value += 1 }

    SectionTitle(stringResource(R.string.set_approvals))
    SwitchRow(stringResource(R.string.ap_family_label), s.callFamily, {
        vm.setApproval("call_family", it)
        if (it && !Perms.granted(ctx, Perm.CallPhone)) callLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
    })
    SwitchRow(stringResource(R.string.ap_hospital_label), s.callHospital, {
        vm.setApproval("call_hospital", it)
        if (it && !Perms.granted(ctx, Perm.CallPhone)) callLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
    })
    SwitchRow(stringResource(R.string.ap_record_label), s.record, { vm.setApproval("record", it) })

    SectionTitle(stringResource(R.string.set_perms))
    Caption(stringResource(R.string.set_perm_hint))
    Perm.entries.filter { Perms.available(it) }.forEach { p ->
        val on = remember(tick, p) { Perms.granted(ctx, p) }
        val (name, short) = permRes(p)
        SwitchRow(stringResource(name), on, { Perms.open(ctx, p, false) }, stringResource(short))
    }
    if (!remember(tick) { Perms.granted(ctx, Perm.Usage) }) {
        Card { Body(stringResource(R.string.set_usage_denied, s.inactivity)) }
    }

    NavRow(stringResource(R.string.set_audit), onAudit)
    if (s.running) {
        SecondaryButton(stringResource(R.string.set_stop), { vm.stopSimha() })
    } else {
        Body(stringResource(R.string.stopped_banner))
        PrimaryButton(stringResource(R.string.set_start), { vm.startSimha() })
    }
}

@Composable
private fun AboutPage(vm: MainViewModel, s: Snap) {
    Body(stringResource(R.string.wr_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
    SectionTitle(stringResource(R.string.r3_updates))
    UpdateBanner()
    SecondaryButton(stringResource(R.string.r3_check_updates), { UpdateState.checkNow() })
    SwitchRow(stringResource(R.string.night), s.night, { vm.setNight(it) })
}

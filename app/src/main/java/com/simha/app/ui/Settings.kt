package com.simha.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.simha.app.MainViewModel
import com.simha.app.Perm
import com.simha.app.Perms
import com.simha.app.R
import com.simha.app.data.CaregiverRow
import com.simha.app.data.Repo
import com.simha.app.data.Snap
import com.simha.app.ui.talk.AiSettingsSection
import com.simha.app.ui.theme.LocalSimha
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
}

@Composable
private fun hoursText(h: Int): String = LocalContext.current.resources.getQuantityString(R.plurals.m_hours, h, h)

@Composable
private fun minutesText(m: Int): String = LocalContext.current.resources.getQuantityString(R.plurals.m_minutes, m, m)

@Composable
fun SettingsScreen(
    vm: MainViewModel,
    s: Snap,
    onBack: () -> Unit,
    onAudit: () -> Unit,
    onContacts: (String) -> Unit,
    onProfile: () -> Unit,
) {
    val ctx = LocalContext.current
    val sl = vm.sl
    val scope = rememberCoroutineScope()
    val tick by vm.resume.collectAsState()
    val contacts by vm.contacts.collectAsState()
    val callLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.resume.value += 1 }
    val title = stringResource(R.string.set_title)

    var cgs by remember { mutableStateOf<List<CaregiverRow>>(emptyList()) }
    var cgErr by remember { mutableStateOf<String?>(null) }
    val reload: () -> Unit = {
        scope.launch {
            sl.repo.caregivers().fold({ cgs = it.filter { c -> c.status != "removed" }; cgErr = null }, { cgErr = it.message ?: it.javaClass.simpleName })
        }
    }
    LaunchedEffect(Unit) { reload() }

    val inactivityLabels = INACTIVITY_HOURS.map { hoursText(it) }
    val waitLabels = WAIT_MINUTES.map { minutesText(it) }
    val inactivityText = if (s.inactivity % 60 == 0) hoursText(s.inactivity / 60) else minutesText(s.inactivity)
    val noFamily = contacts.none { it.role in Repo.FAMILY_ROLES }

    ScreenFrame(title, title, onBack) {
        SectionTitle(stringResource(R.string.set_language))
        listOf("en" to R.string.lang_en, "hi" to R.string.lang_hi, "te" to R.string.lang_te, "kn" to R.string.lang_kn, "ta" to R.string.lang_ta)
            .forEach { (code, res) ->
                OptionRow(stringResource(res), s.lang == code, { if (s.lang != code) vm.setLanguage(code) })
            }

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

        SectionTitle(stringResource(R.string.m_set_contacts))
        if (noFamily) Body(stringResource(R.string.m_contacts_missing), color = LocalSimha.current.emergency)
        SecondaryButton(stringResource(R.string.fam_title), { onContacts("family") })
        SecondaryButton(stringResource(R.string.doc_title), { onContacts("doctor") })

        SectionTitle(stringResource(R.string.m_set_medical))
        Caption(stringResource(R.string.m_med_note))
        SecondaryButton(stringResource(R.string.profile_title), onProfile)

        SectionTitle(stringResource(R.string.m_set_ai))
        AiSettingsSection()

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
        SecondaryButton(stringResource(R.string.cg_invite), {
            scope.launch {
                sl.repo.createInvite().fold({ reload() }, { cgErr = it.message ?: it.javaClass.simpleName })
            }
        })

        SecondaryButton(stringResource(R.string.set_audit), onAudit)
        SwitchRow(stringResource(R.string.night), s.night, { vm.setNight(it) })
        if (s.running) {
            PrimaryButton(stringResource(R.string.set_stop), { vm.stopSimha() })
        } else {
            Body(stringResource(R.string.stopped_banner))
            PrimaryButton(stringResource(R.string.set_start), { vm.startSimha() })
        }
    }
}

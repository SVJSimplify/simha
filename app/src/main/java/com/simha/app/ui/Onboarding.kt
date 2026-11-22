package com.simha.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.simha.app.Actions
import com.simha.app.MainViewModel
import com.simha.app.Perm
import com.simha.app.Perms
import com.simha.app.R
import com.simha.app.data.ProfileEntity
import com.simha.app.data.Snap
import com.simha.app.logic.Answer
import com.simha.app.logic.Reply
import com.simha.app.voice.VoiceIssue

const val OB_STEPS = 7

class PermText(val name: Int, val short: Int, val why: Int, val no: Int)

fun permText(p: Perm): PermText = when (p) {
    Perm.Notifications -> PermText(R.string.perm_notif, R.string.perm_notif_short, R.string.perm_notif_why, R.string.perm_notif_no)
    Perm.Mic -> PermText(R.string.perm_mic, R.string.perm_mic_short, R.string.perm_mic_why, R.string.perm_mic_no)
    Perm.Camera -> PermText(R.string.perm_cam, R.string.perm_cam_short, R.string.perm_cam_why, R.string.perm_cam_no)
    Perm.Location -> PermText(R.string.perm_loc, R.string.perm_loc_short, R.string.perm_loc_why, R.string.perm_loc_no)
    Perm.Usage -> PermText(R.string.perm_usage, R.string.perm_usage_short, R.string.perm_usage_why, R.string.perm_usage_no)
    Perm.FullScreen -> PermText(R.string.perm_full, R.string.perm_full_short, R.string.perm_full_why, R.string.perm_full_no)
    Perm.Battery -> PermText(R.string.perm_batt, R.string.perm_batt_short, R.string.perm_batt_why, R.string.perm_batt_no)
    Perm.CallPhone -> PermText(R.string.perm_call, R.string.perm_call_short, R.string.perm_call_short, R.string.perm_call_no)
}

@Composable
fun OnboardingScreen(vm: MainViewModel, s: Snap, onDone: () -> Unit) {
    when (vm.obStep) {
        0 -> StepLang(vm, s)
        1 -> StepContacts(vm)
        2 -> StepClinic(vm)
        3 -> StepMedical(vm)
        4 -> StepPerms(vm)
        5 -> StepApprovals(vm)
        else -> StepPractice(vm, s, onDone)
    }
}

@Composable
private fun StepLang(vm: MainViewModel, s: Snap) {
    val title = stringResource(R.string.ob1_title)
    val nameLabel = stringResource(R.string.ob1_name_label)
    val langs = listOf("en" to R.string.lang_en, "hi" to R.string.lang_hi, "te" to R.string.lang_te, "kn" to R.string.lang_kn, "ta" to R.string.lang_ta)
    ScreenFrame(title, "$title. $nameLabel", null) {
        Caption(stringResource(R.string.ob_step, 1, OB_STEPS))
        langs.forEach { (code, res) ->
            BigButton(
                stringResource(res),
                { vm.setLanguage(code) },
                kind = if (s.lang == code) Kind.Primary else Kind.Secondary,
            )
        }
        LabeledField(nameLabel, vm.obName, { vm.obName = it }, singleLine = true)
        BigButton(stringResource(R.string.next), {
            vm.saveName(vm.obName)
            vm.obStep = 1
        }, enabled = vm.obName.isNotBlank())
    }
}

@Composable
private fun StepContacts(vm: MainViewModel) {
    val title = stringResource(R.string.ob2_title)
    val body = stringResource(R.string.ob2_body)
    val all by vm.contacts.collectAsState()
    val list = all.filter { it.role in listOf("family", "neighbor", "doctor") }
    var role by rememberSaveable { mutableStateOf("family") }
    val pick = rememberPicker { n, p -> vm.addContact(n, p, role) }
    ScreenFrame(title, "$title. $body", { vm.obStep = 0 }) {
        Caption(stringResource(R.string.ob_step, 2, OB_STEPS))
        Body(body)
        listOf("family", "neighbor", "doctor").forEach { r ->
            BigButton(roleLabel(r), { role = r }, kind = if (role == r) Kind.Primary else Kind.Secondary)
        }
        BigButton(stringResource(R.string.ob2_pick), pick)
        list.forEach { c -> ContactCard(c) { vm.removeContact(c.id) } }
        BigButton(
            stringResource(if (list.isEmpty()) R.string.skip else R.string.next),
            { vm.obStep = 2 },
            kind = if (list.isEmpty()) Kind.Secondary else Kind.Primary,
        )
    }
}

@Composable
private fun StepClinic(vm: MainViewModel) {
    val title = stringResource(R.string.ob3_title)
    val body = stringResource(R.string.ob3_body)
    val all by vm.contacts.collectAsState()
    val list = all.filter { it.role in listOf("doctor", "hospital") }
    val pickDoctor = rememberPicker { n, p -> vm.addContact(n, p, "doctor") }
    val pickHospital = rememberPicker { n, p -> vm.addContact(n, p, "hospital") }
    ScreenFrame(title, "$title. $body", { vm.obStep = 1 }) {
        Caption(stringResource(R.string.ob_step, 3, OB_STEPS))
        Body(body)
        BigButton(stringResource(R.string.ob3_doctor), pickDoctor)
        BigButton(stringResource(R.string.ob3_hospital), pickHospital)
        list.forEach { c -> ContactCard(c) { vm.removeContact(c.id) } }
        BigButton(
            stringResource(if (list.isEmpty()) R.string.skip else R.string.next),
            { vm.obStep = 3 },
            kind = if (list.isEmpty()) Kind.Secondary else Kind.Primary,
        )
    }
}

@Composable
private fun StepMedical(vm: MainViewModel) {
    val title = stringResource(R.string.ob4_title)
    val body = stringResource(R.string.ob4_body)
    val p by vm.profile.collectAsState()
    var cond by remember { mutableStateOf(p.conditions) }
    var med by remember { mutableStateOf(p.medicines) }
    var all by remember { mutableStateOf(p.allergies) }
    ScreenFrame(title, "$title. $body", { vm.obStep = 2 }) {
        Caption(stringResource(R.string.ob_step, 4, OB_STEPS))
        Body(body)
        LabeledField(stringResource(R.string.f_conditions), cond, { cond = it })
        LabeledField(stringResource(R.string.f_medicines), med, { med = it })
        LabeledField(stringResource(R.string.f_allergies), all, { all = it })
        BigButton(stringResource(R.string.next), {
            vm.saveProfile(ProfileEntity(1, cond.trim(), med.trim(), all.trim(), p.doctorName, p.doctorPhone))
            vm.obStep = 4
        })
        BigButton(stringResource(R.string.skip), { vm.obStep = 4 }, kind = Kind.Secondary)
    }
}

@Composable
private fun StepPerms(vm: MainViewModel) {
    val ctx = LocalContext.current
    val list = remember {
        listOf(Perm.Notifications, Perm.Mic, Perm.Camera, Perm.Location, Perm.Usage, Perm.FullScreen, Perm.Battery)
            .filter { Perms.available(it) }
    }
    val idx = vm.obPerm.coerceIn(0, list.size - 1)
    val p = list[idx]
    val t = permText(p)
    var local by remember { mutableIntStateOf(0) }
    val tick by vm.resume.collectAsState()
    val granted = remember(tick, local, p) { Perms.granted(ctx, p) }
    var asked by rememberSaveable(p.name) { mutableStateOf(false) }
    var why by rememberSaveable(p.name) { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { local += 1 }
    val name = stringResource(t.name)
    val short = stringResource(t.short)
    ScreenFrame(name, "$name. $short", { if (vm.obPerm > 0) vm.obPerm = idx - 1 else vm.obStep = 3 }) {
        Caption(stringResource(R.string.ob_step, 5, OB_STEPS))
        Caption(stringResource(R.string.perm_of, idx + 1, list.size))
        Body(short)
        BigButton(stringResource(R.string.why), { why = !why }, kind = Kind.Secondary)
        if (why) Card { Body(stringResource(t.why)) }
        if (granted) {
            Body(stringResource(R.string.perm_allowed), color = MaterialTheme.colorScheme.tertiary)
        } else if (asked) {
            Body(stringResource(R.string.perm_denied))
            Body(stringResource(t.no))
        }
        if (!granted) {
            BigButton(stringResource(R.string.perm_allow), {
                asked = true
                val r = Perms.runtime(p)
                if (r != null) launcher.launch(r) else Perms.open(ctx, p, true)
            })
            if (asked && Perms.runtime(p) != null) {
                BigButton(stringResource(R.string.perm_open_settings), { Perms.open(ctx, p, false) }, kind = Kind.Secondary)
            }
            if (!asked) BigButton(stringResource(R.string.perm_not_now), { asked = true }, kind = Kind.Secondary)
        }
        BigButton(stringResource(R.string.next), {
            if (idx < list.size - 1) vm.obPerm = idx + 1 else vm.obStep = 5
        }, enabled = granted || asked)
    }
}

@Composable
private fun StepApprovals(vm: MainViewModel) {
    val ctx = LocalContext.current
    val kinds = listOf("call_family", "call_hospital", "record")
    val questions = listOf(R.string.ap_family_q, R.string.ap_hospital_q, R.string.ap_record_q)
    val idx = vm.obApproval.coerceIn(0, 2)
    val kind = kinds[idx]
    var answered by rememberSaveable(idx) { mutableStateOf<Boolean?>(null) }
    var local by remember { mutableIntStateOf(0) }
    val tick by vm.resume.collectAsState()
    val phoneOk = remember(tick, local) { Perms.granted(ctx, Perm.CallPhone) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { local += 1 }
    val q = stringResource(questions[idx])
    ScreenFrame(stringResource(R.string.ob6_title), q, { if (idx > 0) vm.obApproval = idx - 1 else vm.obStep = 4 }) {
        Caption(stringResource(R.string.ob_step, 6, OB_STEPS))
        Body(q)
        val a = answered
        if (a == null) {
            BigButton(stringResource(R.string.yes), {
                answered = true
                vm.setApproval(kind, true)
                if (kind != "record" && !Perms.granted(ctx, Perm.CallPhone)) launcher.launch(arrayOf(android.Manifest.permission.CALL_PHONE))
            })
            BigButton(stringResource(R.string.no), {
                answered = false
                vm.setApproval(kind, false)
            }, kind = Kind.Secondary)
        } else {
            Body(stringResource(if (a) R.string.ap_yes_ok else R.string.ap_no_ok))
            if (a && kind != "record") {
                if (phoneOk) {
                    Body(stringResource(R.string.perm_call_allowed), color = MaterialTheme.colorScheme.tertiary)
                } else {
                    Body(stringResource(R.string.perm_call_no))
                }
            }
            BigButton(stringResource(R.string.next), {
                if (idx < 2) vm.obApproval = idx + 1 else vm.obStep = 6
            })
        }
    }
}

@Composable
private fun StepPractice(vm: MainViewModel, s: Snap, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val sl = vm.sl
    val title = stringResource(R.string.ob7_title)
    val greet = stringResource(R.string.ci_greet, vm.obName.ifBlank { s.name })
    var done by remember { mutableStateOf(false) }
    var helpNote by remember { mutableStateOf(false) }
    val issue by sl.voice.issue.collectAsState()
    LaunchedEffect(Unit) {
        sl.voice.say(greet, s.lang)
        if (Actions.has(ctx, android.Manifest.permission.RECORD_AUDIO)) {
            val heard = sl.voice.listen(s.lang) {}.getOrNull()
            if (heard != null) {
                when (Reply.classify(heard)) {
                    Answer.Fine -> {
                        vm.checkinOk()
                        done = true
                    }
                    Answer.Help -> {
                        helpNote = true
                        done = true
                    }
                    Answer.Unknown -> Unit
                }
            }
        }
    }
    ScreenFrame(title, greet, { vm.obStep = 5 }) {
        Caption(stringResource(R.string.ob_step, 7, OB_STEPS))
        Card { Body(stringResource(R.string.ci_q)) }
        if (issue == VoiceIssue.Unavailable) Body(stringResource(R.string.voice_unavailable))
        if (issue == VoiceIssue.LangMissing) Body(stringResource(R.string.voice_lang_missing))
        if (!done) {
            BigButton(stringResource(R.string.ci_fine), {
                vm.checkinOk()
                done = true
            }, kind = Kind.Success)
            BigButton(stringResource(R.string.ci_help), {
                helpNote = true
                done = true
            }, kind = Kind.Emergency)
        } else {
            if (helpNote) Body(stringResource(R.string.practice_help_note))
            Body(stringResource(R.string.practice_done), color = MaterialTheme.colorScheme.tertiary)
            BigButton(stringResource(R.string.finish), {
                vm.finishOnboarding()
                onDone()
            })
        }
    }
}

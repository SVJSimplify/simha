package com.simha.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simha.app.Actions
import com.simha.app.MainViewModel
import com.simha.app.Perm
import com.simha.app.Perms
import com.simha.app.R
import com.simha.app.data.OBK
import com.simha.app.data.Repo
import com.simha.app.data.Snap
import com.simha.app.data.jo
import com.simha.app.data.simhaStore
import com.simha.app.logic.Answer
import com.simha.app.logic.Reply
import com.simha.app.voice.VoiceIssue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

const val OB_STEPS = 10

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

private fun indianPhone(raw: String): String? {
    val t = raw.filter { !it.isWhitespace() && it != '-' && it != '(' && it != ')' }
    val b = when {
        t.startsWith("+91") -> t.drop(3)
        t.startsWith("0091") -> t.drop(4)
        t.length == 12 && t.startsWith("91") -> t.drop(2)
        t.length == 11 && t.startsWith("0") -> t.drop(1)
        else -> t
    }
    return if (b.length == 10 && b.all { it.isDigit() }) "+91$b" else null
}

private fun openMaps(ctx: Context) {
    val loc = Actions.lastLocation(ctx)
    val geo = if (loc != null) "geo:${loc.latitude},${loc.longitude}?q=hospital" else "geo:0,0?q=hospital"
    val base = Intent(Intent.ACTION_VIEW, Uri.parse(geo)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        ctx.startActivity(Intent(base).setPackage("com.google.android.apps.maps"))
    } catch (e: Exception) {
        try {
            ctx.startActivity(base)
        } catch (e2: Exception) {
            try {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/hospital")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e3: Exception) {
            }
        }
    }
}

@Composable
fun OnboardingScreen(vm: MainViewModel, s: Snap, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var ready by remember { mutableStateOf(false) }
    var helper by remember { mutableStateOf(false) }
    var answers by remember { mutableStateOf(mapOf<String, Boolean>()) }
    LaunchedEffect(Unit) {
        val p = ctx.simhaStore.data.first()
        helper = p[OBK.HELPER] ?: false
        answers = (p[OBK.ANSWERED] ?: "").split(",").mapNotNull {
            val kv = it.split(":")
            if (kv.size == 2) kv[0] to (kv[1] == "1") else null
        }.toMap()
        if (vm.obStep == 0) vm.obStep = (p[OBK.STEP] ?: 0).coerceIn(0, OB_STEPS - 1)
        if (vm.obName.isBlank()) vm.obName = s.name
        ready = true
    }
    LaunchedEffect(vm.obStep, ready) {
        if (ready) vm.sl.prefs.edit { this[OBK.STEP] = vm.obStep }
    }
    if (!ready) return
    val go: (Int) -> Unit = { vm.obStep = it }
    when (vm.obStep) {
        0 -> StepWho(
            onChoose = { h ->
                helper = h
                scope.launch { vm.sl.prefs.edit { this[OBK.HELPER] = h } }
                go(1)
            },
            onInvite = {
                vm.setRole("caregiver")
                vm.route.value = "cg_setup"
            },
        )
        1 -> StepName(vm, s, helper, { go(0) }) { go(2) }
        2 -> StepContacts(vm, helper, { go(1) }) { go(3) }
        3 -> StepClinic(vm, helper, { go(2) }) { go(4) }
        4, 5, 6 -> StepMedical(vm, helper, vm.obStep - 4, { go(vm.obStep - 1) }) { go(vm.obStep + 1) }
        7 -> StepPerms(vm, helper, { go(6) }) { go(8) }
        8 -> StepApprovals(vm, helper, answers, { k, v ->
            val m = answers + (k to v)
            answers = m
            scope.launch { vm.sl.prefs.edit { this[OBK.ANSWERED] = m.entries.joinToString(",") { e -> e.key + ":" + (if (e.value) "1" else "0") } } }
        }, { go(7) }) { go(9) }
        else -> StepPractice(vm, s, { go(8) }, onDone)
    }
}

@Composable
private fun Progress(step: Int) {
    Caption(stringResource(R.string.ob_step, step + 1, OB_STEPS))
    LinearProgressIndicator(progress = { (step + 1f) / OB_STEPS }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ChoiceCard(title: String, desc: String, onClick: () -> Unit) {
    val l = LocalLayout.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = l.target),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Body(desc)
        }
    }
}

@Composable
private fun StepWho(onChoose: (Boolean) -> Unit, onInvite: () -> Unit) {
    val title = stringResource(R.string.o_who_title)
    ScreenFrame(title, title, null) {
        Progress(0)
        ChoiceCard(stringResource(R.string.o_who_self), stringResource(R.string.o_who_self_d)) { onChoose(false) }
        ChoiceCard(stringResource(R.string.o_who_other), stringResource(R.string.o_who_other_d)) { onChoose(true) }
        TextLink(stringResource(R.string.o_who_invite), onInvite)
    }
}

@Composable
private fun StepName(vm: MainViewModel, s: Snap, h: Boolean, back: () -> Unit, next: () -> Unit) {
    val title = stringResource(if (h) R.string.o_name_title_other else R.string.o_name_title_self)
    val label = stringResource(if (h) R.string.o_name_label_other else R.string.o_name_label_self)
    val langs = listOf("en" to R.string.lang_en, "hi" to R.string.lang_hi, "te" to R.string.lang_te, "kn" to R.string.lang_kn, "ta" to R.string.lang_ta)
    var tried by rememberSaveable { mutableStateOf(false) }
    ScreenFrame(title, title, back) {
        Progress(1)
        langs.forEach { (code, res) ->
            if (s.lang == code) PrimaryButton(stringResource(res), { vm.setLanguage(code) })
            else SecondaryButton(stringResource(res), { vm.setLanguage(code) })
        }
        Body(title)
        LabeledField(
            label,
            vm.obName,
            { vm.obName = it },
            required = true,
            error = if (tried && vm.obName.isBlank()) stringResource(R.string.o_err_name) else null,
        )
        PrimaryButton(stringResource(R.string.next), {
            if (vm.obName.isBlank()) {
                tried = true
            } else {
                vm.saveName(vm.obName)
                next()
            }
        })
    }
}

@Composable
private fun StepContacts(vm: MainViewModel, h: Boolean, back: () -> Unit, next: () -> Unit) {
    val title = stringResource(R.string.ob2_title)
    val body = stringResource(if (h) R.string.o_contacts_body_other else R.string.o_contacts_body_self)
    val all by vm.contacts.collectAsState()
    val list = all.filter { it.role in Repo.FAMILY_ROLES + "doctor" }
    var form by rememberSaveable { mutableStateOf<String?>(null) }
    var picked by rememberSaveable { mutableStateOf(false) }
    var label by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var tried by rememberSaveable { mutableStateOf(false) }
    var nextTried by rememberSaveable { mutableStateOf(false) }
    val pick = rememberPicker { n, p ->
        label = n
        phone = p
        picked = true
        tried = false
        if (form == null) form = "family"
    }
    val open: (String) -> Unit = {
        form = it
        picked = false
        label = ""
        phone = ""
        tried = false
    }
    ScreenFrame(title, "$title. $body", back) {
        Progress(2)
        Body(body)
        list.forEach { c -> ContactCard(c) { vm.removeContact(c.id) } }
        val f = form
        if (f == null) {
            SecondaryButton(stringResource(R.string.o_add_family), { open("family") })
            SecondaryButton(stringResource(R.string.o_add_neighbour), { open("neighbor") })
            SecondaryButton(stringResource(R.string.o_add_doctor), { open("doctor") })
            SecondaryButton(stringResource(R.string.ob2_pick), pick)
        } else {
            val ph = indianPhone(phone)
            Card {
                Body(roleLabel(f))
                if (picked) {
                    listOf("family", "neighbor", "doctor").forEach { r ->
                        if (f == r) PrimaryButton(roleLabel(r), { form = r }) else SecondaryButton(roleLabel(r), { form = r })
                    }
                }
                LabeledField(
                    stringResource(R.string.o_label),
                    label,
                    { label = it },
                    required = true,
                    error = if (tried && label.isBlank()) stringResource(R.string.o_err_label) else null,
                )
                LabeledField(
                    stringResource(R.string.o_phone),
                    phone,
                    { phone = it },
                    required = true,
                    error = if (tried && ph == null) stringResource(R.string.o_err_phone) else null,
                    keyboard = KeyboardType.Phone,
                )
                PrimaryButton(stringResource(R.string.o_add), {
                    if (label.isBlank() || ph == null) {
                        tried = true
                    } else {
                        vm.addContact(label.trim(), ph, f)
                        form = null
                        picked = false
                        label = ""
                        phone = ""
                        tried = false
                        nextTried = false
                    }
                })
                SecondaryButton(stringResource(R.string.ob2_pick), pick)
                TextLink(stringResource(R.string.o_cancel)) { form = null }
            }
        }
        if (nextTried && list.isEmpty()) Body(stringResource(R.string.o_err_contact), color = MaterialTheme.colorScheme.error)
        PrimaryButton(stringResource(R.string.next), {
            if (list.isEmpty()) nextTried = true else next()
        })
    }
}

@Composable
private fun StepClinic(vm: MainViewModel, h: Boolean, back: () -> Unit, next: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val title = stringResource(R.string.ob3_title)
    val body = stringResource(if (h) R.string.o_clinic_body_other else R.string.o_clinic_body_self)
    val p by vm.profile.collectAsState()
    var dn by remember(p) { mutableStateOf(p.doctorName) }
    var dp by remember(p) { mutableStateOf(p.doctorPhone) }
    var hosp by rememberSaveable { mutableStateOf("") }
    var tried by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (hosp.isBlank()) hosp = ctx.simhaStore.data.first()[OBK.HOSPITAL] ?: ""
    }
    ScreenFrame(title, "$title. $body", back) {
        Progress(3)
        Body(body)
        LabeledField(stringResource(R.string.f_doctor_name), dn, { dn = it })
        LabeledField(
            stringResource(R.string.f_doctor_phone),
            dp,
            { dp = it },
            error = if (tried && dp.isNotBlank() && indianPhone(dp) == null) stringResource(R.string.o_err_phone) else null,
            keyboard = KeyboardType.Phone,
        )
        SecondaryButton(stringResource(R.string.o_find_hospital), { openMaps(ctx) })
        Caption(stringResource(R.string.o_hospital_hint))
        LabeledField(stringResource(R.string.o_hospital_name), hosp, { hosp = it })
        PrimaryButton(stringResource(R.string.next), {
            val ph = if (dp.isBlank()) "" else indianPhone(dp)
            if (ph == null) {
                tried = true
            } else {
                vm.saveProfile(p.copy(doctorName = dn.trim(), doctorPhone = ph))
                scope.launch {
                    vm.sl.prefs.edit { this[OBK.HOSPITAL] = hosp.trim() }
                    if (hosp.isNotBlank()) vm.sl.repo.audit("hospital_set", jo("name" to hosp.trim()))
                }
                next()
            }
        })
        SecondaryButton(stringResource(R.string.skip), next)
    }
}

@Composable
private fun StepMedical(vm: MainViewModel, h: Boolean, kind: Int, back: () -> Unit, next: () -> Unit) {
    val p by vm.profile.collectAsState()
    val name = stringResource(listOf(R.string.f_conditions, R.string.f_medicines, R.string.f_allergies)[kind])
    val why = stringResource(
        listOf(
            if (h) R.string.o_cond_why_other else R.string.o_cond_why_self,
            if (h) R.string.o_med_why_other else R.string.o_med_why_self,
            if (h) R.string.o_all_why_other else R.string.o_all_why_self,
        )[kind]
    )
    val note = stringResource(if (h) R.string.o_remember_other else R.string.o_remember_self)
    var v by remember(p) { mutableStateOf(listOf(p.conditions, p.medicines, p.allergies)[kind]) }
    ScreenFrame(name, "$name. $why $note", back) {
        Progress(4 + kind)
        Body(why)
        Body(note)
        LabeledField(name, v, { v = it }, singleLine = false)
        PrimaryButton(stringResource(R.string.next), {
            val t = v.trim()
            vm.saveProfile(
                when (kind) {
                    0 -> p.copy(conditions = t)
                    1 -> p.copy(medicines = t)
                    else -> p.copy(allergies = t)
                }
            )
            next()
        })
        SecondaryButton(stringResource(R.string.skip), next)
    }
}

@Composable
private fun StepPerms(vm: MainViewModel, h: Boolean, back: () -> Unit, next: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
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
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { local += 1 }
    val name = stringResource(t.name)
    val short = if (p == Perm.Notifications) stringResource(if (h) R.string.o_notif_short_other else R.string.o_notif_short_self) else stringResource(t.short)
    val why = stringResource(t.why)
    val advance: () -> Unit = { if (idx < list.size - 1) vm.obPerm = idx + 1 else next() }
    LaunchedEffect(p, asked, granted) {
        if (asked) vm.sl.repo.audit("permission_" + p.name.lowercase(), jo("granted" to granted))
    }
    ScreenFrame(name, "$name. $short $why", { if (idx > 0) vm.obPerm = idx - 1 else back() }) {
        Progress(7)
        Caption(stringResource(R.string.perm_of, idx + 1, list.size))
        Body(short)
        Body(why)
        if (granted) {
            Body(stringResource(R.string.perm_allowed), color = MaterialTheme.colorScheme.tertiary)
        } else if (asked) {
            Body(stringResource(R.string.perm_denied))
            Body(stringResource(t.no))
        }
        if (!granted) {
            SecondaryButton(stringResource(R.string.perm_allow), {
                asked = true
                val r = Perms.runtime(p)
                if (r != null) launcher.launch(r) else Perms.open(ctx, p, true)
            })
            if (asked && Perms.runtime(p) != null) {
                SecondaryButton(stringResource(R.string.perm_open_settings), { Perms.open(ctx, p, false) })
            }
        }
        if (granted || asked) PrimaryButton(stringResource(R.string.next), advance)
        if (!granted) {
            Spacer(Modifier.height(16.dp))
            TextLink(stringResource(R.string.perm_not_now)) {
                scope.launch { vm.sl.repo.audit("permission_skipped", jo("perm" to p.name)) }
                advance()
            }
        }
    }
}

@Composable
private fun StepApprovals(
    vm: MainViewModel,
    h: Boolean,
    answers: Map<String, Boolean>,
    onAnswer: (String, Boolean) -> Unit,
    back: () -> Unit,
    next: () -> Unit,
) {
    val ctx = LocalContext.current
    val kinds = listOf("call_family", "call_hospital", "record")
    val questions = if (h) {
        listOf(R.string.o_ap_family_q_other, R.string.o_ap_hospital_q_other, R.string.o_ap_record_q_other)
    } else {
        listOf(R.string.ap_family_q, R.string.ap_hospital_q, R.string.ap_record_q)
    }
    val idx = vm.obApproval.coerceIn(0, 2)
    val kind = kinds[idx]
    val a = answers[kind]
    var local by remember { mutableIntStateOf(0) }
    val tick by vm.resume.collectAsState()
    val phoneOk = remember(tick, local) { Perms.granted(ctx, Perm.CallPhone) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { local += 1 }
    val q = stringResource(questions[idx])
    ScreenFrame(stringResource(R.string.ob6_title), q, { if (idx > 0) vm.obApproval = idx - 1 else back() }) {
        Progress(8)
        Body(q)
        val yes: () -> Unit = {
            onAnswer(kind, true)
            vm.setApproval(kind, true)
            if (kind != "record" && !Perms.granted(ctx, Perm.CallPhone)) launcher.launch(arrayOf(android.Manifest.permission.CALL_PHONE))
        }
        val no: () -> Unit = {
            onAnswer(kind, false)
            vm.setApproval(kind, false)
        }
        if (a == true) PrimaryButton(stringResource(R.string.yes), yes) else SecondaryButton(stringResource(R.string.yes), yes)
        if (a == false) PrimaryButton(stringResource(R.string.no), no) else SecondaryButton(stringResource(R.string.no), no)
        if (a != null) {
            Body(stringResource(if (a) R.string.ap_yes_ok else R.string.ap_no_ok))
            if (a && kind != "record") {
                if (phoneOk) {
                    Body(stringResource(R.string.perm_call_allowed), color = MaterialTheme.colorScheme.tertiary)
                } else {
                    Body(stringResource(R.string.perm_call_no))
                }
            }
            PrimaryButton(stringResource(R.string.next), {
                if (idx < 2) vm.obApproval = idx + 1 else next()
            })
        }
    }
}

@Composable
private fun StepPractice(vm: MainViewModel, s: Snap, back: () -> Unit, onDone: () -> Unit) {
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
    ScreenFrame(title, greet, back) {
        Progress(9)
        Card { Body(stringResource(R.string.ci_q)) }
        if (issue == VoiceIssue.Unavailable) Body(stringResource(R.string.voice_unavailable))
        if (issue == VoiceIssue.LangMissing) Body(stringResource(R.string.voice_lang_missing))
        if (!done) {
            PrimaryButton(stringResource(R.string.ci_fine), {
                vm.checkinOk()
                done = true
            })
            EmergencyButton(stringResource(R.string.ci_help), {
                helpNote = true
                done = true
            })
        } else {
            if (helpNote) Body(stringResource(R.string.practice_help_note))
            Body(stringResource(R.string.practice_done), color = MaterialTheme.colorScheme.tertiary)
            PrimaryButton(stringResource(R.string.finish), {
                vm.finishOnboarding()
                onDone()
            })
        }
    }
}

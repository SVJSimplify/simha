package com.simha.app.ui

import android.content.Context
import android.os.Build
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.simha.app.WakeReady
import com.simha.app.WakeReq
import kotlinx.coroutines.delay
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
import com.simha.app.ui.meds.MedicineForm
import com.simha.app.ui.meds.showTime
import com.simha.app.ui.theme.LocalSimha
import com.simha.app.ui.theme.SimhaRadius
import com.simha.app.logic.Answer
import com.simha.app.logic.Reply
import com.simha.app.voice.VoiceIssue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

const val OB_STEPS = 9

private val Emph = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private fun Modifier.fadeUp(i: Int): Modifier = composed {
    val ms = animMs()
    val a = remember { Animatable(if (ms == 0) 1f else 0f) }
    val px = with(LocalDensity.current) { 16.dp.toPx() }
    LaunchedEffect(Unit) {
        if (ms == 0) {
            a.snapTo(1f)
        } else {
            delay(i * 40L)
            a.animateTo(1f, tween(300, easing = Emph))
        }
    }
    graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * px
    }
}

@Composable
private fun Up(i: Int, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().fadeUp(i)) { content() }
}

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
    Perm.Overlay -> PermText(R.string.perm_overlay, R.string.perm_overlay_short, R.string.perm_overlay_why, R.string.perm_overlay_no)
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
    val ms = animMs()
    val slide = with(LocalDensity.current) { 24.dp.roundToPx() }
    AnimatedContent(
        targetState = vm.obStep,
        transitionSpec = {
            if (ms == 0) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val d = if (targetState > initialState) 1 else -1
                (fadeIn(tween(250, easing = Emph)) + slideInHorizontally(tween(250, easing = Emph)) { d * slide }) togetherWith
                    (fadeOut(tween(150)) + slideOutHorizontally(tween(250, easing = Emph)) { -d * slide })
            }
        },
        label = "ob",
    ) { st ->
        when (st) {
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
            4 -> StepHealth(vm, helper, { go(3) }) { go(5) }
            5 -> StepMeds(vm, helper, { go(4) }) { go(6) }
            6 -> StepApprovals(vm, helper, answers, { k, v ->
                val m = answers + (k to v)
                answers = m
                scope.launch { vm.sl.prefs.edit { this[OBK.ANSWERED] = m.entries.joinToString(",") { e -> e.key + ":" + (if (e.value) "1" else "0") } } }
            }, { go(5) }) { go(7) }
            7 -> StepPerms(vm, helper, (answers["call_family"] == true || answers["call_hospital"] == true), { go(6) }) { go(8) }
            else -> StepPractice(vm, s, { go(7) }, onDone)
        }
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
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = l.target)
            .pressClick(onClick),
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
        Up(0) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Progress(0) } }
        Up(1) { ChoiceCard(stringResource(R.string.o_who_self), stringResource(R.string.o_who_self_d)) { onChoose(false) } }
        Up(2) { ChoiceCard(stringResource(R.string.o_who_other), stringResource(R.string.o_who_other_d)) { onChoose(true) } }
        if (com.simha.app.data.Repo.CAREGIVER_ENABLED) Up(3) { TextLink(stringResource(R.string.o_who_invite), onInvite) }
    }
}

@Composable
private fun StepName(vm: MainViewModel, s: Snap, h: Boolean, back: () -> Unit, next: () -> Unit) {
    val title = stringResource(if (h) R.string.o_name_title_other else R.string.o_name_title_self)
    val label = stringResource(if (h) R.string.o_name_label_other else R.string.o_name_label_self)
    val langs = listOf("en" to R.string.lang_en, "hi" to R.string.lang_hi, "te" to R.string.lang_te, "kn" to R.string.lang_kn, "ta" to R.string.lang_ta)
    var tried by rememberSaveable { mutableStateOf(false) }
    ScreenFrame(title, title, back) {
        Up(0) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Progress(1) } }
        langs.forEachIndexed { i, (code, res) ->
            Up(1 + i) {
                if (s.lang == code) PrimaryButton(stringResource(res), { vm.setLanguage(code) })
                else SecondaryButton(stringResource(res), { vm.setLanguage(code) })
            }
        }
        Up(6) { Body(title) }
        Up(7) {
            LabeledField(
                label,
                vm.obName,
                { vm.obName = it },
                required = true,
                error = if (tried && vm.obName.isBlank()) stringResource(R.string.o_err_name) else null,
            )
        }
        Up(8) {
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
        Up(0) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Progress(2) } }
        Up(1) { Body(body) }
        list.forEach { c -> ContactCard(c) { vm.removeContact(c.id) } }
        val f = form
        if (f == null) {
            Up(2) { SecondaryButton(stringResource(R.string.o_add_family), { open("family") }) }
            Up(3) { SecondaryButton(stringResource(R.string.o_add_neighbour), { open("neighbor") }) }
            Up(4) { SecondaryButton(stringResource(R.string.o_add_doctor), { open("doctor") }) }
            Up(5) { SecondaryButton(stringResource(R.string.ob2_pick), pick) }
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
        Up(6) {
            PrimaryButton(stringResource(R.string.next), {
                if (list.isEmpty()) nextTried = true else next()
            })
        }
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
        Up(0) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Progress(3) } }
        Up(1) { Body(body) }
        Up(2) { LabeledField(stringResource(R.string.f_doctor_name), dn, { dn = it }) }
        Up(3) {
            LabeledField(
                stringResource(R.string.f_doctor_phone),
                dp,
                { dp = it },
                error = if (tried && dp.isNotBlank() && indianPhone(dp) == null) stringResource(R.string.o_err_phone) else null,
                keyboard = KeyboardType.Phone,
            )
        }
        Up(4) { SecondaryButton(stringResource(R.string.o_find_hospital), { openMaps(ctx) }) }
        Up(5) { Caption(stringResource(R.string.o_hospital_hint)) }
        Up(6) { LabeledField(stringResource(R.string.o_hospital_name), hosp, { hosp = it }) }
        Up(7) {
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
        }
        Up(8) { SecondaryButton(stringResource(R.string.skip), next) }
    }
}

@Composable
private fun StepHealth(vm: MainViewModel, h: Boolean, back: () -> Unit, next: () -> Unit) {
    val p by vm.profile.collectAsState()
    val title = stringResource(R.string.r4d_health_title)
    val why = stringResource(if (h) R.string.o_cond_why_other else R.string.o_cond_why_self)
    val note = stringResource(if (h) R.string.o_remember_other else R.string.o_remember_self)
    var cond by remember(p) { mutableStateOf(p.conditions) }
    var all by remember(p) { mutableStateOf(p.allergies) }
    ScreenFrame(title, "$title. $why $note", back) {
        Up(0) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Progress(4) } }
        Up(1) { Body(why) }
        Up(2) { Body(note) }
        Up(3) { LabeledField(stringResource(R.string.f_conditions), cond, { cond = it }, singleLine = false) }
        Up(4) { LabeledField(stringResource(R.string.f_allergies), all, { all = it }, singleLine = false) }
        Up(5) {
            PrimaryButton(stringResource(R.string.next), {
                vm.saveProfile(p.copy(conditions = cond.trim(), allergies = all.trim()))
                next()
            })
        }
        Up(6) { SecondaryButton(stringResource(R.string.skip), next) }
    }
}

@Composable
private fun StepMeds(vm: MainViewModel, h: Boolean, back: () -> Unit, next: () -> Unit) {
    val meds by vm.medicines.collectAsState()
    var adding by remember { mutableStateOf(false) }
    val title = stringResource(R.string.r3_ob_meds_title)
    val body = stringResource(if (h) R.string.r3_ob_meds_body_other else R.string.r3_ob_meds_body_self)
    ScreenFrame(title, "$title. $body", back) {
        Up(0) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Progress(5) } }
        Up(1) { Body(body) }
        meds.forEach { m ->
            Card {
                Body(m.name + if (m.dose.isBlank()) "" else " " + m.dose)
                Caption(m.times.joinToString(", ") { showTime(it) })
                SecondaryButton(stringResource(R.string.remove), { vm.deleteMedicine(m.id) })
            }
        }
        if (adding) {
            MedicineForm(null, { vm.saveMedicine(it); adding = false }, { adding = false })
        } else {
            Up(2) { SecondaryButton(stringResource(R.string.r3_meds_add), { adding = true }) }
        }
        Up(3) { PrimaryButton(stringResource(R.string.next), next) }
        Up(4) { SecondaryButton(stringResource(R.string.skip), next) }
    }
}

private class PRow(
    val key: String,
    val icon: Int,
    val name: Int,
    val short: Int,
    val runtime: Array<String>?,
    val granted: (Context, Set<String>) -> Boolean,
    val open: (Context) -> Unit,
)

private fun permRows(h: Boolean, callsOk: Boolean): List<PRow> {
    val rt = listOf(
        Triple(Perm.Notifications, R.drawable.ic_notification, if (h) R.string.o_notif_short_other else R.string.o_notif_short_self),
        Triple(Perm.Mic, R.drawable.ic_mic, R.string.perm_mic_short),
        Triple(Perm.Camera, R.drawable.ic_camera, R.string.perm_cam_short),
        Triple(Perm.Location, R.drawable.ic_heart, R.string.perm_loc_short),
    ) + if (callsOk) listOf(Triple(Perm.CallPhone, R.drawable.ic_phone, R.string.perm_call_short)) else emptyList()
    val runtimeRows = rt.filter { Perms.available(it.first) }.map { (q, ic, sh) ->
        PRow(q.name, ic, permText(q).name, sh, Perms.runtime(q), { c, _ -> Perms.granted(c, q) }, { c -> Perms.open(c, q, false) })
    }
    val wake = listOf(
        WakeReq.FullScreen to R.string.perm_full_short,
        WakeReq.Overlay to R.string.perm_overlay_short,
        WakeReq.Exact to R.string.r3_pe_short,
        WakeReq.Battery to R.string.perm_batt_short,
    ).filter { WakeReady.applies(it.first) }.map { (r, sh) ->
        PRow(r.name, R.drawable.ic_heart, r.label, sh, null, { c, _ -> WakeReady.ok(c, r) == true }, { c -> WakeReady.fix(c, r) })
    }
    val usage = listOf(
        PRow(Perm.Usage.name, R.drawable.ic_heart, R.string.perm_usage, R.string.perm_usage_short, null, { c, _ -> Perms.granted(c, Perm.Usage) }, { c -> Perms.open(c, Perm.Usage, true) })
    )
    val oem = if (WakeReady.applies(WakeReq.Autostart)) {
        listOf(PRow(WakeReq.Autostart.name, R.drawable.ic_heart, R.string.r3_po_name, R.string.r3_po_short, null, { _, v -> WakeReq.Autostart.name in v }, { c -> WakeReady.fix(c, WakeReq.Autostart) }))
    } else {
        emptyList()
    }
    return runtimeRows + wake + usage + oem
}

@Composable
private fun StepPerms(vm: MainViewModel, h: Boolean, callsOk: Boolean, back: () -> Unit, next: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val rows = remember(h, callsOk) { permRows(h, callsOk) }
    val tick by vm.resume.collectAsState()
    var local by remember { mutableIntStateOf(0) }
    var visited by rememberSaveable { mutableStateOf("") }
    var walking by rememberSaveable { mutableStateOf(false) }
    var pending by remember { mutableStateOf(false) }
    var away by remember { mutableStateOf(false) }
    var all by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val ob = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_PAUSE && pending) away = true
            if (e == Lifecycle.Event.ON_RESUME && pending && away) {
                pending = false
                away = false
            }
        }
        owner.lifecycle.addObserver(ob)
        onDispose { owner.lifecycle.removeObserver(ob) }
    }
    val seen = remember(visited) { visited.split(",").filter { it.isNotEmpty() }.toSet() }
    val state = remember(tick, local, seen, pending) { rows.associate { it.key to it.granted(ctx, seen) } }
    val done = state.values.count { it }
    val missingRuntime = remember(tick, local) {
        Perms.runtimeMissing(ctx, rows.filter { it.runtime != null }.mapNotNull { r -> Perm.entries.firstOrNull { it.name == r.key } })
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        local += 1
        scope.launch {
            rows.filter { it.runtime != null }.forEach { r ->
                vm.sl.repo.audit("permission_" + r.key.lowercase(), jo("granted" to r.granted(ctx, emptySet())))
            }
        }
        if (all) {
            all = false
            walking = true
        }
    }
    LaunchedEffect(walking, pending, local, tick) {
        if (!walking || pending) return@LaunchedEffect
        val nxt = rows.firstOrNull { it.runtime == null && it.key !in seen && !it.granted(ctx, seen) }
        if (nxt == null) {
            walking = false
        } else {
            visited = if (visited.isEmpty()) nxt.key else visited + "," + nxt.key
            pending = true
            away = false
            scope.launch { vm.sl.repo.audit("permission_" + nxt.key.lowercase(), jo("opened" to true)) }
            nxt.open(ctx)
        }
    }
    val title = stringResource(R.string.r4d_perm_title)
    val sub = stringResource(R.string.r4d_perm_sub)
    val allOk = done == rows.size
    ScreenFrame(title, "$title. $sub", back) {
        Up(0) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Progress(7) } }
        Up(1) { Body(sub) }
        Up(2) { Caption(stringResource(R.string.r4d_perm_progress, done, rows.size)) }
        rows.forEachIndexed { i, r ->
            val ok = state[r.key] == true
            Up(3 + i) {
                PermRow(r, ok) {
                    val rt = r.runtime
                    if (rt == null) {
                        visited = if (visited.isEmpty()) r.key else visited + "," + r.key
                        r.open(ctx)
                    } else if (r.key in seen) {
                        Actions.appSettings(ctx)
                    } else {
                        visited = if (visited.isEmpty()) r.key else visited + "," + r.key
                        launcher.launch(rt)
                    }
                }
            }
        }
        Up(3 + rows.size) {
            if (allOk) {
                PrimaryButton(stringResource(R.string.next), next)
            } else {
                PrimaryButton(stringResource(R.string.perm_allow), {
                    pending = false
                    walking = false
                    if (missingRuntime.isNotEmpty()) {
                        all = true
                        visited = (seen + rows.filter { it.runtime != null }.map { it.key }).joinToString(",")
                        launcher.launch(missingRuntime)
                    } else {
                        walking = true
                    }
                })
            }
        }
        if (!allOk) {
            Up(4 + rows.size) {
                TextLink(stringResource(R.string.skip)) {
                    scope.launch { vm.sl.repo.audit("permission_skipped", jo("done" to done, "total" to rows.size)) }
                    next()
                }
            }
        }
    }
}

@Composable
private fun PermRow(r: PRow, ok: Boolean, onClick: () -> Unit) {
    val l = LocalLayout.current
    val c = LocalSimha.current
    Surface(
        shape = RoundedCornerShape(SimhaRadius),
        color = c.surface,
        border = BorderStroke(1.dp, c.border),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = l.target)
            .pressClick(onClick, enabled = !ok),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(r.icon), null, Modifier.size(28.dp), tint = c.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(r.name), style = MaterialTheme.typography.titleMedium)
                Caption(stringResource(r.short))
            }
            Spacer(Modifier.width(12.dp))
            if (ok) {
                Text("✓", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.tertiary)
            } else {
                Text(stringResource(R.string.perm_allow), style = MaterialTheme.typography.labelLarge, color = c.primary)
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
    val kinds = listOf("call_family", "call_hospital", "record")
    val questions = if (h) {
        listOf(R.string.o_ap_family_q_other, R.string.o_ap_hospital_q_other, R.string.o_ap_record_q_other)
    } else {
        listOf(R.string.ap_family_q, R.string.ap_hospital_q, R.string.ap_record_q)
    }
    val idx = vm.obApproval.coerceIn(0, 2)
    val kind = kinds[idx]
    val a = answers[kind]
    val q = stringResource(questions[idx])
    ScreenFrame(stringResource(R.string.ob6_title), q, { if (idx > 0) vm.obApproval = idx - 1 else back() }) {
        Up(0) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Progress(6) } }
        Up(1) { Body(q) }
        val yes: () -> Unit = {
            onAnswer(kind, true)
            vm.setApproval(kind, true)
        }
        val no: () -> Unit = {
            onAnswer(kind, false)
            vm.setApproval(kind, false)
        }
        Up(2) { if (a == true) PrimaryButton(stringResource(R.string.yes), yes) else SecondaryButton(stringResource(R.string.yes), yes) }
        Up(3) { if (a == false) PrimaryButton(stringResource(R.string.no), no) else SecondaryButton(stringResource(R.string.no), no) }
        if (a != null) {
            Body(stringResource(if (a) R.string.ap_yes_ok else R.string.ap_no_ok))
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
        Up(0) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Progress(8) } }
        Up(1) { Card { Body(stringResource(R.string.ci_q)) } }
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

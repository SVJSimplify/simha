package com.simha.app.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.simha.app.Actions
import com.simha.app.MainViewModel
import com.simha.app.Perm
import com.simha.app.Perms
import com.simha.app.R
import com.simha.app.data.CallResult
import com.simha.app.data.ContactEntity
import com.simha.app.data.ProfileEntity
import com.simha.app.data.OBK
import com.simha.app.data.Repo
import com.simha.app.data.simhaStore
import com.simha.app.ui.theme.LocalSimha
import kotlinx.coroutines.flow.map
import com.simha.app.data.Snap
import com.simha.app.data.jo
import com.simha.app.logic.EmergencyPlan
import com.simha.app.logic.Step
import com.simha.app.logic.StepId
import kotlinx.coroutines.launch

class PickPhone : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

@Composable
fun rememberPicker(onPick: (String, String) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(PickPhone()) { uri ->
        if (uri != null) {
            ctx.contentResolver.query(
                uri,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) onPick(c.getString(0) ?: "", c.getString(1) ?: "")
            }
        }
    }
    return { launcher.launch(Unit) }
}

@Composable
fun ContactCard(c: ContactEntity, onRemove: () -> Unit) {
    Card {
        Body(c.name + " (" + roleLabel(c.role) + ")")
        Caption(c.phone)
        BigButton(stringResource(R.string.remove), onRemove, kind = Kind.Secondary)
    }
}

@Composable
fun ContactsScreen(vm: MainViewModel, s: Snap, group: String, onBack: () -> Unit) {
    val roles = if (group == "doctor") Repo.CLINIC_ROLES else Repo.FAMILY_ROLES
    val addable = if (group == "doctor") listOf("doctor") else listOf("family", "neighbor")
    val all by vm.contacts.collectAsState()
    val prof by vm.profile.collectAsState()
    val list = all.filter { it.role in roles }
    val scope = rememberCoroutineScope()
    var failed by remember { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf<String?>(null) }
    var label by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var tried by rememberSaveable { mutableStateOf(false) }
    val pick = rememberPicker { n, p ->
        label = n
        phone = p
    }
    val title = stringResource(if (group == "doctor") R.string.doc_title else R.string.fam_title)
    ScreenFrame(title, title, onBack) {
        if (list.isEmpty() && !(group == "doctor" && prof.doctorPhone.isNotBlank())) Body(stringResource(R.string.no_contacts))
        list.forEach { c ->
            Card {
                Body(c.name)
                Caption(roleLabel(c.role))
                Body(c.phone)
                PrimaryButton(stringResource(R.string.call_btn, c.name), {
                    scope.launch { failed = vm.sl.repo.placeCall(c.phone, c.approved, "user_button") == CallResult.Failed }
                })
                SecondaryButton(stringResource(R.string.remove), { vm.removeContact(c.id) })
            }
        }
        if (group == "doctor" && prof.doctorPhone.isNotBlank()) {
            Card {
                Body(prof.doctorName.ifBlank { stringResource(R.string.role_doctor) })
                Caption(roleLabel("doctor"))
                Body(prof.doctorPhone)
                PrimaryButton(stringResource(R.string.call_btn, prof.doctorName.ifBlank { prof.doctorPhone }), {
                    scope.launch { failed = vm.sl.repo.placeCall(prof.doctorPhone, false, "user_button") == CallResult.Failed }
                })
            }
        }
        if (failed) Body(stringResource(R.string.call_failed), color = LocalSimha.current.emergency)
        Caption(stringResource(R.string.call_dialer_note))
        val role = adding
        if (role == null) {
            addable.forEach { r ->
                SecondaryButton(
                    stringResource(
                        when (r) {
                            "family" -> R.string.m_add_family
                            "neighbor" -> R.string.m_add_neighbour
                            else -> R.string.m_add_doctor
                        }
                    ),
                    {
                        label = ""
                        phone = ""
                        tried = false
                        adding = r
                    },
                )
            }
        } else {
            val norm = normalizeContactPhone(phone)
            Card {
                LabeledField(
                    stringResource(R.string.m_label), label, { label = it },
                    required = true,
                    error = if (tried && label.isBlank()) stringResource(R.string.m_err_label) else null,
                )
                LabeledField(
                    stringResource(R.string.m_phone), phone, { phone = it },
                    required = true,
                    error = if (tried && norm == null) stringResource(R.string.m_err_phone) else null,
                    keyboard = KeyboardType.Phone,
                )
                PrimaryButton(stringResource(R.string.save), {
                    tried = true
                    if (label.isNotBlank() && norm != null) {
                        vm.addContact(label.trim(), "+91$norm", role)
                        adding = null
                    }
                })
                SecondaryButton(stringResource(R.string.m_pick_contacts), pick)
                TextLink(stringResource(R.string.m_cancel), { adding = null })
            }
        }
    }
}

private fun openMaps(ctx: Context): Boolean {
    val loc = Actions.lastLocation(ctx)
    val uri = if (loc != null) {
        Uri.parse("https://www.google.com/maps/search/hospital/@" + loc.latitude + "," + loc.longitude + ",14z")
    } else {
        Uri.parse("geo:0,0?q=hospital")
    }
    return try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    } catch (e: Exception) {
        false
    }
}

@Composable
fun ProfileScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val p by vm.profile.collectAsState()
    val hosp by remember { ctx.simhaStore.data.map { it[OBK.HOSPITAL] ?: "" } }.collectAsState("")
    var cond by remember(p) { mutableStateOf(p.conditions) }
    var med by remember(p) { mutableStateOf(p.medicines) }
    var all by remember(p) { mutableStateOf(p.allergies) }
    var dn by remember(p) { mutableStateOf(p.doctorName) }
    var dp by remember(p) { mutableStateOf(p.doctorPhone) }
    var hn by remember(hosp) { mutableStateOf(hosp) }
    var saved by remember { mutableStateOf(false) }
    var tried by remember { mutableStateOf(false) }
    var mapsFailed by remember { mutableStateOf(false) }
    val dpNorm = normalizeContactPhone(dp)
    val dpBad = dp.isNotBlank() && dpNorm == null
    val title = stringResource(R.string.profile_title)
    ScreenFrame(title, title, onBack) {
        LabeledField(stringResource(R.string.f_conditions), cond, { cond = it; saved = false }, singleLine = false)
        LabeledField(stringResource(R.string.f_medicines), med, { med = it; saved = false }, singleLine = false)
        LabeledField(stringResource(R.string.f_allergies), all, { all = it; saved = false }, singleLine = false)
        LabeledField(stringResource(R.string.f_doctor_name), dn, { dn = it; saved = false })
        LabeledField(
            stringResource(R.string.f_doctor_phone), dp, { dp = it; saved = false },
            error = if (tried && dpBad) stringResource(R.string.m_err_phone) else null,
            keyboard = KeyboardType.Phone,
        )
        Caption(stringResource(R.string.m_hospital_hint))
        SecondaryButton(stringResource(R.string.m_find_hospitals), { mapsFailed = !openMaps(ctx) })
        if (mapsFailed) Body(stringResource(R.string.m_maps_failed), color = LocalSimha.current.emergency)
        LabeledField(stringResource(R.string.m_hospital_name), hn, { hn = it; saved = false })
        PrimaryButton(stringResource(R.string.save), {
            tried = true
            if (!dpBad) {
                vm.saveProfile(ProfileEntity(1, cond.trim(), med.trim(), all.trim(), dn.trim(), if (dpNorm != null) "+91$dpNorm" else ""))
                scope.launch {
                    vm.sl.prefs.edit { this[OBK.HOSPITAL] = hn.trim() }
                    vm.sl.repo.audit("profile_updated")
                }
                saved = true
            }
        })
        if (saved) Body(stringResource(R.string.saved), color = LocalSimha.current.success)
    }
}

@Composable
private fun stepText(st: Step): String = when (st.id) {
    StepId.Calm -> stringResource(R.string.st_calm)
    StepId.Call112 -> stringResource(R.string.st_call112)
    StepId.Unlock -> stringResource(R.string.st_unlock)
    StepId.Heart -> stringResource(R.string.st_heart)
    StepId.Diabetes -> stringResource(R.string.st_diabetes)
    StepId.Breath -> stringResource(R.string.st_breath)
    StepId.Meds -> stringResource(R.string.st_meds, st.arg ?: "")
    StepId.Allergy -> stringResource(R.string.st_allergy, st.arg ?: "")
    StepId.Family -> stringResource(R.string.st_family, st.arg ?: "")
    StepId.Record -> stringResource(R.string.st_record)
}

@Composable
fun EmergencyScreen(vm: MainViewModel, s: Snap, onBack: () -> Unit, onMedicalId: () -> Unit) {
    val ctx = LocalContext.current
    val sl = vm.sl
    val scope = rememberCoroutineScope()
    val contacts by vm.contacts.collectAsState()
    val prof by vm.profile.collectAsState()
    val recording by sl.recorder.recording.collectAsState()
    val fam = contacts.firstOrNull { it.role in Repo.FAMILY_ROLES && it.approved && s.callFamily }
    val canRecord = s.record && Perms.granted(ctx, Perm.Mic)
    val steps = remember(prof, fam, canRecord) {
        EmergencyPlan.build(prof.conditions, prof.medicines, prof.allergies, fam?.name, canRecord)
    }
    var idx by rememberSaveable { mutableIntStateOf(0) }
    var info by remember { mutableStateOf<String?>(null) }
    val step = steps[idx.coerceIn(0, steps.size - 1)]
    val text = stepText(step)
    val notice = stringResource(R.string.rec_notice)
    val savedMsg = stringResource(R.string.em_saved)
    LaunchedEffect(Unit) {
        sl.repo.logEvent("help", jo("source" to "emergency_screen"))
        sl.repo.audit("emergency_opened")
    }
    ScreenFrame(stringResource(R.string.em_title), text, onBack) {
        Caption(stringResource(R.string.em_step, idx + 1, steps.size))
        Card { Body(text) }
        when (step.id) {
            StepId.Call112 -> BigButton(stringResource(R.string.em_call112), {
                scope.launch { if (!sl.repo.dialEmergency("emergency_screen")) info = ctx.getString(R.string.call_failed) }
            }, kind = Kind.Emergency)
            StepId.Family -> BigButton(stringResource(R.string.call_btn, step.arg ?: ""), {
                scope.launch {
                    if (fam != null && sl.repo.placeCall(fam.phone, true, "emergency_screen") == CallResult.Failed) info = ctx.getString(R.string.call_failed)
                }
            })
            StepId.Record -> if (!recording) {
                BigButton(stringResource(R.string.em_record), {
                    scope.launch { sl.recorder.start(s.lang, notice).onFailure { info = ctx.getString(R.string.rec_failed, it.message ?: "") } }
                })
            } else {
                BigButton(stringResource(R.string.em_record_stop), {
                    scope.launch {
                        sl.recorder.stopAndUpload().fold(
                            onSuccess = { info = savedMsg },
                            onFailure = { info = ctx.getString(R.string.em_save_failed, it.message ?: "") },
                        )
                    }
                }, kind = Kind.Success)
            }
            else -> Unit
        }
        info?.let { Body(it) }
        if (idx < steps.size - 1) {
            BigButton(stringResource(R.string.next), { idx += 1 })
        } else {
            BigButton(stringResource(R.string.em_done), onBack)
        }
        if (idx > 0) BigButton(stringResource(R.string.back), { idx -= 1 }, kind = Kind.Secondary)
        BigButton(stringResource(R.string.r3_mid_title), onMedicalId, kind = Kind.Secondary)
    }
}

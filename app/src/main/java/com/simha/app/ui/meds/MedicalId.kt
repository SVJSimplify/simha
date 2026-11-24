package com.simha.app.ui.meds

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.simha.app.Actions
import com.simha.app.MainViewModel
import com.simha.app.R
import com.simha.app.data.Medicine
import com.simha.app.ui.Body
import com.simha.app.ui.Caption
import com.simha.app.ui.Card
import com.simha.app.ui.LabeledField
import com.simha.app.ui.PrimaryButton
import com.simha.app.ui.ScreenFrame
import com.simha.app.ui.SwitchRow
import com.simha.app.ui.roleLabel
import com.simha.app.ui.theme.LocalSimha

object MedicalIdNotif {
    private const val CH = "medical_id"
    private const val ID = 7301

    fun show(ctx: Context, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 && !Actions.has(ctx, Manifest.permission.POST_NOTIFICATIONS)) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        val ch = NotificationChannel(CH, title, NotificationManager.IMPORTANCE_LOW)
        ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        nm.createNotificationChannel(ch)
        val n = NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text.lineSequence().firstOrNull().orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        nm.notify(ID, n)
    }

    fun hide(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java)?.cancel(ID)
    }
}

private fun line(label: String, v: String, none: String) = label + ": " + v.trim().ifBlank { none }

@Composable
fun MedicalIdScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val s by vm.snap.collectAsState()
    val p by vm.profile.collectAsState()
    val ui by vm.ui.collectAsState()
    val meds by vm.medicines.collectAsState()
    val contacts by vm.contacts.collectAsState()
    var blood by remember(ui.blood) { mutableStateOf(ui.blood) }
    var saved by remember { mutableStateOf(false) }
    val title = stringResource(R.string.r3_mid_title)
    val none = stringResource(R.string.r3_mid_none)
    val name = s?.name.orEmpty().ifBlank { none }
    val medText = (p.medicines.trim().takeIf { it.isNotEmpty() }?.let { listOf(it) }.orEmpty() +
        meds.filter { it.active }.map { medLine(it) }).joinToString(", ").ifBlank { none }
    val contactText = contacts.joinToString("\n") { it.name + " " + it.phone }.ifBlank { none }
    val lNam = stringResource(R.string.r3_mid_name)
    val lCond = stringResource(R.string.f_conditions)
    val lAll = stringResource(R.string.f_allergies)
    val lMed = stringResource(R.string.f_medicines)
    val lBlood = stringResource(R.string.r3_mid_blood)
    val lCon = stringResource(R.string.r3_mid_contacts)
    val full = listOf(
        line(lNam, name, none), line(lBlood, ui.blood, none), line(lCond, p.conditions, none),
        line(lAll, p.allergies, none), lMed + ": " + medText, lCon + ": " + contactText.replace("\n", "; "),
    ).joinToString("\n")
    val post = { MedicalIdNotif.show(ctx, title, full) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) post() else vm.setIdLock(false)
    }
    LaunchedEffect(full, ui.idLock) { if (ui.idLock) post() }
    ScreenFrame(title, full, onBack) {
        Card {
            Caption(lNam)
            Text(name, style = MaterialTheme.typography.headlineSmall, color = LocalSimha.current.text)
            Caption(lBlood)
            Text(ui.blood.ifBlank { none }, style = MaterialTheme.typography.headlineSmall, color = LocalSimha.current.primary)
            Caption(lCond)
            Body(p.conditions.ifBlank { none })
            Caption(lAll)
            Body(p.allergies.ifBlank { none })
            Caption(lMed)
            Body(medText)
            Caption(lCon)
            contacts.forEach { Body(it.name + " (" + roleLabel(it.role) + ")  " + it.phone) }
            if (contacts.isEmpty()) Body(none)
        }
        LabeledField(lBlood, blood, { blood = it.take(5); saved = false })
        Caption(stringResource(R.string.r3_mid_blood_hint))
        PrimaryButton(stringResource(R.string.save), { vm.setBlood(blood.trim()); saved = true })
        if (saved) Body(stringResource(R.string.saved), color = LocalSimha.current.success)
        SwitchRow(stringResource(R.string.r3_mid_lock), ui.idLock, { on ->
            vm.setIdLock(on)
            if (on) {
                if (Build.VERSION.SDK_INT >= 33 && !Actions.has(ctx, Manifest.permission.POST_NOTIFICATIONS)) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    post()
                }
            } else {
                MedicalIdNotif.hide(ctx)
            }
        }, stringResource(R.string.r3_mid_lock_hint))
    }
}

private fun medLine(m: Medicine) = m.name + if (m.dose.isBlank()) "" else " " + m.dose

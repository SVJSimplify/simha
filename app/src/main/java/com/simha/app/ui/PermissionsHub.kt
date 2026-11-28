package com.simha.app.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.simha.app.Actions
import com.simha.app.Perm
import com.simha.app.Perms
import com.simha.app.R
import com.simha.app.phone.SimhaAccess

private enum class Hub(val label: Int, val why: Int, val off: Int, val minSdk: Int = 1) {
    Mic(R.string.hub_mic, R.string.hub_mic_why, R.string.hub_mic_off),
    Call(R.string.hub_call, R.string.hub_call_why, R.string.hub_call_off),
    Sms(R.string.hub_sms, R.string.hub_sms_why, R.string.hub_sms_off),
    Contacts(R.string.hub_contacts, R.string.hub_contacts_why, R.string.hub_contacts_off),
    Loc(R.string.hub_loc, R.string.hub_loc_why, R.string.hub_loc_off),
    Cam(R.string.hub_cam, R.string.hub_cam_why, R.string.hub_cam_off),
    Notif(R.string.hub_notif, R.string.hub_notif_why, R.string.hub_notif_off, 33),
    Motion(R.string.hub_act, R.string.hub_act_why, R.string.hub_act_off, 29),
    Overlay(R.string.hub_overlay, R.string.hub_overlay_why, R.string.hub_overlay_off),
    Alarm(R.string.hub_alarm, R.string.hub_alarm_why, R.string.hub_alarm_off, 31),
    Battery(R.string.hub_batt, R.string.hub_batt_why, R.string.hub_batt_off),
    Full(R.string.hub_full, R.string.hub_full_why, R.string.hub_full_off, 34),
    Dnd(R.string.hub_dnd, R.string.hub_dnd_why, R.string.hub_dnd_off),
    Write(R.string.hub_write, R.string.hub_write_why, R.string.hub_write_off),
    Access(R.string.hub_access, R.string.hub_access_why, R.string.hub_access_off);

    fun available(): Boolean = Build.VERSION.SDK_INT >= minSdk

    fun runtime(): Array<String> = when (this) {
        Mic -> arrayOf(Manifest.permission.RECORD_AUDIO)
        Call -> arrayOf(Manifest.permission.CALL_PHONE)
        Sms -> arrayOf(Manifest.permission.SEND_SMS)
        Contacts -> arrayOf(Manifest.permission.READ_CONTACTS)
        Loc -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        Cam -> arrayOf(Manifest.permission.CAMERA)
        Notif -> arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        Motion -> arrayOf(Manifest.permission.ACTIVITY_RECOGNITION)
        else -> emptyArray()
    }

    fun granted(c: Context): Boolean {
        if (!available()) return true
        return when (this) {
            Loc -> Actions.has(c, Manifest.permission.ACCESS_FINE_LOCATION) || Actions.has(c, Manifest.permission.ACCESS_COARSE_LOCATION)
            Overlay -> Perms.granted(c, Perm.Overlay)
            Alarm -> if (Build.VERSION.SDK_INT >= 31) c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms() else true
            Battery -> Perms.granted(c, Perm.Battery)
            Full -> Perms.granted(c, Perm.FullScreen)
            Dnd -> c.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
            Write -> Settings.System.canWrite(c)
            Access -> SimhaAccess.enabled(c)
            else -> runtime().all { Actions.has(c, it) }
        }
    }

    fun intent(c: Context, forOff: Boolean): Intent {
        val pkg = Uri.parse("package:" + c.packageName)
        return when (this) {
            Overlay -> Perms.settingsIntent(c, Perm.Overlay, !forOff)
            Alarm ->
                if (Build.VERSION.SDK_INT >= 31) Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg)
                else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
            Battery -> Perms.settingsIntent(c, Perm.Battery, !forOff)
            Full -> Perms.settingsIntent(c, Perm.FullScreen, !forOff)
            Dnd -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            Write -> Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, pkg)
            Access -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
        }
    }
}

private fun open(c: Context, i: Intent) {
    try {
        c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Actions.appSettings(c)
    }
}

private tailrec fun Context.findActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun specials(c: Context): List<Hub> =
    Hub.entries.filter { it.runtime().isEmpty() && it.available() && !it.granted(c) }

@Composable
fun PermissionsHub() {
    val ctx = LocalContext.current
    val act = remember(ctx) { ctx.findActivity() }
    var tick by remember { mutableIntStateOf(0) }
    var confirm by remember { mutableStateOf(false) }
    var offFor by remember { mutableStateOf<Hub?>(null) }
    var queue by remember { mutableStateOf<List<Hub>>(emptyList()) }
    var awaiting by remember { mutableStateOf<Hub?>(null) }
    var away by remember { mutableStateOf(false) }
    var asked by remember { mutableLongStateOf(0L) }

    val bioTitle = stringResource(R.string.hub_bio_title)
    val bioSub = stringResource(R.string.hub_bio_sub)
    val cancel = stringResource(R.string.hub_cancel)

    fun next(rest: List<Hub>) {
        val h = rest.firstOrNull { !it.granted(ctx) }
        if (h == null) {
            queue = emptyList()
            awaiting = null
            return
        }
        queue = rest.dropWhile { it != h }.drop(1)
        awaiting = h
        away = false
        open(ctx, h.intent(ctx, false))
    }

    val all = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        tick++
        next(specials(ctx))
    }
    val one = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        tick++
        if (r.values.any { !it } && SystemClock.elapsedRealtime() - asked < 400) Actions.appSettings(ctx)
    }

    DisposableEffect(act) {
        val lc = act?.lifecycle
        val o = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_PAUSE && awaiting != null) away = true
            if (e == Lifecycle.Event.ON_RESUME) {
                tick++
                if (awaiting != null && away) {
                    away = false
                    next(queue)
                }
            }
        }
        lc?.addObserver(o)
        onDispose { lc?.removeObserver(o) }
    }

    fun begin() {
        val missing = Hub.entries
            .filter { it.available() }
            .flatMap { it.runtime().toList() }
            .distinct()
            .filter { !Actions.has(ctx, it) }
            .toTypedArray()
        if (missing.isEmpty()) next(specials(ctx)) else all.launch(missing)
    }

    fun start() {
        val auth = if (Build.VERSION.SDK_INT >= 30) {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        }
        val a = act
        if (a == null || BiometricManager.from(a).canAuthenticate(auth) != BiometricManager.BIOMETRIC_SUCCESS) {
            begin()
            return
        }
        val b = BiometricPrompt.PromptInfo.Builder().setTitle(bioTitle).setSubtitle(bioSub).setAllowedAuthenticators(auth)
        if (Build.VERSION.SDK_INT < 30) b.setNegativeButtonText(cancel)
        BiometricPrompt(
            a,
            ContextCompat.getMainExecutor(a),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    begin()
                }
            },
        ).authenticate(b.build())
    }

    fun request(h: Hub) {
        val p = h.runtime()
        if (p.isEmpty()) {
            open(ctx, h.intent(ctx, false))
        } else {
            asked = SystemClock.elapsedRealtime()
            one.launch(p)
        }
    }

    val shown = Hub.entries.filter { it.available() }
    val ready = remember(tick) { shown.all { it.granted(ctx) } }

    Body(stringResource(R.string.hub_intro))
    if (ready) {
        Card { Body(stringResource(R.string.hub_all_done)) }
    } else {
        BigButton(stringResource(R.string.hub_all), { confirm = true })
    }
    shown.forEach { h ->
        val on = remember(tick, h) { h.granted(ctx) }
        val state = stringResource(if (on) R.string.hub_on else R.string.hub_off)
        SwitchRow(
            stringResource(h.label),
            on,
            { want -> if (want) request(h) else offFor = h },
            state + ". " + stringResource(h.why),
        )
    }

    if (confirm) {
        HubDialog(
            stringResource(R.string.hub_confirm_title),
            stringResource(R.string.hub_confirm_body),
            stringResource(R.string.hub_continue),
            stringResource(R.string.hub_not_now),
            { confirm = false; start() },
            { confirm = false },
        )
    }
    offFor?.let { h ->
        HubDialog(
            stringResource(R.string.hub_off_title, stringResource(h.label)),
            stringResource(h.off) + "\n\n" + stringResource(R.string.hub_off_hint),
            stringResource(R.string.hub_open_settings),
            stringResource(R.string.hub_not_now),
            { offFor = null; open(ctx, h.intent(ctx, true)) },
            { offFor = null },
        )
    }
}

@Composable
private fun HubDialog(title: String, body: String, yes: String, no: String, onYes: () -> Unit, onNo: () -> Unit) {
    Dialog(onDismissRequest = onNo, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.padding(16.dp)) {
            Card {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) { Body(body) }
                PrimaryButton(yes, onYes)
                SecondaryButton(no, onNo)
            }
        }
    }
}

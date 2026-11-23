package com.simha.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.simha.app.monitor.UsageReader
import java.util.Locale

enum class Perm { Notifications, Mic, Camera, Location, Usage, FullScreen, Battery, CallPhone, Overlay }

object Actions {
    fun has(ctx: Context, p: String): Boolean =
        ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    private fun start(ctx: Context, i: Intent): Boolean = try {
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }

    fun dial(ctx: Context, number: String): Boolean =
        start(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))))

    @SuppressLint("MissingPermission")
    fun call(ctx: Context, number: String): Boolean {
        if (!has(ctx, Manifest.permission.CALL_PHONE)) return false
        return start(ctx, Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(number))))
    }

    fun smsIntent(numbers: List<String>, body: String): Intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + numbers.joinToString(";") { Uri.encode(it) })).putExtra("sms_body", body)

    fun sms(ctx: Context, numbers: List<String>, body: String): Boolean = start(ctx, smsIntent(numbers, body))

    fun appSettings(ctx: Context): Boolean =
        start(ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName)))

    @SuppressLint("MissingPermission")
    fun lastLocation(ctx: Context): Location? {
        if (!has(ctx, Manifest.permission.ACCESS_FINE_LOCATION) && !has(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)) return null
        val lm = ctx.getSystemService(LocationManager::class.java) ?: return null
        return try {
            lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
        } catch (e: SecurityException) {
            null
        }
    }
}

object Perms {
    fun available(p: Perm): Boolean = when (p) {
        Perm.Notifications -> Build.VERSION.SDK_INT >= 33
        Perm.FullScreen -> Build.VERSION.SDK_INT >= 34
        else -> true
    }

    fun runtime(p: Perm): Array<String>? = when (p) {
        Perm.Notifications -> arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        Perm.Mic -> arrayOf(Manifest.permission.RECORD_AUDIO)
        Perm.Camera -> arrayOf(Manifest.permission.CAMERA)
        Perm.Location -> arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        Perm.CallPhone -> arrayOf(Manifest.permission.CALL_PHONE)
        else -> null
    }

    fun granted(ctx: Context, p: Perm): Boolean = when (p) {
        Perm.Notifications -> if (Build.VERSION.SDK_INT >= 33) Actions.has(ctx, Manifest.permission.POST_NOTIFICATIONS) else true
        Perm.Mic -> Actions.has(ctx, Manifest.permission.RECORD_AUDIO)
        Perm.Camera -> Actions.has(ctx, Manifest.permission.CAMERA)
        Perm.Location -> Actions.has(ctx, Manifest.permission.ACCESS_FINE_LOCATION) || Actions.has(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
        Perm.CallPhone -> Actions.has(ctx, Manifest.permission.CALL_PHONE)
        Perm.Usage -> UsageReader.granted(ctx)
        Perm.FullScreen -> if (Build.VERSION.SDK_INT >= 34) {
            ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        } else {
            true
        }
        Perm.Battery -> ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
        Perm.Overlay -> Settings.canDrawOverlays(ctx)
    }

    @SuppressLint("BatteryLife")
    fun settingsIntent(ctx: Context, p: Perm, forRequest: Boolean): Intent {
        val pkg = Uri.parse("package:" + ctx.packageName)
        return when (p) {
            Perm.Usage -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            Perm.Overlay -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkg)
            Perm.FullScreen -> Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg)
            Perm.Battery ->
                if (forRequest) Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg)
                else Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
        }
    }

    fun open(ctx: Context, p: Perm, forRequest: Boolean = false): Boolean {
        val i = settingsIntent(ctx, p, forRequest).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(i)
            true
        } catch (e: Exception) {
            Actions.appSettings(ctx)
        }
    }

    fun snapshot(ctx: Context): String =
        Perm.entries.filter { available(it) }.joinToString(",") { it.name + "=" + (if (granted(ctx, it)) 1 else 0) }
}

fun Context.forLang(tag: String): Context {
    val c = Configuration(resources.configuration)
    c.setLocale(Locale.forLanguageTag(tag))
    return createConfigurationContext(c)
}

enum class WakeReq(val perm: Perm?, val label: Int) {
    Notifications(Perm.Notifications, R.string.perm_notif),
    FullScreen(Perm.FullScreen, R.string.perm_full),
    Overlay(Perm.Overlay, R.string.perm_overlay),
    Exact(null, R.string.r3_exact),
    Battery(Perm.Battery, R.string.perm_batt),
    Mic(Perm.Mic, R.string.perm_mic),
    Autostart(null, R.string.wr_oem),
}

object WakeReady {
    fun applies(r: WakeReq): Boolean = when (r) {
        WakeReq.Exact -> Build.VERSION.SDK_INT >= 31
        WakeReq.Autostart -> com.simha.app.ui.home.Oem.applicable()
        else -> r.perm == null || Perms.available(r.perm)
    }

    fun ok(ctx: Context, r: WakeReq): Boolean? = when (r) {
        WakeReq.Exact -> com.simha.app.ui.home.Oem.exactAllowed(ctx)
        WakeReq.Autostart -> null
        else -> Perms.granted(ctx, r.perm!!)
    }

    fun fix(ctx: Context, r: WakeReq) {
        when (r) {
            WakeReq.Exact -> com.simha.app.ui.home.Oem.openExact(ctx)
            WakeReq.Autostart -> com.simha.app.ui.home.Oem.open(ctx)
            else -> Perms.open(ctx, r.perm!!, r == WakeReq.Battery)
        }
    }

    fun missing(ctx: Context): Boolean = WakeReq.entries.any { applies(it) && ok(ctx, it) == false }
}

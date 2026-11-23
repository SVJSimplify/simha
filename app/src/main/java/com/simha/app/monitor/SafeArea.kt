package com.simha.app.monitor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import com.simha.app.Actions
import com.simha.app.R
import com.simha.app.SimhaApp
import com.simha.app.data.Repo
import com.simha.app.data.jo
import com.simha.app.forLang
import com.simha.app.data.simhaStore
import com.simha.app.ui.home.UiKeys
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.simha.app.wake.Wake
import com.simha.app.wake.WakeStore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.coroutines.resume
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object SafeMath {
    const val RADIUS_M = 1000.0
    const val LONG_MS = 3L * 3_600_000L

    fun distanceM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dp = p2 - p1
        val dl = Math.toRadians(lon2 - lon1)
        val a = sin(dp / 2).pow(2) + cos(p1) * cos(p2) * sin(dl / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }

    fun isNight(t: LocalTime): Boolean = t.hour >= 21 || t.hour < 6

    fun shouldAlert(outsideSince: Long, now: Long, zone: ZoneId): Boolean =
        isNight(Instant.ofEpochMilli(now).atZone(zone).toLocalTime()) || now - outsideSince > LONG_MS
}

object SafeArea {
    private const val CHECK_MS = 10L * 60_000L
    private const val FRESH_MS = 10L * 60_000L
    private const val BG = "android.permission.ACCESS_BACKGROUND_LOCATION"

    fun homeSet(ctx: Context): Boolean = WakeStore.str(ctx, "safe_lat") != null

    fun backgroundGranted(ctx: Context): Boolean = Build.VERSION.SDK_INT < 29 || Actions.has(ctx, BG)

    fun setHome(ctx: Context): Boolean {
        val loc = Actions.lastLocation(ctx) ?: return false
        WakeStore.put(ctx, "safe_lat" to loc.latitude.toString(), "safe_lon" to loc.longitude.toString(), "safe_since" to 0L, "safe_alerted" to false)
        Wake.audit(ctx, "safe_area_home_set")
        return true
    }

    fun setHomeHere(ctx: Context): Boolean = setHome(ctx)

    fun enable(ctx: Context, on: Boolean) {
        val app = ctx.applicationContext
        WakeStore.put(app, "safe_since" to 0L, "safe_alerted" to false)
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.simhaStore.edit { it[UiKeys.SAFE] = on }
            } catch (e: Exception) {
                Log.w("Simha", "safe area pref failed", e)
            }
        }
        Wake.audit(app, "safe_area_set", jo("on" to on))
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private suspend fun fresh(ctx: Context): Location? {
        val last = Actions.lastLocation(ctx)
        if (last != null && System.currentTimeMillis() - last.time < FRESH_MS) return last
        val lm = ctx.getSystemService(LocationManager::class.java) ?: return null
        val prov = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER).firstOrNull {
            try {
                lm.isProviderEnabled(it)
            } catch (e: Exception) {
                false
            }
        } ?: return null
        return withTimeoutOrNull(20_000L) {
            suspendCancellableCoroutine<Location?> { cont ->
                val l = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (cont.isActive) cont.resume(location)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

                    override fun onProviderEnabled(provider: String) {}

                    override fun onProviderDisabled(provider: String) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
                try {
                    lm.requestSingleUpdate(prov, l, Looper.getMainLooper())
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
                cont.invokeOnCancellation {
                    try {
                        lm.removeUpdates(l)
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    suspend fun check(ctx: Context) {
        if (!Wake.ui(ctx).safe) return
        val lat = WakeStore.str(ctx, "safe_lat")?.toDoubleOrNull() ?: return
        val lon = WakeStore.str(ctx, "safe_lon")?.toDoubleOrNull() ?: return
        val now = System.currentTimeMillis()
        if (now - WakeStore.long(ctx, "safe_last") < CHECK_MS) return
        WakeStore.put(ctx, "safe_last" to now)
        if (!backgroundGranted(ctx)) {
            Log.w("Simha", "safe area needs background location")
            return
        }
        val loc = fresh(ctx) ?: return
        if (loc.hasAccuracy() && loc.accuracy > 800f) return
        if (SafeMath.distanceM(lat, lon, loc.latitude, loc.longitude) <= SafeMath.RADIUS_M) {
            WakeStore.put(ctx, "safe_since" to 0L, "safe_alerted" to false)
            return
        }
        val since = WakeStore.long(ctx, "safe_since").takeIf { it > 0L } ?: now.also { WakeStore.put(ctx, "safe_since" to it) }
        if (WakeStore.bool(ctx, "safe_alerted") || !SafeMath.shouldAlert(since, now, ZoneId.systemDefault())) return
        WakeStore.put(ctx, "safe_alerted" to true)
        alert(ctx, loc)
    }

    private suspend fun alert(ctx: Context, loc: Location) {
        val sl = (ctx.applicationContext as SimhaApp).sl
        val p = sl.prefs.snap()
        val lc = ctx.applicationContext.forLang(p.lang)
        val numbers = sl.repo.contactsNow().filter { it.role in Repo.FAMILY_ROLES }.map { it.phone }
        if (numbers.isEmpty()) {
            sl.repo.logEvent("alert", jo("kind" to "safe_area", "sent" to false, "reason" to "no_contacts"))
            sl.repo.audit("safe_area_alert_no_contacts")
            return
        }
        val link = " " + lc.getString(R.string.sms_loc, "https://maps.google.com/?q=" + loc.latitude + "," + loc.longitude)
        val ok = Notifier.smsAlert(lc, numbers, lc.getString(R.string.wake_sms_safearea, p.name) + link)
        sl.repo.logEvent("alert", jo("kind" to "safe_area", "sms_prepared" to ok))
        sl.repo.audit("safe_area_alert", jo("notified" to ok, "recipients" to numbers.size))
    }
}

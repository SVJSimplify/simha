package com.simha.app.phone

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import java.util.ArrayDeque

class Undoable(val label: String, val undo: suspend () -> Boolean)

object Journal {
    private val stack = ArrayDeque<Undoable>()

    @Synchronized
    fun push(u: Undoable) {
        stack.push(u)
        while (stack.size > 20) stack.removeLast()
    }

    @Synchronized
    private fun pop(): Undoable? = if (stack.isEmpty()) null else stack.pop()

    suspend fun undo(): Undoable? {
        val u = pop() ?: return null
        return if (runCatching { u.undo() }.getOrDefault(false)) u else null
    }

    @Synchronized
    fun peek(): Undoable? = stack.peekFirst()
}

sealed interface Res {
    data class Ok(val value: Int = 0) : Res
    data object NeedPermission : Res
    data object Missing : Res
    data object Failed : Res
}

object Phone {
    private val streams = intArrayOf(AudioManager.STREAM_MUSIC, AudioManager.STREAM_RING, AudioManager.STREAM_ALARM)

    fun volume(ctx: Context, dir: Int): Res {
        val am = ctx.getSystemService(AudioManager::class.java) ?: return Res.Failed
        val before = streams.associateWith { am.getStreamVolume(it) }
        streams.forEach { s ->
            val max = am.getStreamMaxVolume(s)
            val cur = am.getStreamVolume(s)
            val step = (max * 0.3f).toInt().coerceAtLeast(1)
            val v = when (dir) {
                2 -> max
                0 -> if (s == AudioManager.STREAM_MUSIC) 0 else cur
                1 -> (cur + step).coerceAtMost(max)
                else -> (cur - step).coerceAtLeast(if (s == AudioManager.STREAM_ALARM) 1 else 0)
            }
            runCatching { am.setStreamVolume(s, v, 0) }
        }
        Journal.push(Undoable("volume") {
            before.forEach { (s, v) -> runCatching { am.setStreamVolume(s, v, 0) } }
            true
        })
        val m = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return Res.Ok(am.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / m)
    }

    @Volatile
    private var torchOn = false

    fun torch(ctx: Context, on: Boolean, record: Boolean = true): Res {
        val cm = ctx.getSystemService(CameraManager::class.java) ?: return Res.Failed
        val id = runCatching {
            cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
        }.getOrNull() ?: return Res.Missing
        return try {
            val was = torchOn
            cm.setTorchMode(id, on)
            torchOn = on
            if (record) Journal.push(Undoable("torch") { torch(ctx, was, false) is Res.Ok })
            Res.Ok()
        } catch (e: Exception) {
            Res.Failed
        }
    }

    fun brightness(ctx: Context, dir: Int): Res {
        if (!Settings.System.canWrite(ctx)) return Res.NeedPermission
        val cr = ctx.contentResolver
        val mode = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        val cur = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128)
        val v = when (dir) {
            2 -> 255
            1 -> (cur + 70).coerceAtMost(255)
            else -> (cur - 70).coerceAtLeast(20)
        }
        return try {
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, v)
            Journal.push(Undoable("brightness") {
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, cur)
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
            })
            Res.Ok(v * 100 / 255)
        } catch (e: Exception) {
            Res.Failed
        }
    }

    fun dnd(ctx: Context, on: Boolean): Res {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return Res.Failed
        if (!nm.isNotificationPolicyAccessGranted) return Res.NeedPermission
        val before = nm.currentInterruptionFilter
        return try {
            nm.setInterruptionFilter(if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL)
            Journal.push(Undoable("dnd") { runCatching { nm.setInterruptionFilter(before) }.isSuccess })
            Res.Ok()
        } catch (e: Exception) {
            Res.Failed
        }
    }

    fun battery(ctx: Context): Pair<Int, Boolean> {
        val bm = ctx.getSystemService(BatteryManager::class.java)
        val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        return pct to (bm?.isCharging == true)
    }

    private val aliases = mapOf(
        "whatsapp" to listOf("whatsapp", "whats app", "व्हाट्सएप", "वॉट्सऐप", "వాట్సాప్", "ವಾಟ್ಸಾಪ್", "வாட்ஸ்அப்"),
        "youtube" to listOf("youtube", "यूट्यूब", "యూట్యూబ్", "ಯೂಟ್ಯೂಬ್", "யூடியூப்"),
        "camera" to listOf("camera", "कैमरा", "కెమెరా", "ಕ್ಯಾಮೆರಾ", "கேமரா"),
        "photos" to listOf("photos", "gallery", "फोटो", "गैलरी", "ఫోటోలు", "గ్యాలరీ", "ಫೋಟೋ", "ಗ್ಯಾಲರಿ", "புகைப்படம்", "கேலரி"),
        "settings" to listOf("settings", "सेटिंग", "సెట్టింగ్", "ಸೆಟ್ಟಿಂಗ್", "அமைப்பு"),
        "phone" to listOf("dialer", "फोन", "ఫోన్", "ಫೋನ್", "போன்"),
        "messages" to listOf("messages", "sms", "मैसेज", "संदेश", "మెసేజ్", "ಮೆಸೇಜ್", "மெசேஜ்"),
        "calculator" to listOf("calculator", "कैलकुलेटर", "కాలిక్యులేటర్", "ಕ್ಯಾಲ್ಕುಲೇಟರ್", "கால்குலேட்டர்"),
        "clock" to listOf("clock", "घड़ी", "గడియారం", "ಗಡಿಯಾರ", "கடிகாரம்"),
        "google pay" to listOf("google pay", "gpay", "जीपे", "गूगल पे"),
        "phonepe" to listOf("phonepe", "phone pe", "फोनपे"),
        "paytm" to listOf("paytm", "पेटीएम"),
    )

    fun canonical(spoken: String): String {
        val s = spoken.lowercase().trim()
        return aliases.entries.firstOrNull { (_, v) -> v.any { s.contains(it) } }?.key ?: s
    }

    fun openApp(ctx: Context, spoken: String): Pair<Res, String> {
        val want = canonical(spoken)
        val pm = ctx.packageManager
        val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = runCatching { pm.queryIntentActivities(launch, 0) }.getOrDefault(emptyList())
        val best = apps.mapNotNull { ri ->
            val label = ri.loadLabel(pm).toString()
            val l = label.lowercase()
            val score = when {
                l == want -> 0
                l.startsWith(want) || want.startsWith(l) -> 1
                l.contains(want) || want.contains(l) -> 2
                lev(l, want) <= 2 -> 3
                else -> null
            }
            score?.let { Triple(it, label, ri.activityInfo.packageName) }
        }.minByOrNull { it.first } ?: return Res.Missing to spoken
        val i = pm.getLaunchIntentForPackage(best.third) ?: return Res.Missing to best.second
        return try {
            ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Res.Ok() to best.second
        } catch (e: Exception) {
            Res.Failed to best.second
        }
    }

    fun music(ctx: Context, query: String): Res {
        val q = query.trim()
        val yt = Intent(Intent.ACTION_SEARCH).setPackage("com.google.android.youtube").putExtra("query", q).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { ctx.startActivity(yt) }.isSuccess) return Res.Ok()
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(q))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (runCatching { ctx.startActivity(web) }.isSuccess) Res.Ok() else Res.Failed
    }

    fun whatsappChat(ctx: Context, phone: String): Res {
        val digits = phone.filter { it.isDigit() }.let { if (it.length == 10) "91$it" else it }
        val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits")).setPackage("com.whatsapp").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { ctx.startActivity(i) }.isSuccess) return Res.Ok()
        return if (runCatching { ctx.packageManager.getPackageInfo("com.whatsapp", 0) }.isSuccess) Res.Failed else Res.Missing
    }

    fun lev(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val cur = IntArray(b.length + 1)
            cur[0] = i
            for (j in 1..b.length) cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            prev = cur
        }
        return prev[b.length]
    }
}

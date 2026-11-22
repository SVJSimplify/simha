package com.simha.app.monitor

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process

object UsageReader {
    private const val FOREGROUND = 1
    private const val BACKGROUND = 2
    private const val SCREEN_INTERACTIVE = 15

    @Suppress("DEPRECATION")
    fun granted(ctx: Context): Boolean {
        val a = ctx.getSystemService(AppOpsManager::class.java) ?: return false
        val m = a.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        return m == AppOpsManager.MODE_ALLOWED
    }

    fun lastUse(ctx: Context, now: Long): Long? {
        val u = ctx.getSystemService(UsageStatsManager::class.java) ?: return null
        val ev = u.queryEvents(now - 24L * 60 * 60 * 1000, now) ?: return null
        val e = UsageEvents.Event()
        var last = 0L
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            if (e.packageName == ctx.packageName) continue
            val t = e.eventType
            val relevant = t == FOREGROUND || t == BACKGROUND || (Build.VERSION.SDK_INT >= 28 && t == SCREEN_INTERACTIVE)
            if (relevant && e.timeStamp > last) last = e.timeStamp
        }
        return last.takeIf { it > 0 }
    }
}

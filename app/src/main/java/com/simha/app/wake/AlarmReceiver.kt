package com.simha.app.wake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val kind = intent.getStringExtra(Wake.X_KIND) ?: return
        val tag = intent.getStringExtra(Wake.X_TAG).orEmpty()
        val at = intent.getLongExtra(Wake.X_AT, System.currentTimeMillis())
        val req = intent.getStringExtra(Wake.X_REQ)
        val pr = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                Wake.onAlarm(app, kind, tag, at, req)
            } catch (e: Exception) {
                Log.e("Simha", "alarm $kind failed", e)
            } finally {
                pr.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val a = intent.action
        if (a != Intent.ACTION_BOOT_COMPLETED && a != Intent.ACTION_MY_PACKAGE_REPLACED && a != Intent.ACTION_TIMEZONE_CHANGED) return
        val app = context.applicationContext
        val pr = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                Wake.scheduleNow(app)
                if (a != Intent.ACTION_TIMEZONE_CHANGED) Wake.restartService(app)
            } catch (e: Exception) {
                Log.e("Simha", "boot reschedule failed", e)
            } finally {
                pr.finish()
            }
        }
    }
}

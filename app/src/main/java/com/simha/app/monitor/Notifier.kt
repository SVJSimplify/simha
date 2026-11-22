package com.simha.app.monitor

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.simha.app.Actions
import com.simha.app.MainActivity
import com.simha.app.R

object Notifier {
    const val CH_SERVICE = "service"
    const val CH_CHECKIN = "checkin"
    const val CH_ALERT = "alert"
    const val CH_REMINDER = "reminder"
    const val ID_SERVICE = 1
    const val ID_CHECKIN = 2
    const val ID_ESCALATE = 3
    const val ID_ALERT = 4
    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CH_SERVICE, ctx.getString(R.string.ch_service), NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(CH_CHECKIN, ctx.getString(R.string.ch_checkin), NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CH_ALERT, ctx.getString(R.string.ch_alert), NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CH_REMINDER, ctx.getString(R.string.ch_reminder), NotificationManager.IMPORTANCE_DEFAULT),
            )
        )
    }

    private fun open(ctx: Context, action: String?, code: Int, extras: Intent.() -> Unit = {}): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).apply {
            this.action = action
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            extras()
        }
        return PendingIntent.getActivity(ctx, code, i, FLAGS)
    }

    fun serviceNotification(ctx: Context): Notification =
        NotificationCompat.Builder(ctx, CH_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ctx.getString(R.string.n_service_title))
            .setContentText(ctx.getString(R.string.n_service_text))
            .setOngoing(true)
            .setContentIntent(open(ctx, null, 10))
            .build()

    @SuppressLint("MissingPermission")
    private fun post(ctx: Context, id: Int, n: Notification): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && !Actions.has(ctx, Manifest.permission.POST_NOTIFICATIONS)) return false
        NotificationManagerCompat.from(ctx).notify(id, n)
        return true
    }

    fun checkin(ctx: Context, name: String): Boolean {
        val pi = open(ctx, MainActivity.ACTION_CHECKIN, 11)
        val n = NotificationCompat.Builder(ctx, CH_CHECKIN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ctx.getString(R.string.n_checkin_title, name))
            .setContentText(ctx.getString(R.string.n_checkin_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .build()
        return post(ctx, ID_CHECKIN, n)
    }

    fun escalate(ctx: Context, contactName: String, number: String): Boolean {
        val pi = open(ctx, MainActivity.ACTION_ESCALATE, 12) {
            putExtra(MainActivity.EXTRA_NAME, contactName)
            putExtra(MainActivity.EXTRA_NUMBER, number)
        }
        val n = NotificationCompat.Builder(ctx, CH_ALERT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ctx.getString(R.string.n_escalate_title, contactName))
            .setContentText(ctx.getString(R.string.n_escalate_text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .build()
        return post(ctx, ID_ESCALATE, n)
    }

    fun smsAlert(ctx: Context, numbers: List<String>, body: String): Boolean {
        val pi = PendingIntent.getActivity(
            ctx, 13, Actions.smsIntent(numbers, body).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), FLAGS,
        )
        val n = NotificationCompat.Builder(ctx, CH_ALERT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ctx.getString(R.string.n_alert_title))
            .setContentText(ctx.getString(R.string.n_alert_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        return post(ctx, ID_ALERT, n)
    }

    fun reminder(ctx: Context, text: String): Boolean {
        val n = NotificationCompat.Builder(ctx, CH_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ctx.getString(R.string.n_reminder_title))
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(open(ctx, null, 14))
            .build()
        return post(ctx, 100 + (System.currentTimeMillis() % 1000).toInt(), n)
    }
}

package com.simha.app.update

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.simha.app.R
import java.util.concurrent.TimeUnit

class UpdateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val ctx = com.simha.app.LocaleCtx.wrapped(applicationContext)
        val info = Updater.check(ctx).getOrElse { return Result.retry() }
        if (info == null) {
            NotificationManagerCompat.from(ctx).cancel(NOTIF_ID)
            return Result.success()
        }
        val prefs = ctx.getSharedPreferences("update", Context.MODE_PRIVATE)
        if (prefs.getInt("notified", 0) >= info.versionCode) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, ctx.getString(R.string.upd_channel), NotificationManager.IMPORTANCE_DEFAULT)
        )
        val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName) ?: return Result.success()
        val pi = PendingIntent.getActivity(ctx, 20, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ctx.getString(R.string.upd_notif_title))
            .setContentText(ctx.getString(R.string.upd_available, info.versionCode))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(ctx).notify(NOTIF_ID, n)
        prefs.edit().putInt("notified", info.versionCode).apply()
        return Result.success()
    }

    companion object {
        const val CHANNEL = "update"
        const val NOTIF_ID = 200

        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<UpdateWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("update_check", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}

package com.simha.app.monitor

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.simha.app.SimhaApp
import com.simha.app.forLang
import java.util.concurrent.TimeUnit

class ActivityWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Monitor.tick(applicationContext)
        val sl = (applicationContext as SimhaApp).sl
        sl.repo.flush()
        runCatching { sl.guardian.maintain() }
        return Result.success()
    }
}

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result =
        if ((applicationContext as SimhaApp).sl.repo.flush().isSuccess) Result.success() else Result.retry()
}

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val sl = (applicationContext as SimhaApp).sl
        val lc = applicationContext.forLang(sl.prefs.snap().lang)
        val text = inputData.getString("text") ?: return Result.failure()
        if (!Notifier.reminder(lc, text)) sl.repo.audit("notification_blocked", com.simha.app.data.jo("kind" to "reminder"))
        return Result.success()
    }
}

object Work {
    fun schedule(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<ActivityWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("activity", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun cancel(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork("activity")
    }

    fun scheduleSync(ctx: Context) {
        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork("sync", ExistingWorkPolicy.KEEP, req)
    }

    fun reminder(ctx: Context, text: String, minutes: Int) {
        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(minutes.toLong(), TimeUnit.MINUTES)
            .setInputData(workDataOf("text" to text))
            .build()
        WorkManager.getInstance(ctx).enqueue(req)
    }
}

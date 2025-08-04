package com.example.teamnovapersonalprojectprojectingkotlin.wroker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.domain.model.sync.SyncCoordinator
import java.util.concurrent.TimeUnit

class SyncWorker(
    ctx: Context,
    params: WorkerParameters,
    private val coordinator: SyncCoordinator
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = try {
        coordinator.syncAll()
        Result.success()
    } catch (t: Throwable) {
        Log.e("SyncWorker", t.stackTrace.toString())
        Result.retry()
    }

}

fun enqueuePeriodicSync(context: Context) {
    val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "periodic-sync", ExistingPeriodicWorkPolicy.KEEP, req
    )
}
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

/**
 * 15분 주기 동기화 기능 - 비활성화됨
 *
 * 페이지별 1분 쿨타임 동기화로 대체되었습니다.
 * 사용자 액션 기반 동기화만으로 충분하므로 배터리 절약을 위해 주기적 동기화는 비활성화합니다.
 */
@Deprecated("Replaced by page-specific sync with 1-minute cooldown. No longer needed for battery optimization.")
fun enqueuePeriodicSync(context: Context) {
    // 15분 주기 동기화 비활성화 - 페이지별 동기화로 대체됨
    Log.d("SyncWorker", "⚠️ Periodic sync is disabled - using page-specific sync instead")

    /* 기존 15분 주기 동기화 코드 (비활성화됨)
    val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "periodic-sync", ExistingPeriodicWorkPolicy.KEEP, req
    )
    */
}
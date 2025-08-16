package com.example.domain_usecase.usecase.sync

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.AggregateRoot
import com.example.domain.model.sync.SyncCursorStore
import com.example.domain.model.sync.SyncPort
import com.example.domain.model.sync.SyncScope
import com.example.orchestrator.DefaultSyncManager
import com.example.orchestrator.MessageSyncPortFactory
import javax.inject.Inject

/**
 * 얇은 동기화 UseCase: 스트림명을 받아 SyncManagerFactory로 위임하여 동기화 실행
 * - 서버 → 로컬 증분 동기화만 수행 (커서 기반)
 */
class SyncUseCase @Inject constructor(
    private val cursorStore: SyncCursorStore,
    private val messageSyncPortFactory: MessageSyncPortFactory,
) {
    companion object {
        private const val TAG = "SyncUseCase"
    }

    suspend operator fun invoke(stream: String): CustomResult<Unit, Exception> {
        return try {
            val ports: List<SyncPort<AggregateRoot>> = when {
                stream.startsWith("messages-") -> {
                    val channelId = stream.removePrefix("messages-")
                    @Suppress("UNCHECKED_CAST")
                    listOf(messageSyncPortFactory.create(channelId)) as List<SyncPort<AggregateRoot>>
                }

                else -> emptyList()
            }
            val manager = DefaultSyncManager(
                ports = ports,
                cursorStore = cursorStore,
                pageSize = 100,
            )
            manager.sync(SyncScope.Stream(stream))
            Log.d(TAG, "sync ok stream=$stream")
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "sync failed stream=$stream: ${e.message}")
            CustomResult.Failure(e)
        }
    }

    suspend fun syncChannel(channelId: String): CustomResult<Unit, Exception> =
        invoke("messages-$channelId")
}

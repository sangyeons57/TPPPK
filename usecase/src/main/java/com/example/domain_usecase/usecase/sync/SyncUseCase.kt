package com.example.domain_usecase.usecase.sync

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.AggregateRoot
import com.example.domain.model.sync.SyncCursorStore
import com.example.domain.model.sync.SyncPort
import com.example.domain.model.sync.SyncScope
import com.example.orchestrator.DefaultSyncManager
import com.example.orchestrator.MessageSyncPortFactory
import javax.inject.Inject

/**
 * 특정 채널의 메시지 증분 동기화를 실행하는 UseCase
 *
 * 채널별로 동적으로 MessageSyncPort를 생성하여 DefaultSyncManager로 동기화를 수행합니다.
 * 네트워크 부하를 최소화하면서 변경된 데이터만 동기화합니다.
 */
class SyncUseCase @Inject constructor(
    private val messageSyncPortFactory: MessageSyncPortFactory,
    private val cursorStore: SyncCursorStore
) {
    companion object {
        private const val TAG = "SyncUseCase"
    }

    /**
     * 특정 채널의 메시지 증분 동기화 실행
     *
     * @param streamName 동기화할 스트림 이름 (예: "messages-channelId" 또는 "messages")
     * @return 동기화 성공/실패 결과
     */
    suspend operator fun invoke(streamName: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "=".repeat(60))
            Log.d(TAG, "🚀 SYNC PROCESS STARTED")
            Log.d(TAG, "📋 Stream Name: $streamName")
            Log.d(TAG, "⏰ Start Time: ${System.currentTimeMillis()}")
            Log.d(TAG, "=".repeat(60))

            // 스트림명에서 채널 ID 추출 
            val channelId = when {
                streamName.startsWith("messages-") -> {
                    val extractedId = streamName.removePrefix("messages-")
                    Log.d(TAG, "📍 Channel ID extracted from stream: '$extractedId'")
                    extractedId
                }

                streamName == "messages" -> {
                    // 기본 테스트 채널 사용
                    val defaultChannelId = "temp_dm_channel_123"
                    Log.d(
                        TAG,
                        "📍 Using default channel ID for 'messages' stream: '$defaultChannelId'"
                    )
                    defaultChannelId
                }

                else -> {
                    Log.e(TAG, "❌ SYNC FAILED: Unsupported stream format")
                    Log.e(TAG, "   Expected formats:")
                    Log.e(TAG, "   - 'messages-{channelId}' (specific channel)")
                    Log.e(TAG, "   - 'messages' (default test channel)")
                    Log.e(TAG, "   Received: '$streamName'")
                    return CustomResult.Failure(IllegalArgumentException("Unsupported stream format: $streamName"))
                }
            }

            Log.d(TAG, "🔧 Creating MessageSyncPort for channel: '$channelId'")

            // 현재 커서 상태 확인
            val currentCursor = cursorStore.getCursor(streamName)
            Log.d(TAG, "📍 Current cursor for stream '$streamName': $currentCursor")

            // 채널별 MessageSyncPort 생성
            val messageSyncPort = messageSyncPortFactory.create(channelId)
            Log.d(TAG, "✅ MessageSyncPort created successfully")

            // 해당 채널용 DefaultSyncManager 생성
            val channelSyncManager = DefaultSyncManager(
                ports = listOf(messageSyncPort) as List<SyncPort<AggregateRoot>>,
                cursorStore = cursorStore,
                pageSize = 50
            )
            Log.d(TAG, "✅ DefaultSyncManager created with pageSize: 50")

            Log.d(TAG, "🔄 Starting synchronization process...")
            Log.d(TAG, "   - Channel: $channelId")
            Log.d(TAG, "   - Stream: $streamName")
            Log.d(TAG, "   - Sync Scope: ${SyncScope.Stream(streamName)}")

            // 동기화 실행
            val syncStartTime = System.currentTimeMillis()
            channelSyncManager.sync(SyncScope.Stream(streamName))
            val syncDuration = System.currentTimeMillis() - syncStartTime

            // 동기화 후 커서 상태 확인
            val newCursor = cursorStore.getCursor(streamName)
            Log.d(TAG, "📍 New cursor after sync: $newCursor")

            Log.d(TAG, "=".repeat(60))
            Log.d(TAG, "✅ SYNC PROCESS COMPLETED SUCCESSFULLY")
            Log.d(TAG, "⏱️  Duration: ${syncDuration}ms")
            Log.d(TAG, "📋 Stream: $streamName")
            Log.d(TAG, "📍 Channel: $channelId")
            Log.d(TAG, "🔄 Cursor Updated: $currentCursor → $newCursor")
            Log.d(TAG, "=".repeat(60))
            
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "=".repeat(60))
            Log.e(TAG, "💥 SYNC PROCESS FAILED")
            Log.e(TAG, "📋 Stream: $streamName")
            Log.e(TAG, "❌ Error Type: ${e.javaClass.simpleName}")
            Log.e(TAG, "💬 Error Message: ${e.message}")
            Log.e(TAG, "📚 Stack Trace:", e)
            Log.e(TAG, "=".repeat(60))
            CustomResult.Failure(e)
        }
    }

    /**
     * 편의 메서드: 채널 ID로 직접 동기화
     */
    suspend fun syncChannel(channelId: String): CustomResult<Unit, Exception> {
        return invoke("messages-$channelId")
    }
}
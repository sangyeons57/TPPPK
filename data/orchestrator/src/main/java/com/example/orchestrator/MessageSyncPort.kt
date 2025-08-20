package com.example.orchestrator

import android.util.Log
import com.example.data_datasource.remote.MessageRemoteDataSource
import com.example.core_common.constants.ChannelConstants
import com.example.data_model.local.MessageDao
import com.example.data_model.local.OutboxDao
import com.example.data_model.local.toModel
import com.example.domain.model.base.Message
import com.example.domain.model.sync.ApplyOutcome
import com.example.domain.model.sync.ConflictResolver
import com.example.domain.model.sync.FailedEvent
import com.example.domain.model.sync.OutBoxRecord
import com.example.domain.model.sync.PushResult
import com.example.domain.model.sync.RemoteBatch
import com.example.domain.model.sync.SyncPort
import com.example.domain.vo.CollectionPath
import com.example.mapper.message.MessageMapper
import javax.inject.Inject

/**
 * 메시지 증분 동기화를 위한 SyncPort 구현체
 *
 * Firestore ↔ Room DB 간의 양방향 증분 동기화를 담당합니다.
 * - pullSince: Firestore에서 커서 기반으로 변경된 메시지들을 가져옴 (MessageRemoteDataSource 직접 사용)
 * - applyRemote: Room DB에 원격 데이터를 적용하여 Paging3 자동 업데이트 트리거 (MessageRepository 사용)
 * - pushToRemote: 로컬 변경사항을 Firestore로 전송 (OutBox 패턴)
 */
class MessageSyncPort @Inject constructor(
    private val messageRemoteDataSource: MessageRemoteDataSource, // Firestore 직접 접근
    private val messageDao: MessageDao, // Room DB 직접 접근
    private val messageMapper: MessageMapper, // Message <-> MessageEntity 변환
    private val outboxDao: OutboxDao, // OutBox 접근
    private val channelId: String
) : SyncPort<Message> {

    companion object {
        private const val TAG = "MessageSyncPort"
    }

    override val name = "${ChannelConstants.STREAM_MESSAGES}-$channelId"

    // ================================
    // 서버 → 로컬 (Pull) 동기화
    // ================================

    /**
     * Firestore에서 lastSyncTime 이후의 변경된 메시지들을 가져옵니다.
     *
     * SyncMetadata의 cursor 필드에는 실제로 lastSyncTime(epochMilli)이 저장됩니다.
     *
     * @param cursor 마지막 동기화 시간 (epochMilli string, null이면 처음부터)
     * @param limit 한 번에 가져올 메시지 수
     * @return 원격 배치 데이터
     */
    override suspend fun pullSince(cursor: String?, limit: Int): RemoteBatch<Message> {
        return try {
            val path = CollectionPath.dmChannelMessages(channelId)
            messageRemoteDataSource.setCollection(path)

            val dtoBatch = messageRemoteDataSource.pullSince(cursor, limit)
            val items = dtoBatch.items.map { dto ->
                val fixed = if (dto.channelId.isBlank()) dto.copy(channelId = channelId) else dto
                messageMapper.dtoToDomain(fixed)
            }

            RemoteBatch(
                items = items,
                tombstones = emptyList(),
                nextCursor = dtoBatch.nextCursor,
                hasMore = dtoBatch.hasMore,
                watermark = dtoBatch.watermark
            )
        } catch (e: Exception) {
            Log.e(TAG, "pullSince failed (cursor=$cursor, limit=$limit)", e)
            // Exception을 다시 던져서 상위 계층에서 처리하도록 함
            throw e
        }
    }

    /**
     * 원격에서 가져온 메시지들을 Room DB에 적용합니다.
     *
     * @param batch 원격 배치 데이터
     * @param resolver 충돌 해결자 (로컬/원격 데이터 충돌 시 사용)
     * @return 적용 결과 (성공/실패)
     */
    override suspend fun applyRemote(
        batch: RemoteBatch<Message>,
        resolver: ConflictResolver<Message>
    ): ApplyOutcome {
        Log.d(TAG, "📥 Applying ${batch.items.size} remote messages to Room DB")

        return try {
            var successCount = 0
            var failCount = 0

            // 각 메시지를 Room DB에 직접 저장
            batch.items.forEach { message ->
                try {
                    // Message를 MessageEntity로 변환하여 Room DB에 저장
                    val messageEntity = messageMapper.domainToEntity(message)
                    messageDao.upsert(messageEntity)
                    successCount++
                    Log.d(TAG, "✅ Applied message to Room DB: id=${message.id.value}")
                } catch (e: Exception) {
                    failCount++
                    Log.e(TAG, "❌ Failed to apply message to Room DB: id=${message.id.value}", e)
                }
            }

            Log.d(TAG, "📊 Apply remote complete: success=$successCount, failed=$failCount")

            // 성공한 메시지가 하나라도 있으면 성공으로 처리
            // Room DB 변경 → Paging3 자동 UI 업데이트 트리거됨
            ApplyOutcome(success = successCount > 0)

        } catch (e: Exception) {
            Log.e(TAG, "💥 Exception during applyRemote", e)
            ApplyOutcome(success = false, error = e)
        }
    }

    // ================================
    // 로컬 → 서버 (Push) 동기화
    // ================================

    /**
     * OutBox에서 전송 대기 중인 메시지들을 가져옵니다.
     *
     * @param limit 한 번에 처리할 레코드 수
     * @return 전송할 OutBox 레코드들
     */
    override suspend fun readOutboxBatch(limit: Int): List<OutBoxRecord> {
        Log.d(TAG, "📤 Reading outbox batch (generic), limit: $limit")
        return try {
            val pending = outboxDao.peek(ChannelConstants.STREAM_MESSAGES, limit)
            pending.mapNotNull { e ->
                try {
                    val json = org.json.JSONObject(e.payload)
                    if (json.optString(ChannelConstants.KEY_CHANNEL_ID) == channelId) e.toModel() else null
                } catch (_: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to read outbox batch", e)
            emptyList()
        }
    }

    /**
     * OutBox 레코드들을 Firestore로 전송합니다.
     *
     * @param events 전송할 이벤트들
     * @return 전송 결과 (성공/실패 ID 리스트)
     */
    override suspend fun pushToRemote(events: List<OutBoxRecord>): PushResult {
        Log.d(TAG, "🚀 Pushing ${events.size} events to remote")
        return try {
            messageRemoteDataSource.push(events)
        } catch (e: Exception) {
            Log.e(TAG, "💥 Exception during pushToRemote", e)
            PushResult(
                successIds = emptyList(),
                failIds = events.map { FailedEvent(it.id, e.message ?: "Push failed", 5000L) }
            )
        }
    }

    /**
     * 성공적으로 전송된 OutBox 레코드들을 확인 처리합니다.
     */
    override suspend fun ackOutbox(successIds: List<String>) {
        Log.d(TAG, "✅ Acknowledging ${successIds.size} successful outbox records")
        try {
            if (successIds.isNotEmpty()) {
                outboxDao.markDispatched(successIds)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to ack outbox records", e)
        }
    }

    /**
     * 실패한 OutBox 레코드들을 재시도 대기 상태로 변경합니다.
     */
    override suspend fun retryOutBox(failed: List<FailedEvent>) {
        Log.d(TAG, "🔄 Marking ${failed.size} events for retry")
        try {
            val ids = failed.map { it.id }
            if (ids.isNotEmpty()) {
                outboxDao.markFailed(ids)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to mark events for retry", e)
        }
    }

}

/**
 * 채널별 MessageSyncPort 팩토리
 */
class MessageSyncPortFactory @Inject constructor(
    private val messageRemoteDataSource: MessageRemoteDataSource,
    private val messageDao: MessageDao,
    private val messageMapper: MessageMapper,
    private val outboxDao: OutboxDao
) {
    /**
     * 특정 채널용 MessageSyncPort 생성
     */
    fun create(channelId: String): MessageSyncPort {
        return MessageSyncPort(
            messageRemoteDataSource,
            messageDao,
            messageMapper,
            outboxDao,
            channelId
        )
    }
}

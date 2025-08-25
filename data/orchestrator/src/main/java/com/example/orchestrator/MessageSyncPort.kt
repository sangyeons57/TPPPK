package com.example.orchestrator

import com.example.core_common.constants.ChannelConstants
import com.example.data_datasource.remote.MessageRemoteDataSource
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
import com.example.domain.vo.ChannelId
import com.example.domain.vo.CollectionPath
import com.example.mapper.message.MessageMapper
import com.example.orchestrator.util.SyncLogger
import javax.inject.Inject

/**
 * 메시지 증분 동기화를 위한 SyncPort 구현체
 *
 * Firestore ↔ Room DB 간의 양방향 증분 동기화를 담당합니다.
 * - pullSince: Firestore에서 커서 기반으로 변경된 메시지들을 가져옴 (MessageRemoteDataSource 직접 사용)
 * - applyRemote: Room DB에 원격 데이터를 적용하여 Paging3 자동 업데이트 트리거 (MessageRepository 사용)
 * - pushToRemote: 로컬 변경사항을 Firestore로 전송 (OutBox 패턴)
 */
class MessageSyncPort(
    private val messageRemoteDataSource: MessageRemoteDataSource, // Firestore 직접 접근
    private val messageDao: MessageDao, // Room DB 직접 접근
    private val messageMapper: MessageMapper, // Message <-> MessageEntity 변환
    private val outboxDao: OutboxDao, // OutBox 접근
    private val channelId: ChannelId
) : SyncPort<Message> {

    private val logger = SyncLogger("MessageSyncPort")

    override val name = "${ChannelConstants.STREAM_MESSAGES}-${channelId.value}"

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
        logger.logPullStart(name, cursor)
        return try {
            val path = CollectionPath.messages(channelId)
            messageRemoteDataSource.setCollection(path)

            val dtoBatch = messageRemoteDataSource.pullSince(cursor, limit)
            val items = dtoBatch.items.map { dto ->
                val fixed =
                    if (dto.channelId.isBlank()) dto.copy(channelId = channelId.value) else dto
                messageMapper.dtoToDomain(fixed)
            }

            val batch = RemoteBatch(
                items = items,
                tombstones = emptyList(),
                nextCursor = dtoBatch.nextCursor,
                hasMore = dtoBatch.hasMore,
                watermark = dtoBatch.watermark
            )

            logger.logPullSuccess(name, batch)
            batch
        } catch (e: Exception) {
            logger.logPullFailure(name, cursor, e)
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
        logger.logApplyStart(name, batch.items.size)

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
                } catch (e: Exception) {
                    failCount++
                    logger.error("Failed to apply message to Room DB: id=${message.id.value}", e)
                }
            }

            logger.logApplyResult(name, successCount, failCount)

            // 성공한 메시지가 하나라도 있으면 성공으로 처리
            // Room DB 변경 → Paging3 자동 UI 업데이트 트리거됨
            ApplyOutcome(success = successCount > 0)

        } catch (e: Exception) {
            logger.logApplyFailure(name, e)
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
        return try {
            val pending = outboxDao.peek(ChannelConstants.STREAM_MESSAGES, limit)
            val filtered = pending.mapNotNull { e ->
                try {
                    val json = org.json.JSONObject(e.payload)
                    if (json.optString(ChannelConstants.KEY_CHANNEL_ID) == channelId.value) e.toModel() else null
                } catch (_: Exception) {
                    null
                }
            }
            logger.logOutboxOperation("Read", name, filtered.size)
            filtered
        } catch (e: Exception) {
            logger.error("Failed to read outbox batch", e)
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
        return try {
            val result = messageRemoteDataSource.push(events)
            logger.logPushResult(name, result.successIds.size, result.failIds.size)
            result
        } catch (e: Exception) {
            logger.logException("pushToRemote", name, e)
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
        try {
            if (successIds.isNotEmpty()) {
                outboxDao.markDispatched(successIds)
                logger.logOutboxOperation("Ack", name, successIds.size)
            }
        } catch (e: Exception) {
            logger.error("Failed to ack outbox records", e)
        }
    }

    /**
     * 실패한 OutBox 레코드들을 재시도 대기 상태로 변경합니다.
     */
    override suspend fun retryOutBox(failed: List<FailedEvent>) {
        try {
            val ids = failed.map { it.id }
            if (ids.isNotEmpty()) {
                outboxDao.markFailed(ids)
                logger.logOutboxOperation("Retry", name, ids.size)
            }
        } catch (e: Exception) {
            logger.error("Failed to mark events for retry", e)
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
    fun create(channelId: ChannelId): MessageSyncPort {
        return MessageSyncPort(
            messageRemoteDataSource,
            messageDao,
            messageMapper,
            outboxDao,
            channelId
        )
    }
}

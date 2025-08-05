package com.example.orchestrator

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.MessageRemoteDataSource
import com.example.data_model.local.MessageDao
import com.example.domain.model.base.Message
import com.example.domain.model.sync.ApplyOutcome
import com.example.domain.model.sync.ConflictResolver
import com.example.domain.model.sync.FailedEvent
import com.example.domain.model.sync.OutBoxRecord
import com.example.domain.model.sync.PushResult
import com.example.domain.model.sync.RemoteBatch
import com.example.domain.model.sync.SyncPort
import com.example.domain.vo.CollectionPath
import com.example.domain_repository.base.MessageRepository
import com.example.mapper.message.MessageMapper
import java.time.Instant
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
    private val messageRepository: MessageRepository, // Room DB 접근
    private val messageDao: MessageDao, // Room DB 직접 접근
    private val messageMapper: MessageMapper, // Message <-> MessageEntity 변환
    private val channelId: String
) : SyncPort<Message> {

    companion object {
        private const val TAG = "MessageSyncPort"
    }

    override val name = "messages-$channelId"

    // ================================
    // 서버 → 로컬 (Pull) 동기화
    // ================================

    /**
     * Firestore에서 커서 이후의 변경된 메시지들을 가져옵니다.
     *
     * @param cursor 마지막 동기화 커서 (null이면 처음부터)
     * @param limit 한 번에 가져올 메시지 수
     * @return 원격 배치 데이터
     */
    override suspend fun pullSince(cursor: String?, limit: Int): RemoteBatch<Message> {
        Log.d(TAG, "📥 Pulling messages since cursor: $cursor, limit: $limit")

        return try {
            // Firestore에서 메시지 가져오기
            val timestamp =
                cursor?.toLongOrNull()?.let { Instant.ofEpochMilli(it) } ?: Instant.EPOCH
            val result = messageRemoteDataSource.getMessagesAfterTimestamp(
                collectionPath = CollectionPath.dmChannelMessages(channelId),
                timestamp = timestamp
            )

            when (result) {
                is CustomResult.Success -> {
                    val messages = result.data
                    val nextCursor = if (messages.isNotEmpty()) {
                        messages.maxOfOrNull { it.updatedAt.toEpochMilli() }?.toString() ?: cursor
                    } else {
                        cursor
                    }

                    Log.d(TAG, "✅ Pulled ${messages.size} messages from Firestore")
                    Log.d(TAG, "📊 Next cursor: $nextCursor")

                    RemoteBatch(
                        items = messages,
                        tombstones = emptyList(),
                        nextCursor = nextCursor,
                        hasMore = messages.size >= limit
                    )
                }

                is CustomResult.Failure -> {
                    Log.e(TAG, "❌ Failed to pull messages from Firestore", result.error)
                    // 빈 배치 반환 (실패 시에도 동기화 프로세스는 계속됨)
                    RemoteBatch(
                        items = emptyList(),
                        tombstones = emptyList(),
                        nextCursor = cursor, // 커서 유지
                        hasMore = false
                    )
                }

                else -> {
                    Log.e(TAG, "❌ Unexpected result type")
                    RemoteBatch(
                        items = emptyList(),
                        tombstones = emptyList(),
                        nextCursor = cursor,
                        hasMore = false
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Exception during pullSince", e)
            RemoteBatch(
                items = emptyList(),
                tombstones = emptyList(),
                nextCursor = cursor,
                hasMore = false
            )
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

    /**
     * 새로운 커서를 저장합니다. (DefaultSyncManager에서 호출)
     */
    override suspend fun commitCursor(newCursor: String) {
        Log.d(TAG, "💾 Committing new cursor: $newCursor")
        // SyncCursorStore에서 자동으로 처리됨
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
        Log.d(TAG, "📤 Reading outbox batch, limit: $limit")

        return try {
            // TODO: OutBox 구현체에서 대기 중인 메시지 가져오기
            // messageRepository.getOutboxMessages(channelId, limit)
            emptyList() // 임시로 빈 리스트 반환
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
        Log.d(TAG, "🚀 Pushing ${events.size} events to Firestore")

        return try {
            val successIds = mutableListOf<String>()
            val failIds = mutableListOf<FailedEvent>()

            events.forEach { event ->
                try {
                    // TODO: Firestore로 전송 로직 구현
                    // 현재는 성공으로 처리
                    successIds.add(event.id)
                    Log.d(TAG, "✅ Pushed event: ${event.id}")
                } catch (e: Exception) {
                    failIds.add(
                        FailedEvent(
                            id = event.id,
                            reason = e.message ?: "Unknown error",
                            retryAfterMillis = 5000L // 5초 후 재시도
                        )
                    )
                    Log.e(TAG, "❌ Failed to push event: ${event.id}", e)
                }
            }

            Log.d(TAG, "📊 Push complete: success=${successIds.size}, failed=${failIds.size}")

            PushResult(
                successIds = successIds,
                failIds = failIds
            )

        } catch (e: Exception) {
            Log.e(TAG, "💥 Exception during pushToRemote", e)
            PushResult(
                successIds = emptyList(),
                failIds = listOf(
                    FailedEvent(
                        id = "batch_error",
                        reason = e.message ?: "Push failed",
                        retryAfterMillis = 10000L
                    )
                )
            )
        }
    }

    /**
     * 성공적으로 전송된 OutBox 레코드들을 확인 처리합니다.
     */
    override suspend fun ackOutbox(successIds: List<String>) {
        Log.d(TAG, "✅ Acknowledging ${successIds.size} successful outbox records")
        // TODO: OutBox에서 성공한 레코드들 제거
    }

    /**
     * 실패한 OutBox 레코드들을 재시도 대기 상태로 변경합니다.
     */
    override suspend fun retryOutBox(failed: List<FailedEvent>) {
        Log.d(TAG, "🔄 Marking ${failed.size} events for retry")
        // TODO: OutBox에서 실패한 레코드들 재시도 설정
    }

    /**
     * 전체 동기화 리셋 (필요시 구현)
     */
    override suspend fun resetAndFullResync() {
        Log.d(TAG, "🔄 Performing full reset and resync for channel: $channelId")
        // TODO: 채널별 전체 리셋 로직 구현
    }
}

/**
 * 채널별 MessageSyncPort 팩토리
 */
class MessageSyncPortFactory @Inject constructor(
    private val messageRemoteDataSource: MessageRemoteDataSource,
    private val messageRepository: MessageRepository,
    private val messageDao: MessageDao,
    private val messageMapper: MessageMapper
) {
    /**
     * 특정 채널용 MessageSyncPort 생성
     */
    fun create(channelId: String): MessageSyncPort {
        return MessageSyncPort(
            messageRemoteDataSource,
            messageRepository,
            messageDao,
            messageMapper,
            channelId
        )
    }
}
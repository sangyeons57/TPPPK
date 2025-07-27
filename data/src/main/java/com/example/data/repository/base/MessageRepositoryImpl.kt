package com.example.data.repository.base

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data.datasource.local.LocalMessagesDataSource
import com.example.data.datasource.remote.MessageRemoteDataSource
import com.example.data.mapper.MessagesMapper
import com.example.data.model.remote.MessageDTO
import com.example.data.model.remote.toDto
import com.example.domain.model.base.Message
import com.example.domain.repository.factory.context.MessageRepositoryFactoryContext
import com.example.domain.repository.base.MessageRepository
import com.example.domain.repository.base.SyncResult
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

/**
 * Remote Message Repository Implementation (Sync-Only)
 * 클라이언트 주도 동기화 전용 구현체
 *
 * 🔒 제약사항:
 * - Room 직접 접근 금지 (LocalDataSource를 통한 저장만 허용)
 * - Flow/LiveData 반환 금지
 * - 직접적인 UI 데이터 제공 금지
 *
 * ✅ 역할:
 * - Firestore에서 증분 데이터 fetch
 * - Outbox 데이터를 Firestore에 push
 * - 동기화 충돌 해결
 * - SyncMetadata 관리
 */
class MessageRepositoryImpl @Inject constructor(
    private val messageRemoteDataSource: MessageRemoteDataSource,
    private val localMessagesDataSource: LocalMessagesDataSource,
    override val factoryContext: MessageRepositoryFactoryContext
) : MessageRepository {

    companion object {
        private const val TAG = "MessageRepositoryImpl"
        private const val SYNC_BATCH_SIZE = 100
    }

    override suspend fun syncFromServer(
        lastSyncCursor: Long?,
        channelId: String?
    ): CustomResult<SyncResult<Message>, Exception> {
        return try {
            Log.d(TAG, "syncFromServer: cursor=$lastSyncCursor, channelId=$channelId")

            // 1. Firestore에서 증분 데이터 가져오기
            val query = buildIncrementalQuery(lastSyncCursor, channelId)
            val querySnapshot = query.get().await()

            // 2. DTO를 Domain 모델로 변환
            val messages = querySnapshot.documents.mapNotNull { document ->
                try {
                    val dto = document.toObject(MessageDTO::class.java)?.copy(id = document.id)
                    dto?.let { MessagesMapper.toDomain(MessagesMapper.toEntity(it)) }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to convert document ${document.id}", e)
                    null
                }
            }

            // 3. LocalDataSource에 저장 (Room DB)
            if (messages.isNotEmpty()) {
                localMessagesDataSource.saveMessages(messages)
                Log.d(TAG, "Saved ${messages.size} messages to local DB")
            }

            // 4. 동기화 커서 업데이트
            val newCursor = messages.maxOfOrNull { it.updatedAt.toEpochMilli() }
            if (newCursor != null) {
                localMessagesDataSource.updateSyncCursor(
                    cursor = newCursor,
                    timestamp = System.currentTimeMillis(),
                    channelId = channelId
                )
            }

            // 5. 결과 반환
            val hasMore = querySnapshot.size() >= SYNC_BATCH_SIZE
            CustomResult.Success(
                SyncResult(
                    data = messages,
                    nextCursor = newCursor ?: lastSyncCursor,
                    hasMore = hasMore
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "syncFromServer failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun syncToServer(channelId: String?): CustomResult<Int, Exception> {
        return try {
            Log.d(TAG, "syncToServer: channelId=$channelId")

            // 1. LocalDataSource에서 Outbox 작업 가져오기
            val outboxOperations = localMessagesDataSource.getPendingOutboxOperations()
            val filteredOperations = if (channelId != null) {
                outboxOperations.filter { operation ->
                    // 해당 채널의 메시지인지 확인
                    val message = localMessagesDataSource.getMessageById(operation.messageId)
                    message?.channelId == channelId
                }
            } else {
                outboxOperations
            }

            var processedCount = 0

            // 2. 각 Outbox 작업 처리
            for (operation in filteredOperations) {
                try {
                    when (operation.operation) {
                        "CREATE" -> {
                            val message =
                                localMessagesDataSource.getMessageById(operation.messageId)
                            if (message != null) {
                                val result = messageRemoteDataSource.create(message.toDto())
                                if (result is CustomResult.Success) {
                                    localMessagesDataSource.markOutboxOperationComplete(operation.id)
                                    processedCount++
                                }
                            }
                        }

                        "UPDATE" -> {
                            val message =
                                localMessagesDataSource.getMessageById(operation.messageId)
                            if (message != null) {
                                val result = messageRemoteDataSource.update(
                                    message.id,
                                    message.getChangedFields()
                                )
                                if (result is CustomResult.Success) {
                                    localMessagesDataSource.markOutboxOperationComplete(operation.id)
                                    processedCount++
                                }
                            }
                        }

                        "DELETE" -> {
                            val result = messageRemoteDataSource.delete(operation.messageId)
                            if (result is CustomResult.Success) {
                                localMessagesDataSource.markOutboxOperationComplete(operation.id)
                                processedCount++
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to process outbox operation ${operation.id}", e)
                    // 재시도 횟수 증가
                    localMessagesDataSource.incrementOutboxRetries(operation.id)
                }
            }

            Log.d(TAG, "Processed $processedCount outbox operations")
            CustomResult.Success(processedCount)

        } catch (e: Exception) {
            Log.e(TAG, "syncToServer failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun forceSyncAll(channelId: String?): CustomResult<Int, Exception> {
        return try {
            Log.d(TAG, "forceSyncAll: channelId=$channelId")

            var totalSynced = 0
            var lastCursor: Long? = null

            // 기존 동기화 커서 무시하고 전체 동기화
            do {
                val result = syncFromServer(lastCursor, channelId)
                when (result) {
                    is CustomResult.Success -> {
                        totalSynced += result.data.data.size
                        lastCursor = result.data.nextCursor

                        // 더 이상 데이터가 없으면 종료
                        if (!result.data.hasMore || result.data.data.isEmpty()) {
                            break
                        }
                    }

                    is CustomResult.Failure -> {
                        return result
                    }

                    else -> break
                }
            } while (true)

            Log.d(TAG, "Force sync completed: $totalSynced messages")
            CustomResult.Success(totalSynced)

        } catch (e: Exception) {
            Log.e(TAG, "forceSyncAll failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun resolveConflicts(
        conflictedMessageIds: List<String>
    ): CustomResult<Int, Exception> {
        return try {
            Log.d(TAG, "resolveConflicts: ${conflictedMessageIds.size} conflicts")

            var resolvedCount = 0

            for (messageId in conflictedMessageIds) {
                try {
                    // 서버 우선 정책: 서버 데이터로 로컬 덮어쓰기
                    val serverResult = messageRemoteDataSource.findById(messageId)
                    if (serverResult is CustomResult.Success) {
                        val serverDto = serverResult.data
                        val serverEntity = MessagesMapper.toEntity(serverDto)
                        val serverMessage = MessagesMapper.toDomain(serverEntity)

                        // 로컬에 서버 데이터 저장 (충돌 해결)
                        localMessagesDataSource.saveMessage(serverMessage)
                        resolvedCount++
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to resolve conflict for message $messageId", e)
                }
            }

            Log.d(TAG, "Resolved $resolvedCount conflicts")
            CustomResult.Success(resolvedCount)

        } catch (e: Exception) {
            Log.e(TAG, "resolveConflicts failed", e)
            CustomResult.Failure(e)
        }
    }

    private fun buildIncrementalQuery(
        lastSyncCursor: Long?,
        channelId: String?
    ): Query {
        var query = messageRemoteDataSource.getCollectionReference()
            .orderBy("updatedAt", Query.Direction.ASCENDING)
            .limit(SYNC_BATCH_SIZE.toLong())

        // 증분 동기화: 마지막 커서 이후 데이터만
        if (lastSyncCursor != null) {
            val cursorTimestamp = Instant.ofEpochMilli(lastSyncCursor)
            query = query.whereGreaterThan("updatedAt", cursorTimestamp)
        }

        // 특정 채널만 동기화
        if (channelId != null) {
            query = query.whereEqualTo("channelId", channelId)
        }

        return query
    }
}

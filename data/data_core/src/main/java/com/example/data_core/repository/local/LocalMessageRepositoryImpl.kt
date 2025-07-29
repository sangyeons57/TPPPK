package com.example.data_core.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.local.LocalMessagesDataSource
import com.example.domain.model.base.Message
import com.example.domain.repository.local.LocalMessageRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Message Repository Implementation (SSOT)
 * Room Database 전용 구현체 - UI에 직접 데이터 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지
 *
 * ✅ 역할:
 * - LocalDataSource를 통한 Room DB 접근
 * - Flow로 UI에 실시간 데이터 제공
 * - 로컬 CRUD 작업 처리
 * - Outbox 관리 (동기화 대상 저장)
 */
@Singleton
class LocalMessageRepositoryImpl @Inject constructor(
    private val localMessagesDataSource: LocalMessagesDataSource
) : LocalMessageRepository {

    companion object {
        private const val TAG = "LocalMessageRepository"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeMessagesByChannel(channelId: String): Flow<List<Message>> {
        Log.d(TAG, "observeMessagesByChannel: $channelId")
        return localMessagesDataSource.observeMessagesByChannel(channelId)
    }

    override fun observeMessageById(messageId: String): Flow<Message?> {
        Log.d(TAG, "observeMessageById: $messageId")
        return localMessagesDataSource.observeMessageById(messageId)
    }

    override fun observeUnreadMessageCount(
        channelId: String,
        userId: String,
        lastReadTimestamp: Instant
    ): Flow<Int> {
        Log.d(TAG, "observeUnreadMessageCount: channel=$channelId, user=$userId")
        return localMessagesDataSource.observeUnreadMessageCount(channelId, userId, lastReadTimestamp)
    }

    // === 단순 읽기 작업 ===

    override suspend fun getMessageById(messageId: String): Message? {
        Log.d(TAG, "getMessageById: $messageId")
        return try {
            localMessagesDataSource.getMessageById(messageId)
        } catch (e: Exception) {
            Log.e(TAG, "getMessageById failed", e)
            null
        }
    }

    override suspend fun getMessagesByChannel(channelId: String, limit: Int?): List<Message> {
        Log.d(TAG, "getMessagesByChannel: channel=$channelId, limit=$limit")
        return try {
            localMessagesDataSource.getMessagesByChannel(channelId, limit)
        } catch (e: Exception) {
            Log.e(TAG, "getMessagesByChannel failed", e)
            emptyList()
        }
    }

    override suspend fun getMessagesByType(messageType: String): List<Message> {
        Log.d(TAG, "getMessagesByType: $messageType")
        return try {
            localMessagesDataSource.getMessagesByType(messageType)
        } catch (e: Exception) {
            Log.e(TAG, "getMessagesByType failed", e)
            emptyList()
        }
    }

    override suspend fun getMessagesByUser(userId: String): List<Message> {
        Log.d(TAG, "getMessagesByUser: $userId")
        return try {
            localMessagesDataSource.getMessagesByUser(userId)
        } catch (e: Exception) {
            Log.e(TAG, "getMessagesByUser failed", e)
            emptyList()
        }
    }

    override suspend fun searchMessages(query: String, channelId: String?): List<Message> {
        Log.d(TAG, "searchMessages: query='$query', channelId=$channelId")
        return try {
            localMessagesDataSource.searchMessages(query, channelId)
        } catch (e: Exception) {
            Log.e(TAG, "searchMessages failed", e)
            emptyList()
        }
    }

    override suspend fun getMessagesByTimeRange(
        channelId: String,
        startTime: Instant,
        endTime: Instant
    ): List<Message> {
        Log.d(TAG, "getMessagesByTimeRange: channel=$channelId, $startTime to $endTime")
        return try {
            localMessagesDataSource.getMessagesByTimeRange(channelId, startTime, endTime)
        } catch (e: Exception) {
            Log.e(TAG, "getMessagesByTimeRange failed", e)
            emptyList()
        }
    }

    override suspend fun getLastMessage(channelId: String): Message? {
        Log.d(TAG, "getLastMessage: $channelId")
        return try {
            localMessagesDataSource.getLastMessage(channelId)
        } catch (e: Exception) {
            Log.e(TAG, "getLastMessage failed", e)
            null
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveMessage(message: Message): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveMessage: ${message.id}")

            // 1. Room DB에 저장
            localMessagesDataSource.saveMessage(message)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (message.isNew) "CREATE" else "UPDATE"
            localMessagesDataSource.addToOutbox(
                messageId = message.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            Log.d(TAG, "Message saved and added to outbox: ${message.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveMessage failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveMessages(messages: List<Message>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveMessages: ${messages.size} messages")

            if (messages.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localMessagesDataSource.saveMessages(messages)

            Log.d(TAG, "Bulk messages saved: ${messages.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveMessages failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteMessage(messageId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteMessage: $messageId")

            // 1. Room DB에서 삭제 (실제로는 soft delete)
            localMessagesDataSource.deleteMessage(messageId)

            // 2. Outbox에 삭제 작업 추가
            localMessagesDataSource.addToOutbox(
                messageId = messageId,
                operation = "DELETE",
                payload = null
            )

            Log.d(TAG, "Message deleted and added to outbox: $messageId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteMessage failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteMessagesByChannel(channelId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteMessagesByChannel: $channelId")

            // 해당 채널의 모든 메시지 삭제
            localMessagesDataSource.deleteMessagesByChannel(channelId)

            Log.d(TAG, "All messages deleted for channel: $channelId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteMessagesByChannel failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 유틸리티 ===

    override suspend fun messageExists(messageId: String): Boolean {
        return try {
            localMessagesDataSource.messageExists(messageId)
        } catch (e: Exception) {
            Log.e(TAG, "messageExists failed", e)
            false
        }
    }

    override suspend fun getMessageCount(channelId: String): Int {
        return try {
            localMessagesDataSource.getMessageCount(channelId)
        } catch (e: Exception) {
            Log.e(TAG, "getMessageCount failed", e)
            0
        }
    }

    override suspend fun getTotalMessageCount(): Int {
        return try {
            localMessagesDataSource.getTotalMessageCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalMessageCount failed", e)
            0
        }
    }

    override suspend fun getUnreadMessageCount(
        channelId: String,
        userId: String,
        lastReadTimestamp: Instant
    ): Int {
        return try {
            localMessagesDataSource.getUnreadMessageCount(channelId, userId, lastReadTimestamp)
        } catch (e: Exception) {
            Log.e(TAG, "getUnreadMessageCount failed", e)
            0
        }
    }

    override suspend fun clearAllMessages(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllMessages")

            localMessagesDataSource.clearAllMessages()

            Log.d(TAG, "All messages cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "clearAllMessages failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getMessagesUpdatedAfter(
        timestamp: Instant,
        channelId: String?
    ): List<Message> {
        return try {
            localMessagesDataSource.getMessagesUpdatedAfter(timestamp, channelId)
        } catch (e: Exception) {
            Log.e(TAG, "getMessagesUpdatedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun addToOutbox(
        messageId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "addToOutbox: messageId=$messageId, operation=$operation")

            localMessagesDataSource.addToOutbox(messageId, operation, payload)

            Log.d(TAG, "Added to outbox: $messageId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}
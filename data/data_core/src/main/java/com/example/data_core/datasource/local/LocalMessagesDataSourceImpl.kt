package com.example.data_core.datasource.local

import com.example.data_core.dao.MessagesDao
import com.example.data_core.dao.OutboxDao
import com.example.data_core.dao.SyncMetadataDao
import com.example.data_core.model.local.OutboxEntity
import com.example.domain.model.base.Message
import com.example.mapper.MessageEntityMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로컬 메시지 데이터 저장소 구현체
 * Room Database를 사용하여 3-tier 클라이언트 주도 동기화를 제공합니다
 */
@Singleton
class LocalMessagesDataSourceImpl @Inject constructor(
    private val messagesDao: MessagesDao,
    private val outboxDao: OutboxDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val mapper: MessageEntityMapper
) : LocalMessagesDataSource {

    companion object {
        private const val COLLECTION_NAME = "messages"
    }

    // === 기본 CRUD 작업 ===

    override suspend fun getMessageById(messageId: String): Message? {
        val entity = messagesDao.getMessageById(messageId) ?: return null
        return mapper.toDomain(entity)
    }

    override suspend fun getMessagesByChannel(channelId: String, limit: Int?): List<Message> {
        val entities = if (limit != null) {
            messagesDao.getMessagesByChannelWithLimit(channelId, limit)
        } else {
            messagesDao.getMessagesByChannel(channelId)
        }
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getMessagesByType(messageType: String): List<Message> {
        val entities = messagesDao.getMessagesByType(messageType)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getMessagesByUser(userId: String): List<Message> {
        val entities = messagesDao.getMessagesByUser(userId)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun searchMessages(query: String, channelId: String?): List<Message> {
        val entities = if (channelId != null) {
            messagesDao.searchMessagesInChannel(query, channelId)
        } else {
            messagesDao.searchMessages(query)
        }
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getMessagesByTimeRange(
        channelId: String,
        startTime: Instant,
        endTime: Instant
    ): List<Message> {
        val entities = messagesDao.getMessagesByTimeRange(channelId, startTime, endTime)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun saveMessage(message: Message) {
        val entity = mapper.toEntity(message)
        messagesDao.insertMessage(entity)
    }

    override suspend fun saveMessages(messages: List<Message>) {
        if (messages.isEmpty()) return

        val entities = messages.map { mapper.toEntity(it) }
        messagesDao.insertMessages(entities)
    }

    override suspend fun deleteMessage(messageId: String) {
        messagesDao.deleteMessage(messageId)
    }

    override suspend fun deleteMessagesByChannel(channelId: String) {
        messagesDao.deleteMessagesByChannel(channelId)
    }

    // === 3-tier 동기화 지원 ===

    override suspend fun getMessagesUpdatedAfter(
        timestamp: Instant,
        channelId: String?
    ): List<Message> {
        val entities = if (channelId != null) {
            messagesDao.getMessagesUpdatedAfterInChannel(timestamp, channelId)
        } else {
            messagesDao.getMessagesUpdatedAfter(timestamp)
        }
        return entities.map { mapper.toDomain(it) }
    }

    override fun observeMessagesByChannel(channelId: String): Flow<List<Message>> {
        return messagesDao.observeMessagesByChannel(channelId).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeMessageById(messageId: String): Flow<Message?> {
        return messagesDao.observeMessageById(messageId).map { entity ->
            entity?.let { mapper.toDomain(it) }
        }
    }

    // === Outbox 관리 ===

    override suspend fun addToOutbox(messageId: String, operation: String, payload: String?) {
        val outboxEntity = OutboxEntity(
            id = UUID.randomUUID().toString(),
            collectionName = COLLECTION_NAME,
            entityId = messageId,
            operation = operation,
            payload = payload,
            localTimestamp = System.currentTimeMillis(),
            retries = 0
        )
        outboxDao.insertOperation(outboxEntity)
    }

    override suspend fun getPendingOutboxOperations(): List<MessageOutboxOperation> {
        val entities = outboxDao.getPendingOperationsByCollection(COLLECTION_NAME)
        return entities.map { entity ->
            MessageOutboxOperation(
                id = entity.id,
                messageId = entity.entityId,
                operation = entity.operation,
                payload = entity.payload,
                localTimestamp = entity.localTimestamp,
                retries = entity.retries
            )
        }
    }

    override suspend fun markOutboxOperationComplete(operationId: String) {
        outboxDao.deleteOperation(operationId)
    }

    override suspend fun incrementOutboxRetries(operationId: String) {
        outboxDao.incrementRetries(operationId, System.currentTimeMillis())
    }

    // === 동기화 메타데이터 관리 ===

    override suspend fun getLastSyncCursor(channelId: String?): Long? {
        val collectionKey = if (channelId != null) {
            "${COLLECTION_NAME}_$channelId"
        } else {
            COLLECTION_NAME
        }
        return syncMetadataDao.getLastServerCursor(collectionKey)
    }

    override suspend fun updateSyncCursor(cursor: Long, timestamp: Long, channelId: String?) {
        val collectionKey = if (channelId != null) {
            "${COLLECTION_NAME}_$channelId"
        } else {
            COLLECTION_NAME
        }

        // 동기화 메타데이터가 없으면 초기화
        if (!syncMetadataDao.syncMetadataExists(collectionKey)) {
            syncMetadataDao.initializeSyncMetadata(collectionKey)
        }

        syncMetadataDao.updateSyncStatus(collectionKey, cursor, timestamp)
    }

    // === 유틸리티 ===

    override suspend fun messageExists(messageId: String): Boolean {
        return messagesDao.messageExists(messageId)
    }

    override suspend fun getMessageCount(channelId: String): Int {
        return messagesDao.getMessageCountByChannel(channelId)
    }

    override suspend fun getTotalMessageCount(): Int {
        return messagesDao.getTotalMessageCount()
    }

    override suspend fun getLastMessage(channelId: String): Message? {
        val entity = messagesDao.getLastMessageByChannel(channelId) ?: return null
        return mapper.toDomain(entity)
    }

    override suspend fun getUnreadMessageCount(
        channelId: String,
        userId: String,
        lastReadTimestamp: Instant
    ): Int {
        return messagesDao.getUnreadMessageCount(channelId, userId, lastReadTimestamp)
    }

    override suspend fun clearAllMessages() {
        messagesDao.deleteAllMessages()
        outboxDao.deleteOperationsByCollection(COLLECTION_NAME)
        // 채널별 sync metadata도 정리
        val allChannelKeys = syncMetadataDao.getAllSyncMetadata()
            .filter { it.collectionName.startsWith(COLLECTION_NAME) }
        allChannelKeys.forEach { metadata ->
            syncMetadataDao.deleteSyncMetadata(metadata.collectionName)
        }
    }
}
package com.example.data.datasource.local

import com.example.data.dao.MessageAttachmentsDao
import com.example.data.dao.SyncMetadataDao
import com.example.data.dao.OutboxDao
import com.example.data.mapper.MessageAttachmentsMapper
import com.example.data.model.local.OutboxEntity
import com.example.domain.model.base.MessageAttachment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로컬 메시지 첨부파일 데이터 저장소 구현체
 * Room Database를 사용하여 3-tier 클라이언트 주도 동기화를 제공합니다
 */
@Singleton
class LocalMessageAttachmentsDataSourceImpl @Inject constructor(
    private val messageAttachmentsDao: MessageAttachmentsDao,
    private val outboxDao: OutboxDao,
    private val syncMetadataDao: SyncMetadataDao
) : LocalMessageAttachmentsDataSource {

    companion object {
        private const val COLLECTION_NAME = "message_attachments"
    }

    // === 기본 CRUD 작업 ===

    override suspend fun getAttachmentById(attachmentId: String): MessageAttachment? {
        val entity = messageAttachmentsDao.getAttachmentById(attachmentId) ?: return null
        return MessageAttachmentsMapper.toDomain(entity)
    }

    override suspend fun getAttachmentsByMessage(messageId: String): List<MessageAttachment> {
        val entities = messageAttachmentsDao.getAttachmentsByMessage(messageId)
        return MessageAttachmentsMapper.toDomainList(entities)
    }

    override suspend fun getAttachmentsByFileType(fileType: String): List<MessageAttachment> {
        val entities = messageAttachmentsDao.getAttachmentsByFileType(fileType)
        return MessageAttachmentsMapper.toDomainList(entities)
    }

    override suspend fun getAttachmentsByUploadStatus(uploadStatus: String): List<MessageAttachment> {
        val entities = messageAttachmentsDao.getAttachmentsByUploadStatus(uploadStatus)
        return MessageAttachmentsMapper.toDomainList(entities)
    }

    override suspend fun getAttachmentsByChannel(
        channelId: String,
        fileType: String?
    ): List<MessageAttachment> {
        val entities = if (fileType != null) {
            messageAttachmentsDao.getAttachmentsByChannelAndFileType(channelId, fileType)
        } else {
            messageAttachmentsDao.getAttachmentsByChannel(channelId)
        }
        return MessageAttachmentsMapper.toDomainList(entities)
    }

    override suspend fun getAttachmentsByUser(userId: String): List<MessageAttachment> {
        val entities = messageAttachmentsDao.getAttachmentsByUser(userId)
        return MessageAttachmentsMapper.toDomainList(entities)
    }

    override suspend fun searchAttachmentsByFileName(query: String): List<MessageAttachment> {
        val entities = messageAttachmentsDao.searchAttachmentsByFileName(query)
        return MessageAttachmentsMapper.toDomainList(entities)
    }

    override suspend fun saveAttachment(attachment: MessageAttachment) {
        val entity = MessageAttachmentsMapper.toEntity(attachment)
        messageAttachmentsDao.insertAttachment(entity)
    }

    override suspend fun saveAttachments(attachments: List<MessageAttachment>) {
        if (attachments.isEmpty()) return

        val entities = MessageAttachmentsMapper.toEntityList(attachments)
        messageAttachmentsDao.insertAttachments(entities)
    }

    override suspend fun deleteAttachment(attachmentId: String) {
        messageAttachmentsDao.deleteAttachment(attachmentId)
    }

    override suspend fun deleteAttachmentsByMessage(messageId: String) {
        messageAttachmentsDao.deleteAttachmentsByMessage(messageId)
    }

    // === 3-tier 동기화 지원 ===

    override suspend fun getAttachmentsUpdatedAfter(timestamp: Instant): List<MessageAttachment> {
        val entities = messageAttachmentsDao.getAttachmentsUpdatedAfter(timestamp)
        return MessageAttachmentsMapper.toDomainList(entities)
    }

    override fun observeAttachmentsByMessage(messageId: String): Flow<List<MessageAttachment>> {
        return messageAttachmentsDao.observeAttachmentsByMessage(messageId).map { entities ->
            MessageAttachmentsMapper.toDomainList(entities)
        }
    }

    override fun observeAttachmentById(attachmentId: String): Flow<MessageAttachment?> {
        return messageAttachmentsDao.observeAttachmentById(attachmentId).map { entity ->
            entity?.let { MessageAttachmentsMapper.toDomain(it) }
        }
    }

    override fun observeAttachmentsByUploadStatus(uploadStatus: String): Flow<List<MessageAttachment>> {
        return messageAttachmentsDao.observeAttachmentsByUploadStatus(uploadStatus)
            .map { entities ->
                MessageAttachmentsMapper.toDomainList(entities)
            }
    }

    // === Outbox 관리 ===

    override suspend fun addToOutbox(attachmentId: String, operation: String, payload: String?) {
        val outboxEntity = OutboxEntity(
            id = UUID.randomUUID().toString(),
            collectionName = COLLECTION_NAME,
            entityId = attachmentId,
            operation = operation,
            payload = payload,
            localTimestamp = System.currentTimeMillis(),
            retries = 0
        )
        outboxDao.insertOperation(outboxEntity)
    }

    override suspend fun getPendingOutboxOperations(): List<MessageAttachmentOutboxOperation> {
        val entities = outboxDao.getPendingOperationsByCollection(COLLECTION_NAME)
        return entities.map { entity ->
            MessageAttachmentOutboxOperation(
                id = entity.id,
                attachmentId = entity.entityId,
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

    override suspend fun getLastSyncCursor(): Long? {
        return syncMetadataDao.getLastServerCursor(COLLECTION_NAME)
    }

    override suspend fun updateSyncCursor(cursor: Long, timestamp: Long) {
        // 동기화 메타데이터가 없으면 초기화
        if (!syncMetadataDao.syncMetadataExists(COLLECTION_NAME)) {
            syncMetadataDao.initializeSyncMetadata(COLLECTION_NAME)
        }

        syncMetadataDao.updateSyncStatus(COLLECTION_NAME, cursor, timestamp)
    }

    // === 유틸리티 ===

    override suspend fun attachmentExists(attachmentId: String): Boolean {
        return messageAttachmentsDao.attachmentExists(attachmentId)
    }

    override suspend fun getAttachmentCount(messageId: String): Int {
        return messageAttachmentsDao.getAttachmentCountByMessage(messageId)
    }

    override suspend fun getTotalAttachmentCount(): Int {
        return messageAttachmentsDao.getTotalAttachmentCount()
    }

    override suspend fun getAttachmentCountByFileType(fileType: String): Int {
        return messageAttachmentsDao.getAttachmentCountByFileType(fileType)
    }

    override suspend fun getFailedAttachments(): List<MessageAttachment> {
        val entities = messageAttachmentsDao.getAttachmentsByUploadStatus("FAILED")
        return MessageAttachmentsMapper.toDomainList(entities)
    }

    override suspend fun clearAllAttachments() {
        messageAttachmentsDao.deleteAllAttachments()
        outboxDao.deleteOperationsByCollection(COLLECTION_NAME)
        syncMetadataDao.deleteSyncMetadata(COLLECTION_NAME)
    }
}
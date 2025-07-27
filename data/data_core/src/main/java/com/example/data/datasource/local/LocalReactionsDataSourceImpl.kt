package com.example.data.datasource.local

import com.example.data.dao.OutboxDao
import com.example.data.dao.ReactionsDao
import com.example.data.dao.SyncMetadataDao
import com.example.data.mapper.ReactionsMapper
import com.example.data.model.local.OutboxEntity
import com.example.domain.model.base.Reaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로컬 리액션 데이터 저장소 구현체
 * Room Database를 사용하여 3-tier 클라이언트 주도 동기화를 제공합니다
 */
@Singleton
class LocalReactionsDataSourceImpl @Inject constructor(
    private val reactionsDao: ReactionsDao,
    private val outboxDao: OutboxDao,
    private val syncMetadataDao: SyncMetadataDao
) : LocalReactionsDataSource {

    companion object {
        private const val COLLECTION_NAME = "reactions"
    }

    // === 기본 CRUD 작업 ===

    override suspend fun getReactionById(reactionId: String): Reaction? {
        val entity = reactionsDao.getReactionById(reactionId) ?: return null
        return ReactionsMapper.toDomain(entity)
    }

    override suspend fun getReactionsByMessage(messageId: String): List<Reaction> {
        val entities = reactionsDao.getReactionsByMessage(messageId)
        return ReactionsMapper.toDomainList(entities)
    }

    override suspend fun getReactionsByMessageAndEmoji(
        messageId: String,
        emoji: String
    ): List<Reaction> {
        val entities = reactionsDao.getReactionsByMessageAndEmoji(messageId, emoji)
        return ReactionsMapper.toDomainList(entities)
    }

    override suspend fun getReactionsByUser(userId: String): List<Reaction> {
        val entities = reactionsDao.getReactionsByUser(userId)
        return ReactionsMapper.toDomainList(entities)
    }

    override suspend fun getReactionsByMessageAndUser(
        messageId: String,
        userId: String
    ): List<Reaction> {
        val entities = reactionsDao.getReactionsByMessageAndUser(messageId, userId)
        return ReactionsMapper.toDomainList(entities)
    }

    override suspend fun getReactionsByEmoji(emoji: String): List<Reaction> {
        val entities = reactionsDao.getReactionsByEmoji(emoji)
        return ReactionsMapper.toDomainList(entities)
    }

    override suspend fun getReactionsByChannel(channelId: String): List<Reaction> {
        val entities = reactionsDao.getReactionsByChannel(channelId)
        return ReactionsMapper.toDomainList(entities)
    }

    override suspend fun saveReaction(reaction: Reaction) {
        val entity = ReactionsMapper.toEntity(reaction)
        reactionsDao.insertReaction(entity)
    }

    override suspend fun saveReactions(reactions: List<Reaction>) {
        if (reactions.isEmpty()) return

        val entities = ReactionsMapper.toEntityList(reactions)
        reactionsDao.insertReactions(entities)
    }

    override suspend fun deleteReaction(reactionId: String) {
        reactionsDao.deleteReaction(reactionId)
    }

    override suspend fun deleteReactionsByMessage(messageId: String) {
        reactionsDao.deleteReactionsByMessage(messageId)
    }

    override suspend fun deleteReactionsByUser(userId: String) {
        reactionsDao.deleteReactionsByUser(userId)
    }

    override suspend fun deleteReactionByMessageAndUser(
        messageId: String,
        userId: String,
        emoji: String?
    ) {
        if (emoji != null) {
            reactionsDao.deleteReactionByMessageUserAndEmoji(messageId, userId, emoji)
        } else {
            reactionsDao.deleteReactionsByMessageAndUser(messageId, userId)
        }
    }

    // === 3-tier 동기화 지원 ===

    override suspend fun getReactionsUpdatedAfter(timestamp: Instant): List<Reaction> {
        val entities = reactionsDao.getReactionsUpdatedAfter(timestamp)
        return ReactionsMapper.toDomainList(entities)
    }

    override fun observeReactionsByMessage(messageId: String): Flow<List<Reaction>> {
        return reactionsDao.observeReactionsByMessage(messageId).map { entities ->
            ReactionsMapper.toDomainList(entities)
        }
    }

    override fun observeReactionById(reactionId: String): Flow<Reaction?> {
        return reactionsDao.observeReactionById(reactionId).map { entity ->
            entity?.let { ReactionsMapper.toDomain(it) }
        }
    }

    override fun observeReactionCountByMessageAndEmoji(
        messageId: String,
        emoji: String
    ): Flow<Int> {
        return reactionsDao.observeReactionCountByMessageAndEmoji(messageId, emoji)
    }

    override fun observeReactionStatsByMessage(messageId: String): Flow<Map<String, Int>> {
        return reactionsDao.observeReactionStatsByMessage(messageId)
    }

    // === Outbox 관리 ===

    override suspend fun addToOutbox(reactionId: String, operation: String, payload: String?) {
        val outboxEntity = OutboxEntity(
            id = UUID.randomUUID().toString(),
            collectionName = COLLECTION_NAME,
            entityId = reactionId,
            operation = operation,
            payload = payload,
            localTimestamp = System.currentTimeMillis(),
            retries = 0
        )
        outboxDao.insertOperation(outboxEntity)
    }

    override suspend fun getPendingOutboxOperations(): List<ReactionOutboxOperation> {
        val entities = outboxDao.getPendingOperationsByCollection(COLLECTION_NAME)
        return entities.map { entity ->
            ReactionOutboxOperation(
                id = entity.id,
                reactionId = entity.entityId,
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

    override suspend fun reactionExists(reactionId: String): Boolean {
        return reactionsDao.reactionExists(reactionId)
    }

    override suspend fun hasUserReactedWithEmoji(
        messageId: String,
        userId: String,
        emoji: String
    ): Boolean {
        return reactionsDao.hasUserReactedWithEmoji(messageId, userId, emoji)
    }

    override suspend fun getReactionCount(messageId: String): Int {
        return reactionsDao.getReactionCountByMessage(messageId)
    }

    override suspend fun getTotalReactionCount(): Int {
        return reactionsDao.getTotalReactionCount()
    }

    override suspend fun getReactionStatsByMessage(messageId: String): Map<String, Int> {
        return reactionsDao.getReactionStatsByMessage(messageId)
    }

    override suspend fun getPopularEmojis(limit: Int): Map<String, Int> {
        return reactionsDao.getPopularEmojis(limit)
    }

    override suspend fun clearAllReactions() {
        reactionsDao.deleteAllReactions()
        outboxDao.deleteOperationsByCollection(COLLECTION_NAME)
        syncMetadataDao.deleteSyncMetadata(COLLECTION_NAME)
    }
}
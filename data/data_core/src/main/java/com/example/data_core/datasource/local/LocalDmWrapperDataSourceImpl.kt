package com.example.data_core.datasource.local

import com.example.data_core.dao.DmWrapperDao
import com.example.data_core.dao.OutboxDao
import com.example.data_core.dao.SyncMetadataDao
import com.example.data_core.model.local.OutboxEntity
import com.example.domain.model.base.DMWrapper
import com.example.mapper.DMWrapperEntityMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로컬 DM 래퍼 데이터 저장소 구현체
 * Room Database를 사용하여 3-tier 클라이언트 주도 동기화를 제공합니다
 */
@Singleton
class LocalDMWrapperDataSourceImpl @Inject constructor(
    private val dmWrapperDao: DmWrapperDao,
    private val outboxDao: OutboxDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val mapper: DMWrapperEntityMapper
) : LocalDMWrapperDataSource {

    companion object {
        private const val COLLECTION_NAME = "dm_wrapper"
    }

    // === 기본 CRUD 작업 ===

    override suspend fun getDMWrapperById(wrapperId: String): DMWrapper? {
        val entity = dmWrapperDao.getDMWrapperById(wrapperId) ?: return null
        return mapper.toDomain(entity)
    }

    override suspend fun getDMWrappersByUser(userId: String): List<DMWrapper> {
        val entities = dmWrapperDao.getDMWrappersByUser(userId)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getDMWrappersByChannel(dmChannelId: String): List<DMWrapper> {
        val entities = dmWrapperDao.getDMWrappersByChannel(dmChannelId)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getDMWrapperByUserAndChannel(
        userId: String,
        dmChannelId: String
    ): DMWrapper? {
        val entity = dmWrapperDao.getDMWrapperByUserAndChannel(userId, dmChannelId) ?: return null
        return mapper.toDomain(entity)
    }

    override suspend fun getUnreadDMWrappersByUser(userId: String): List<DMWrapper> {
        val entities = dmWrapperDao.getUnreadDMWrappersByUser(userId)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getAllDMWrappers(): List<DMWrapper> {
        val entities = dmWrapperDao.getAllDMWrappers()
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun saveDMWrapper(dmWrapper: DMWrapper) {
        val entity = mapper.toEntity(dmWrapper)
        dmWrapperDao.insertDMWrapper(entity)
    }

    override suspend fun saveDMWrappers(dmWrappers: List<DMWrapper>) {
        if (dmWrappers.isEmpty()) return

        val entities = dmWrappers.map { mapper.toEntity(it) }
        dmWrapperDao.insertDMWrappers(entities)
    }

    override suspend fun deleteDMWrapper(wrapperId: String) {
        dmWrapperDao.deleteDMWrapper(wrapperId)
    }

    override suspend fun deleteDMWrappersByUser(userId: String) {
        dmWrapperDao.deleteDMWrappersByUser(userId)
    }

    override suspend fun deleteDMWrappersByChannel(dmChannelId: String) {
        dmWrapperDao.deleteDMWrappersByChannel(dmChannelId)
    }

    // === 3-tier 동기화 지원 ===

    override suspend fun getDMWrappersUpdatedAfter(timestamp: Instant): List<DMWrapper> {
        val entities = dmWrapperDao.getDMWrappersUpdatedAfter(timestamp)
        return entities.map { mapper.toDomain(it) }
    }

    override fun observeDMWrappersByUser(userId: String): Flow<List<DMWrapper>> {
        return dmWrapperDao.observeDMWrappersByUser(userId).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeDMWrapperById(wrapperId: String): Flow<DMWrapper?> {
        return dmWrapperDao.observeDMWrapperById(wrapperId).map { entity ->
            entity?.let { mapper.toDomain(it) }
        }
    }

    override fun observeUnreadDMWrappersByUser(userId: String): Flow<List<DMWrapper>> {
        return dmWrapperDao.observeUnreadDMWrappersByUser(userId).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeDMWrappersByUserName(userName: String, limit: Int): Flow<List<DMWrapper>> {
        return dmWrapperDao.observeDMWrappersByUserName(userName, limit).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeDMWrappersWithRecentMessages(): Flow<List<DMWrapper>> {
        return dmWrapperDao.observeDMWrappersWithRecentMessages().map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeDMWrappers(wrapperIds: List<String>): Flow<List<DMWrapper>> {
        return dmWrapperDao.observeDMWrappers(wrapperIds).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    // === Outbox 관리 ===

    override suspend fun addToOutbox(wrapperId: String, operation: String, payload: String?) {
        val outboxEntity = OutboxEntity(
            id = UUID.randomUUID().toString(),
            collectionName = COLLECTION_NAME,
            entityId = wrapperId,
            operation = operation,
            payload = payload,
            localTimestamp = System.currentTimeMillis(),
            retries = 0
        )
        outboxDao.insertOperation(outboxEntity)
    }

    override suspend fun getPendingOutboxOperations(): List<DMWrapperOutboxOperation> {
        val entities = outboxDao.getPendingOperationsByCollection(COLLECTION_NAME)
        return entities.map { entity ->
            DMWrapperOutboxOperation(
                id = entity.id,
                wrapperId = entity.documentId,
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

    override suspend fun dmWrapperExists(wrapperId: String): Boolean {
        return dmWrapperDao.dmWrapperExists(wrapperId)
    }

    override suspend fun getDMWrapperCount(userId: String): Int {
        return dmWrapperDao.getDMWrapperCountByUser(userId)
    }

    override suspend fun getTotalDMWrapperCount(): Int {
        return dmWrapperDao.getTotalDMWrapperCount()
    }

    override suspend fun getUnreadDMWrapperCount(userId: String): Int {
        return dmWrapperDao.getUnreadDMWrapperCountByUser(userId)
    }

    override suspend fun updateLastReadAt(wrapperId: String, lastReadAt: Instant) {
        dmWrapperDao.updateLastReadAt(wrapperId, lastReadAt)
    }

    override suspend fun clearAllDMWrappers() {
        dmWrapperDao.deleteAllDMWrappers()
        outboxDao.deleteOperationsByCollection(COLLECTION_NAME)
        syncMetadataDao.deleteSyncMetadata(COLLECTION_NAME)
    }

    override suspend fun searchDMWrappersByUserName(userName: String, limit: Int): List<DMWrapper> {
        return dmWrapperDao.searchDMWrappersByUserName(userName, limit).map { mapper.toDomain(it) }
    }

    override suspend fun getDMWrappersByIds(wrapperIds: List<String>): List<DMWrapper> {
        return dmWrapperDao.getDMWrappersByIds(wrapperIds).map { mapper.toDomain(it) }
    }

    override suspend fun getDMWrappersWithRecentMessages(): List<DMWrapper> {
        return dmWrapperDao.getDMWrappersWithRecentMessages().map { mapper.toDomain(it) }
    }

    override suspend fun getDMWrappersByOtherUsers(otherUserIds: List<String>): List<DMWrapper> {
        return dmWrapperDao.getDMWrappersByOtherUsers(otherUserIds).map { mapper.toDomain(it) }
    }

    override suspend fun dmWrapperExistsWithOtherUser(otherUserId: String): Boolean {
        return dmWrapperDao.dmWrapperExistsWithOtherUser(otherUserId)
    }

    override suspend fun getDMWrapperCountWithRecentMessages(): Int {
        return dmWrapperDao.getDMWrapperCountWithRecentMessages()
    }
}
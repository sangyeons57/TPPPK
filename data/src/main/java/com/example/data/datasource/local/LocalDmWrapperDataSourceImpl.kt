package com.example.data.datasource.local

import com.example.data.dao.DmWrapperDao
import com.example.data.dao.SyncMetadataDao
import com.example.data.dao.OutboxDao
import com.example.data.mapper.DmWrapperMapper
import com.example.data.model.local.OutboxEntity
import com.example.domain.model.base.DMWrapper
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
    private val syncMetadataDao: SyncMetadataDao
) : LocalDMWrapperDataSource {

    companion object {
        private const val COLLECTION_NAME = "dm_wrapper"
    }

    // === 기본 CRUD 작업 ===

    override suspend fun getDMWrapperById(wrapperId: String): DMWrapper? {
        val entity = dmWrapperDao.getDMWrapperById(wrapperId) ?: return null
        return DmWrapperMapper.toDomain(entity)
    }

    override suspend fun getDMWrappersByUser(userId: String): List<DMWrapper> {
        val entities = dmWrapperDao.getDMWrappersByUser(userId)
        return DmWrapperMapper.toDomainList(entities)
    }

    override suspend fun getDMWrappersByChannel(dmChannelId: String): List<DMWrapper> {
        val entities = dmWrapperDao.getDMWrappersByChannel(dmChannelId)
        return DmWrapperMapper.toDomainList(entities)
    }

    override suspend fun getDMWrapperByUserAndChannel(
        userId: String,
        dmChannelId: String
    ): DMWrapper? {
        val entity = dmWrapperDao.getDMWrapperByUserAndChannel(userId, dmChannelId) ?: return null
        return DmWrapperMapper.toDomain(entity)
    }

    override suspend fun getUnreadDMWrappersByUser(userId: String): List<DMWrapper> {
        val entities = dmWrapperDao.getUnreadDMWrappersByUser(userId)
        return DmWrapperMapper.toDomainList(entities)
    }

    override suspend fun getAllDMWrappers(): List<DMWrapper> {
        val entities = dmWrapperDao.getAllDMWrappers()
        return DmWrapperMapper.toDomainList(entities)
    }

    override suspend fun saveDMWrapper(dmWrapper: DMWrapper) {
        val entity = DmWrapperMapper.toEntity(dmWrapper)
        dmWrapperDao.insertDMWrapper(entity)
    }

    override suspend fun saveDMWrappers(dmWrappers: List<DMWrapper>) {
        if (dmWrappers.isEmpty()) return

        val entities = DmWrapperMapper.toEntityList(dmWrappers)
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
        return DmWrapperMapper.toDomainList(entities)
    }

    override fun observeDMWrappersByUser(userId: String): Flow<List<DMWrapper>> {
        return dmWrapperDao.observeDMWrappersByUser(userId).map { entities ->
            DmWrapperMapper.toDomainList(entities)
        }
    }

    override fun observeDMWrapperById(wrapperId: String): Flow<DMWrapper?> {
        return dmWrapperDao.observeDMWrapperById(wrapperId).map { entity ->
            entity?.let { DmWrapperMapper.toDomain(it) }
        }
    }

    override fun observeUnreadDMWrappersByUser(userId: String): Flow<List<DMWrapper>> {
        return dmWrapperDao.observeUnreadDMWrappersByUser(userId).map { entities ->
            DmWrapperMapper.toDomainList(entities)
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
                wrapperId = entity.entityId,
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
}
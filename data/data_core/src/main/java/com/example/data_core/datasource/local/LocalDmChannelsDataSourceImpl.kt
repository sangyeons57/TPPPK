package com.example.data_core.datasource.local

import com.example.data_core.dao.DmChannelsDao
import com.example.data_core.dao.OutboxDao
import com.example.data_core.dao.SyncMetadataDao
import com.example.data_core.model.local.OutboxEntity
import com.example.domain.model.base.DMChannel
import com.example.domain.model.enum.DMChannelStatus
import com.example.mapper.DMChannelEntityMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로컬 DM 채널 데이터 저장소 구현체
 * Room Database를 사용하여 3-tier 클라이언트 주도 동기화를 제공합니다
 */
@Singleton
class LocalDMChannelsDataSourceImpl @Inject constructor(
    private val dmChannelsDao: DmChannelsDao,
    private val outboxDao: OutboxDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val mapper: DMChannelEntityMapper
) : LocalDMChannelsDataSource {

    companion object {
        private const val COLLECTION_NAME = "dm_channels"
    }

    // === 기본 CRUD 작업 ===

    override suspend fun getDMChannelById(channelId: String): DMChannel? {
        val entity = dmChannelsDao.getDmChannelById(channelId) ?: return null
        return mapper.toDomain(entity)
    }

    override suspend fun getDMChannelsByUser(userId: String): List<DMChannel> {
        val entities = dmChannelsDao.getDmChannelsByUser(userId)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getDMChannelBetweenUsers(user1Id: String, user2Id: String): DMChannel? {
        val entity = dmChannelsDao.getDmChannelBetweenUsers(user1Id, user2Id) ?: return null
        return mapper.toDomain(entity)
    }

    override suspend fun getDMChannelsByActiveStatus(isActive: Boolean): List<DMChannel> {
        val entities = dmChannelsDao.getDmChannelsByActiveStatus(isActive)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getAllDMChannels(): List<DMChannel> {
        val entities = dmChannelsDao.getAllDmChannels()
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun saveDMChannel(dmChannel: DMChannel) {
        val entity = mapper.toEntity(dmChannel)
        dmChannelsDao.insertDmChannel(entity)
    }

    override suspend fun saveDMChannels(dmChannels: List<DMChannel>) {
        if (dmChannels.isEmpty()) return

        val entities = dmChannels.map { mapper.toEntity(it) }
        dmChannelsDao.insertDmChannels(entities)
    }

    override suspend fun deleteDMChannel(channelId: String) {
        dmChannelsDao.deleteDmChannel(channelId)
    }

    override suspend fun deleteDMChannelsByUser(userId: String) {
        dmChannelsDao.deleteDmChannelsByUser(userId)
    }

    // === 3-tier 동기화 지원 ===

    override suspend fun getDMChannelsUpdatedAfter(timestamp: Instant): List<DMChannel> {
        val entities = dmChannelsDao.getDmChannelsUpdatedAfter(timestamp)
        return entities.map { mapper.toDomain(it) }
    }

    override fun observeDMChannelsByUser(userId: String): Flow<List<DMChannel>> {
        return dmChannelsDao.observeDmChannelsByUser(userId).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeDMChannelById(channelId: String): Flow<DMChannel?> {
        return dmChannelsDao.observeDmChannelById(channelId).map { entity ->
            entity?.let { mapper.toDomain(it) }
        }
    }

    override fun observeAllDMChannels(): Flow<List<DMChannel>> {
        return dmChannelsDao.observeAllDmChannels().map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeDMChannelsByStatus(status: DMChannelStatus): Flow<List<DMChannel>> {
        return dmChannelsDao.observeDmChannelsByStatus(status.name).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeDMChannelUpdatedAt(channelId: String): Flow<Long?> {
        return dmChannelsDao.observeDmChannelById(channelId).map { entity ->
            entity?.updatedAt?.toEpochMilli()
        }
    }

    override fun observeBlockedDMChannelsByUser(userId: String): Flow<List<DMChannel>> {
        return dmChannelsDao.observeBlockedDmChannelsByUser(userId).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    // === Outbox 관리 ===

    override suspend fun addToOutbox(channelId: String, operation: String, payload: String?) {
        val outboxEntity = OutboxEntity(
            id = UUID.randomUUID().toString(),
            collectionName = COLLECTION_NAME,
            entityId = channelId,
            operation = operation,
            payload = payload,
            localTimestamp = System.currentTimeMillis(),
            retries = 0
        )
        outboxDao.insertOperation(outboxEntity)
    }

    override suspend fun getPendingOutboxOperations(): List<DMChannelOutboxOperation> {
        val entities = outboxDao.getPendingOperationsByCollection(COLLECTION_NAME)
        return entities.map { entity ->
            DMChannelOutboxOperation(
                id = entity.id,
                channelId = entity.documentId,
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

    override suspend fun dmChannelExists(channelId: String): Boolean {
        return dmChannelsDao.dmChannelExists(channelId)
    }

    override suspend fun getDMChannelCount(userId: String): Int {
        return dmChannelsDao.getDmChannelCountByUser(userId)
    }

    override suspend fun getTotalDMChannelCount(): Int {
        return dmChannelsDao.getTotalDmChannelCount()
    }

    override suspend fun getActiveDMChannelCount(): Int {
        return dmChannelsDao.getActiveDmChannelCount()
    }

    override suspend fun clearAllDMChannels() {
        dmChannelsDao.deleteAllDmChannels()
        outboxDao.deleteOperationsByCollection(COLLECTION_NAME)
        syncMetadataDao.deleteSyncMetadata(COLLECTION_NAME)
    }

    override suspend fun getDMChannelsByStatus(status: DMChannelStatus): List<DMChannel> {
        val entities = dmChannelsDao.getDmChannelsByStatus(status.name)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getDMChannelsByIds(channelIds: List<String>): List<DMChannel> {
        val entities = dmChannelsDao.getDmChannelsByIds(channelIds)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getBlockedDMChannelsByUser(userId: String): List<DMChannel> {
        val entities = dmChannelsDao.getBlockedDmChannelsByUser(userId)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun dmChannelExistsBetweenUsers(user1Id: String, user2Id: String): Boolean {
        return dmChannelsDao.getDmChannelBetweenUsers(user1Id, user2Id) != null
    }

    override suspend fun getDMChannelCountByStatus(status: DMChannelStatus): Int {
        return dmChannelsDao.getDmChannelCountByStatus(status.name)
    }

    override fun observeDMChannelsByActiveStatus(isActive: Boolean): Flow<List<DMChannel>> {
        return dmChannelsDao.observeDmChannelsByActiveStatus(isActive).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }
}
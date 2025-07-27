package com.example.data.datasource.local

import com.example.data.dao.SchedulesDao
import com.example.data.dao.SyncMetadataDao
import com.example.data.dao.OutboxDao
import com.example.data.mapper.SchedulesMapper
import com.example.data.model.local.OutboxEntity
import com.example.domain.model.base.Schedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalSchedulesDataSourceImpl @Inject constructor(
    private val schedulesDao: SchedulesDao,
    private val outboxDao: OutboxDao,
    private val syncMetadataDao: SyncMetadataDao
) : LocalSchedulesDataSource {

    companion object {
        private const val COLLECTION_NAME = "schedules"
    }

    override suspend fun getScheduleById(scheduleId: String): Schedule? {
        val entity = schedulesDao.getScheduleById(scheduleId) ?: return null
        return SchedulesMapper.toDomain(entity)
    }

    override suspend fun getSchedulesByProject(projectId: String): List<Schedule> {
        val entities = schedulesDao.getSchedulesByProject(projectId)
        return SchedulesMapper.toDomainList(entities)
    }

    override suspend fun getSchedulesByUser(userId: String): List<Schedule> {
        val entities = schedulesDao.getSchedulesByUser(userId)
        return SchedulesMapper.toDomainList(entities)
    }

    override suspend fun getSchedulesByDateRange(
        startDate: Instant,
        endDate: Instant
    ): List<Schedule> {
        val entities = schedulesDao.getSchedulesByDateRange(startDate, endDate)
        return SchedulesMapper.toDomainList(entities)
    }

    override suspend fun getSchedulesByChannel(channelId: String): List<Schedule> {
        val entities = schedulesDao.getSchedulesByChannel(channelId)
        return SchedulesMapper.toDomainList(entities)
    }

    override suspend fun getAllSchedules(): List<Schedule> {
        val entities = schedulesDao.getAllSchedules()
        return SchedulesMapper.toDomainList(entities)
    }

    override suspend fun saveSchedule(schedule: Schedule) {
        val entity = SchedulesMapper.toEntity(schedule)
        schedulesDao.insertSchedule(entity)
    }

    override suspend fun saveSchedules(schedules: List<Schedule>) {
        if (schedules.isEmpty()) return
        val entities = SchedulesMapper.toEntityList(schedules)
        schedulesDao.insertSchedules(entities)
    }

    override suspend fun deleteSchedule(scheduleId: String) {
        schedulesDao.deleteSchedule(scheduleId)
    }

    override suspend fun deleteSchedulesByProject(projectId: String) {
        schedulesDao.deleteSchedulesByProject(projectId)
    }

    override suspend fun getSchedulesUpdatedAfter(timestamp: Instant): List<Schedule> {
        val entities = schedulesDao.getSchedulesUpdatedAfter(timestamp)
        return SchedulesMapper.toDomainList(entities)
    }

    override fun observeSchedulesByProject(projectId: String): Flow<List<Schedule>> {
        return schedulesDao.observeSchedulesByProject(projectId).map { entities ->
            SchedulesMapper.toDomainList(entities)
        }
    }

    override fun observeScheduleById(scheduleId: String): Flow<Schedule?> {
        return schedulesDao.observeScheduleById(scheduleId).map { entity ->
            entity?.let { SchedulesMapper.toDomain(it) }
        }
    }

    override suspend fun addToOutbox(scheduleId: String, operation: String, payload: String?) {
        val outboxEntity = OutboxEntity(
            id = UUID.randomUUID().toString(),
            collectionName = COLLECTION_NAME,
            entityId = scheduleId,
            operation = operation,
            payload = payload,
            localTimestamp = System.currentTimeMillis(),
            retries = 0
        )
        outboxDao.insertOperation(outboxEntity)
    }

    override suspend fun getPendingOutboxOperations(): List<ScheduleOutboxOperation> {
        val entities = outboxDao.getPendingOperationsByCollection(COLLECTION_NAME)
        return entities.map { entity ->
            ScheduleOutboxOperation(
                id = entity.id,
                scheduleId = entity.entityId,
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

    override suspend fun getLastSyncCursor(): Long? {
        return syncMetadataDao.getLastServerCursor(COLLECTION_NAME)
    }

    override suspend fun updateSyncCursor(cursor: Long, timestamp: Long) {
        if (!syncMetadataDao.syncMetadataExists(COLLECTION_NAME)) {
            syncMetadataDao.initializeSyncMetadata(COLLECTION_NAME)
        }
        syncMetadataDao.updateSyncStatus(COLLECTION_NAME, cursor, timestamp)
    }

    override suspend fun scheduleExists(scheduleId: String): Boolean {
        return schedulesDao.scheduleExists(scheduleId)
    }

    override suspend fun getScheduleCount(projectId: String): Int {
        return schedulesDao.getScheduleCountByProject(projectId)
    }

    override suspend fun clearAllSchedules() {
        schedulesDao.deleteAllSchedules()
        outboxDao.deleteOperationsByCollection(COLLECTION_NAME)
        syncMetadataDao.deleteSyncMetadata(COLLECTION_NAME)
    }
}
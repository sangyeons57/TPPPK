package com.example.data_core.datasource.local

import com.example.data_core.dao.OutboxDao
import com.example.data_core.dao.SchedulesDao
import com.example.data_core.dao.SyncMetadataDao
import com.example.data_core.model.local.OutboxEntity
import com.example.domain.model.base.Schedule
import com.example.domain.model.enum.ScheduleStatus
import com.example.mapper.ScheduleEntityMapper
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
    private val syncMetadataDao: SyncMetadataDao,
    private val mapper: ScheduleEntityMapper
) : LocalSchedulesDataSource {

    companion object {
        private const val COLLECTION_NAME = "schedules"
    }

    override suspend fun getScheduleById(scheduleId: String): Schedule? {
        val entity = schedulesDao.getScheduleById(scheduleId) ?: return null
        return mapper.toDomain(entity)
    }

    override suspend fun getSchedulesByProject(projectId: String): List<Schedule> {
        val entities = schedulesDao.getSchedulesByProject(projectId)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getSchedulesByUser(userId: String): List<Schedule> {
        val entities = schedulesDao.getSchedulesByUser(userId)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getSchedulesByDateRange(
        startDate: Instant,
        endDate: Instant
    ): List<Schedule> {
        val entities = schedulesDao.getSchedulesByDateRange(startDate, endDate)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getSchedulesByChannel(channelId: String): List<Schedule> {
        val entities = schedulesDao.getSchedulesByChannel(channelId)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getAllSchedules(): List<Schedule> {
        val entities = schedulesDao.getAllSchedules()
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun saveSchedule(schedule: Schedule) {
        val entity = mapper.toEntity(schedule)
        schedulesDao.insertSchedule(entity)
    }

    override suspend fun saveSchedules(schedules: List<Schedule>) {
        if (schedules.isEmpty()) return
        val entities = schedules.map { mapper.toEntity(it) }
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
        return entities.map { mapper.toDomain(it) }
    }

    override fun observeSchedulesByProject(projectId: String): Flow<List<Schedule>> {
        return schedulesDao.observeSchedulesByProject(projectId).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeScheduleById(scheduleId: String): Flow<Schedule?> {
        return schedulesDao.observeScheduleById(scheduleId).map { entity ->
            entity?.let { mapper.toDomain(it) }
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
                scheduleId = entity.documentId,
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

    override fun observeSchedulesByOwner(ownerId: String): Flow<List<Schedule>> {
        return schedulesDao.observeSchedulesByOwner(ownerId).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeSchedulesByStatus(status: ScheduleStatus): Flow<List<Schedule>> {
        return schedulesDao.observeSchedulesByStatus(status.name).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeSchedulesByTimeRange(startTime: Instant, endTime: Instant): Flow<List<Schedule>> {
        return schedulesDao.observeSchedulesByTimeRange(startTime, endTime).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeSchedulesByDate(date: Instant): Flow<List<Schedule>> {
        return schedulesDao.observeSchedulesByDate(date).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeSchedulesByTitle(title: String, limit: Int): Flow<List<Schedule>> {
        return schedulesDao.searchSchedulesByTitle(title, limit).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeAllSchedules(): Flow<List<Schedule>> {
        return schedulesDao.observeAllSchedules().map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeScheduleUpdatedAt(scheduleId: String): Flow<Long?> {
        return schedulesDao.observeScheduleById(scheduleId).map { it?.updatedAt?.toEpochMilli() }
    }

    override fun observeSchedules(scheduleIds: List<String>): Flow<List<Schedule>> {
        return schedulesDao.observeSchedules(scheduleIds).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeUpcomingSchedules(): Flow<List<Schedule>> {
        return schedulesDao.observeUpcomingSchedules().map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeTodaySchedules(): Flow<List<Schedule>> {
        return schedulesDao.observeTodaySchedules().map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override suspend fun getSchedulesByOwner(ownerId: String): List<Schedule> {
        return schedulesDao.getSchedulesByOwner(ownerId).map { mapper.toDomain(it) }
    }

    override suspend fun getSchedulesByStatus(status: ScheduleStatus): List<Schedule> {
        return schedulesDao.getSchedulesByStatus(status.name).map { mapper.toDomain(it) }
    }

    override suspend fun getSchedulesByTimeRange(startTime: Instant, endTime: Instant): List<Schedule> {
        return schedulesDao.getSchedulesByTimeRange(startTime, endTime).map { mapper.toDomain(it) }
    }

    override suspend fun getSchedulesByDate(date: Instant): List<Schedule> {
        return schedulesDao.getSchedulesByDate(date).map { mapper.toDomain(it) }
    }

    override suspend fun searchSchedulesByTitle(title: String, limit: Int): List<Schedule> {
        return schedulesDao.searchSchedulesByTitle(title, limit).map { mapper.toDomain(it) }
    }

    override suspend fun getAllSchedules(limit: Int?): List<Schedule> {
        return schedulesDao.getAllSchedules().map { mapper.toDomain(it) }
    }

    override suspend fun getSchedulesByIds(scheduleIds: List<String>): List<Schedule> {
        return schedulesDao.getSchedulesByIds(scheduleIds).map { mapper.toDomain(it) }
    }

    override suspend fun getUpcomingSchedules(): List<Schedule> {
        return schedulesDao.getUpcomingSchedules().map { mapper.toDomain(it) }
    }

    override suspend fun getTodaySchedules(): List<Schedule> {
        return schedulesDao.getTodaySchedules().map { mapper.toDomain(it) }
    }

    override suspend fun getCompletedSchedulesInRange(startTime: Instant, endTime: Instant): List<Schedule> {
        return schedulesDao.getCompletedSchedulesInRange(startTime, endTime).map { mapper.toDomain(it) }
    }

    override suspend fun getScheduleCountByOwner(ownerId: String): Int {
        return schedulesDao.getScheduleCountByOwner(ownerId)
    }

    override suspend fun getScheduleCountByStatus(status: ScheduleStatus): Int {
        return schedulesDao.getScheduleCountByStatus(status.name)
    }

    override suspend fun getTotalScheduleCount(): Int {
        return schedulesDao.getTotalScheduleCount()
    }

    override suspend fun getScheduleCountByDate(date: Instant): Int {
        return schedulesDao.getScheduleCountByDate(date)
    }

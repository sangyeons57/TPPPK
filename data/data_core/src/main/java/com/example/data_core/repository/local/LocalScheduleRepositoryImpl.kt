package com.example.data_core.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.local.LocalSchedulesDataSource
import com.example.data_core.repository.local.base.BaseLocalRepositoryImpl
import com.example.domain.model.base.Schedule
import com.example.domain.model.enum.ScheduleStatus
import com.example.domain.model.vo.schedule.ScheduleContent
import com.example.domain.model.vo.schedule.ScheduleTitle
import com.example.domain.repository.local.LocalScheduleRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Schedule Repository Implementation (SSOT)
 * BaseLocalRepositoryImpl 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지
 *
 * ✅ 역할:
 * - BaseLocalRepositoryImpl의 공통 CRUD 기능 상속 (80%)
 * - Schedule 도메인 특화 기능만 구현 (20%)
 * - LocalDataSource를 통한 Room DB 접궼
 * - Flow로 UI에 실시간 데이터 제공
 * - 로컬 CRUD 작업 처리
 * - Outbox 관리 (동기화 대상 저장)
 *
 * 📋 BaseLocalRepository 메서드 구현:
 * - observeEntityById -> observeScheduleById로 위임
 * - observeAllEntities -> observeAllSchedules로 위임
 * - observeEntityUpdatedAt -> observeScheduleUpdatedAt로 위임
 * - getEntityById -> getScheduleById로 위임
 * - getEntitiesByIds -> getSchedulesByIds로 위임
 * - getAllEntities -> getAllSchedules로 위임
 * - saveEntity -> saveSchedule로 위임
 * - saveEntities -> saveSchedules로 위임
 * - deleteEntity -> deleteSchedule로 위임
 * - Plus SyncableRepository methods
 */
@Singleton
class LocalScheduleRepositoryImpl @Inject constructor(
    private val localSchedulesDataSource: LocalSchedulesDataSource
) : BaseLocalRepositoryImpl<Schedule>(), LocalScheduleRepository {

    companion object {
        private const val TAG = "LocalScheduleRepository"
    }

    // === BaseLocalRepository 메서드 구현 (도메인 특화 메서드로 위임) ===

    override fun observeEntityById(entityId: String): Flow<Schedule?> = 
        observeScheduleById(entityId)

    override fun observeAllEntities(): Flow<List<Schedule>> = 
        observeAllSchedules()

    override fun observeEntityUpdatedAt(entityId: String): Flow<Long?> = 
        observeScheduleUpdatedAt(entityId)

    override suspend fun getEntityById(entityId: String): CustomResult<Schedule?, Exception> = 
        handleOperation("getScheduleById($entityId)", TAG) {
            getScheduleById(entityId)
        }

    override suspend fun getEntitiesByIds(entityIds: List<String>): CustomResult<List<Schedule>, Exception> = 
        handleOperation("getSchedulesByIds(${entityIds.size})", TAG) {
            getSchedulesByIds(entityIds)
        }

    override suspend fun getAllEntities(limit: Int?): CustomResult<List<Schedule>, Exception> = 
        handleOperation("getAllSchedules($limit)", TAG) {
            getAllSchedules(limit)
        }

    override suspend fun saveEntity(entity: Schedule): CustomResult<Unit, Exception> = 
        saveSchedule(entity)

    override suspend fun saveEntities(entities: List<Schedule>): CustomResult<Unit, Exception> = 
        saveSchedules(entities)

    override suspend fun deleteEntity(entityId: String): CustomResult<Unit, Exception> = 
        deleteSchedule(entityId)

    override suspend fun getEntitiesUpdatedAfter(timestamp: Instant): CustomResult<List<Schedule>, Exception> = 
        handleOperation("getSchedulesUpdatedAfter($timestamp)", TAG) {
            getSchedulesUpdatedAfter(timestamp)
        }

    override suspend fun clearAllEntities(): CustomResult<Unit, Exception> = 
        clearAllSchedules()

    override suspend fun getTotalEntityCount(): CustomResult<Int, Exception> = 
        handleOperation("getTotalScheduleCount", TAG) {
            getTotalScheduleCount()
        }

    override suspend fun entityExists(entityId: String): CustomResult<Boolean, Exception> = 
        handleOperation("scheduleExists($entityId)", TAG) {
            scheduleExists(entityId)
        }

    override suspend fun addToOutbox(
        entityId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return handleOperation("addToOutbox($entityId, $operation)", TAG) {
            localSchedulesDataSource.addToOutbox(entityId, operation, payload)
        }
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeScheduleById(scheduleId: String): Flow<Schedule?> {
        logDebug( "observeScheduleById: $scheduleId")
        return localSchedulesDataSource.observeScheduleById(scheduleId)
    }

    override fun observeSchedulesByProject(projectId: String): Flow<List<Schedule>> {
        logDebug( "observeSchedulesByProject: $projectId")
        return localSchedulesDataSource.observeSchedulesByProject(projectId)
    }

    override fun observeSchedulesByOwner(ownerId: String): Flow<List<Schedule>> {
        logDebug( "observeSchedulesByOwner: $ownerId")
        return localSchedulesDataSource.observeSchedulesByOwner(ownerId)
    }

    override fun observeSchedulesByStatus(status: ScheduleStatus): Flow<List<Schedule>> {
        logDebug( "observeSchedulesByStatus: $status")
        return localSchedulesDataSource.observeSchedulesByStatus(status)
    }

    override fun observeSchedulesByTimeRange(
        startTime: Instant,
        endTime: Instant
    ): Flow<List<Schedule>> {
        logDebug( "observeSchedulesByTimeRange: $startTime to $endTime")
        return localSchedulesDataSource.observeSchedulesByTimeRange(startTime, endTime)
    }

    override fun observeSchedulesByDate(date: Instant): Flow<List<Schedule>> {
        logDebug( "observeSchedulesByDate: $date")
        return localSchedulesDataSource.observeSchedulesByDate(date)
    }

    override fun observeSchedulesByTitle(title: String, limit: Int): Flow<List<Schedule>> {
        logDebug( "observeSchedulesByTitle: title='$title', limit=$limit")
        return localSchedulesDataSource.observeSchedulesByTitle(title, limit)
    }

    override fun observeAllSchedules(): Flow<List<Schedule>> {
        logDebug( "observeAllSchedules")
        return localSchedulesDataSource.observeAllSchedules()
    }

    override fun observeScheduleUpdatedAt(scheduleId: String): Flow<Long?> {
        logDebug( "observeScheduleUpdatedAt: $scheduleId")
        return localSchedulesDataSource.observeScheduleUpdatedAt(scheduleId)
    }

    override fun observeSchedules(scheduleIds: List<String>): Flow<List<Schedule>> {
        logDebug( "observeSchedules: ${scheduleIds.size} schedules")
        return localSchedulesDataSource.observeSchedules(scheduleIds)
    }

    override fun observeUpcomingSchedules(): Flow<List<Schedule>> {
        logDebug( "observeUpcomingSchedules")
        return localSchedulesDataSource.observeUpcomingSchedules()
    }

    override fun observeTodaySchedules(): Flow<List<Schedule>> {
        logDebug( "observeTodaySchedules")
        return localSchedulesDataSource.observeTodaySchedules()
    }

    // === 단순 읽기 작업 ===

    override suspend fun getScheduleById(scheduleId: String): Schedule? {
        logDebug( "getScheduleById: $scheduleId")
        return try {
            localSchedulesDataSource.getScheduleById(scheduleId)
        } catch (e: Exception) {
            logError( "getScheduleById failed", e)
            null
        }
    }

    override suspend fun getSchedulesByProject(projectId: String): List<Schedule> {
        logDebug( "getSchedulesByProject: $projectId")
        return try {
            localSchedulesDataSource.getSchedulesByProject(projectId)
        } catch (e: Exception) {
            logError( "getSchedulesByProject failed", e)
            emptyList()
        }
    }

    override suspend fun getSchedulesByOwner(ownerId: String): List<Schedule> {
        logDebug( "getSchedulesByOwner: $ownerId")
        return try {
            localSchedulesDataSource.getSchedulesByOwner(ownerId)
        } catch (e: Exception) {
            logError( "getSchedulesByOwner failed", e)
            emptyList()
        }
    }

    override suspend fun getSchedulesByStatus(status: ScheduleStatus): List<Schedule> {
        logDebug( "getSchedulesByStatus: $status")
        return try {
            localSchedulesDataSource.getSchedulesByStatus(status)
        } catch (e: Exception) {
            logError( "getSchedulesByStatus failed", e)
            emptyList()
        }
    }

    override suspend fun getSchedulesByTimeRange(
        startTime: Instant,
        endTime: Instant
    ): List<Schedule> {
        logDebug( "getSchedulesByTimeRange: $startTime to $endTime")
        return try {
            localSchedulesDataSource.getSchedulesByTimeRange(startTime, endTime)
        } catch (e: Exception) {
            logError( "getSchedulesByTimeRange failed", e)
            emptyList()
        }
    }

    override suspend fun getSchedulesByDate(date: Instant): List<Schedule> {
        logDebug( "getSchedulesByDate: $date")
        return localSchedulesDataSource.getSchedulesByDate(date)
    }

    override suspend fun searchSchedulesByTitle(title: String, limit: Int): List<Schedule> {
        logDebug( "searchSchedulesByTitle: title='$title', limit=$limit")
        return try {
            localSchedulesDataSource.searchSchedulesByTitle(title, limit)
        } catch (e: Exception) {
            logError( "searchSchedulesByTitle failed", e)
            emptyList()
        }
    }

    override suspend fun getAllSchedules(limit: Int?): List<Schedule> {
        logDebug( "getAllSchedules: limit=$limit")
        return try {
            localSchedulesDataSource.getAllSchedules(limit)
        } catch (e: Exception) {
            logError( "getAllSchedules failed", e)
            emptyList()
        }
    }

    override suspend fun getSchedulesByIds(scheduleIds: List<String>): List<Schedule> {
        logDebug( "getSchedulesByIds: ${scheduleIds.size} schedules")
        return try {
            localSchedulesDataSource.getSchedulesByIds(scheduleIds)
        } catch (e: Exception) {
            logError( "getSchedulesByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getUpcomingSchedules(): List<Schedule> {
        logDebug( "getUpcomingSchedules")
        return try {
            localSchedulesDataSource.getUpcomingSchedules()
        } catch (e: Exception) {
            logError( "getUpcomingSchedules failed", e)
            emptyList()
        }
    }

    override suspend fun getTodaySchedules(): List<Schedule> {
        logDebug( "getTodaySchedules")
        return localSchedulesDataSource.getTodaySchedules()
    }

    override suspend fun getCompletedSchedulesInRange(
        startTime: Instant,
        endTime: Instant
    ): List<Schedule> {
        logDebug( "getCompletedSchedulesInRange: $startTime to $endTime")
        return try {
            localSchedulesDataSource.getCompletedSchedulesInRange(startTime, endTime)
        } catch (e: Exception) {
            logError( "getCompletedSchedulesInRange failed", e)
            emptyList()
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveSchedule(schedule: Schedule): CustomResult<Unit, Exception> {
        return try {
            logDebug( "saveSchedule: ${schedule.id}")

            // 1. Room DB에 저장
            localSchedulesDataSource.saveSchedule(schedule)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (schedule.isNew) "CREATE" else "UPDATE"
            localSchedulesDataSource.addToOutbox(
                scheduleId = schedule.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            logDebug( "Schedule saved and added to outbox: ${schedule.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            logError( "saveSchedule failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveSchedules(schedules: List<Schedule>): CustomResult<Unit, Exception> {
        return try {
            logDebug( "saveSchedules: ${schedules.size} schedules")

            if (schedules.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localSchedulesDataSource.saveSchedules(schedules)

            logDebug( "Bulk schedules saved: ${schedules.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            logError( "saveSchedules failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteSchedule(scheduleId: String): CustomResult<Unit, Exception> {
        return try {
            logDebug( "deleteSchedule: $scheduleId")

            // 1. Room DB에서 삭제 (실제로는 soft delete)
            localSchedulesDataSource.deleteSchedule(scheduleId)

            // 2. Outbox에 삭제 작업 추가
            localSchedulesDataSource.addToOutbox(
                scheduleId = scheduleId,
                operation = "DELETE",
                payload = null
            )

            logDebug( "Schedule deleted and added to outbox: $scheduleId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            logError( "deleteSchedule failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteSchedulesByProject(projectId: String): CustomResult<Unit, Exception> {
        return try {
            logDebug( "deleteSchedulesByProject: $projectId")

            localSchedulesDataSource.deleteSchedulesByProject(projectId)

            logDebug( "Schedules deleted for project: $projectId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            logError( "deleteSchedulesByProject failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateSchedule(
        scheduleId: String,
        title: ScheduleTitle?,
        content: ScheduleContent?,
        startTime: Instant?,
        endTime: Instant?,
        status: ScheduleStatus?
    ): CustomResult<Unit, Exception> {
        return try {
            logDebug( "updateSchedule: scheduleId=$scheduleId")

            // 1. 현재 일정 조회
            val currentSchedule = localSchedulesDataSource.getScheduleById(scheduleId)
                ?: return CustomResult.Failure(IllegalArgumentException("Schedule not found: $scheduleId"))

            // 2. 업데이트 적용
            var updatedSchedule = currentSchedule

            // Apply updates using domain methods
            if (title != null && content != null) {
                updatedSchedule.updateDetails(title, content)
            }

            if (startTime != null && endTime != null) {
                updatedSchedule.reschedule(startTime, endTime)
            }

            status?.let { updatedSchedule.changeStatus(it) }

            // 3. 저장 (Outbox 포함)
            saveSchedule(updatedSchedule)

            logDebug( "Schedule updated: $scheduleId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            logError( "updateSchedule failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateScheduleDetails(
        scheduleId: String,
        title: ScheduleTitle,
        content: ScheduleContent
    ): CustomResult<Unit, Exception> {
        return updateSchedule(scheduleId, title, content, null, null, null)
    }

    override suspend fun reschedule(
        scheduleId: String,
        newStartTime: Instant,
        newEndTime: Instant
    ): CustomResult<Unit, Exception> {
        return updateSchedule(scheduleId, null, null, newStartTime, newEndTime, null)
    }

    override suspend fun changeScheduleStatus(
        scheduleId: String,
        newStatus: ScheduleStatus
    ): CustomResult<Unit, Exception> {
        return updateSchedule(scheduleId, null, null, null, null, newStatus)
    }

    // === 유틸리티 ===

    override suspend fun scheduleExists(scheduleId: String): Boolean {
        return try {
            localSchedulesDataSource.scheduleExists(scheduleId)
        } catch (e: Exception) {
            logError( "scheduleExists failed", e)
            false
        }
    }

    override suspend fun titleExistsInProject(
        projectId: String,
        title: ScheduleTitle,
        excludeScheduleId: String?
    ): Boolean {
        return try {
            getSchedulesByProject(projectId).any { schedule ->
                schedule.title == title && schedule.id.value != excludeScheduleId
            }
        } catch (e: Exception) {
            logError( "titleExistsInProject failed", e)
            false
        }
    }

    override suspend fun getScheduleCountByProject(projectId: String): Int {
        return try {
            localSchedulesDataSource.getScheduleCountByProject(projectId)
        } catch (e: Exception) {
            logError( "getScheduleCountByProject failed", e)
            0
        }
    }

    override suspend fun getScheduleCountByOwner(ownerId: String): Int {
        return try {
            localSchedulesDataSource.getScheduleCountByOwner(ownerId)
        } catch (e: Exception) {
            logError( "getScheduleCountByOwner failed", e)
            0
        }
    }

    override suspend fun getScheduleCountByStatus(status: ScheduleStatus): Int {
        return try {
            localSchedulesDataSource.getScheduleCountByStatus(status)
        } catch (e: Exception) {
            logError( "getScheduleCountByStatus failed", e)
            0
        }
    }

    override suspend fun getTotalScheduleCount(): Int {
        return try {
            localSchedulesDataSource.getTotalScheduleCount()
        } catch (e: Exception) {
            logError( "getTotalScheduleCount failed", e)
            0
        }
    }

    override suspend fun getScheduleCountByDate(date: Instant): Int {
        return try {
            getSchedulesByDate(date).size
        } catch (e: Exception) {
            logError( "getScheduleCountByDate failed", e)
            0
        }
    }

    override suspend fun clearAllSchedules(): CustomResult<Unit, Exception> {
        return try {
            logDebug( "clearAllSchedules")

            localSchedulesDataSource.clearAllSchedules()

            logDebug( "All schedules cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            logError( "clearAllSchedules failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getSchedulesUpdatedAfter(timestamp: Instant): List<Schedule> {
        return try {
            localSchedulesDataSource.getSchedulesUpdatedAfter(timestamp)
        } catch (e: Exception) {
            logError( "getSchedulesUpdatedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun addToOutbox(
        scheduleId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return try {
            logDebug( "addToOutbox: scheduleId=$scheduleId, operation=$operation")

            localSchedulesDataSource.addToOutbox(scheduleId, operation, payload)

            logDebug( "Added to outbox: $scheduleId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            logError( "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}
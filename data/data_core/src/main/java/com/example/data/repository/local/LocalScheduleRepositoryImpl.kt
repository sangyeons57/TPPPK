package com.example.data.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data.datasource.local.LocalSchedulesDataSource
import com.example.domain.model.base.Schedule
import com.example.domain.model.enum.ScheduleStatus
import com.example.domain.model.vo.schedule.ScheduleContent
import com.example.domain.model.vo.schedule.ScheduleTitle
import com.example.domain.repository.local.LocalScheduleRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Schedule Repository Implementation (SSOT)
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
class LocalScheduleRepositoryImpl @Inject constructor(
    private val localSchedulesDataSource: LocalSchedulesDataSource
) : LocalScheduleRepository {

    companion object {
        private const val TAG = "LocalScheduleRepository"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeScheduleById(scheduleId: String): Flow<Schedule?> {
        Log.d(TAG, "observeScheduleById: $scheduleId")
        return localSchedulesDataSource.observeScheduleById(scheduleId)
    }

    override fun observeSchedulesByProject(projectId: String): Flow<List<Schedule>> {
        Log.d(TAG, "observeSchedulesByProject: $projectId")
        return localSchedulesDataSource.observeSchedulesByProject(projectId)
    }

    override fun observeSchedulesByOwner(ownerId: String): Flow<List<Schedule>> {
        Log.d(TAG, "observeSchedulesByOwner: $ownerId")
        return localSchedulesDataSource.observeSchedulesByOwner(ownerId)
    }

    override fun observeSchedulesByStatus(status: ScheduleStatus): Flow<List<Schedule>> {
        Log.d(TAG, "observeSchedulesByStatus: $status")
        return localSchedulesDataSource.observeSchedulesByStatus(status)
    }

    override fun observeSchedulesByTimeRange(
        startTime: Instant,
        endTime: Instant
    ): Flow<List<Schedule>> {
        Log.d(TAG, "observeSchedulesByTimeRange: $startTime to $endTime")
        return localSchedulesDataSource.observeSchedulesByTimeRange(startTime, endTime)
    }

    override fun observeSchedulesByDate(date: Instant): Flow<List<Schedule>> {
        Log.d(TAG, "observeSchedulesByDate: $date")
        val dayStart = date.truncatedTo(ChronoUnit.DAYS)
        val dayEnd = dayStart.plus(1, ChronoUnit.DAYS)
        return observeSchedulesByTimeRange(dayStart, dayEnd)
    }

    override fun observeSchedulesByTitle(title: String, limit: Int): Flow<List<Schedule>> {
        Log.d(TAG, "observeSchedulesByTitle: title='$title', limit=$limit")
        return kotlinx.coroutines.flow.map(observeAllSchedules()) { schedules ->
            schedules.filter { schedule ->
                schedule.title.value.contains(title, ignoreCase = true)
            }.take(limit)
        }
    }

    override fun observeAllSchedules(): Flow<List<Schedule>> {
        Log.d(TAG, "observeAllSchedules")
        return localSchedulesDataSource.observeAllSchedules()
    }

    override fun observeScheduleUpdatedAt(scheduleId: String): Flow<Long?> {
        Log.d(TAG, "observeScheduleUpdatedAt: $scheduleId")
        return kotlinx.coroutines.flow.map(observeScheduleById(scheduleId)) { schedule ->
            schedule?.updatedAt?.toEpochMilli()
        }
    }

    override fun observeSchedules(scheduleIds: List<String>): Flow<List<Schedule>> {
        Log.d(TAG, "observeSchedules: ${scheduleIds.size} schedules")
        return localSchedulesDataSource.observeSchedules(scheduleIds)
    }

    override fun observeUpcomingSchedules(): Flow<List<Schedule>> {
        Log.d(TAG, "observeUpcomingSchedules")
        val now = Instant.now()
        return kotlinx.coroutines.flow.map(observeAllSchedules()) { schedules ->
            schedules.filter { it.startTime.isAfter(now) }
                .sortedBy { it.startTime }
        }
    }

    override fun observeTodaySchedules(): Flow<List<Schedule>> {
        Log.d(TAG, "observeTodaySchedules")
        return observeSchedulesByDate(Instant.now())
    }

    // === 단순 읽기 작업 ===

    override suspend fun getScheduleById(scheduleId: String): Schedule? {
        Log.d(TAG, "getScheduleById: $scheduleId")
        return try {
            localSchedulesDataSource.getScheduleById(scheduleId)
        } catch (e: Exception) {
            Log.e(TAG, "getScheduleById failed", e)
            null
        }
    }

    override suspend fun getSchedulesByProject(projectId: String): List<Schedule> {
        Log.d(TAG, "getSchedulesByProject: $projectId")
        return try {
            localSchedulesDataSource.getSchedulesByProject(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getSchedulesByProject failed", e)
            emptyList()
        }
    }

    override suspend fun getSchedulesByOwner(ownerId: String): List<Schedule> {
        Log.d(TAG, "getSchedulesByOwner: $ownerId")
        return try {
            localSchedulesDataSource.getSchedulesByOwner(ownerId)
        } catch (e: Exception) {
            Log.e(TAG, "getSchedulesByOwner failed", e)
            emptyList()
        }
    }

    override suspend fun getSchedulesByStatus(status: ScheduleStatus): List<Schedule> {
        Log.d(TAG, "getSchedulesByStatus: $status")
        return try {
            localSchedulesDataSource.getSchedulesByStatus(status)
        } catch (e: Exception) {
            Log.e(TAG, "getSchedulesByStatus failed", e)
            emptyList()
        }
    }

    override suspend fun getSchedulesByTimeRange(
        startTime: Instant,
        endTime: Instant
    ): List<Schedule> {
        Log.d(TAG, "getSchedulesByTimeRange: $startTime to $endTime")
        return try {
            localSchedulesDataSource.getSchedulesByTimeRange(startTime, endTime)
        } catch (e: Exception) {
            Log.e(TAG, "getSchedulesByTimeRange failed", e)
            emptyList()
        }
    }

    override suspend fun getSchedulesByDate(date: Instant): List<Schedule> {
        Log.d(TAG, "getSchedulesByDate: $date")
        val dayStart = date.truncatedTo(ChronoUnit.DAYS)
        val dayEnd = dayStart.plus(1, ChronoUnit.DAYS)
        return getSchedulesByTimeRange(dayStart, dayEnd)
    }

    override suspend fun searchSchedulesByTitle(title: String, limit: Int): List<Schedule> {
        Log.d(TAG, "searchSchedulesByTitle: title='$title', limit=$limit")
        return try {
            getAllSchedules().filter { schedule ->
                schedule.title.value.contains(title, ignoreCase = true)
            }.take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "searchSchedulesByTitle failed", e)
            emptyList()
        }
    }

    override suspend fun getAllSchedules(limit: Int?): List<Schedule> {
        Log.d(TAG, "getAllSchedules: limit=$limit")
        return try {
            localSchedulesDataSource.getAllSchedules(limit)
        } catch (e: Exception) {
            Log.e(TAG, "getAllSchedules failed", e)
            emptyList()
        }
    }

    override suspend fun getSchedulesByIds(scheduleIds: List<String>): List<Schedule> {
        Log.d(TAG, "getSchedulesByIds: ${scheduleIds.size} schedules")
        return try {
            localSchedulesDataSource.getSchedulesByIds(scheduleIds)
        } catch (e: Exception) {
            Log.e(TAG, "getSchedulesByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getUpcomingSchedules(): List<Schedule> {
        Log.d(TAG, "getUpcomingSchedules")
        return try {
            val now = Instant.now()
            getAllSchedules().filter { it.startTime.isAfter(now) }
                .sortedBy { it.startTime }
        } catch (e: Exception) {
            Log.e(TAG, "getUpcomingSchedules failed", e)
            emptyList()
        }
    }

    override suspend fun getTodaySchedules(): List<Schedule> {
        Log.d(TAG, "getTodaySchedules")
        return getSchedulesByDate(Instant.now())
    }

    override suspend fun getCompletedSchedulesInRange(
        startTime: Instant,
        endTime: Instant
    ): List<Schedule> {
        Log.d(TAG, "getCompletedSchedulesInRange: $startTime to $endTime")
        return try {
            getSchedulesByTimeRange(startTime, endTime).filter { schedule ->
                schedule.status == ScheduleStatus.CONFIRMED // Assuming CONFIRMED means completed
            }
        } catch (e: Exception) {
            Log.e(TAG, "getCompletedSchedulesInRange failed", e)
            emptyList()
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveSchedule(schedule: Schedule): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveSchedule: ${schedule.id}")

            // 1. Room DB에 저장
            localSchedulesDataSource.saveSchedule(schedule)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (schedule.isNew) "CREATE" else "UPDATE"
            localSchedulesDataSource.addToOutbox(
                scheduleId = schedule.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            Log.d(TAG, "Schedule saved and added to outbox: ${schedule.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveSchedule failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveSchedules(schedules: List<Schedule>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveSchedules: ${schedules.size} schedules")

            if (schedules.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localSchedulesDataSource.saveSchedules(schedules)

            Log.d(TAG, "Bulk schedules saved: ${schedules.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveSchedules failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteSchedule(scheduleId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteSchedule: $scheduleId")

            // 1. Room DB에서 삭제 (실제로는 soft delete)
            localSchedulesDataSource.deleteSchedule(scheduleId)

            // 2. Outbox에 삭제 작업 추가
            localSchedulesDataSource.addToOutbox(
                scheduleId = scheduleId,
                operation = "DELETE",
                payload = null
            )

            Log.d(TAG, "Schedule deleted and added to outbox: $scheduleId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteSchedule failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteSchedulesByProject(projectId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteSchedulesByProject: $projectId")

            localSchedulesDataSource.deleteSchedulesByProject(projectId)

            Log.d(TAG, "Schedules deleted for project: $projectId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteSchedulesByProject failed", e)
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
            Log.d(TAG, "updateSchedule: scheduleId=$scheduleId")

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

            Log.d(TAG, "Schedule updated: $scheduleId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateSchedule failed", e)
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
            Log.e(TAG, "scheduleExists failed", e)
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
            Log.e(TAG, "titleExistsInProject failed", e)
            false
        }
    }

    override suspend fun getScheduleCountByProject(projectId: String): Int {
        return try {
            localSchedulesDataSource.getScheduleCountByProject(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getScheduleCountByProject failed", e)
            0
        }
    }

    override suspend fun getScheduleCountByOwner(ownerId: String): Int {
        return try {
            localSchedulesDataSource.getScheduleCountByOwner(ownerId)
        } catch (e: Exception) {
            Log.e(TAG, "getScheduleCountByOwner failed", e)
            0
        }
    }

    override suspend fun getScheduleCountByStatus(status: ScheduleStatus): Int {
        return try {
            localSchedulesDataSource.getScheduleCountByStatus(status)
        } catch (e: Exception) {
            Log.e(TAG, "getScheduleCountByStatus failed", e)
            0
        }
    }

    override suspend fun getTotalScheduleCount(): Int {
        return try {
            localSchedulesDataSource.getTotalScheduleCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalScheduleCount failed", e)
            0
        }
    }

    override suspend fun getScheduleCountByDate(date: Instant): Int {
        return try {
            getSchedulesByDate(date).size
        } catch (e: Exception) {
            Log.e(TAG, "getScheduleCountByDate failed", e)
            0
        }
    }

    override suspend fun clearAllSchedules(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllSchedules")

            localSchedulesDataSource.clearAllSchedules()

            Log.d(TAG, "All schedules cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "clearAllSchedules failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getSchedulesUpdatedAfter(timestamp: Instant): List<Schedule> {
        return try {
            localSchedulesDataSource.getSchedulesUpdatedAfter(timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "getSchedulesUpdatedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun addToOutbox(
        scheduleId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "addToOutbox: scheduleId=$scheduleId, operation=$operation")

            localSchedulesDataSource.addToOutbox(scheduleId, operation, payload)

            Log.d(TAG, "Added to outbox: $scheduleId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}
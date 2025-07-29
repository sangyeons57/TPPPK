package com.example.domain.repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Schedule
import com.example.domain.model.enum.ScheduleStatus
import com.example.domain.model.vo.schedule.ScheduleContent
import com.example.domain.model.vo.schedule.ScheduleTitle
import com.example.domain.repository.local.base.BaseLocalRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Local Schedule Repository Interface (SSOT)
 * BaseLocalRepository 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지 (Remote ScheduleRepository 사용)
 *
 * ✅ 역할:
 * - BaseLocalRepository의 공통 CRUD 기능 상속 (80%)
 * - Schedule 도메인 특화 기능만 추가 정의 (20%)
 * - Flow로 UI에 실시간 데이터 제공 (Observer Pattern)
 * - Outbox 관리 (동기화 대상 저장)
 *
 * 📋 BaseLocalRepository 상속 메서드:
 * - observeEntityById -> observeScheduleById
 * - observeAllEntities -> observeAllSchedules
 * - observeEntityUpdatedAt -> observeScheduleUpdatedAt
 * - getEntityById -> getScheduleById
 * - getEntitiesByIds -> getSchedulesByIds
 * - getAllEntities -> getAllSchedules
 * - saveEntity -> saveSchedule
 * - saveEntities -> saveSchedules
 * - deleteEntity -> deleteSchedule
 * - Plus SyncableRepository methods (addToOutbox, clearAllEntities, etc.)
 */
interface LocalScheduleRepository : BaseLocalRepository<Schedule> {

    // === BaseLocalRepository 메서드 (구현체에서 일정 전용 메서드로 매핑) ===
    // observeEntityById -> observeScheduleById
    // observeAllEntities -> observeAllSchedules  
    // observeEntityUpdatedAt -> observeScheduleUpdatedAt
    // getEntityById -> getScheduleById
    // getEntitiesByIds -> getSchedulesByIds
    // getAllEntities -> getAllSchedules
    // saveEntity -> saveSchedule
    // saveEntities -> saveSchedules
    // deleteEntity -> deleteSchedule
    // getEntitiesUpdatedAfter -> getSchedulesUpdatedAfter
    // clearAllEntities -> clearAllSchedules
    // getTotalEntityCount -> getTotalScheduleCount
    // entityExists -> scheduleExists

    // === 관찰자 패턴 (UI 반응형) ===

    /**
     * 특정 일정을 실시간 관찰
     * @param scheduleId 일정 ID
     * @return 일정 Flow (null 가능)
     */
    fun observeScheduleById(scheduleId: String): Flow<Schedule?>

    /**
     * 프로젝트의 모든 일정을 실시간 관찰
     * @param projectId 프로젝트 ID
     * @return 일정 목록 Flow
     */
    fun observeSchedulesByProject(projectId: String): Flow<List<Schedule>>

    /**
     * 소유자의 모든 일정을 실시간 관찰
     * @param ownerId 소유자 ID
     * @return 일정 목록 Flow
     */
    fun observeSchedulesByOwner(ownerId: String): Flow<List<Schedule>>

    /**
     * 상태별 일정을 실시간 관찰
     * @param status 일정 상태
     * @return 일정 목록 Flow
     */
    fun observeSchedulesByStatus(status: ScheduleStatus): Flow<List<Schedule>>

    /**
     * 특정 시간 범위의 일정을 실시간 관찰
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 일정 목록 Flow
     */
    fun observeSchedulesByTimeRange(startTime: Instant, endTime: Instant): Flow<List<Schedule>>

    /**
     * 특정 날짜의 일정을 실시간 관찰
     * @param date 날짜 (해당 날짜의 시작부터 끝까지)
     * @return 일정 목록 Flow
     */
    fun observeSchedulesByDate(date: Instant): Flow<List<Schedule>>

    /**
     * 제목으로 일정 검색을 실시간 관찰
     * @param title 검색할 제목
     * @param limit 제한 개수
     * @return 일정 목록 Flow
     */
    fun observeSchedulesByTitle(title: String, limit: Int = 10): Flow<List<Schedule>>

    /**
     * 모든 일정을 실시간 관찰
     * @return 전체 일정 목록 Flow
     */
    fun observeAllSchedules(): Flow<List<Schedule>>

    /**
     * 특정 일정의 updatedAt 필드 변경을 실시간 관찰
     * @param scheduleId 일정 ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observeScheduleUpdatedAt(scheduleId: String): Flow<Long?>

    /**
     * 주어진 ID 목록에 해당하는 일정 목록을 실시간 관찰
     * @param scheduleIds 일정 ID 목록
     * @return 일정 목록 Flow
     */
    fun observeSchedules(scheduleIds: List<String>): Flow<List<Schedule>>

    /**
     * 예정된 일정들을 실시간 관찰 (현재 시간 이후)
     * @return 예정된 일정 목록 Flow
     */
    fun observeUpcomingSchedules(): Flow<List<Schedule>>

    /**
     * 오늘의 일정들을 실시간 관찰
     * @return 오늘의 일정 목록 Flow
     */
    fun observeTodaySchedules(): Flow<List<Schedule>>

    // === 단순 읽기 작업 ===

    /**
     * 일정 ID로 조회
     * @param scheduleId 일정 ID
     * @return 일정 (없으면 null)
     */
    suspend fun getScheduleById(scheduleId: String): Schedule?

    /**
     * 프로젝트의 모든 일정 조회
     * @param projectId 프로젝트 ID
     * @return 일정 목록
     */
    suspend fun getSchedulesByProject(projectId: String): List<Schedule>

    /**
     * 소유자의 모든 일정 조회
     * @param ownerId 소유자 ID
     * @return 일정 목록
     */
    suspend fun getSchedulesByOwner(ownerId: String): List<Schedule>

    /**
     * 상태별 일정 조회
     * @param status 일정 상태
     * @return 일정 목록
     */
    suspend fun getSchedulesByStatus(status: ScheduleStatus): List<Schedule>

    /**
     * 특정 시간 범위의 일정 조회
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 일정 목록
     */
    suspend fun getSchedulesByTimeRange(startTime: Instant, endTime: Instant): List<Schedule>

    /**
     * 특정 날짜의 일정 조회
     * @param date 날짜
     * @return 일정 목록
     */
    suspend fun getSchedulesByDate(date: Instant): List<Schedule>

    /**
     * 제목으로 일정 검색
     * @param title 검색할 제목
     * @param limit 제한 개수
     * @return 일정 목록
     */
    suspend fun searchSchedulesByTitle(title: String, limit: Int = 10): List<Schedule>

    /**
     * 모든 일정 조회
     * @param limit 제한 개수 (null이면 전체)
     * @return 일정 목록
     */
    suspend fun getAllSchedules(limit: Int? = null): List<Schedule>

    /**
     * 여러 일정 ID로 조회
     * @param scheduleIds 일정 ID 목록
     * @return 일정 목록
     */
    suspend fun getSchedulesByIds(scheduleIds: List<String>): List<Schedule>

    /**
     * 예정된 일정들 조회 (현재 시간 이후)
     * @return 예정된 일정 목록
     */
    suspend fun getUpcomingSchedules(): List<Schedule>

    /**
     * 오늘의 일정들 조회
     * @return 오늘의 일정 목록
     */
    suspend fun getTodaySchedules(): List<Schedule>

    /**
     * 특정 기간 내 완료된 일정들 조회
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 완료된 일정 목록
     */
    suspend fun getCompletedSchedulesInRange(startTime: Instant, endTime: Instant): List<Schedule>

    // === 쓰기 작업 (Outbox 포함) ===

    /**
     * 일정 저장 (생성/수정)
     * @param schedule 저장할 일정
     * @return 성공 여부
     */
    suspend fun saveSchedule(schedule: Schedule): CustomResult<Unit, Exception>

    /**
     * 일정 대량 저장 (동기화용)
     * @param schedules 저장할 일정 목록
     * @return 성공 여부
     */
    suspend fun saveSchedules(schedules: List<Schedule>): CustomResult<Unit, Exception>

    /**
     * 일정 삭제 (Soft Delete)
     * @param scheduleId 일정 ID
     * @return 성공 여부
     */
    suspend fun deleteSchedule(scheduleId: String): CustomResult<Unit, Exception>

    /**
     * 프로젝트별 일정 일괄 삭제
     * @param projectId 프로젝트 ID
     * @return 성공 여부
     */
    suspend fun deleteSchedulesByProject(projectId: String): CustomResult<Unit, Exception>

    /**
     * 일정 정보 업데이트 (로컬)
     * @param scheduleId 일정 ID
     * @param title 새로운 제목 (nullable)
     * @param content 새로운 내용 (nullable)
     * @param startTime 새로운 시작 시간 (nullable)
     * @param endTime 새로운 종료 시간 (nullable)
     * @param status 새로운 상태 (nullable)
     * @return 성공 여부
     */
    suspend fun updateSchedule(
        scheduleId: String,
        title: ScheduleTitle? = null,
        content: ScheduleContent? = null,
        startTime: Instant? = null,
        endTime: Instant? = null,
        status: ScheduleStatus? = null
    ): CustomResult<Unit, Exception>

    /**
     * 일정 세부사항 업데이트
     * @param scheduleId 일정 ID
     * @param title 새로운 제목
     * @param content 새로운 내용
     * @return 성공 여부
     */
    suspend fun updateScheduleDetails(
        scheduleId: String,
        title: ScheduleTitle,
        content: ScheduleContent
    ): CustomResult<Unit, Exception>

    /**
     * 일정 재조정
     * @param scheduleId 일정 ID
     * @param newStartTime 새로운 시작 시간
     * @param newEndTime 새로운 종료 시간
     * @return 성공 여부
     */
    suspend fun reschedule(
        scheduleId: String,
        newStartTime: Instant,
        newEndTime: Instant
    ): CustomResult<Unit, Exception>

    /**
     * 일정 상태 변경
     * @param scheduleId 일정 ID
     * @param newStatus 새로운 상태
     * @return 성공 여부
     */
    suspend fun changeScheduleStatus(
        scheduleId: String,
        newStatus: ScheduleStatus
    ): CustomResult<Unit, Exception>

    // === 유틸리티 ===

    /**
     * 일정 존재 여부 확인
     * @param scheduleId 일정 ID
     * @return 존재 여부
     */
    suspend fun scheduleExists(scheduleId: String): Boolean

    /**
     * 제목 중복 확인 (같은 프로젝트 내)
     * @param projectId 프로젝트 ID
     * @param title 일정 제목
     * @param excludeScheduleId 제외할 일정 ID (수정시 자기 자신 제외)
     * @return 중복 여부
     */
    suspend fun titleExistsInProject(
        projectId: String,
        title: ScheduleTitle,
        excludeScheduleId: String? = null
    ): Boolean

    /**
     * 프로젝트별 일정 수 조회
     * @param projectId 프로젝트 ID
     * @return 일정 수
     */
    suspend fun getScheduleCountByProject(projectId: String): Int

    /**
     * 소유자별 일정 수 조회
     * @param ownerId 소유자 ID
     * @return 일정 수
     */
    suspend fun getScheduleCountByOwner(ownerId: String): Int

    /**
     * 상태별 일정 수 조회
     * @param status 일정 상태
     * @return 해당 상태의 일정 수
     */
    suspend fun getScheduleCountByStatus(status: ScheduleStatus): Int

    /**
     * 전체 일정 수 조회
     * @return 일정 수
     */
    suspend fun getTotalScheduleCount(): Int

    /**
     * 특정 날짜의 일정 수 조회
     * @param date 날짜
     * @return 해당 날짜의 일정 수
     */
    suspend fun getScheduleCountByDate(date: Instant): Int

    /**
     * 모든 일정 삭제 (초기화)
     * @return 성공 여부
     */
    suspend fun clearAllSchedules(): CustomResult<Unit, Exception>

    // === 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 일정 조회
     * @param timestamp 기준 시간
     * @return 업데이트된 일정 목록
     */
    suspend fun getSchedulesUpdatedAfter(timestamp: Instant): List<Schedule>

}
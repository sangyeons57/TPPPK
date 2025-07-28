package com.example.domain.repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Task
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.task.TaskContent
import com.example.domain.model.vo.task.TaskOrder
import com.example.domain.model.vo.task.TaskStatus
import com.example.domain.model.vo.task.TaskType
import com.example.domain.repository.local.base.BaseLocalRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Local Task Repository Interface (SSOT)
 * BaseLocalRepository 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지 (Remote TaskRepository 사용)
 *
 * ✅ 역할:
 * - BaseLocalRepository의 공통 CRUD 기능 상속 (80%)
 * - Task 도메인 특화 기능만 추가 정의 (20%)
 * - Flow로 UI에 실시간 데이터 제공 (Observer Pattern)
 * - Outbox 관리 (동기화 대상 저장)
 *
 * 📋 BaseLocalRepository 상속 메서드:
 * - observeEntityById -> observeTaskById
 * - observeAllEntities -> observeAllTasks
 * - observeEntityUpdatedAt -> observeTaskUpdatedAt
 * - getEntityById -> getTaskById
 * - getEntitiesByIds -> getTasksByIds
 * - getAllEntities -> getAllTasks
 * - saveEntity -> saveTask
 * - saveEntities -> saveTasks
 * - deleteEntity -> deleteTask
 * - Plus SyncableRepository methods (addToOutbox, clearAllEntities, etc.)
 */
interface LocalTaskRepository : BaseLocalRepository<Task> {

    // === BaseLocalRepository 메서드 (구현체에서 작업 전용 메서드로 매핑) ===
    // observeEntityById -> observeTaskById
    // observeAllEntities -> observeAllTasks  
    // observeEntityUpdatedAt -> observeTaskUpdatedAt
    // getEntityById -> getTaskById
    // getEntitiesByIds -> getTasksByIds
    // getAllEntities -> getAllTasks
    // saveEntity -> saveTask
    // saveEntities -> saveTasks
    // deleteEntity -> deleteTask
    // getEntitiesUpdatedAfter -> getTasksUpdatedAfter
    // clearAllEntities -> clearAllTasks
    // getTotalEntityCount -> getTotalTaskCount
    // entityExists -> taskExists

    // === 관찰자 패턴 (UI 반응형) ===

    /**
     * 특정 작업을 실시간 관찰
     * @param taskId 작업 ID
     * @return 작업 Flow (null 가능)
     */
    fun observeTaskById(taskId: String): Flow<Task?>

    /**
     * 타입별 작업을 실시간 관찰
     * @param taskType 작업 타입
     * @return 작업 목록 Flow
     */
    fun observeTasksByType(taskType: TaskType): Flow<List<Task>>

    /**
     * 상태별 작업을 실시간 관찰
     * @param status 작업 상태
     * @return 작업 목록 Flow
     */
    fun observeTasksByStatus(status: TaskStatus): Flow<List<Task>>

    /**
     * 완료된 작업들을 실시간 관찰
     * @return 완료된 작업 목록 Flow
     */
    fun observeCompletedTasks(): Flow<List<Task>>

    /**
     * 진행 중인 작업들을 실시간 관찰
     * @return 진행 중인 작업 목록 Flow
     */
    fun observeInProgressTasks(): Flow<List<Task>>

    /**
     * 대기 중인 작업들을 실시간 관찰
     * @return 대기 중인 작업 목록 Flow
     */
    fun observePendingTasks(): Flow<List<Task>>

    /**
     * 특정 사용자가 체크한 작업들을 실시간 관찰
     * @param userId 사용자 ID
     * @return 체크된 작업 목록 Flow
     */
    fun observeTasksCheckedByUser(userId: String): Flow<List<Task>>

    /**
     * 순서별로 작업을 실시간 관찰
     * @return 순서대로 정렬된 작업 목록 Flow
     */
    fun observeTasksByOrder(): Flow<List<Task>>

    /**
     * 내용으로 작업 검색을 실시간 관찰
     * @param content 검색할 내용
     * @param limit 제한 개수
     * @return 작업 목록 Flow
     */
    fun observeTasksByContent(content: String, limit: Int = 10): Flow<List<Task>>

    /**
     * 모든 작업을 실시간 관찰
     * @return 전체 작업 목록 Flow
     */
    fun observeAllTasks(): Flow<List<Task>>

    /**
     * 특정 작업의 updatedAt 필드 변경을 실시간 관찰
     * @param taskId 작업 ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observeTaskUpdatedAt(taskId: String): Flow<Long?>

    /**
     * 주어진 ID 목록에 해당하는 작업 목록을 실시간 관찰
     * @param taskIds 작업 ID 목록
     * @return 작업 목록 Flow
     */
    fun observeTasks(taskIds: List<String>): Flow<List<Task>>

    /**
     * 특정 순서 범위의 작업들을 실시간 관찰
     * @param minOrder 최소 순서
     * @param maxOrder 최대 순서
     * @return 작업 목록 Flow
     */
    fun observeTasksByOrderRange(minOrder: Int, maxOrder: Int): Flow<List<Task>>

    // === 단순 읽기 작업 ===

    /**
     * 작업 ID로 조회
     * @param taskId 작업 ID
     * @return 작업 (없으면 null)
     */
    suspend fun getTaskById(taskId: String): Task?

    /**
     * 타입별 작업 조회
     * @param taskType 작업 타입
     * @return 작업 목록
     */
    suspend fun getTasksByType(taskType: TaskType): List<Task>

    /**
     * 상태별 작업 조회
     * @param status 작업 상태
     * @return 작업 목록
     */
    suspend fun getTasksByStatus(status: TaskStatus): List<Task>

    /**
     * 완료된 작업들 조회
     * @return 완료된 작업 목록
     */
    suspend fun getCompletedTasks(): List<Task>

    /**
     * 진행 중인 작업들 조회
     * @return 진행 중인 작업 목록
     */
    suspend fun getInProgressTasks(): List<Task>

    /**
     * 대기 중인 작업들 조회
     * @return 대기 중인 작업 목록
     */
    suspend fun getPendingTasks(): List<Task>

    /**
     * 특정 사용자가 체크한 작업들 조회
     * @param userId 사용자 ID
     * @return 체크된 작업 목록
     */
    suspend fun getTasksCheckedByUser(userId: String): List<Task>

    /**
     * 순서별로 작업 조회
     * @return 순서대로 정렬된 작업 목록
     */
    suspend fun getTasksByOrder(): List<Task>

    /**
     * 내용으로 작업 검색
     * @param content 검색할 내용
     * @param limit 제한 개수
     * @return 작업 목록
     */
    suspend fun searchTasksByContent(content: String, limit: Int = 10): List<Task>

    /**
     * 모든 작업 조회
     * @param limit 제한 개수 (null이면 전체)
     * @return 작업 목록
     */
    suspend fun getAllTasks(limit: Int? = null): List<Task>

    /**
     * 여러 작업 ID로 조회
     * @param taskIds 작업 ID 목록
     * @return 작업 목록
     */
    suspend fun getTasksByIds(taskIds: List<String>): List<Task>

    /**
     * 특정 순서 범위의 작업들 조회
     * @param minOrder 최소 순서
     * @param maxOrder 최대 순서
     * @return 작업 목록
     */
    suspend fun getTasksByOrderRange(minOrder: Int, maxOrder: Int): List<Task>

    /**
     * 특정 시간 이후 체크된 작업들 조회
     * @param timestamp 기준 시간
     * @return 체크된 작업 목록
     */
    suspend fun getTasksCheckedAfter(timestamp: Instant): List<Task>

    // === 쓰기 작업 (Outbox 포함) ===

    /**
     * 작업 저장 (생성/수정)
     * @param task 저장할 작업
     * @return 성공 여부
     */
    suspend fun saveTask(task: Task): CustomResult<Unit, Exception>

    /**
     * 작업 대량 저장 (동기화용)
     * @param tasks 저장할 작업 목록
     * @return 성공 여부
     */
    suspend fun saveTasks(tasks: List<Task>): CustomResult<Unit, Exception>

    /**
     * 작업 삭제 (Soft Delete)
     * @param taskId 작업 ID
     * @return 성공 여부
     */
    suspend fun deleteTask(taskId: String): CustomResult<Unit, Exception>

    /**
     * 작업 정보 업데이트 (로컬)
     * @param taskId 작업 ID
     * @param taskType 새로운 작업 타입 (nullable)
     * @param status 새로운 상태 (nullable)
     * @param content 새로운 내용 (nullable)
     * @param order 새로운 순서 (nullable)
     * @param checkedBy 체크한 사용자 (nullable)
     * @param checkedAt 체크한 시간 (nullable)
     * @return 성공 여부
     */
    suspend fun updateTask(
        taskId: String,
        taskType: TaskType? = null,
        status: TaskStatus? = null,
        content: TaskContent? = null,
        order: TaskOrder? = null,
        checkedBy: UserId? = null,
        checkedAt: Instant? = null
    ): CustomResult<Unit, Exception>

    /**
     * 작업 타입 업데이트
     * @param taskId 작업 ID
     * @param newTaskType 새로운 작업 타입
     * @return 성공 여부
     */
    suspend fun updateTaskType(taskId: String, newTaskType: TaskType): CustomResult<Unit, Exception>

    /**
     * 작업 타입 업데이트 (체크 정보 포함)
     * @param taskId 작업 ID
     * @param newTaskType 새로운 작업 타입
     * @param checkedBy 체크한 사용자
     * @param checkedAt 체크한 시간
     * @return 성공 여부
     */
    suspend fun updateTaskTypeWithCheck(
        taskId: String,
        newTaskType: TaskType,
        checkedBy: UserId?,
        checkedAt: Instant?
    ): CustomResult<Unit, Exception>

    /**
     * 작업 상태 업데이트
     * @param taskId 작업 ID
     * @param newStatus 새로운 상태
     * @return 성공 여부
     */
    suspend fun updateTaskStatus(
        taskId: String,
        newStatus: TaskStatus
    ): CustomResult<Unit, Exception>

    /**
     * 작업 내용 업데이트
     * @param taskId 작업 ID
     * @param newContent 새로운 내용
     * @return 성공 여부
     */
    suspend fun updateTaskContent(
        taskId: String,
        newContent: TaskContent
    ): CustomResult<Unit, Exception>

    /**
     * 작업 순서 업데이트
     * @param taskId 작업 ID
     * @param newOrder 새로운 순서
     * @return 성공 여부
     */
    suspend fun updateTaskOrder(taskId: String, newOrder: TaskOrder): CustomResult<Unit, Exception>

    /**
     * 작업 완료 처리
     * @param taskId 작업 ID
     * @return 성공 여부
     */
    suspend fun completeTask(taskId: String): CustomResult<Unit, Exception>

    /**
     * 작업 진행 중으로 변경
     * @param taskId 작업 ID
     * @return 성공 여부
     */
    suspend fun startTaskProgress(taskId: String): CustomResult<Unit, Exception>

    /**
     * 작업을 대기 중으로 변경
     * @param taskId 작업 ID
     * @return 성공 여부
     */
    suspend fun markTaskAsPending(taskId: String): CustomResult<Unit, Exception>

    /**
     * 작업 순서 재정렬
     * @param taskOrderMap 작업 ID와 새로운 순서 매핑
     * @return 성공 여부
     */
    suspend fun reorderTasks(taskOrderMap: Map<String, Int>): CustomResult<Unit, Exception>

    // === 유틸리티 ===

    /**
     * 작업 존재 여부 확인
     * @param taskId 작업 ID
     * @return 존재 여부
     */
    suspend fun taskExists(taskId: String): Boolean

    /**
     * 내용 중복 확인
     * @param content 작업 내용
     * @param excludeTaskId 제외할 작업 ID (수정시 자기 자신 제외)
     * @return 중복 여부
     */
    suspend fun contentExists(content: TaskContent, excludeTaskId: String? = null): Boolean

    /**
     * 타입별 작업 수 조회
     * @param taskType 작업 타입
     * @return 해당 타입의 작업 수
     */
    suspend fun getTaskCountByType(taskType: TaskType): Int

    /**
     * 상태별 작업 수 조회
     * @param status 작업 상태
     * @return 해당 상태의 작업 수
     */
    suspend fun getTaskCountByStatus(status: TaskStatus): Int

    /**
     * 전체 작업 수 조회
     * @return 작업 수
     */
    suspend fun getTotalTaskCount(): Int

    /**
     * 완료된 작업 수 조회
     * @return 완료된 작업 수
     */
    suspend fun getCompletedTaskCount(): Int

    /**
     * 진행 중인 작업 수 조회
     * @return 진행 중인 작업 수
     */
    suspend fun getInProgressTaskCount(): Int

    /**
     * 대기 중인 작업 수 조회
     * @return 대기 중인 작업 수
     */
    suspend fun getPendingTaskCount(): Int

    /**
     * 다음 작업 순서 조회
     * @return 다음 순서 번호
     */
    suspend fun getNextTaskOrder(): Int

    /**
     * 모든 작업 삭제 (초기화)
     * @return 성공 여부
     */
    suspend fun clearAllTasks(): CustomResult<Unit, Exception>

    // === 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 작업 조회
     * @param timestamp 기준 시간
     * @return 업데이트된 작업 목록
     */
    suspend fun getTasksUpdatedAfter(timestamp: Instant): List<Task>

    /**
     * Outbox에 작업 추가 (서버 동기화 대기열)
     * @param taskId 작업 ID
     * @param operation 작업 타입 (CREATE, UPDATE, DELETE)
     * @param payload 작업 데이터 (JSON)
     * @return 성공 여부
     */
    suspend fun addToOutbox(
        taskId: String,
        operation: String,
        payload: String? = null
    ): CustomResult<Unit, Exception>
}
package com.example.domain_repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.enum.OutBoxStatus
import com.example.domain.model.sync.OutBox

/**
 * OutBox 관련 Repository 인터페이스
 * UseCase에서 OutBox 처리를 위한 도메인 계층 인터페이스
 */
interface OutBoxRepository {

    // ================================
    // UseCase용 - OutBox 추가/수정
    // ================================

    /**
     * 새로운 OutBox를 추가합니다.
     * @param outBox 추가할 OutBox
     * @return 성공/실패 결과
     */
    suspend fun insert(outBox: OutBox<*>): CustomResult<Unit, Exception>

    /**
     * 여러 OutBox를 일괄 추가합니다.
     * @param outBoxes 추가할 OutBox 목록
     * @return 성공/실패 결과
     */
    suspend fun insertAll(outBoxes: List<OutBox<*>>): CustomResult<Unit, Exception>

    /**
     * OutBox를 업데이트합니다.
     * @param outBox 업데이트할 OutBox
     * @return 성공/실패 결과
     */
    suspend fun update(outBox: OutBox<*>): CustomResult<Unit, Exception>

    // ================================
    // SyncManager용 - 조회 작업
    // ================================

    /**
     * ID로 OutBox를 조회합니다.
     * @param id OutBox ID
     * @return OutBox 또는 null
     */
    suspend fun getById(id: String): CustomResult<OutBox<*>?, Exception>

    /**
     * 특정 상태의 OutBox들을 조회합니다.
     * @param status OutBox 상태
     * @return OutBox 목록
     */
    suspend fun getByStatus(status: OutBoxStatus): CustomResult<List<OutBox<*>>, Exception>

    /**
     * 여러 상태의 OutBox들을 조회합니다.
     * @param statuses OutBox 상태 목록
     * @return OutBox 목록
     */
    suspend fun getByStatuses(statuses: List<OutBoxStatus>): CustomResult<List<OutBox<*>>, Exception>

    /**
     * 특정 엔티티 타입과 ID의 OutBox들을 조회합니다.
     * @param entityType 엔티티 타입
     * @param entityId 엔티티 ID
     * @return OutBox 목록
     */
    suspend fun getByEntityTypeAndId(
        entityType: String,
        entityId: String
    ): CustomResult<List<OutBox<*>>, Exception>

    // ================================
    // SyncManager용 - 배치 처리
    // ================================

    /**
     * 대기 중인 OutBox 작업들을 조회합니다.
     * @param limit 최대 조회 개수
     * @return OutBox 목록
     */
    suspend fun getPendingOperations(limit: Int): CustomResult<List<OutBox<*>>, Exception>

    /**
     * 상태별 OutBox ID들의 상태를 일괄 업데이트합니다.
     * @param ids 업데이트할 OutBox ID 목록
     * @param newStatus 새로운 상태
     * @return 업데이트된 개수
     */
    suspend fun updateStatusByIds(
        ids: List<String>,
        newStatus: OutBoxStatus
    ): CustomResult<Int, Exception>

    /**
     * 재시도 가능한 실패 상태의 OutBox들을 PENDING으로 리셋합니다.
     * @return 리셋된 개수
     */
    suspend fun resetRetryableFailedOperations(): CustomResult<Int, Exception>

    // ================================
    // SyncManager용 - 정리 및 관리
    // ================================

    /**
     * 완료된 OutBox들을 삭제합니다.
     * @return 삭제된 개수
     */
    suspend fun deleteCompleted(): CustomResult<Int, Exception>

    /**
     * OutBox를 삭제합니다.
     * @param outBox 삭제할 OutBox
     * @return 성공/실패 결과
     */
    suspend fun delete(outBox: OutBox<*>): CustomResult<Unit, Exception>

    /**
     * 만료된 OutBox들을 삭제합니다.
     * @param timeoutMs 타임아웃 시간 (밀리초)
     * @return 삭제된 개수
     */
    suspend fun deleteExpiredOperations(timeoutMs: Long): CustomResult<Int, Exception>

    // ================================
    // SyncManager용 - 통계 및 모니터링
    // ================================

    /**
     * 특정 상태의 OutBox 개수를 조회합니다.
     * @param status OutBox 상태
     * @return OutBox 개수
     */
    suspend fun getCountByStatus(status: OutBoxStatus): CustomResult<Int, Exception>

    /**
     * 전체 OutBox 개수를 조회합니다.
     * @return 전체 OutBox 개수
     */
    suspend fun getTotalCount(): CustomResult<Int, Exception>

    /**
     * OutBox 상태별 통계를 조회합니다.
     * @return 상태별 OutBox 개수 맵
     */
    suspend fun getStatusStatistics(): CustomResult<Map<OutBoxStatus, Int>, Exception>

    // ================================
    // 테스트 및 디버깅용
    // ================================

    /**
     * 디버깅용으로 모든 OutBox를 조회합니다.
     * @return 생성시간 내림차순으로 정렬된 모든 OutBox 목록
     */
    suspend fun getAllForDebug(): CustomResult<List<OutBox<*>>, Exception>

    /**
     * 모든 OutBox를 삭제합니다.
     * @return 성공/실패 결과
     */
    suspend fun deleteAll(): CustomResult<Unit, Exception>
}
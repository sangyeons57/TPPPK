package com.example.data_core.datasource.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.enum.OutBoxStatus
import com.example.domain.model.sync.OutBox

/**
 * OutBox 로컬 데이터 소스 인터페이스
 * Repository에서 트랜잭션 내 OutBox 추가 및 SyncManager에서 도메인 모델 처리용
 */
interface OutBoxDataSource {

    // ================================
    // Repository용 - 트랜잭션 내 OutBox 추가
    // ================================
    
    /**
     * 단일 OutBox 작업 추가 (Repository 트랜잭션 내에서 사용)
     * @param outBox 추가할 OutBox 도메인 모델
     * @return 성공/실패 결과
     */
    suspend fun insert(outBox: OutBox): CustomResult<Unit, Exception>
    
    /**
     * 여러 OutBox 작업들을 배치로 추가 (Repository 트랜잭션 내에서 사용)
     * @param outBoxes 추가할 OutBox 도메인 모델 목록
     * @return 성공/실패 결과
     */
    suspend fun insertAll(outBoxes: List<OutBox>): CustomResult<Unit, Exception>

    // ================================
    // SyncManager용 - 도메인 모델 처리
    // ================================
    
    /**
     * ID로 특정 OutBox 작업 조회
     * @param id OutBox ID
     * @return OutBox 도메인 모델 (없으면 null)
     */
    suspend fun getById(id: String): CustomResult<OutBox?, Exception>
    
    /**
     * 특정 상태의 OutBox 작업들을 우선순위 순으로 조회
     * @param status 조회할 상태
     * @return 우선순위 오름차순으로 정렬된 OutBox 목록
     */
    suspend fun getByStatus(status: OutBoxStatus): CustomResult<List<OutBox>, Exception>
    
    /**
     * 여러 상태의 OutBox 작업들을 우선순위 순으로 조회
     * @param statuses 조회할 상태 목록
     * @return 우선순위 오름차순으로 정렬된 OutBox 목록
     */
    suspend fun getByStatuses(statuses: List<OutBoxStatus>): CustomResult<List<OutBox>, Exception>
    
    /**
     * 특정 엔티티의 OutBox 작업들 조회
     * @param entityType 엔티티 타입
     * @param entityId 엔티티 ID
     * @return 해당 엔티티의 OutBox 목록
     */
    suspend fun getByEntityTypeAndId(entityType: String, entityId: String): CustomResult<List<OutBox>, Exception>

    // ================================
    // SyncManager용 - 배치 처리
    // ================================
    
    /**
     * 대기 중인 OutBox 작업들을 제한된 개수만큼 조회 (SyncManager 배치 처리용)
     * @param limit 조회할 최대 개수
     * @return 우선순위 오름차순으로 정렬된 대기 중인 OutBox 목록
     */
    suspend fun getPendingOperations(limit: Int): CustomResult<List<OutBox>, Exception>
    
    /**
     * OutBox 작업 상태 업데이트 (SyncManager에서 처리 상태 변경용)
     * @param outBox 업데이트할 OutBox 도메인 모델
     * @return 성공/실패 결과
     */
    suspend fun update(outBox: OutBox): CustomResult<Unit, Exception>
    
    /**
     * 여러 OutBox 작업들의 상태를 일괄 업데이트
     * @param ids 업데이트할 OutBox ID 목록
     * @param newStatus 새로운 상태
     * @return 업데이트된 작업 개수
     */
    suspend fun updateStatusByIds(ids: List<String>, newStatus: OutBoxStatus): CustomResult<Int, Exception>
    
    /**
     * 실패한 작업들 중 재시도 가능한 것들을 PENDING 상태로 복원
     * @return 복원된 작업 개수
     */
    suspend fun resetRetryableFailedOperations(): CustomResult<Int, Exception>

    // ================================
    // SyncManager용 - 정리 및 관리
    // ================================
    
    /**
     * 완료된 OutBox 작업들을 삭제 (정리용)
     * @return 삭제된 작업 개수
     */
    suspend fun deleteCompleted(): CustomResult<Int, Exception>
    
    /**
     * OutBox 작업 삭제
     * @param outBox 삭제할 OutBox 도메인 모델
     * @return 성공/실패 결과
     */
    suspend fun delete(outBox: OutBox): CustomResult<Unit, Exception>
    
    /**
     * 만료된 OutBox 작업들을 삭제
     * @param timeoutMs 타임아웃 시간 (milliseconds)
     * @return 삭제된 작업 개수
     */
    suspend fun deleteExpiredOperations(timeoutMs: Long): CustomResult<Int, Exception>

    // ================================
    // SyncManager용 - 통계 및 모니터링
    // ================================
    
    /**
     * 특정 상태의 OutBox 작업 개수 조회
     * @param status 조회할 상태
     * @return 해당 상태의 작업 개수
     */
    suspend fun getCountByStatus(status: OutBoxStatus): CustomResult<Int, Exception>
    
    /**
     * 전체 OutBox 작업 개수 조회
     * @return 전체 작업 개수
     */
    suspend fun getTotalCount(): CustomResult<Int, Exception>
    
    /**
     * 상태별 OutBox 작업 통계 조회
     * @return 상태별 작업 개수 통계
     */
    suspend fun getStatusStatistics(): CustomResult<Map<OutBoxStatus, Int>, Exception>

    // ================================
    // 테스트 및 디버깅용
    // ================================
    
    /**
     * 모든 OutBox 작업을 생성시간 순으로 조회 (디버깅용)
     * @return 생성시간 내림차순으로 정렬된 모든 OutBox 목록
     */
    suspend fun getAllForDebug(): CustomResult<List<OutBox>, Exception>
    
    /**
     * 모든 OutBox 작업 삭제 (테스트/초기화용)
     * @return 성공/실패 결과
     */
    suspend fun deleteAll(): CustomResult<Unit, Exception>
}
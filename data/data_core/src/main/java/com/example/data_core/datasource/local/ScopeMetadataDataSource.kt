package com.example.data_core.datasource.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.sync.ScopeMetadata

/**
 * ScopeMetadata 로컬 데이터 소스 인터페이스
 * SyncManager에서 동기화 메타데이터 관리용
 */
interface ScopeMetadataDataSource {

    // ================================
    // 기본 CRUD 작업
    // ================================
    
    /**
     * ScopeMetadata 저장/업데이트
     * @param scopeMetadata 저장할 ScopeMetadata 도메인 모델
     * @return 성공/실패 결과
     */
    suspend fun save(scopeMetadata: ScopeMetadata): CustomResult<Unit, Exception>
    
    /**
     * 여러 ScopeMetadata들을 배치로 저장/업데이트
     * @param scopeMetadataList 저장할 ScopeMetadata 도메인 모델 목록
     * @return 성공/실패 결과
     */
    suspend fun saveAll(scopeMetadataList: List<ScopeMetadata>): CustomResult<Unit, Exception>
    
    /**
     * ScopeMetadata 삭제
     * @param scopeMetadata 삭제할 ScopeMetadata 도메인 모델
     * @return 성공/실패 결과
     */
    suspend fun delete(scopeMetadata: ScopeMetadata): CustomResult<Unit, Exception>

    // ================================
    // SyncManager용 - 키-값 조회
    // ================================
    
    /**
     * 키로 특정 ScopeMetadata 조회
     * @param key 조회할 메타데이터 키
     * @return ScopeMetadata 도메인 모델 (없으면 null)
     */
    suspend fun getByKey(key: String): CustomResult<ScopeMetadata?, Exception>
    
    /**
     * 모든 ScopeMetadata 조회
     * @return 모든 ScopeMetadata 도메인 모델 목록
     */
    suspend fun getAll(): CustomResult<List<ScopeMetadata>, Exception>
    
    /**
     * 키 패턴으로 ScopeMetadata 조회 (컬렉션별 메타데이터 조회용)
     * @param keyPattern LIKE 패턴 (예: "messages_%")
     * @return 패턴에 매치되는 ScopeMetadata 목록
     */
    suspend fun getByKeyPattern(keyPattern: String): CustomResult<List<ScopeMetadata>, Exception>
    
    /**
     * 특정 시간 이후에 동기화된 메타데이터들 조회
     * @param afterTimestampMs 기준 시간 (epoch milliseconds)
     * @return 해당 시간 이후 동기화된 메타데이터 목록
     */
    suspend fun getSyncedAfter(afterTimestampMs: Long): CustomResult<List<ScopeMetadata>, Exception>
    
    /**
     * 키의 존재 여부 확인
     * @param key 확인할 메타데이터 키
     * @return 존재 여부
     */
    suspend fun keyExists(key: String): CustomResult<Boolean, Exception>

    // ================================
    // SyncManager용 - 키 관리
    // ================================
    
    /**
     * 키로 특정 ScopeMetadata 삭제
     * @param key 삭제할 메타데이터 키
     * @return 삭제된 행의 개수
     */
    suspend fun deleteByKey(key: String): CustomResult<Int, Exception>
    
    /**
     * 키 패턴으로 ScopeMetadata 삭제 (컬렉션별 메타데이터 정리용)
     * @param keyPattern LIKE 패턴 (예: "messages_%")
     * @return 삭제된 행의 개수
     */
    suspend fun deleteByKeyPattern(keyPattern: String): CustomResult<Int, Exception>

    // ================================
    // SyncManager용 - 동기화 상태 관리
    // ================================
    
    /**
     * 에러가 발생한 메타데이터들 조회 (에러 복구용)
     * @param minErrorCount 최소 에러 개수 (기본값: 1)
     * @return 에러가 발생한 메타데이터 목록
     */
    suspend fun getErrorMetadata(minErrorCount: Long = 1): CustomResult<List<ScopeMetadata>, Exception>
    
    /**
     * 최근 동기화 활동이 활발한 메타데이터들 조회
     * @param minSyncCount 최소 동기화 횟수
     * @param afterTimestampMs 기준 시간 이후
     * @return 활발한 동기화 메타데이터 목록
     */
    suspend fun getActiveSyncMetadata(minSyncCount: Long, afterTimestampMs: Long): CustomResult<List<ScopeMetadata>, Exception>
    
    /**
     * 모든 메타데이터의 에러 카운트 초기화
     * @return 업데이트된 행의 개수
     */
    suspend fun resetAllErrorCounts(): CustomResult<Int, Exception>

    // ================================
    // SyncManager용 - 정리 및 관리
    // ================================
    
    /**
     * 전체 메타데이터 개수 조회
     * @return 전체 메타데이터 개수
     */
    suspend fun getTotalCount(): CustomResult<Int, Exception>
    
    /**
     * 오래된 메타데이터 정리 (특정 시간 이전 동기화된 것들)
     * @param beforeTimestampMs 기준 시간 이전 (epoch milliseconds)
     * @return 삭제된 행의 개수
     */
    suspend fun deleteOldMetadata(beforeTimestampMs: Long): CustomResult<Int, Exception>
    
    /**
     * 모든 ScopeMetadata 삭제 (테스트/초기화용)
     * @return 성공/실패 결과
     */
    suspend fun deleteAll(): CustomResult<Unit, Exception>

    // ================================
    // SyncManager용 - 통계 및 모니터링
    // ================================
    
    /**
     * 동기화 통계 조회 (총 동기화 횟수, 평균 에러 횟수 등)
     * @return 동기화 통계 정보
     */
    suspend fun getSyncStatistics(): CustomResult<SyncStatistics, Exception>

    // ================================
    // 테스트 및 디버깅용
    // ================================
    
    /**
     * 모든 ScopeMetadata를 최근 동기화 순으로 조회 (디버깅용)
     * @return 최근 동기화 시간 내림차순으로 정렬된 모든 메타데이터 목록
     */
    suspend fun getAllForDebug(): CustomResult<List<ScopeMetadata>, Exception>
}

/**
 * 동기화 통계 데이터 클래스
 */
data class SyncStatistics(
    val totalCount: Int,
    val totalSyncCount: Long,
    val avgSyncCount: Double,
    val totalErrorCount: Long,
    val avgErrorCount: Double,
    val lastSyncTimeMs: Long?
)
package com.example.domain.repository.infrastructure

import com.example.core_common.result.CustomResult

/**
 * Sync Metadata Repository Interface for production-level sync infrastructure
 * 컬렉션별 동기화 메타데이터 관리를 위한 공용 인프라 Repository
 *
 * 🎯 책임:
 * - 컬렉션별 동기화 커서 관리
 * - 클럭 스큐 측정 및 보정
 * - 동기화 상태 모니터링
 * - 성능 통계 수집
 *
 * 📋 아키텍처:
 * - Repository → DataSource → DAO 패턴 준수
 * - SyncManager에서 Pull 동기화시 사용
 * - 모든 컬렉션에 대해 독립적인 메타데이터 관리
 */
interface SyncMetaRepository {

    // === 동기화 커서 관리 ===

    /**
     * 마지막 동기화 커서 조회
     *
     * @param collectionName 컬렉션 이름
     * @return 마지막 서버 커서 (없으면 null)
     */
    suspend fun getLastSyncCursor(collectionName: String): CustomResult<Long?, Exception>

    /**
     * 동기화 커서 업데이트
     *
     * @param collectionName 컬렉션 이름
     * @param cursor 새 서버 커서
     * @param timestamp 동기화 완료 시간
     * @return 성공시 Unit, 실패시 Exception
     */
    suspend fun updateSyncCursor(
        collectionName: String,
        cursor: Long,
        timestamp: Long
    ): CustomResult<Unit, Exception>

    /**
     * 마지막 성공적인 동기화 시간 조회
     *
     * @param collectionName 컬렉션 이름
     * @return 마지막 성공 동기화 시간 (없으면 null)
     */
    suspend fun getLastSuccessfulSyncTime(collectionName: String): CustomResult<Long?, Exception>

    // === 클럭 스큐 관리 ===

    /**
     * 클럭 스큐 측정 및 저장
     *
     * @param collectionName 컬렉션 이름
     * @param serverTime 서버 시간
     * @param clientTime 클라이언트 시간
     * @return 측정된 클럭 스큐
     */
    suspend fun measureAndStoreClockSkew(
        collectionName: String,
        serverTime: Long,
        clientTime: Long
    ): CustomResult<Long, Exception>

    /**
     * 클럭 스큐 조회
     *
     * @param collectionName 컬렉션 이름
     * @return 클럭 스큐 (없으면 null)
     */
    suspend fun getClockSkew(collectionName: String): CustomResult<Long?, Exception>

    /**
     * 클라이언트 시간을 서버 시간으로 조정
     *
     * @param collectionName 컬렉션 이름
     * @param clientTime 클라이언트 시간
     * @return 조정된 시간
     */
    suspend fun adjustClientTime(
        collectionName: String,
        clientTime: Long
    ): CustomResult<Long, Exception>

    /**
     * 클럭 스큐가 측정이 필요한지 확인
     *
     * @param collectionName 컬렉션 이름
     * @return 측정 필요 여부
     */
    suspend fun needsClockSkewMeasurement(collectionName: String): CustomResult<Boolean, Exception>

    // === 동기화 상태 관리 ===

    /**
     * 동기화 성공 기록
     *
     * @param collectionName 컬렉션 이름
     * @param syncDurationMs 동기화 소요 시간
     * @return 성공시 Unit, 실패시 Exception
     */
    suspend fun recordSyncSuccess(
        collectionName: String,
        syncDurationMs: Long
    ): CustomResult<Unit, Exception>

    /**
     * 동기화 실패 기록
     *
     * @param collectionName 컬렉션 이름
     * @param errorMessage 실패 원인
     * @return 성공시 Unit, 실패시 Exception
     */
    suspend fun recordSyncFailure(
        collectionName: String,
        errorMessage: String
    ): CustomResult<Unit, Exception>

    /**
     * 동기화 상태가 건강한지 확인
     *
     * @param collectionName 컬렉션 이름
     * @return 건강 상태
     */
    suspend fun isSyncHealthy(collectionName: String): CustomResult<Boolean, Exception>

    /**
     * 연속 실패 횟수 조회
     *
     * @param collectionName 컬렉션 이름
     * @return 연속 실패 횟수
     */
    suspend fun getConsecutiveFailureCount(collectionName: String): CustomResult<Int, Exception>

    // === 동기화 필요 판단 ===

    /**
     * 동기화가 필요한 컬렉션들 조회
     *
     * @param maxAgeMs 동기화 필요 판단 기준 시간 (밀리초)
     * @return 동기화 필요한 컬렉션 목록
     */
    suspend fun getCollectionsNeedingSync(
        maxAgeMs: Long = 3600000L // 1시간
    ): CustomResult<List<String>, Exception>

    /**
     * 특정 컬렉션이 동기화가 필요한지 확인
     *
     * @param collectionName 컬렉션 이름
     * @param maxAgeMs 기준 시간
     * @return 동기화 필요 여부
     */
    suspend fun needsSync(
        collectionName: String,
        maxAgeMs: Long = 3600000L
    ): CustomResult<Boolean, Exception>

    // === 메타데이터 초기화 및 관리 ===

    /**
     * 컬렉션의 동기화 메타데이터 초기화
     *
     * @param collectionName 컬렉션 이름
     * @return 성공시 Unit, 실패시 Exception
     */
    suspend fun initializeSyncMetadata(collectionName: String): CustomResult<Unit, Exception>

    /**
     * 컬렉션의 동기화 메타데이터 존재 여부 확인
     *
     * @param collectionName 컬렉션 이름
     * @return 존재 여부
     */
    suspend fun hasSyncMetadata(collectionName: String): CustomResult<Boolean, Exception>

    /**
     * 컬렉션의 동기화 메타데이터 삭제
     *
     * @param collectionName 컬렉션 이름
     * @return 성공시 Unit, 실패시 Exception
     */
    suspend fun deleteSyncMetadata(collectionName: String): CustomResult<Unit, Exception>

    /**
     * 모든 동기화 메타데이터 삭제 (개발/테스트용)
     *
     * @return 성공시 Unit, 실패시 Exception
     */
    suspend fun clearAllSyncMetadata(): CustomResult<Unit, Exception>

    // === 통계 및 모니터링 ===

    /**
     * 전체 동기화 통계 조회
     *
     * @return 동기화 통계
     */
    suspend fun getAllSyncStatistics(): CustomResult<List<SyncCollectionStatistics>, Exception>

    /**
     * 특정 컬렉션의 동기화 통계 조회
     *
     * @param collectionName 컬렉션 이름
     * @return 동기화 통계
     */
    suspend fun getSyncStatistics(collectionName: String): CustomResult<SyncCollectionStatistics?, Exception>

    /**
     * 평균 동기화 시간 조회
     *
     * @param collectionName 컬렉션 이름
     * @return 평균 동기화 시간 (밀리초)
     */
    suspend fun getAverageSyncDuration(collectionName: String): CustomResult<Long?, Exception>

    // === 컬렉션 관리 ===

    /**
     * 등록된 모든 컬렉션 목록 조회
     *
     * @return 컬렉션 이름 목록
     */
    suspend fun getAllRegisteredCollections(): CustomResult<List<String>, Exception>

    /**
     * 컬렉션 등록 (자동 메타데이터 초기화)
     *
     * @param collectionName 컬렉션 이름
     * @return 성공시 Unit, 실패시 Exception
     */
    suspend fun registerCollection(collectionName: String): CustomResult<Unit, Exception>

    /**
     * 컬렉션 등록 해제
     *
     * @param collectionName 컬렉션 이름
     * @return 성공시 Unit, 실패시 Exception
     */
    suspend fun unregisterCollection(collectionName: String): CustomResult<Unit, Exception>
}

/**
 * 컬렉션별 동기화 통계
 */
data class SyncCollectionStatistics(
    val collectionName: String,
    val lastSyncCursor: Long,
    val lastSuccessfulSync: Long,
    val clockSkew: Long?,
    val clockSkewMeasuredAt: Long?,
    val consecutiveSuccessCount: Int,
    val consecutiveFailureCount: Int,
    val averageSyncDurationMs: Long?,
    val isHealthy: Boolean,
    val needsSync: Boolean,
    val hasAcceptableClockSkew: Boolean
)
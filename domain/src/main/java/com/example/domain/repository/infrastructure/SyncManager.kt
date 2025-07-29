package com.example.domain.repository.infrastructure

import com.example.core_common.result.CustomResult
import kotlinx.coroutines.flow.Flow

/**
 * SyncManager Interface for production-level SSOT synchronization orchestration
 * 전체 동기화 워크플로우를 조율하는 중앙 관리자
 *
 * 🎯 책임:
 * - Push 동기화: Outbox 작업을 서버로 전송
 * - Pull 동기화: 서버 변경사항을 로컬로 가져오기
 * - 충돌 해결 및 재시도 로직
 * - 동기화 상태 모니터링 및 헬스체크
 * - 적응형 동기화 간격 조정
 *
 * 📋 아키텍처:
 * - OutboxRepository와 SyncMetaRepository 조율
 * - Repository → DataSource → DAO 패턴 존중
 * - Clean Architecture 원칙 준수
 * - 도메인 로직과 인프라 로직 분리
 */
interface SyncManager {

    // === Push 동기화 (Local → Server) ===

    /**
     * 대기 중인 Outbox 작업들을 서버로 전송
     *
     * @param batchSize 한 번에 처리할 작업 수
     * @param leaseTimeoutMs 작업 임대 시간 (밀리초)
     * @return 처리된 작업 수
     */
    suspend fun processPendingOutboxOperations(
        batchSize: Int = 10,
        leaseTimeoutMs: Long = 300000L // 5분
    ): CustomResult<Int, Exception>

    /**
     * 특정 컬렉션의 Outbox 작업들만 처리
     *
     * @param collectionName 대상 컬렉션
     * @param batchSize 배치 크기
     * @return 처리된 작업 수
     */
    suspend fun processOutboxForCollection(
        collectionName: String,
        batchSize: Int = 10
    ): CustomResult<Int, Exception>

    /**
     * 실패한 Outbox 작업들 재시도
     *
     * @param maxRetries 최대 재시도 횟수
     * @return 재시도된 작업 수
     */
    suspend fun retryFailedOutboxOperations(
        maxRetries: Int = 3
    ): CustomResult<Int, Exception>

    // === Pull 동기화 (Server → Local) ===

    /**
     * 서버에서 증분 변경사항을 가져와 로컬에 적용
     *
     * @param collectionName 대상 컬렉션
     * @param batchSize 한 번에 가져올 문서 수
     * @return 동기화된 문서 수
     */
    suspend fun pullIncrementalChanges(
        collectionName: String,
        batchSize: Int = 50
    ): CustomResult<Int, Exception>

    /**
     * 모든 등록된 컬렉션에 대해 증분 동기화 수행
     *
     * @param maxAgeMs 동기화 필요 판단 기준 시간
     * @return 동기화된 컬렉션 수
     */
    suspend fun pullChangesForAllCollections(
        maxAgeMs: Long = 3600000L // 1시간
    ): CustomResult<Int, Exception>

    /**
     * 특정 컬렉션의 전체 동기화 (초기 동기화 또는 복구용)
     *
     * @param collectionName 대상 컬렉션
     * @param forceFullSync 강제 전체 동기화 여부
     * @return 동기화된 문서 수
     */
    suspend fun pullFullSync(
        collectionName: String,
        forceFullSync: Boolean = false
    ): CustomResult<Int, Exception>

    // === 양방향 동기화 ===

    /**
     * 전체 동기화 실행 (Push + Pull)
     *
     * @param pushFirst Push를 먼저 실행할지 여부
     * @return 동기화 결과 요약
     */
    suspend fun performFullSync(
        pushFirst: Boolean = true
    ): CustomResult<SyncResult, Exception>

    /**
     * 특정 컬렉션에 대한 양방향 동기화
     *
     * @param collectionName 대상 컬렉션
     * @return 동기화 결과
     */
    suspend fun syncCollection(
        collectionName: String
    ): CustomResult<SyncResult, Exception>

    // === 충돌 해결 ===

    /**
     * 동기화 충돌 해결 전략 적용
     *
     * @param collectionName 대상 컬렉션
     * @param conflictStrategy 충돌 해결 전략
     * @return 해결된 충돌 수
     */
    suspend fun resolveConflicts(
        collectionName: String,
        conflictStrategy: ConflictResolutionStrategy = ConflictResolutionStrategy.SERVER_WINS
    ): CustomResult<Int, Exception>

    // === 상태 모니터링 ===

    /**
     * 실시간 동기화 상태 관찰
     *
     * @return 동기화 상태 Flow
     */
    fun observeSyncStatus(): Flow<SyncStatus>

    /**
     * 동기화 진행률 관찰
     *
     * @return 진행률 Flow (0.0 ~ 1.0)
     */
    fun observeSyncProgress(): Flow<Float>

    /**
     * 현재 동기화 상태 조회
     *
     * @return 동기화 상태
     */
    suspend fun getCurrentSyncStatus(): CustomResult<SyncManagerStatus, Exception>

    /**
     * 동기화 헬스체크
     *
     * @return 건강 상태 및 문제점
     */
    suspend fun performHealthCheck(): CustomResult<SyncHealthReport, Exception>

    // === 설정 및 제어 ===

    /**
     * 자동 동기화 시작
     *
     * @param intervalMs 동기화 간격 (밀리초)
     * @return 성공 여부
     */
    suspend fun startAutoSync(
        intervalMs: Long = 300000L // 5분
    ): CustomResult<Unit, Exception>

    /**
     * 자동 동기화 중지
     *
     * @return 성공 여부
     */
    suspend fun stopAutoSync(): CustomResult<Unit, Exception>

    /**
     * 진행 중인 동기화 취소
     *
     * @return 성공 여부
     */
    suspend fun cancelOngoingSync(): CustomResult<Unit, Exception>

    /**
     * 클럭 스큐 보정 수행
     *
     * @param collectionName 대상 컬렉션 (null이면 모든 컬렉션)
     * @return 측정된 클럭 스큐
     */
    suspend fun calibrateClockSkew(
        collectionName: String? = null
    ): CustomResult<Map<String, Long>, Exception>

    // === 통계 및 분석 ===

    /**
     * 동기화 성능 통계 조회
     *
     * @return 성능 통계
     */
    suspend fun getSyncPerformanceMetrics(): CustomResult<SyncPerformanceMetrics, Exception>

    /**
     * 컬렉션별 동기화 통계 조회
     *
     * @return 컬렉션별 통계
     */
    suspend fun getCollectionSyncStatistics(): CustomResult<List<SyncCollectionStatistics>, Exception>
}

/**
 * 동기화 결과
 */
data class SyncResult(
    val pushedOperations: Int,
    val pulledDocuments: Int,
    val resolvedConflicts: Int,
    val duration: Long,
    val errors: List<String>
)

/**
 * 동기화 상태 열거형
 */
enum class SyncStatus {
    IDLE,           // 대기 중
    PUSHING,        // Push 동기화 중
    PULLING,        // Pull 동기화 중
    RESOLVING,      // 충돌 해결 중
    COMPLETED,      // 완료
    FAILED,         // 실패
    CANCELLED       // 취소됨
}

/**
 * SyncManager 전체 상태
 */
data class SyncManagerStatus(
    val currentStatus: SyncStatus,
    val isAutoSyncEnabled: Boolean,
    val lastSyncTime: Long?,
    val pendingOutboxCount: Int,
    val activeOperations: List<String>,
    val healthStatus: SyncHealthStatus
)

/**
 * 동기화 헬스 상태
 */
enum class SyncHealthStatus {
    HEALTHY,        // 정상
    WARNING,        // 경고 (성능 저하 등)
    CRITICAL,       // 심각 (다수 실패)
    OFFLINE         // 오프라인
}

/**
 * 동기화 헬스 리포트
 */
data class SyncHealthReport(
    val status: SyncHealthStatus,
    val issues: List<String>,
    val recommendations: List<String>,
    val collectionHealths: Map<String, SyncHealthStatus>
)

/**
 * 충돌 해결 전략
 */
enum class ConflictResolutionStrategy {
    SERVER_WINS,    // 서버 우선
    CLIENT_WINS,    // 클라이언트 우선
    MERGE,          // 병합 시도
    MANUAL          // 수동 해결 필요
}

/**
 * 동기화 성능 메트릭
 */
data class SyncPerformanceMetrics(
    val averagePushDuration: Long,
    val averagePullDuration: Long,
    val successRate: Float,
    val throughput: Float, // 문서/초
    val networkLatency: Long,
    val conflictRate: Float
)
package com.example.domain_usecase.sync

import com.example.core_common.result.CustomResult
import com.example.domain.model.sync.ScopeMetadata
import java.time.Instant

/**
 * 동기화 관리를 위한 UseCase Manager 인터페이스
 *
 * Clean Architecture와 SSOT 원칙에 따라 설계:
 * - OutBox 패턴을 통한 안정적인 데이터 동기화
 * - ScopeMetadata를 통한 동기화 상태 추적
 * - 실패 시 재시도 및 충돌 방지
 * - UseCase 계층에서 여러 Repository를 조합한 복합 비즈니스 로직
 *
 * 역할:
 * - OutBox에서 대기 중인 작업 읽기 및 처리
 * - 동기화 후 OutBox 상태 변경
 * - ScopeMetadata 갱신 (동기화 커서, 성공/실패 통계)
 * - 재시도 로직 및 오류 처리
 *
 * 책임 범위:
 * ✅ OutBox 큐 처리 (읽기, 상태 변경)
 * ✅ ScopeMetadata 갱신
 * ✅ 실패 작업 재시도
 * ✅ 동기화 상태 추적
 * ❌ OutBox 저장 (개별 UseCase의 책임)
 * ✅ 비즈니스 로직 (UseCase 계층의 책임)
 */
interface SyncManager {

    // ================================
    // 핵심 동기화 작업
    // ================================

    /**
     * OutBox 큐에서 대기 중인 작업들을 처리합니다.
     *
     * 처리 과정:
     * 1. PENDING 상태의 OutBox 작업들을 조회
     * 2. 각 작업을 원격 서버로 전송
     * 3. 성공 시: COMPLETED 상태로 변경
     * 4. 실패 시: FAILED 상태로 변경 및 재시도 정책 적용
     * 5. ScopeMetadata 업데이트 (성공/실패 통계)
     *
     * @param batchSize 한 번에 처리할 작업 개수 (기본값: 10)
     * @return 동기화 결과 (처리된 작업 수, 성공/실패 통계)
     */
    suspend fun processOutBoxQueue(batchSize: Int = 10): CustomResult<SyncResult, Exception>

    /**
     * 특정 스코프의 동기화 커서를 업데이트합니다.
     *
     * 동기화 성공 시 호출되어 다음 동기화 시작점을 기록합니다.
     * ScopeMetadata의 lastCursor, lastSyncedAt, syncCount를 업데이트합니다.
     *
     * @param scopeKey 스코프 키 (예: "messages_channel123", "users_project456")
     * @param cursor 새로운 동기화 커서 (타임스탬프)
     * @return 성공/실패 결과
     */
    suspend fun updateSyncScope(scopeKey: String, cursor: Instant): CustomResult<Unit, Exception>

    /**
     * 특정 스코프의 동기화 상태를 조회합니다.
     *
     * @param scopeKey 스코프 키
     * @return ScopeMetadata (없으면 null)
     */
    suspend fun getSyncStatus(scopeKey: String): CustomResult<ScopeMetadata?, Exception>

    // ================================
    // 재시도 및 복구 작업
    // ================================

    /**
     * 실패한 OutBox 작업들을 재시도 가능한 상태로 리셋합니다.
     *
     * FAILED 상태에서 재시도 가능한 작업들을 PENDING으로 변경하여
     * 다음 processOutBoxQueue() 호출 시 재처리되도록 합니다.
     *
     * @return 리셋된 작업 개수
     */
    suspend fun resetFailedOperations(): CustomResult<Int, Exception>

    /**
     * 특정 스코프의 동기화 에러를 기록합니다.
     *
     * @param scopeKey 스코프 키
     * @param errorMessage 에러 메시지
     * @return 성공/실패 결과
     */
    suspend fun recordSyncError(
        scopeKey: String,
        errorMessage: String
    ): CustomResult<Unit, Exception>

    /**
     * 모든 스코프의 에러 카운트를 초기화합니다.
     *
     * @return 초기화된 스코프 개수
     */
    suspend fun resetAllErrorCounts(): CustomResult<Int, Exception>

    // ================================
    // 상태 조회 및 모니터링
    // ================================

    /**
     * 전체 동기화 상태 요약을 조회합니다.
     *
     * @return 동기화 상태 요약 (대기 중인 작업 수, 에러 발생한 스코프 수 등)
     */
    suspend fun getSyncStatusSummary(): CustomResult<SyncStatusSummary, Exception>

    /**
     * 에러가 발생한 동기화 스코프들을 조회합니다.
     *
     * @param minErrorCount 최소 에러 개수 (기본값: 1)
     * @return 에러가 발생한 ScopeMetadata 목록
     */
    suspend fun getErrorScopes(minErrorCount: Long = 1): CustomResult<List<ScopeMetadata>, Exception>

    // ================================
    // 정리 및 유지보수 작업
    // ================================

    /**
     * 완료된 OutBox 작업들을 정리합니다.
     *
     * @return 정리된 작업 개수
     */
    suspend fun cleanupCompletedOperations(): CustomResult<Int, Exception>

    /**
     * 만료된 OutBox 작업들을 정리합니다.
     *
     * @param timeoutMs 타임아웃 시간 (밀리초)
     * @return 정리된 작업 개수
     */
    suspend fun cleanupExpiredOperations(timeoutMs: Long): CustomResult<Int, Exception>

    /**
     * 오래된 ScopeMetadata를 정리합니다.
     *
     * @param beforeTimestamp 기준 시간 이전의 메타데이터 삭제
     * @return 정리된 메타데이터 개수
     */
    suspend fun cleanupOldMetadata(beforeTimestamp: Instant): CustomResult<Int, Exception>
}

/**
 * OutBox 큐 처리 결과
 */
data class SyncResult(
    val totalProcessed: Int,
    val successCount: Int,
    val failureCount: Int,
    val skippedCount: Int,
    val processingTimeMs: Long,
    val errors: List<SyncError>
)

/**
 * 동기화 에러 정보
 */
data class SyncError(
    val outBoxId: String,
    val entityType: String,
    val entityId: String,
    val errorMessage: String,
    val retryCount: Int,
    val lastAttemptAt: Instant
)

/**
 * 전체 동기화 상태 요약
 */
data class SyncStatusSummary(
    val pendingOperationsCount: Int,
    val failedOperationsCount: Int,
    val completedOperationsCount: Int,
    val activeScopesCount: Int,
    val errorScopesCount: Int,
    val totalSyncOperations: Long,
    val lastSyncAt: Instant?
)
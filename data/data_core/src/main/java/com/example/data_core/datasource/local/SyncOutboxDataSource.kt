package com.example.data_core.datasource.local

import com.example.data_model.local.OutboxEntity
import kotlinx.coroutines.flow.Flow

/**
 * Sync Outbox Data Source Interface
 * 동기화 Outbox 작업을 위한 데이터 소스 인터페이스
 *
 * 🎯 역할:
 * - Outbox 작업 관리 (생성, 조회, 삭제)
 * - 동기화 상태 모니터링
 * - 재시도 로직 지원
 * - Repository와 DAO 사이의 추상화 계층
 *
 * 📋 주요 기능:
 * - addToOutbox: 동기화 대기열에 작업 추가
 * - getPendingOperations: 대기 중인 작업 조회
 * - markOperationComplete: 작업 완료 처리
 * - observeOperations: 실시간 작업 상태 모니터링
 */
interface SyncOutboxDataSource {

    // === Basic Operations ===

    /**
     * Outbox에 동기화 작업 추가
     * @param entityId 엔티티 ID
     * @param collectionName 컬렉션 이름
     * @param operation 작업 타입 (CREATE, UPDATE, DELETE)
     * @param payload 작업 데이터 (JSON 직렬화된 변경사항)
     */
    suspend fun addToOutbox(
        entityId: String,
        collectionName: String,
        operation: String,
        payload: String? = null
    )

    /**
     * 여러 Outbox 작업 일괄 추가
     * @param operations 작업 목록
     */
    suspend fun addMultipleToOutbox(operations: List<OutboxEntity>)

    // === Query Operations ===

    /**
     * 특정 컬렉션의 대기 중인 작업 조회
     * @param collectionName 컬렉션 이름
     * @return 대기 중인 작업 목록
     */
    suspend fun getPendingOperationsByCollection(collectionName: String): List<OutboxEntity>

    /**
     * 모든 대기 중인 작업 조회
     * @return 전체 대기 작업 목록
     */
    suspend fun getAllPendingOperations(): List<OutboxEntity>

    /**
     * 특정 엔티티의 작업 조회
     * @param collectionName 컬렉션 이름
     * @param entityId 엔티티 ID
     * @return 해당 엔티티의 작업 목록
     */
    suspend fun getOperationsByEntity(collectionName: String, entityId: String): List<OutboxEntity>

    /**
     * 실패한 작업들 조회 (재시도 필요)
     * @return 실패한 작업 목록
     */
    suspend fun getFailedOperations(): List<OutboxEntity>

    // === Completion & Cleanup ===

    /**
     * 작업 완료 처리 (Outbox에서 제거)
     * @param operationId 작업 ID
     */
    suspend fun markOperationComplete(operationId: String)

    /**
     * 엔티티 관련 모든 작업 제거
     * @param collectionName 컬렉션 이름
     * @param entityId 엔티티 ID
     */
    suspend fun removeOperationsByEntity(collectionName: String, entityId: String)

    /**
     * 컬렉션 관련 모든 작업 제거
     * @param collectionName 컬렉션 이름
     */
    suspend fun removeOperationsByCollection(collectionName: String)

    // === Retry Management ===

    /**
     * 작업 재시도 횟수 증가
     * @param operationId 작업 ID
     */
    suspend fun incrementRetries(operationId: String)

    /**
     * 작업 재시도 횟수 초기화
     * @param operationId 작업 ID
     */
    suspend fun resetRetries(operationId: String)

    /**
     * 재시도 가능한 작업들 조회 (백오프 시간 고려)
     * @param beforeTimestamp 기준 시간 (이전 작업들만 조회)
     * @return 재시도 가능한 작업 목록
     */
    suspend fun getRetryableOperations(beforeTimestamp: Long): List<OutboxEntity>

    // === Real-time Observations ===

    /**
     * 모든 작업 실시간 관찰
     * @return 작업 목록 Flow
     */
    fun observeAllOperations(): Flow<List<OutboxEntity>>

    /**
     * 특정 컬렉션 작업 실시간 관찰
     * @param collectionName 컬렉션 이름
     * @return 작업 목록 Flow
     */
    fun observeOperationsByCollection(collectionName: String): Flow<List<OutboxEntity>>

    /**
     * 실패한 작업 수 실시간 관찰
     * @return 실패 작업 수 Flow
     */
    fun observeFailedOperationCount(): Flow<Int>

    // === Statistics ===

    /**
     * 컬렉션별 작업 수 조회
     * @param collectionName 컬렉션 이름
     * @return 작업 수
     */
    suspend fun getOperationCountByCollection(collectionName: String): Int

    /**
     * 전체 작업 수 조회
     * @return 전체 작업 수
     */
    suspend fun getTotalOperationCount(): Int

    /**
     * 엔티티 관련 대기 작업 존재 여부 확인
     * @param collectionName 컬렉션 이름
     * @param entityId 엔티티 ID
     * @return 대기 작업 존재 여부
     */
    suspend fun hasEntityPendingOperations(collectionName: String, entityId: String): Boolean

    /**
     * 가장 오래된 작업 시간 조회
     * @return 가장 오래된 작업의 타임스탬프
     */
    suspend fun getOldestOperationTimestamp(): Long?

    // === Maintenance ===

    /**
     * 오래된 작업들 정리
     * @param beforeTimestamp 기준 시간 (이전 작업들 삭제)
     */
    suspend fun deleteOldOperations(beforeTimestamp: Long)

    /**
     * 모든 작업 삭제 (전체 초기화)
     */
    suspend fun deleteAllOperations()
}
package com.example.data_core.repository.local.base

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.local.SyncMetadataDataSource
import com.example.data_core.datasource.local.SyncOutboxDataSource
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.OutboxCollectionType
import com.example.domain.repository.local.base.BaseLocalRepository
import com.example.domain.repository.local.base.OutboxOperation
import com.example.domain.repository.local.base.SyncableRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Base Local Repository Implementation (SSOT Pattern)
 * BaseLocalRepository와 SyncableRepository 모두 구현
 *
 * 🎯 역할:
 * - BaseLocalRepository: 기본 CRUD 기능 구현
 * - SyncableRepository: 동기화 전용 기능 구현
 * - 로그 처리 및 예외 처리 표준화
 * - 하위 클래스에서 도메인 특화 메서드만 구현하도록 강제
 *
 * 📋 구현 전략:
 * - Abstract class로 공통 기능 제공
 * - CRUD와 Sync 기능을 명확히 분리
 * - Template Method Pattern 적용
 * - 하위 클래스는 도메인 특화 메서드만 오버라이드
 *
 * @param T 엔티티 타입 (Category, Project, User 등)
 */
abstract class BaseLocalRepositoryImpl<T> : BaseLocalRepository<T>,
    SyncableRepository<T> where T : AggregateRoot {

    companion object {
        private const val TAG = "BaseLocalRepository"
    }

    // === 동기화 관련 추상 속성 ===

    /**
     * Sync Outbox DataSource - Outbox 작업 관리를 위한 데이터 소스
     * Repository → DataSource → DAO 아키텍처 준수
     */
    abstract val syncOutboxDataSource: SyncOutboxDataSource

    /**
     * Sync Metadata DataSource - 동기화 메타데이터 관리를 위한 데이터 소스
     * 동기화 커서 및 상태 관리
     */
    abstract val syncMetadataDataSource: SyncMetadataDataSource

    /**
     * Collection Type - OutboxCollectionType enum 사용으로 타입 안전성 보장
     * 각 하위 클래스에서 OutboxCollectionType.fromDomainType<T>()로 구현
     */
    abstract val collectionType: OutboxCollectionType

    // === BaseLocalRepository 인터페이스 구현 ===
    // 하위 클래스에서 도메인 특화 메서드로 위임

    /**
     * BaseLocalRepository.observeEntityById 구현
     * 하위 클래스의 도메인 특화 메서드로 위임
     */
    abstract override fun observeEntityById(entityId: String): Flow<T?>

    /**
     * BaseLocalRepository.observeAllEntities 구현
     * 하위 클래스의 도메인 특화 메서드로 위임
     */
    abstract override fun observeAllEntities(): Flow<List<T>>

    /**
     * BaseLocalRepository.observeEntityUpdatedAt 구현
     * 하위 클래스의 도메인 특화 메서드로 위임
     */
    abstract override fun observeEntityUpdatedAt(entityId: String): Flow<Long?>

    /**
     * BaseLocalRepository.getEntityById 구현
     * 하위 클래스의 도메인 특화 메서드로 위임
     */
    abstract override suspend fun getEntityById(entityId: String): CustomResult<T?, Exception>

    /**
     * BaseLocalRepository.getEntitiesByIds 구현
     * 하위 클래스의 도메인 특화 메서드로 위임
     */
    abstract override suspend fun getEntitiesByIds(entityIds: List<String>): CustomResult<List<T>, Exception>

    /**
     * BaseLocalRepository.getAllEntities 구현
     * 하위 클래스의 도메인 특화 메서드로 위임
     */
    abstract override suspend fun getAllEntities(limit: Int?): CustomResult<List<T>, Exception>

    /**
     * BaseLocalRepository.saveEntity 구현
     * 하위 클래스의 도메인 특화 메서드로 위임
     */
    abstract override suspend fun saveEntity(entity: T): CustomResult<Unit, Exception>

    /**
     * BaseLocalRepository.saveEntities 구현
     * 하위 클래스의 도메인 특화 메서드로 위임
     */
    abstract override suspend fun saveEntities(entities: List<T>): CustomResult<Unit, Exception>

    /**
     * BaseLocalRepository.deleteEntity 구현
     * 하위 클래스의 도메인 특화 메서드로 위임
     */
    abstract override suspend fun deleteEntity(entityId: String): CustomResult<Unit, Exception>

    // === SyncableRepository 인터페이스 구현 ===
    // 하위 클래스에서 도메인 특화 메서드로 위임

    /**
     * SyncableRepository.getEntitiesUpdatedAfter 구현
     * 하위 클래스의 도메인 특화 메서드로 위임
     */
    abstract override suspend fun getEntitiesUpdatedAfter(timestamp: Instant): CustomResult<List<T>, Exception>

    /**
     * SyncableRepository.addToOutbox 공통 구현
     * SyncOutboxDataSource를 통한 일관된 동기화 작업 관리
     */
    override suspend fun addToOutbox(
        entityId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return handleOperation("addToOutbox($entityId, $operation)", TAG) {
            syncOutboxDataSource.addToOutbox(
                entityId = entityId,
                collectionName = collectionType.collectionName,
                operation = operation,
                payload = payload
            )
        }
    }

    /**
     * SyncableRepository.clearAllEntities 구현
     * 하위 클래스의 도메인 특화 메서드로 위임
     */
    abstract override suspend fun clearAllEntities(): CustomResult<Unit, Exception>

    /**
     * SyncableRepository.getTotalEntityCount 구현
     * 하위 클래스의 도메인 특화 메서드로 위임 (필요시 오버라이드)
     */
    override suspend fun getTotalEntityCount(): CustomResult<Int, Exception> {
        return handleOperation("getTotalEntityCount", TAG) {
            getTotalEntityCountInternal()
        }
    }

    /**
     * SyncableRepository.entityExists 구현
     * 하위 클래스의 도메인 특화 메서드로 위임 (필요시 오버라이드)
     */
    override suspend fun entityExists(entityId: String): CustomResult<Boolean, Exception> {
        return handleOperation("entityExists($entityId)", TAG) {
            entityExistsInternal(entityId)
        }
    }

    // === 하위 클래스에서 구현해야 하는 추상 메서드 ===

    /**
     * 전체 엔티티 수 조회 (내부 구현)
     * @return 엔티티 수
     */
    abstract suspend fun getTotalEntityCountInternal(): Int

    /**
     * 엔티티 존재 여부 확인 (내부 구현)
     * @param entityId 엔티티 ID
     * @return 존재 여부
     */
    abstract suspend fun entityExistsInternal(entityId: String): Boolean

    // === 공통 유틸리티 메서드 ===

    /**
     * 로그 출력 헬퍼 메서드
     * @param message 로그 메시지
     * @param tag 로그 태그 (하위 클래스에서 제공)
     */
    protected fun logDebug(message: String, tag: String = TAG) {
        Log.d(tag, message)
    }

    /**
     * 예외 로그 출력 헬퍼 메서드
     * @param message 로그 메시지
     * @param exception 예외
     * @param tag 로그 태그 (하위 클래스에서 제공)
     */
    protected fun logError(message: String, exception: Exception, tag: String = TAG) {
        Log.e(tag, message, exception)
    }

    /**
     * 공통 예외 처리 헬퍼 메서드
     * @param operation 수행 중인 작업 이름
     * @param block 실행할 블록
     * @return CustomResult
     */
    protected suspend fun <R> handleOperation(
        operation: String,
        tag: String = TAG,
        block: suspend () -> R
    ): CustomResult<R, Exception> {
        return try {
            logDebug("$operation - 시작", tag)
            val result = block()
            logDebug("$operation - 성공", tag)
            CustomResult.Success(result)
        } catch (exception: Exception) {
            logError("$operation - 실패", exception, tag)
            CustomResult.Failure(exception)
        }
    }

    // === 새로운 SyncableRepository 메서드 구현 ===

    /**
     * 대기 중인 Outbox 작업 목록 조회
     */
    override suspend fun getPendingOutboxOperations(): CustomResult<List<OutboxOperation>, Exception> {
        return handleOperation("getPendingOutboxOperations", TAG) {
            val outboxEntities =
                syncOutboxDataSource.getPendingOperationsByCollection(collectionType.collectionName)
            outboxEntities.map { entity ->
                OutboxOperation(
                    id = entity.id,
                    entityId = entity.documentId,
                    operation = entity.operation,
                    payload = entity.payload,
                    localTimestamp = entity.localTimestamp,
                    retries = entity.retries
                )
            }
        }
    }

    /**
     * Outbox 작업 완료 처리
     */
    override suspend fun markOutboxOperationComplete(operationId: String): CustomResult<Unit, Exception> {
        return handleOperation("markOutboxOperationComplete($operationId)", TAG) {
            syncOutboxDataSource.markOperationComplete(operationId)
        }
    }

    /**
     * 실패한 Outbox 작업 목록 조회
     */
    override suspend fun getFailedOutboxOperations(): CustomResult<List<OutboxOperation>, Exception> {
        return handleOperation("getFailedOutboxOperations", TAG) {
            val outboxEntities = syncOutboxDataSource.getFailedOperations()
            outboxEntities.map { entity ->
                OutboxOperation(
                    id = entity.id,
                    entityId = entity.documentId,
                    operation = entity.operation,
                    payload = entity.payload,
                    localTimestamp = entity.localTimestamp,
                    retries = entity.retries
                )
            }
        }
    }

    /**
     * 마지막 동기화 커서 조회
     */
    override suspend fun getLastSyncCursor(): CustomResult<Long?, Exception> {
        return handleOperation("getLastSyncCursor", TAG) {
            syncMetadataDataSource.getLastSyncCursor(collectionType.collectionName)
        }
    }

    /**
     * 동기화 커서 업데이트
     */
    override suspend fun updateSyncCursor(
        cursor: Long,
        timestamp: Long
    ): CustomResult<Unit, Exception> {
        return handleOperation("updateSyncCursor($cursor, $timestamp)", TAG) {
            syncMetadataDataSource.updateSyncCursor(
                collectionType.collectionName,
                cursor,
                timestamp
            )
        }
    }

    /**
     * 마지막 성공 동기화 시간 조회
     */
    override suspend fun getLastSuccessfulSyncTime(): CustomResult<Long?, Exception> {
        return handleOperation("getLastSuccessfulSyncTime", TAG) {
            syncMetadataDataSource.getLastSuccessfulSyncTime(collectionType.collectionName)
        }
    }
}
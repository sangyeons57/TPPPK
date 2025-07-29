package com.example.data_core.repository.local.base

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.AggregateRoot
import com.example.domain.repository.local.base.BaseLocalRepository
import kotlinx.coroutines.flow.Flow

/**
 * Base Local Repository Implementation (Clean SSOT Pattern)
 * BaseLocalRepository만 구현하여 순수 CRUD 기능에 집중
 *
 * 🎯 역할:
 * - BaseLocalRepository: 기본 CRUD 기능 구현
 * - 로그 처리 및 예외 처리 표준화
 * - 하위 클래스에서 도메인 특화 메서드만 구현하도록 강제
 * - 동기화는 OutboxRepository를 통해 별도 처리
 *
 * 📋 구현 전략:
 * - Abstract class로 공통 CRUD 기능 제공
 * - Template Method Pattern 적용
 * - 동기화 로직은 완전히 분리하여 인프라 Repository로 위임
 * - 하위 클래스는 도메인 특화 메서드만 오버라이드
 *
 * @param T 엔티티 타입 (Category, Project, User 등)
 */
abstract class BaseLocalRepositoryImpl<T> : BaseLocalRepository<T> where T : AggregateRoot {

    companion object {
        private const val TAG = "BaseLocalRepository"
    }

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

    // === BaseLocalRepository 공통 구현 메서드들 ===
    
    /**
     * BaseLocalRepository.clearAllEntities 구현
     * 하위 클래스의 도메인 특화 메서드로 위임
     */
    abstract override suspend fun clearAllEntities(): CustomResult<Unit, Exception>

    /**
     * BaseLocalRepository.getTotalEntityCount 구현
     * 하위 클래스의 도메인 특화 메서드로 위임 (필요시 오버라이드)
     */
    override suspend fun getTotalEntityCount(): CustomResult<Int, Exception> {
        return handleOperation("getTotalEntityCount", TAG) {
            getTotalEntityCountInternal()
        }
    }

    /**
     * BaseLocalRepository.entityExists 구현
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
}
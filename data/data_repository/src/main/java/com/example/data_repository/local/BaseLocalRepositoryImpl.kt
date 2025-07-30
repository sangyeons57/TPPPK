package com.example.data_repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.DocumentId
import com.example.domain_repository.local.BaseLocalRepository
import kotlinx.coroutines.flow.Flow

/**
 * 로컬 저장소의 기본 Repository 구현체
 * 공통적인 로컬 저장소 로직 제공
 * OutBox 작업은 구체 클래스에서 처리 (LocalMessageRepositoryImpl 등)
 */
abstract class BaseLocalRepositoryImpl<T : AggregateRoot> : BaseLocalRepository<T> {

    // ================================
    // 추상 메서드 - 하위 클래스에서 구현 필요
    // ================================
    
    /**
     * DataSource에서 단일 엔티티 저장
     * @param entity 저장할 도메인 엔티티
     * @return 성공/실패 결과
     */
    protected abstract suspend fun saveToDataSource(entity: T): CustomResult<Unit, Exception>
    
    /**
     * DataSource에서 여러 엔티티 배치 저장
     * @param entities 저장할 도메인 엔티티 목록
     * @return 성공/실패 결과
     */
    protected abstract suspend fun saveAllToDataSource(entities: List<T>): CustomResult<Unit, Exception>
    
    /**
     * DataSource에서 엔티티 삭제
     * @param entity 삭제할 도메인 엔티티
     * @return 성공/실패 결과
     */
    protected abstract suspend fun deleteFromDataSource(entity: T): CustomResult<Unit, Exception>
    
    /**
     * DataSource에서 ID로 엔티티 조회
     * @param id 엔티티 ID
     * @return 도메인 엔티티 (없으면 null)
     */
    protected abstract suspend fun findByIdFromDataSource(id: DocumentId): CustomResult<T?, Exception>
    
    /**
     * DataSource에서 모든 엔티티 조회
     * @return 모든 도메인 엔티티 목록
     */
    protected abstract suspend fun findAllFromDataSource(): CustomResult<List<T>, Exception>
    
    /**
     * DataSource에서 ID로 엔티티 관찰
     * @param id 관찰할 엔티티 ID
     * @return 도메인 엔티티 Flow
     */
    protected abstract fun observeFromDataSource(id: DocumentId): Flow<CustomResult<T?, Exception>>
    
    /**
     * DataSource에서 모든 엔티티 관찰
     * @return 모든 도메인 엔티티 Flow
     */
    protected abstract fun observeAllFromDataSource(): Flow<CustomResult<List<T>, Exception>>
    
    /**
     * DataSource에서 ID로 엔티티 물리적 삭제
     * @param id 삭제할 엔티티 ID
     * @return 삭제된 개수
     */
    protected abstract suspend fun deleteByIdFromDataSource(id: DocumentId): CustomResult<Int, Exception>
    
    /**
     * DataSource에서 삭제 마크된 엔티티들 완전 제거
     * @return 삭제된 엔티티 개수
     */
    protected abstract suspend fun deleteMarkedEntitiesFromDataSource(): CustomResult<Int, Exception>
    
    /**
     * DataSource에서 전체 엔티티 개수 조회
     * @return 전체 엔티티 개수
     */
    protected abstract suspend fun getTotalCountFromDataSource(): CustomResult<Int, Exception>
    
    /**
     * DataSource에서 엔티티 존재 여부 확인
     * @param id 확인할 엔티티 ID
     * @return 존재 여부
     */
    protected abstract suspend fun existsInDataSource(id: DocumentId): CustomResult<Boolean, Exception>
    
    /**
     * DataSource에서 디버깅용 전체 조회
     * @return 생성시간 내림차순으로 정렬된 모든 엔티티 목록
     */
    protected abstract suspend fun getAllForDebugFromDataSource(): CustomResult<List<T>, Exception>
    
    /**
     * DataSource에서 모든 엔티티 삭제
     * @return 성공/실패 결과
     */
    protected abstract suspend fun deleteAllFromDataSource(): CustomResult<Unit, Exception>

    // ================================
    // 기본 CRUD 작업 구현
    // ================================
    
    override suspend fun save(entity: T): CustomResult<Unit, Exception> {
        return try {
            saveToDataSource(entity)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun saveAll(entities: List<T>): CustomResult<Unit, Exception> {
        return try {
            saveAllToDataSource(entities)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun delete(entity: T): CustomResult<Unit, Exception> {
        return try {
            deleteFromDataSource(entity)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    // ================================
    // 기본 조회 작업 구현
    // ================================
    
    override suspend fun findById(id: DocumentId): CustomResult<T?, Exception> {
        return try {
            findByIdFromDataSource(id)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun findAll(): CustomResult<List<T>, Exception> {
        return try {
            findAllFromDataSource()
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    // ================================
    // 반응형 조회 작업 구현 (Flow)
    // ================================
    
    override fun observe(id: DocumentId): Flow<CustomResult<T?, Exception>> {
        return observeFromDataSource(id)
    }
    
    override fun observeAll(): Flow<CustomResult<List<T>, Exception>> {
        return observeAllFromDataSource()
    }

    // ================================
    // 정리 및 관리 작업 구현
    // ================================
    
    override suspend fun deleteById(id: DocumentId): CustomResult<Int, Exception> {
        return try {
            deleteByIdFromDataSource(id)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun deleteMarkedEntities(): CustomResult<Int, Exception> {
        return try {
            deleteMarkedEntitiesFromDataSource()
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    // ================================
    // 통계 및 개수 조회 구현
    // ================================
    
    override suspend fun getTotalCount(): CustomResult<Int, Exception> {
        return try {
            getTotalCountFromDataSource()
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    // ================================
    // 존재 여부 확인 구현
    // ================================
    
    override suspend fun exists(id: DocumentId): CustomResult<Boolean, Exception> {
        return try {
            existsInDataSource(id)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    // ================================
    // 테스트 및 디버깅용 구현
    // ================================
    
    override suspend fun getAllForDebug(): CustomResult<List<T>, Exception> {
        return try {
            getAllForDebugFromDataSource()
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun deleteAll(): CustomResult<Unit, Exception> {
        return try {
            deleteAllFromDataSource()
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}
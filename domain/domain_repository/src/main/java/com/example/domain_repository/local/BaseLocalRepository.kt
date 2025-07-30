package com.example.domain_repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.DocumentId
import kotlinx.coroutines.flow.Flow

/**
 * 로컬 저장소의 기본 Repository 인터페이스
 * Firebase와 독립적인 로컬 전용 저장소 계약 정의
 */
interface BaseLocalRepository<T : AggregateRoot> {

    // ================================
    // 기본 CRUD 작업
    // ================================
    
    /**
     * 엔티티 저장 (생성/업데이트)
     * @param entity 저장할 도메인 엔티티
     * @return 성공/실패 결과
     */
    suspend fun save(entity: T): CustomResult<Unit, Exception>
    
    /**
     * 여러 엔티티들을 배치로 저장
     * @param entities 저장할 도메인 엔티티 목록
     * @return 성공/실패 결과
     */
    suspend fun saveAll(entities: List<T>): CustomResult<Unit, Exception>
    
    /**
     * 엔티티 삭제 (논리적 삭제)
     * @param entity 삭제할 도메인 엔티티
     * @return 성공/실패 결과
     */
    suspend fun delete(entity: T): CustomResult<Unit, Exception>

    // ================================
    // 기본 조회 작업
    // ================================
    
    /**
     * ID로 특정 엔티티 조회
     * @param id 엔티티 ID
     * @return 도메인 엔티티 (없으면 null)
     */
    suspend fun findById(id: DocumentId): CustomResult<T?, Exception>
    
    /**
     * 모든 엔티티 조회
     * @return 모든 도메인 엔티티 목록
     */
    suspend fun findAll(): CustomResult<List<T>, Exception>

    // ================================
    // 반응형 조회 작업 (Flow)
    // ================================
    
    /**
     * ID로 특정 엔티티 관찰
     * @param id 관찰할 엔티티 ID
     * @return 도메인 엔티티 Flow
     */
    fun observe(id: DocumentId): Flow<CustomResult<T?, Exception>>
    
    /**
     * 모든 엔티티 관찰
     * @return 모든 도메인 엔티티 Flow
     */
    fun observeAll(): Flow<CustomResult<List<T>, Exception>>

    // ================================
    // 정리 및 관리 작업
    // ================================
    
    /**
     * ID로 특정 엔티티 물리적 삭제
     * @param id 삭제할 엔티티 ID
     * @return 삭제된 개수
     */
    suspend fun deleteById(id: DocumentId): CustomResult<Int, Exception>
    
    /**
     * 삭제 마크된 엔티티들 완전 제거
     * @return 삭제된 엔티티 개수
     */
    suspend fun deleteMarkedEntities(): CustomResult<Int, Exception>

    // ================================
    // 통계 및 개수 조회
    // ================================
    
    /**
     * 전체 엔티티 개수 조회
     * @return 전체 엔티티 개수
     */
    suspend fun getTotalCount(): CustomResult<Int, Exception>

    // ================================
    // 존재 여부 확인
    // ================================
    
    /**
     * 엔티티 ID 존재 여부 확인
     * @param id 확인할 엔티티 ID
     * @return 존재 여부
     */
    suspend fun exists(id: DocumentId): CustomResult<Boolean, Exception>

    // ================================
    // 테스트 및 디버깅용
    // ================================
    
    /**
     * 모든 엔티티를 생성시간 순으로 조회 (디버깅용)
     * @return 생성시간 내림차순으로 정렬된 모든 엔티티 목록
     */
    suspend fun getAllForDebug(): CustomResult<List<T>, Exception>
    
    /**
     * 모든 엔티티 삭제 (테스트/초기화용)
     * @return 성공/실패 결과
     */
    suspend fun deleteAll(): CustomResult<Unit, Exception>
}
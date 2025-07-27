package com.example.data.datasource.local

import com.example.domain.model.base.Category
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 로컬 카테고리 데이터 저장소 인터페이스
 * 3-tier 클라이언트 주도 동기화 아키텍처를 지원합니다
 */
interface LocalCategoriesDataSource {

    // === 기본 CRUD 작업 ===

    /**
     * 카테고리 ID로 단일 카테고리 조회
     * @param categoryId 카테고리 ID
     * @return 카테고리 정보 (없으면 null)
     */
    suspend fun getCategoryById(categoryId: String): Category?

    /**
     * 카테고리 이름으로 조회 (정확히 일치)
     * @param name 카테고리 이름
     * @return 카테고리 정보 (없으면 null)
     */
    suspend fun getCategoryByName(name: String): Category?

    /**
     * 카테고리 이름으로 검색 (부분 일치)
     * @param name 검색할 이름
     * @param limit 제한 개수
     * @return 카테고리 목록
     */
    suspend fun searchCategoriesByName(name: String, limit: Int = 10): List<Category>

    /**
     * 여러 카테고리 ID로 조회
     * @param categoryIds 카테고리 ID 목록
     * @return 카테고리 목록
     */
    suspend fun getCategoriesByIds(categoryIds: List<String>): List<Category>

    /**
     * 전체 카테고리 조회
     * @param limit 제한 개수 (null이면 전체)
     * @return 카테고리 목록
     */
    suspend fun getAllCategories(limit: Int? = null): List<Category>

    /**
     * 특정 프로젝트의 카테고리들을 순서대로 조회
     * @param projectId 프로젝트 ID
     * @return 카테고리 목록 (order 순서)
     */
    suspend fun getCategoriesByProject(projectId: String): List<Category>

    /**
     * 특정 순서 범위의 카테고리들 조회
     * @param minOrder 최소 순서
     * @param maxOrder 최대 순서
     * @return 카테고리 목록
     */
    suspend fun getCategoriesByOrderRange(minOrder: Int, maxOrder: Int): List<Category>

    /**
     * NoCategory 조회 (프로젝트별)
     * @param projectId 프로젝트 ID
     * @return NoCategory 인스턴스 (없으면 null)
     */
    suspend fun getNoCategoryByProject(projectId: String): Category?

    /**
     * 단일 카테고리 정보 저장
     * @param category 저장할 카테고리 정보
     */
    suspend fun saveCategory(category: Category)

    /**
     * 카테고리 목록 배치 저장
     * @param categories 저장할 카테고리 목록
     */
    suspend fun saveCategories(categories: List<Category>)

    /**
     * 카테고리 삭제
     * @param categoryId 삭제할 카테고리 ID
     */
    suspend fun deleteCategory(categoryId: String)

    // === 3-tier 동기화 지원 ===

    /**
     * 특정 시점 이후 업데이트된 카테고리들을 조회 (증분 동기화용)
     * @param timestamp 기준 시간
     * @return 업데이트된 카테고리 목록
     */
    suspend fun getCategoriesUpdatedAfter(timestamp: Instant): List<Category>

    /**
     * 카테고리 실시간 관찰
     * @param categoryId 카테고리 ID
     * @return 카테고리 정보 Flow
     */
    fun observeCategoryById(categoryId: String): Flow<Category?>

    /**
     * 카테고리 이름으로 실시간 관찰 (정확히 일치)
     * @param name 카테고리 이름
     * @return 카테고리 정보 Flow
     */
    fun observeByName(name: String): Flow<Category?>

    /**
     * 카테고리 이름으로 실시간 관찰 (부분 일치)
     * @param name 검색할 이름
     * @param limit 제한 개수
     * @return 카테고리 목록 Flow
     */
    fun observeAllByName(name: String, limit: Int = 10): Flow<List<Category>>

    /**
     * 특정 프로젝트의 카테고리들을 실시간 관찰
     * @param projectId 프로젝트 ID
     * @return 카테고리 목록 Flow (order 순서)
     */
    fun observeCategoriesByProject(projectId: String): Flow<List<Category>>

    /**
     * 여러 카테고리 ID로 실시간 관찰
     * @param categoryIds 카테고리 ID 목록
     * @return 카테고리 목록 Flow
     */
    fun observeCategories(categoryIds: List<String>): Flow<List<Category>>

    /**
     * 특정 카테고리의 updatedAt 필드 실시간 관찰
     * @param categoryId 카테고리 ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observeCategoryUpdatedAt(categoryId: String): Flow<Long?>

    /**
     * 모든 카테고리 실시간 관찰
     * @return 전체 카테고리 목록 Flow
     */
    fun observeAllCategories(): Flow<List<Category>>

    /**
     * 특정 순서 범위의 카테고리들을 실시간 관찰
     * @param minOrder 최소 순서
     * @param maxOrder 최대 순서
     * @return 카테고리 목록 Flow
     */
    fun observeCategoriesByOrderRange(minOrder: Int, maxOrder: Int): Flow<List<Category>>

    // === Outbox 관리 ===

    /**
     * 카테고리 변경사항을 Outbox에 기록
     * @param categoryId 카테고리 ID
     * @param operation 작업 유형 (CREATE, UPDATE, DELETE)
     * @param payload 변경 데이터 (선택적)
     */
    suspend fun addToOutbox(categoryId: String, operation: String, payload: String? = null)

    /**
     * 대기 중인 Outbox 작업 목록 조회
     * @return 대기 중인 작업 목록
     */
    suspend fun getPendingOutboxOperations(): List<CategoryOutboxOperation>

    /**
     * Outbox 작업 완료 처리
     * @param operationId 작업 ID
     */
    suspend fun markOutboxOperationComplete(operationId: String)

    /**
     * Outbox 작업 재시도 증가
     * @param operationId 작업 ID
     */
    suspend fun incrementOutboxRetries(operationId: String)

    // === 동기화 메타데이터 관리 ===

    /**
     * 마지막 동기화 커서 조회
     * @return 마지막 서버 커서 (밀리초)
     */
    suspend fun getLastSyncCursor(): Long?

    /**
     * 동기화 커서 업데이트
     * @param cursor 새로운 서버 커서
     * @param timestamp 동기화 시간
     */
    suspend fun updateSyncCursor(cursor: Long, timestamp: Long)

    // === 유틸리티 ===

    /**
     * 카테고리 존재 여부 확인
     * @param categoryId 카테고리 ID
     * @return 존재 여부
     */
    suspend fun categoryExists(categoryId: String): Boolean

    /**
     * 카테고리 이름 중복 확인
     * @param name 카테고리 이름
     * @param excludeCategoryId 제외할 카테고리 ID (수정시 자기 자신 제외)
     * @return 중복 여부
     */
    suspend fun nameExists(name: String, excludeCategoryId: String? = null): Boolean

    /**
     * 전체 카테고리 수 조회
     * @return 카테고리 수
     */
    suspend fun getTotalCategoryCount(): Int

    /**
     * 특정 프로젝트의 카테고리 수 조회
     * @param projectId 프로젝트 ID
     * @return 카테고리 수
     */
    suspend fun getCategoryCountByProject(projectId: String): Int

    /**
     * 다음 카테고리 순서 조회
     * @param projectId 프로젝트 ID
     * @return 다음 순서 번호
     */
    suspend fun getNextCategoryOrder(projectId: String): Int

    /**
     * 모든 카테고리 데이터 삭제 (개발/테스트용)
     */
    suspend fun clearAllCategories()
}

/**
 * 카테고리 Outbox 작업 정보
 */
data class CategoryOutboxOperation(
    val id: String,
    val categoryId: String,
    val operation: String,
    val payload: String?,
    val localTimestamp: Long,
    val retries: Int
)
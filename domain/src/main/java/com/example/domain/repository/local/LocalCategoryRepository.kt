package com.example.domain.repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.repository.local.base.BaseLocalRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Local Category Repository Interface (SSOT)
 * BaseLocalRepository 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지 (Remote CategoryRepository 사용)
 *
 * ✅ 역할:
 * - BaseLocalRepository의 공통 CRUD 기능 상속 (80%)
 * - Category 도메인 특화 기능만 추가 정의 (20%)
 * - Flow로 UI에 실시간 데이터 제공 (Observer Pattern)
 * - Outbox 관리 (동기화 대상 저장)
 *
 * 📋 BaseLocalRepository 상속 메서드:
 * - observeEntityById -> observeCategoryById
 * - observeAllEntities -> observeAllCategories
 * - observeEntityUpdatedAt -> observeCategoryUpdatedAt
 * - getEntityById -> getCategoryById
 * - getEntitiesByIds -> getCategoriesByIds
 * - getAllEntities -> getAllCategories
 * - saveEntity -> saveCategory (기본 버전)
 * - saveEntities -> saveCategories
 * - deleteEntity -> deleteCategory
 * - Plus SyncableRepository methods (addToOutbox, clearAllEntities, etc.)
 */
interface LocalCategoryRepository : BaseLocalRepository<Category> {


    /**
     * 주어진 이름과 정확히 일치하는 카테고리를 실시간 관찰
     * @param name 카테고리 이름
     * @return 카테고리 Flow
     */
    fun observeByName(name: CategoryName): Flow<Category?>

    /**
     * 주어진 이름을 포함하는 카테고리 목록을 실시간 관찰
     * @param name 검색할 이름
     * @param limit 제한 개수
     * @return 카테고리 목록 Flow
     */
    fun observeAllByName(name: String, limit: Int = 10): Flow<List<Category>>

    /**
     * 특정 프로젝트의 카테고리들을 순서대로 실시간 관찰
     * @param projectId 프로젝트 ID
     * @return 카테고리 목록 Flow (order 순서)
     */
    fun observeCategoriesByProject(projectId: String): Flow<List<Category>>

    /**
     * 주어진 ID 목록에 해당하는 카테고리 목록을 실시간 관찰
     * @param categoryIds 카테고리 ID 목록
     * @return 카테고리 목록 Flow
     */
    fun observeCategories(categoryIds: List<String>): Flow<List<Category>>

    /**
     * 특정 카테고리의 updatedAt 필드 변경을 실시간 관찰
     * @param categoryId 카테고리 ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observeCategoryUpdatedAt(categoryId: String): Flow<Long?>

    /**
     * 모든 카테고리를 실시간 관찰
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

    // === 단순 읽기 작업 ===

    /**
     * 카테고리 ID로 조회
     * @param categoryId 카테고리 ID
     * @return 카테고리 (없으면 null)
     */
    suspend fun getCategoryById(categoryId: String): Category?

    /**
     * 카테고리 이름으로 조회 (정확히 일치)
     * @param name 카테고리 이름
     * @return 카테고리 (없으면 null)
     */
    suspend fun getCategoryByName(name: CategoryName): Category?

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

    // === 쓰기 작업 (Outbox 포함) ===

    /**
     * 카테고리 저장 (생성/수정)
     * @param category 저장할 카테고리
     * @return 성공 여부
     */
    suspend fun saveCategory(category: Category, projectId: String): CustomResult<Unit, Exception>

    /**
     * 카테고리 대량 저장 (동기화용)
     * @param categories 저장할 카테고리 목록
     * @param projectId 카테고리들이 속한 프로젝트의 ID
     * @return 성공 여부
     */
    suspend fun saveCategories(categories: List<Category>, projectId: String): CustomResult<Unit, Exception>

    /**
     * 카테고리 삭제 (Soft Delete)
     * @param categoryId 카테고리 ID
     * @return 성공 여부
     */
    suspend fun deleteCategory(categoryId: String): CustomResult<Unit, Exception>

    /**
     * 카테고리 정보 업데이트 (로컬)
     * @param categoryId 카테고리 ID
     * @param name 새로운 이름 (nullable)
     * @param order 새로운 순서 (nullable)
     * @return 성공 여부
     */
    suspend fun updateCategory(
        categoryId: String,
        name: CategoryName? = null,
        order: CategoryOrder? = null
    ): CustomResult<Unit, Exception>

    /**
     * 카테고리 순서 재정렬
     * @param categoryOrderMap 카테고리 ID와 새로운 순서 매핑
     * @return 성공 여부
     */
    suspend fun reorderCategories(categoryOrderMap: Map<String, Int>): CustomResult<Unit, Exception>

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
    suspend fun nameExists(name: CategoryName, excludeCategoryId: String? = null): Boolean

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
     * 모든 카테고리 삭제 (초기화)
     * @return 성공 여부
     */
    suspend fun clearAllCategories(): CustomResult<Unit, Exception>

    // === 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 카테고리 조회
     * @param timestamp 기준 시간
     * @return 업데이트된 카테고리 목록
     */
    suspend fun getCategoriesUpdatedAfter(timestamp: Instant): List<Category>

    /**
     * Outbox에 작업 추가 (서버 동기화 대기열)
     * @param categoryId 카테고리 ID
     * @param operation 작업 타입 (CREATE, UPDATE, DELETE)
     * @param payload 작업 데이터 (JSON)
     * @return 성공 여부
     */
    suspend fun addToOutbox(
        categoryId: String,
        operation: String,
        payload: String? = null
    ): CustomResult<Unit, Exception>
}
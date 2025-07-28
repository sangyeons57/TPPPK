package com.example.domain.repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.repository.local.base.BaseLocalRepository
import kotlinx.coroutines.flow.Flow

/**
 * Local Category Repository Interface (SSOT) - Refactored
 * BaseLocalRepository를 상속받아 공통 기능 활용
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지 (Remote CategoryRepository 사용)
 *
 * ✅ 역할:
 * - BaseLocalRepository의 공통 CRUD 기능 상속
 * - Category 도메인 특화 기능만 추가 정의
 * - 코드 중복 제거 및 일관성 보장
 *
 * 📋 상속받은 공통 기능:
 * - observeEntityById(categoryId) -> observeCategoryById
 * - observeAllEntities() -> observeAllCategories  
 * - saveEntity(category) -> saveCategory
 * - deleteEntity(categoryId) -> deleteCategory
 * - getEntitiesUpdatedAfter(timestamp) -> getCategoriesUpdatedAfter
 * - addToOutbox(categoryId, operation, payload)
 */
interface LocalCategoryRepositoryRefactored : BaseLocalRepository<Category> {

    // === 도메인 특화 관찰자 메서드 ===

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

    // === 도메인 특화 읽기 메서드 ===

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
     * 특정 프로젝트의 카테고리들을 순서대로 조회
     * @param projectId 프로젝트 ID
     * @return 카테고리 목록 (order 순서)
     */
    suspend fun getCategoriesByProject(projectId: String): List<Category>

    // === 도메인 특화 쓰기 메서드 ===

    /**
     * 카테고리 이름 업데이트 (로컬)
     * @param categoryId 카테고리 ID
     * @param name 새로운 이름
     * @return 성공 여부
     */
    suspend fun updateCategoryName(
        categoryId: String,
        name: CategoryName
    ): CustomResult<Unit, Exception>

    /**
     * 카테고리 순서 업데이트 (로컬)
     * @param categoryId 카테고리 ID
     * @param order 새로운 순서
     * @return 성공 여부
     */
    suspend fun updateCategoryOrder(
        categoryId: String,
        order: CategoryOrder
    ): CustomResult<Unit, Exception>

    /**
     * 카테고리 순서 재정렬
     * @param categoryOrderMap 카테고리 ID와 새로운 순서 매핑
     * @return 성공 여부
     */
    suspend fun reorderCategories(categoryOrderMap: Map<String, Int>): CustomResult<Unit, Exception>

    // === 도메인 특화 유틸리티 ===

    /**
     * 카테고리 이름 중복 확인
     * @param name 카테고리 이름
     * @param projectId 프로젝트 ID
     * @param excludeCategoryId 제외할 카테고리 ID (수정시 자기 자신 제외)
     * @return 중복 여부
     */
    suspend fun nameExists(
        name: CategoryName,
        projectId: String,
        excludeCategoryId: String? = null
    ): Boolean

    /**
     * 특정 프로젝트의 카테고리 수 조회
     * @param projectId 프로젝트 ID
     * @return 카테고리 수
     */
    suspend fun getCategoryCountByProject(projectId: String): Int

    /**
     * 프로젝트의 다음 카테고리 순서 조회
     * @param projectId 프로젝트 ID
     * @return 다음 순서 번호
     */
    suspend fun getNextCategoryOrder(projectId: String): Int

    // === BaseLocalRepository 별칭 메서드 (편의성) ===

    /**
     * observeEntityById의 별칭
     */
    fun observeCategoryById(categoryId: String): Flow<Category?> = 
        observeEntityById(categoryId)

    /**
     * observeAllEntities의 별칭
     */
    fun observeAllCategories(): Flow<List<Category>> = 
        observeAllEntities()

    /**
     * observeEntityUpdatedAt의 별칭
     */
    fun observeCategoryUpdatedAt(categoryId: String): Flow<Long?> = 
        observeEntityUpdatedAt(categoryId)

    /**
     * getEntityById의 별칭
     */
    suspend fun getCategoryById(categoryId: String): Category? = 
        getEntityById(categoryId)

    /**
     * saveEntity의 별칭
     */
    suspend fun saveCategory(category: Category): CustomResult<Unit, Exception> = 
        saveEntity(category)

    /**
     * saveEntities의 별칭
     */
    suspend fun saveCategories(categories: List<Category>): CustomResult<Unit, Exception> = 
        saveEntities(categories)

    /**
     * deleteEntity의 별칭
     */
    suspend fun deleteCategory(categoryId: String): CustomResult<Unit, Exception> = 
        deleteEntity(categoryId)

    /**
     * getEntitiesUpdatedAfter의 별칭
     */
    suspend fun getCategoriesUpdatedAfter(timestamp: java.time.Instant): List<Category> = 
        getEntitiesUpdatedAfter(timestamp)

    /**
     * clearAllEntities의 별칭
     */
    suspend fun clearAllCategories(): CustomResult<Unit, Exception> = 
        clearAllEntities()

    /**
     * getTotalEntityCount의 별칭
     */
    suspend fun getTotalCategoryCount(): Int = 
        getTotalEntityCount()

    /**
     * entityExists의 별칭
     */
    suspend fun categoryExists(categoryId: String): Boolean = 
        entityExists(categoryId)
}
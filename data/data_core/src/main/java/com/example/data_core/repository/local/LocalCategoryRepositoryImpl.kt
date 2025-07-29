package com.example.data_core.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.local.LocalProjectCategoriesDataSource
import com.example.data_core.repository.local.base.BaseLocalRepositoryImpl
import com.example.domain.model.base.Category
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.repository.infrastructure.OutboxRepository
import com.example.domain.repository.local.LocalCategoryRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Category Repository Implementation (Clean Architecture)
 * BaseLocalRepositoryImpl 상속으로 공통 CRUD 기능 자동 제공
 * OutboxRepository를 통한 동기화 처리 분리
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지
 *
 * ✅ 역할:
 * - BaseLocalRepositoryImpl의 공통 CRUD 기능 상속 (80%)
 * - Category 도메인 특화 기능만 구현 (20%)
 * - LocalDataSource를 통한 Room DB 접근
 * - Flow로 UI에 실시간 데이터 제공
 * - 로컬 CRUD 작업 처리
 * - OutboxRepository를 통한 동기화 작업 위임
 *
 * 📋 BaseLocalRepository 메서드 구현:
 * - observeEntityById -> observeCategoryById로 위임
 * - observeAllEntities -> observeAllCategories로 위임
 * - observeEntityUpdatedAt -> observeCategoryUpdatedAt로 위임
 * - getEntityById -> getCategoryById로 위임
 * - getEntitiesByIds -> getCategoriesByIds로 위임
 * - getAllEntities -> getAllCategories로 위임
 * - saveEntity -> saveCategory로 위임 (+ OutboxRepository.enqueue)
 * - saveEntities -> saveCategories로 위임
 * - deleteEntity -> deleteCategory로 위임 (+ OutboxRepository.enqueue)
 */
@Singleton
class LocalCategoryRepositoryImpl @Inject constructor(
    private val localProjectCategoriesDataSource: LocalProjectCategoriesDataSource,
    private val outboxRepository: OutboxRepository
) : BaseLocalRepositoryImpl<Category>(), LocalCategoryRepository {

    companion object {
        private const val TAG = "LocalCategoryRepository"
        private const val COLLECTION_NAME = "categories"
    }

    // === BaseLocalRepository 메서드 구현 (도메인 특화 메서드로 위임) ===

    override fun observeEntityById(entityId: String): Flow<Category?> = 
        observeCategoryById(entityId)

    override fun observeAllEntities(): Flow<List<Category>> = 
        observeAllCategories()

    override fun observeEntityUpdatedAt(entityId: String): Flow<Long?> = 
        observeCategoryUpdatedAt(entityId)

    override suspend fun getEntityById(entityId: String): CustomResult<Category?, Exception> = 
        handleOperation("getCategoryById($entityId)", TAG) {
            getCategoryById(entityId)
        }

    override suspend fun getEntitiesByIds(entityIds: List<String>): CustomResult<List<Category>, Exception> = 
        handleOperation("getCategoriesByIds(${entityIds.size})", TAG) {
            getCategoriesByIds(entityIds)
        }

    override suspend fun getAllEntities(limit: Int?): CustomResult<List<Category>, Exception> = 
        handleOperation("getAllCategories($limit)", TAG) {
            getAllCategories(limit)
        }

    override suspend fun saveEntity(entity: Category): CustomResult<Unit, Exception> = 
        saveCategory(entity, entity.projectId ?: throw IllegalArgumentException("Category must have projectId"))

    override suspend fun saveEntities(entities: List<Category>): CustomResult<Unit, Exception> = 
        saveCategories(entities, entities.firstOrNull()?.projectId ?: throw IllegalArgumentException("Categories must have projectId"))

    override suspend fun deleteEntity(entityId: String): CustomResult<Unit, Exception> = 
        deleteCategory(entityId)


    override suspend fun clearAllEntities(): CustomResult<Unit, Exception> = 
        clearAllCategories()

    // === BaseLocalRepositoryImpl 추상 메서드 구현 ===

    override suspend fun getTotalEntityCountInternal(): Int {
        return getTotalCategoryCount()
    }

    override suspend fun entityExistsInternal(entityId: String): Boolean {
        return categoryExists(entityId)
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeCategoryById(categoryId: String): Flow<Category?> {
        Log.d(TAG, "observeCategoryById: $categoryId")
        return localProjectCategoriesDataSource.observeCategoryById(categoryId)
    }

    override fun observeByName(name: CategoryName): Flow<Category?> {
        Log.d(TAG, "observeByName: ${name.value}")
        return localProjectCategoriesDataSource.observeByName(name.value)
    }

    override fun observeAllByName(name: String, limit: Int): Flow<List<Category>> {
        Log.d(TAG, "observeAllByName: name='$name', limit=$limit")
        return localProjectCategoriesDataSource.observeAllByName(name, limit)
    }

    override fun observeCategoriesByProject(projectId: String): Flow<List<Category>> {
        Log.d(TAG, "observeCategoriesByProject: $projectId")
        return localProjectCategoriesDataSource.observeCategoriesByProject(projectId)
    }

    override fun observeCategories(categoryIds: List<String>): Flow<List<Category>> {
        Log.d(TAG, "observeCategories: ${categoryIds.size} categories")
        return localProjectCategoriesDataSource.observeCategories(categoryIds)
    }

    override fun observeCategoryUpdatedAt(categoryId: String): Flow<Long?> {
        Log.d(TAG, "observeCategoryUpdatedAt: $categoryId")
        return localProjectCategoriesDataSource.observeCategoryUpdatedAt(categoryId)
    }

    override fun observeAllCategories(): Flow<List<Category>> {
        Log.d(TAG, "observeAllCategories")
        return localProjectCategoriesDataSource.observeAllCategories()
    }

    override fun observeCategoriesByOrderRange(minOrder: Int, maxOrder: Int): Flow<List<Category>> {
        Log.d(TAG, "observeCategoriesByOrderRange: $minOrder-$maxOrder")
        return localProjectCategoriesDataSource.observeCategoriesByOrderRange(minOrder, maxOrder)
    }

    // === 단순 읽기 작업 ===

    override suspend fun getCategoryById(categoryId: String): Category? {
        Log.d(TAG, "getCategoryById: $categoryId")
        return try {
            localProjectCategoriesDataSource.getCategoryById(categoryId)
        } catch (e: Exception) {
            Log.e(TAG, "getCategoryById failed", e)
            null
        }
    }

    override suspend fun getCategoryByName(name: CategoryName): Category? {
        Log.d(TAG, "getCategoryByName: ${name.value}")
        return try {
            localProjectCategoriesDataSource.getCategoryByName(name.value)
        } catch (e: Exception) {
            Log.e(TAG, "getCategoryByName failed", e)
            null
        }
    }

    override suspend fun searchCategoriesByName(name: String, limit: Int): List<Category> {
        Log.d(TAG, "searchCategoriesByName: name='$name', limit=$limit")
        return try {
            localProjectCategoriesDataSource.searchCategoriesByName(name, limit)
        } catch (e: Exception) {
            Log.e(TAG, "searchCategoriesByName failed", e)
            emptyList()
        }
    }

    override suspend fun getCategoriesByIds(categoryIds: List<String>): List<Category> {
        Log.d(TAG, "getCategoriesByIds: ${categoryIds.size} categories")
        return try {
            localProjectCategoriesDataSource.getCategoriesByIds(categoryIds)
        } catch (e: Exception) {
            Log.e(TAG, "getCategoriesByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getAllCategories(limit: Int?): List<Category> {
        Log.d(TAG, "getAllCategories: limit=$limit")
        return try {
            localProjectCategoriesDataSource.getAllCategories(limit)
        } catch (e: Exception) {
            Log.e(TAG, "getAllCategories failed", e)
            emptyList()
        }
    }

    override suspend fun getCategoriesByProject(projectId: String): List<Category> {
        Log.d(TAG, "getCategoriesByProject: $projectId")
        return try {
            localProjectCategoriesDataSource.getCategoriesByProject(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getCategoriesByProject failed", e)
            emptyList()
        }
    }

    override suspend fun getCategoriesByOrderRange(minOrder: Int, maxOrder: Int): List<Category> {
        Log.d(TAG, "observeCategoriesByOrderRange: $minOrder-$maxOrder")
        return try {
            localProjectCategoriesDataSource.getCategoriesByOrderRange(minOrder, maxOrder)
        } catch (e: Exception) {
            Log.e(TAG, "getCategoriesByOrderRange failed", e)
            emptyList()
        }
    }

    override suspend fun getNoCategoryByProject(projectId: String): Category? {
        Log.d(TAG, "getNoCategoryByProject: $projectId")
        return try {
            localProjectCategoriesDataSource.getNoCategoryByProject(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getNoCategoryByProject failed", e)
            null
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveCategory(category: Category, projectId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveCategory: ${category.id}")

            // 1. Room DB에 저장
            localProjectCategoriesDataSource.saveCategory(category, projectId)

            // 2. OutboxRepository를 통한 동기화 작업 추가
            val operation = if (category.isNew) "CREATE" else "UPDATE"
            val outboxResult = outboxRepository.enqueue(
                collectionName = COLLECTION_NAME,
                documentId = category.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            when (outboxResult) {
                is CustomResult.Success -> {
                    Log.d(TAG, "Category saved and added to outbox: ${category.id}")
                    CustomResult.Success(Unit)
                }

                is CustomResult.Failure -> {
                    Log.e(TAG, "Failed to add category to outbox", outboxResult.error)
                    // DB 저장은 성공했지만 Outbox 추가 실패 - 경고만 출력하고 성공 처리
                    CustomResult.Success(Unit)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "saveCategory failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveCategories(categories: List<Category>, projectId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveCategories: ${categories.size} categories")

            if (categories.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localProjectCategoriesDataSource.saveCategories(categories, projectId)

            Log.d(TAG, "Bulk categories saved: ${categories.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveCategories failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteCategory(categoryId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteCategory: $categoryId")

            // 1. Room DB에서 삭제 (실제로는 soft delete)
            localProjectCategoriesDataSource.deleteCategory(categoryId)

            // 2. OutboxRepository를 통한 삭제 작업 추가
            val outboxResult = outboxRepository.enqueue(
                collectionName = COLLECTION_NAME,
                documentId = categoryId,
                operation = "DELETE",
                payload = null
            )

            when (outboxResult) {
                is CustomResult.Success -> {
                    Log.d(TAG, "Category deleted and added to outbox: $categoryId")
                    CustomResult.Success(Unit)
                }

                is CustomResult.Failure -> {
                    Log.e(TAG, "Failed to add delete operation to outbox", outboxResult.error)
                    // DB 삭제는 성공했지만 Outbox 추가 실패 - 경고만 출력하고 성공 처리
                    CustomResult.Success(Unit)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "deleteCategory failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateCategory(
        categoryId: String,
        name: CategoryName?,
        order: CategoryOrder?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "updateCategory: categoryId=$categoryId, name=$name, order=$order")

            // 1. 현재 카테고리 조회
            val currentCategory = localProjectCategoriesDataSource.getCategoryById(categoryId)
                ?: return CustomResult.Failure(IllegalArgumentException("Category not found: $categoryId"))

            // 2. 업데이트된 카테고리 생성 (필요한 필드만 수정)
            var updatedCategory = currentCategory

            if (name != null || order != null) {
                updatedCategory = updatedCategory.update(name, order)
            }

            // 3. 저장 (Outbox 포함)
            saveCategory(updatedCategory)

            Log.d(TAG, "Category updated: $categoryId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateCategory failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun reorderCategories(categoryOrderMap: Map<String, Int>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "reorderCategories: ${categoryOrderMap.size} categories")

            // 각 카테고리의 순서를 업데이트
            for ((categoryId, newOrder) in categoryOrderMap) {
                updateCategory(categoryId, null, CategoryOrder(newOrder))
            }

            Log.d(TAG, "Categories reordered successfully")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "reorderCategories failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 유틸리티 ===

    override suspend fun categoryExists(categoryId: String): Boolean {
        return try {
            localProjectCategoriesDataSource.categoryExists(categoryId)
        } catch (e: Exception) {
            Log.e(TAG, "categoryExists failed", e)
            false
        }
    }

    override suspend fun nameExists(name: CategoryName, excludeCategoryId: String?): Boolean {
        return try {
            localProjectCategoriesDataSource.nameExists(name.value, excludeCategoryId)
        } catch (e: Exception) {
            Log.e(TAG, "nameExists failed", e)
            false
        }
    }

    override suspend fun getTotalCategoryCount(): Int {
        return try {
            localProjectCategoriesDataSource.getTotalCategoryCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalCategoryCount failed", e)
            0
        }
    }

    override suspend fun getCategoryCountByProject(projectId: String): Int {
        return try {
            localProjectCategoriesDataSource.getCategoryCountByProject(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getCategoryCountByProject failed", e)
            0
        }
    }

    override suspend fun getNextCategoryOrder(projectId: String): Int {
        return try {
            localProjectCategoriesDataSource.getNextCategoryOrder(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getNextCategoryOrder failed", e)
            1
        }
    }

    override suspend fun clearAllCategories(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllCategories")

            localProjectCategoriesDataSource.clearAllCategories()

            Log.d(TAG, "All categories cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "clearAllCategories failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getCategoriesUpdatedAfter(timestamp: Instant): List<Category> {
        return try {
            localProjectCategoriesDataSource.getCategoriesUpdatedAfter(timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "getCategoriesUpdatedAfter failed", e)
            emptyList()
        }
    }
}

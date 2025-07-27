package com.example.data.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data.datasource.local.LocalCategoriesDataSource
import com.example.domain.model.base.Category
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.repository.local.LocalCategoryRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Category Repository Implementation (SSOT)
 * Room Database 전용 구현체 - UI에 직접 데이터 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지
 *
 * ✅ 역할:
 * - LocalDataSource를 통한 Room DB 접근
 * - Flow로 UI에 실시간 데이터 제공
 * - 로컬 CRUD 작업 처리
 * - Outbox 관리 (동기화 대상 저장)
 */
@Singleton
class LocalCategoryRepositoryImpl @Inject constructor(
    private val localCategoriesDataSource: LocalCategoriesDataSource
) : LocalCategoryRepository {

    companion object {
        private const val TAG = "LocalCategoryRepository"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeCategoryById(categoryId: String): Flow<Category?> {
        Log.d(TAG, "observeCategoryById: $categoryId")
        return localCategoriesDataSource.observeCategoryById(categoryId)
    }

    override fun observeByName(name: CategoryName): Flow<Category?> {
        Log.d(TAG, "observeByName: ${name.value}")
        return localCategoriesDataSource.observeByName(name.value)
    }

    override fun observeAllByName(name: String, limit: Int): Flow<List<Category>> {
        Log.d(TAG, "observeAllByName: name='$name', limit=$limit")
        return localCategoriesDataSource.observeAllByName(name, limit)
    }

    override fun observeCategoriesByProject(projectId: String): Flow<List<Category>> {
        Log.d(TAG, "observeCategoriesByProject: $projectId")
        return localCategoriesDataSource.observeCategoriesByProject(projectId)
    }

    override fun observeCategories(categoryIds: List<String>): Flow<List<Category>> {
        Log.d(TAG, "observeCategories: ${categoryIds.size} categories")
        return localCategoriesDataSource.observeCategories(categoryIds)
    }

    override fun observeCategoryUpdatedAt(categoryId: String): Flow<Long?> {
        Log.d(TAG, "observeCategoryUpdatedAt: $categoryId")
        return localCategoriesDataSource.observeCategoryUpdatedAt(categoryId)
    }

    override fun observeAllCategories(): Flow<List<Category>> {
        Log.d(TAG, "observeAllCategories")
        return localCategoriesDataSource.observeAllCategories()
    }

    override fun observeCategoriesByOrderRange(minOrder: Int, maxOrder: Int): Flow<List<Category>> {
        Log.d(TAG, "observeCategoriesByOrderRange: $minOrder-$maxOrder")
        return localCategoriesDataSource.observeCategoriesByOrderRange(minOrder, maxOrder)
    }

    // === 단순 읽기 작업 ===

    override suspend fun getCategoryById(categoryId: String): Category? {
        Log.d(TAG, "getCategoryById: $categoryId")
        return try {
            localCategoriesDataSource.getCategoryById(categoryId)
        } catch (e: Exception) {
            Log.e(TAG, "getCategoryById failed", e)
            null
        }
    }

    override suspend fun getCategoryByName(name: CategoryName): Category? {
        Log.d(TAG, "getCategoryByName: ${name.value}")
        return try {
            localCategoriesDataSource.getCategoryByName(name.value)
        } catch (e: Exception) {
            Log.e(TAG, "getCategoryByName failed", e)
            null
        }
    }

    override suspend fun searchCategoriesByName(name: String, limit: Int): List<Category> {
        Log.d(TAG, "searchCategoriesByName: name='$name', limit=$limit")
        return try {
            localCategoriesDataSource.searchCategoriesByName(name, limit)
        } catch (e: Exception) {
            Log.e(TAG, "searchCategoriesByName failed", e)
            emptyList()
        }
    }

    override suspend fun getCategoriesByIds(categoryIds: List<String>): List<Category> {
        Log.d(TAG, "getCategoriesByIds: ${categoryIds.size} categories")
        return try {
            localCategoriesDataSource.getCategoriesByIds(categoryIds)
        } catch (e: Exception) {
            Log.e(TAG, "getCategoriesByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getAllCategories(limit: Int?): List<Category> {
        Log.d(TAG, "getAllCategories: limit=$limit")
        return try {
            localCategoriesDataSource.getAllCategories(limit)
        } catch (e: Exception) {
            Log.e(TAG, "getAllCategories failed", e)
            emptyList()
        }
    }

    override suspend fun getCategoriesByProject(projectId: String): List<Category> {
        Log.d(TAG, "getCategoriesByProject: $projectId")
        return try {
            localCategoriesDataSource.getCategoriesByProject(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getCategoriesByProject failed", e)
            emptyList()
        }
    }

    override suspend fun getCategoriesByOrderRange(minOrder: Int, maxOrder: Int): List<Category> {
        Log.d(TAG, "getCategoriesByOrderRange: $minOrder-$maxOrder")
        return try {
            localCategoriesDataSource.getCategoriesByOrderRange(minOrder, maxOrder)
        } catch (e: Exception) {
            Log.e(TAG, "getCategoriesByOrderRange failed", e)
            emptyList()
        }
    }

    override suspend fun getNoCategoryByProject(projectId: String): Category? {
        Log.d(TAG, "getNoCategoryByProject: $projectId")
        return try {
            localCategoriesDataSource.getNoCategoryByProject(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getNoCategoryByProject failed", e)
            null
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveCategory(category: Category): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveCategory: ${category.id}")

            // 1. Room DB에 저장
            localCategoriesDataSource.saveCategory(category)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (category.isNew) "CREATE" else "UPDATE"
            localCategoriesDataSource.addToOutbox(
                categoryId = category.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            Log.d(TAG, "Category saved and added to outbox: ${category.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveCategory failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveCategories(categories: List<Category>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveCategories: ${categories.size} categories")

            if (categories.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localCategoriesDataSource.saveCategories(categories)

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
            localCategoriesDataSource.deleteCategory(categoryId)

            // 2. Outbox에 삭제 작업 추가
            localCategoriesDataSource.addToOutbox(
                categoryId = categoryId,
                operation = "DELETE",
                payload = null
            )

            Log.d(TAG, "Category deleted and added to outbox: $categoryId")
            CustomResult.Success(Unit)

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
            val currentCategory = localCategoriesDataSource.getCategoryById(categoryId)
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
            localCategoriesDataSource.categoryExists(categoryId)
        } catch (e: Exception) {
            Log.e(TAG, "categoryExists failed", e)
            false
        }
    }

    override suspend fun nameExists(name: CategoryName, excludeCategoryId: String?): Boolean {
        return try {
            localCategoriesDataSource.nameExists(name.value, excludeCategoryId)
        } catch (e: Exception) {
            Log.e(TAG, "nameExists failed", e)
            false
        }
    }

    override suspend fun getTotalCategoryCount(): Int {
        return try {
            localCategoriesDataSource.getTotalCategoryCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalCategoryCount failed", e)
            0
        }
    }

    override suspend fun getCategoryCountByProject(projectId: String): Int {
        return try {
            localCategoriesDataSource.getCategoryCountByProject(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getCategoryCountByProject failed", e)
            0
        }
    }

    override suspend fun getNextCategoryOrder(projectId: String): Int {
        return try {
            localCategoriesDataSource.getNextCategoryOrder(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getNextCategoryOrder failed", e)
            1
        }
    }

    override suspend fun clearAllCategories(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllCategories")

            localCategoriesDataSource.clearAllCategories()

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
            localCategoriesDataSource.getCategoriesUpdatedAfter(timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "getCategoriesUpdatedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun addToOutbox(
        categoryId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "addToOutbox: categoryId=$categoryId, operation=$operation")

            localCategoriesDataSource.addToOutbox(categoryId, operation, payload)

            Log.d(TAG, "Added to outbox: $categoryId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}
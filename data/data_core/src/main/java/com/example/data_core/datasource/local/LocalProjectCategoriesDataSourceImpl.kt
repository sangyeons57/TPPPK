package com.example.data_core.datasource.local

import android.util.Log
import com.example.data_core.dao.CategoriesDao
import com.example.data_core.dao.OutboxDao
import com.example.domain.model.base.Category
import com.example.mapper.entity.CategoryEntityMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalProjectCategoriesDataSourceImpl @Inject constructor(
    private val categoriesDao: CategoriesDao,
    private val outboxDao: OutboxDao,
    private val mapper: CategoryEntityMapper
) : LocalProjectCategoriesDataSource {

    companion object {
        private const val TAG = "LocalCategoriesDataSource"
        private const val COLLECTION_NAME = "categories"
    }

    override suspend fun getCategoryById(categoryId: String): Category? {
        return try {
            categoriesDao.getCategoryById(categoryId)?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get category by ID: $categoryId", e)
            null
        }
    }

    override suspend fun getCategoriesByProject(projectId: String): List<Category> {
        return try {
            categoriesDao.getCategoriesByProject(projectId).map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get categories by project: $projectId", e)
            emptyList()
        }
    }

    override suspend fun saveCategory(category: Category, projectId: String) {
        try {
            val entity = mapper.toEntity(category, projectId)
            categoriesDao.insertCategory(entity)
            Log.d(TAG, "Category saved: ${category.id.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save category: ${category.id.value}", e)
            throw e
        }
    }

    override suspend fun saveCategories(categories: List<Category>, projectId: String) {
        try {
            if (categories.isEmpty()) return
            val entities = categories.map { mapper.toEntity(it, projectId) }
            categoriesDao.insertCategories(entities)
            Log.d(TAG, "Categories saved: ${categories.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save categories", e)
            throw e
        }
    }

    override suspend fun deleteCategory(categoryId: String) {
        try {
            categoriesDao.deleteCategory(categoryId)
            Log.d(TAG, "Category deleted: $categoryId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete category: $categoryId", e)
            throw e
        }
    }

    override suspend fun getCategoriesUpdatedAfter(timestamp: Instant): List<Category> {
        return try {
            categoriesDao.getCategoriesUpdatedAfter(timestamp).map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get categories updated after: $timestamp", e)
            emptyList()
        }
    }

    override fun observeCategoryById(categoryId: String): Flow<Category?> {
        return categoriesDao.observeCategoryById(categoryId).map { it?.let { mapper.toDomain(it) } }
    }

    override fun observeCategoriesByProject(projectId: String): Flow<List<Category>> {
        return categoriesDao.observeCategoriesByProject(projectId).map { list ->
            list.map { mapper.toDomain(it) }
        }
    }

    override suspend fun getNextCategoryOrder(projectId: String): Int {
        return try {
            (categoriesDao.getMaxOrderInProject(projectId) ?: 0) + 1
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get next category order for project: $projectId", e)
            1
        }
    }

    override suspend fun clearAllCategories() {
        try {
            categoriesDao.deleteAllCategories()
            Log.d(TAG, "All categories cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all categories", e)
            throw e
        }
    }
}
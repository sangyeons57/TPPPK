package com.example.data.datasource.local

import android.util.Log
import com.example.data.dao.CategoriesDao
import com.example.data.dao.OutboxDao
import com.example.data.dao.SyncMetadataDao
import com.example.data.mapper.CategoriesMapper
import com.example.data.model.local.OutboxEntity
import com.example.data.model.local.SyncMetadataEntity
import com.example.domain.model.base.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Categories DataSource Implementation
 * Room Database를 통한 카테고리 데이터 관리
 * 3-tier 클라이언트 주도 동기화 아키텍처 지원
 */
@Singleton
class LocalCategoriesDataSourceImpl @Inject constructor(
    private val categoriesDao: CategoriesDao,
    private val outboxDao: OutboxDao,
    private val syncMetadataDao: SyncMetadataDao
) : LocalCategoriesDataSource {

    companion object {
        private const val TAG = "LocalCategoriesDataSource"
        private const val COLLECTION_NAME = "categories"
    }

    // === 기본 CRUD 작업 ===

    override suspend fun getCategoryById(categoryId: String): Category? {
        return try {
            val entity = categoriesDao.getCategoryById(categoryId)
            entity?.let { CategoriesMapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get category by ID: $categoryId", e)
            null
        }
    }

    override suspend fun getCategoryByName(name: String): Category? {
        return try {
            // CategoriesDao에 이 메서드가 없으면 추가 필요
            // 현재는 getAllCategories로 대체
            val entities = categoriesDao.getAllCategories()
            val entity = entities.find { it.name == name }
            entity?.let { CategoriesMapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get category by name: $name", e)
            null
        }
    }

    override suspend fun searchCategoriesByName(name: String, limit: Int): List<Category> {
        return try {
            // 부분 일치 검색 - DAO에 메서드 추가 필요할 수 있음
            val entities = categoriesDao.getAllCategories()
            val filtered = entities.filter { it.name.contains(name, ignoreCase = true) }
                .take(limit)
            CategoriesMapper.toDomainList(filtered)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search categories by name: $name", e)
            emptyList()
        }
    }

    override suspend fun getCategoriesByIds(categoryIds: List<String>): List<Category> {
        return try {
            val categories = mutableListOf<Category>()
            for (id in categoryIds) {
                getCategoryById(id)?.let { categories.add(it) }
            }
            categories
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get categories by IDs", e)
            emptyList()
        }
    }

    override suspend fun getAllCategories(limit: Int?): List<Category> {
        return try {
            val entities = categoriesDao.getAllCategories()
            val limitedEntities = if (limit != null) entities.take(limit) else entities
            CategoriesMapper.toDomainList(limitedEntities)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all categories", e)
            emptyList()
        }
    }

    override suspend fun getCategoriesByProject(projectId: String): List<Category> {
        return try {
            val entities = categoriesDao.getCategoriesByProject(projectId)
            CategoriesMapper.toDomainList(entities)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get categories by project: $projectId", e)
            emptyList()
        }
    }

    override suspend fun getCategoriesByOrderRange(minOrder: Int, maxOrder: Int): List<Category> {
        return try {
            // DAO에 이 메서드 추가 필요할 수 있음
            val entities = categoriesDao.getAllCategories()
            val filtered = entities.filter { it.order in minOrder..maxOrder }
            CategoriesMapper.toDomainList(filtered)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get categories by order range: $minOrder-$maxOrder", e)
            emptyList()
        }
    }

    override suspend fun getNoCategoryByProject(projectId: String): Category? {
        return try {
            val entities = categoriesDao.getCategoriesByProject(projectId)
            val noCategoryEntity = entities.find { !it.isCategory }
            noCategoryEntity?.let { CategoriesMapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get NoCategory by project: $projectId", e)
            null
        }
    }

    override suspend fun saveCategory(category: Category) {
        try {
            // projectId가 필요한데 Category 도메인 모델에 없음
            // 임시로 빈 문자열 사용, 실제 구현에서는 수정 필요
            val projectId = "" // TODO: Category 모델에 projectId 추가하거나 매개변수로 받기
            val entity = CategoriesMapper.toEntity(category, projectId)
            categoriesDao.insertCategory(entity)
            Log.d(TAG, "Category saved: ${category.id.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save category: ${category.id.value}", e)
            throw e
        }
    }

    override suspend fun saveCategories(categories: List<Category>) {
        try {
            if (categories.isEmpty()) return

            // projectId 문제 동일
            val projectId = "" // TODO: 수정 필요
            val entities = CategoriesMapper.toEntityList(categories, projectId)
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

    // === 3-tier 동기화 지원 ===

    override suspend fun getCategoriesUpdatedAfter(timestamp: Instant): List<Category> {
        return try {
            val entities = categoriesDao.getCategoriesUpdatedAfter(timestamp)
            CategoriesMapper.toDomainList(entities)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get categories updated after: $timestamp", e)
            emptyList()
        }
    }

    override fun observeCategoryById(categoryId: String): Flow<Category?> {
        return try {
            categoriesDao.observeCategoryById(categoryId)
                .map { entity -> entity?.let { CategoriesMapper.toDomain(it) } }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to observe category: $categoryId", e)
            kotlinx.coroutines.flow.flowOf(null)
        }
    }

    override fun observeByName(name: String): Flow<Category?> {
        return try {
            kotlinx.coroutines.flow.flow {
                emit(getCategoryByName(name))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to observe category by name: $name", e)
            kotlinx.coroutines.flow.flowOf(null)
        }
    }

    override fun observeAllByName(name: String, limit: Int): Flow<List<Category>> {
        return try {
            kotlinx.coroutines.flow.flow {
                emit(searchCategoriesByName(name, limit))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to observe categories by name: $name", e)
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    override fun observeCategoriesByProject(projectId: String): Flow<List<Category>> {
        return try {
            categoriesDao.observeCategoriesByProject(projectId)
                .map { entities -> CategoriesMapper.toDomainList(entities) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to observe categories by project: $projectId", e)
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    override fun observeCategories(categoryIds: List<String>): Flow<List<Category>> {
        return try {
            kotlinx.coroutines.flow.flow {
                emit(getCategoriesByIds(categoryIds))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to observe categories by IDs", e)
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    override fun observeCategoryUpdatedAt(categoryId: String): Flow<Long?> {
        return try {
            kotlinx.coroutines.flow.flow {
                val category = getCategoryById(categoryId)
                emit(category?.updatedAt?.toEpochMilli())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to observe category updatedAt: $categoryId", e)
            kotlinx.coroutines.flow.flowOf(null)
        }
    }

    override fun observeAllCategories(): Flow<List<Category>> {
        return try {
            categoriesDao.observeAllCategories()
                .map { entities -> CategoriesMapper.toDomainList(entities) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to observe all categories", e)
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    override fun observeCategoriesByOrderRange(minOrder: Int, maxOrder: Int): Flow<List<Category>> {
        return try {
            kotlinx.coroutines.flow.flow {
                emit(getCategoriesByOrderRange(minOrder, maxOrder))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to observe categories by order range: $minOrder-$maxOrder", e)
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    // === Outbox 관리 ===

    override suspend fun addToOutbox(categoryId: String, operation: String, payload: String?) {
        try {
            val outboxEntity = OutboxEntity.create(
                entityType = COLLECTION_NAME,
                entityId = categoryId,
                operation = operation,
                payload = payload
            )
            outboxDao.insertOutboxEntry(outboxEntity)
            Log.d(TAG, "Added to outbox: $categoryId, $operation")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add to outbox: $categoryId", e)
            throw e
        }
    }

    override suspend fun getPendingOutboxOperations(): List<CategoryOutboxOperation> {
        return try {
            val outboxEntries = outboxDao.getPendingEntriesByEntityType(COLLECTION_NAME)
            outboxEntries.map { entry ->
                CategoryOutboxOperation(
                    id = entry.id,
                    categoryId = entry.entityId,
                    operation = entry.operation,
                    payload = entry.payload,
                    localTimestamp = entry.localTimestamp,
                    retries = entry.retries
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get pending outbox operations", e)
            emptyList()
        }
    }

    override suspend fun markOutboxOperationComplete(operationId: String) {
        try {
            outboxDao.markEntryProcessed(operationId)
            Log.d(TAG, "Outbox operation marked complete: $operationId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark outbox operation complete: $operationId", e)
            throw e
        }
    }

    override suspend fun incrementOutboxRetries(operationId: String) {
        try {
            outboxDao.incrementRetries(operationId)
            Log.d(TAG, "Outbox retries incremented: $operationId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to increment outbox retries: $operationId", e)
            throw e
        }
    }

    // === 동기화 메타데이터 관리 ===

    override suspend fun getLastSyncCursor(): Long? {
        return try {
            val metadata = syncMetadataDao.getSyncMetadata(COLLECTION_NAME)
            metadata?.lastServerCursor
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get last sync cursor", e)
            null
        }
    }

    override suspend fun updateSyncCursor(cursor: Long, timestamp: Long) {
        try {
            val metadata = SyncMetadataEntity(
                entityType = COLLECTION_NAME,
                lastServerCursor = cursor,
                lastSyncTimestamp = timestamp
            )
            syncMetadataDao.insertOrUpdateSyncMetadata(metadata)
            Log.d(TAG, "Sync cursor updated: $cursor")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update sync cursor", e)
            throw e
        }
    }

    // === 유틸리티 ===

    override suspend fun categoryExists(categoryId: String): Boolean {
        return try {
            getCategoryById(categoryId) != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check category existence: $categoryId", e)
            false
        }
    }

    override suspend fun nameExists(name: String, excludeCategoryId: String?): Boolean {
        return try {
            val entities = categoriesDao.getAllCategories()
            entities.any { entity ->
                entity.name == name && (excludeCategoryId == null || entity.id != excludeCategoryId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check name existence: $name", e)
            false
        }
    }

    override suspend fun getTotalCategoryCount(): Int {
        return try {
            categoriesDao.getAllCategories().size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get total category count", e)
            0
        }
    }

    override suspend fun getCategoryCountByProject(projectId: String): Int {
        return try {
            categoriesDao.getCategoryCountByProject(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get category count by project: $projectId", e)
            0
        }
    }

    override suspend fun getNextCategoryOrder(projectId: String): Int {
        return try {
            val maxOrder = categoriesDao.getMaxOrderInProject(projectId) ?: 0
            maxOrder + 1
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
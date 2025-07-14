package com.example.domain.usecase.project.category

import com.example.core_common.result.CustomResult
import com.example.domain.event.EventDispatcher
import com.example.domain.model.base.Category
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.repository.base.CategoryRepository
import javax.inject.Inject

/**
 * Use case for reordering categories and normalizing their order values.
 * This ensures that category orders are sequential (0.0, 1.0, 2.0, etc.)
 * instead of potentially having gaps (0.0, 1.0, 4.0, 5.0, etc.)
 */
interface ReorderCategoriesUseCase {
    /**
     * Invokes the use case to reorder and normalize category orders.
     *
     * @param projectId The project ID containing the categories
     * @param categoryIds List of category IDs in their desired order
     * @return A [CustomResult] indicating success (Unit) or failure (Exception)
     */
    suspend operator fun invoke(
        projectId: DocumentId,
        categoryIds: List<String>
    ): CustomResult<Unit, Exception>
}

/**
 * Implementation of [ReorderCategoriesUseCase].
 *
 * @property categoryRepository The repository to interact with category data.
 */
class ReorderCategoriesUseCaseImpl @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ReorderCategoriesUseCase {
    
    override suspend fun invoke(
        projectId: DocumentId,
        categoryIds: List<String>
    ): CustomResult<Unit, Exception> {
        try {
            // Get all categories for the project
            val allCategoriesResult = categoryRepository.getAll()
            if (allCategoriesResult !is CustomResult.Success) {
                return CustomResult.Failure(Exception("Failed to fetch categories"))
            }
            
            val allCategories = allCategoriesResult.data
            
            // Create a map for quick lookup
            val categoryMap = allCategories.associateBy { it.id.value }
            
            // Validate that all provided IDs exist
            val missingIds = categoryIds.filter { it !in categoryMap }
            if (missingIds.isNotEmpty()) {
                return CustomResult.Failure(IllegalArgumentException("Categories not found: $missingIds"))
            }
            
            // Update each category with its new normalized order
            categoryIds.forEachIndexed { index, categoryId ->
                val category = categoryMap[categoryId]
                    ?: return CustomResult.Failure(IllegalArgumentException("Category not found: $categoryId"))
                
                val newOrder = CategoryOrder(index.toDouble())
                category.changeOrder(newOrder)
                
                // Save the updated category
                when (val saveResult = categoryRepository.save(category)) {
                    is CustomResult.Success -> {
                        EventDispatcher.publish(category)
                    }
                    is CustomResult.Failure -> {
                        return CustomResult.Failure(saveResult.error)
                    }
                    else -> {
                        return CustomResult.Failure(Exception("Failed to save category: $categoryId"))
                    }
                }
            }
            
            return CustomResult.Success(Unit)
            
        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }
}
package com.example.domain.usecase.category

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.repository.CategoryRepository
import java.time.Instant
import javax.inject.Inject

/**
 * Use case for updating a category's details.
 * It handles the business logic for validating and preparing a category for update.
 */
interface UpdateCategoryUseCase {
    /**
     * Invokes the use case to update a category.
     *
     * @param categoryToUpdate The original category object that needs to be updated.
     * @param newName The new name for the category. Must not be blank.
     * @param newOrder The new order for the category. Must be between 0.0 and totalCategories (inclusive).
     * @param totalCategories The total number of categories, used for validating the newOrder.
     * @return A [CustomResult] indicating success (Unit) or failure (Exception).
     */
    suspend operator fun invoke(
        projectId: String, // Added projectId
        categoryToUpdate: Category,
        newName: String,
        newOrder: Double,
        totalCategories: Int
    ): CustomResult<Unit, Exception>
}

/**
 * Implementation of [UpdateCategoryUseCase].
 *
 * @property categoryRepository The repository to interact with category data.
 */
class UpdateCategoryUseCaseImpl @Inject constructor(
    private val categoryRepository: CategoryRepository
) : UpdateCategoryUseCase {
    override suspend fun invoke(
        projectId: String, // Added projectId
        categoryToUpdate: Category,
        newName: String,
        newOrder: Double,
        totalCategories: Int
    ): CustomResult<Unit, Exception> {
        // Validate new name
        if (newName.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Category name cannot be blank."))
        }

        // Validate new order
        // Assuming order is 0-indexed if it directly maps to list indices, or 1-indexed if it's a position.
        // The prompt stated "order의 최소값은 0 최대값은 카테고리 개수", so if 3 categories, max order is 3.0.
        // This implies orders could be 0.0, 1.0, 2.0, 3.0 if totalCategories is 3 (allowing 4 positions including 0).
        // Or, if totalCategories is the count, and orders are like 0, 1, ..., count-1, then max order is totalCategories - 1.
        // Let's stick to the prompt: min 0, max totalCategories.
        if (newOrder < 0.0 || newOrder > totalCategories) {
            return CustomResult.Failure(IllegalArgumentException("Invalid category order. Must be between 0.0 and $totalCategories."))
        }

        // Perform update within aggregate to keep invariants and raise events
        categoryToUpdate.update(newName, newOrder)

        return categoryRepository.updateCategory(projectId, categoryToUpdate)
    }
}

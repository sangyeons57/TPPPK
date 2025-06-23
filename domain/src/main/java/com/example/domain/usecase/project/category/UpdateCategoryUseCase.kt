package com.example.domain.usecase.project.category

import com.example.core_common.result.CustomResult
import com.example.domain.event.EventDispatcher
import com.example.domain.model.base.Category
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.repository.base.CategoryRepository
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
        projectId: DocumentId, // Added projectId
        categoryToUpdate: Category,
        newName: CategoryName,
        newOrder: CategoryOrder,
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
        projectId: DocumentId,
        categoryToUpdate: Category,
        newName: CategoryName,
        newOrder: CategoryOrder,
        totalCategories: Int
    ): CustomResult<Unit, Exception> {
        // Validate new name
        if (newName.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Category name cannot be blank."))
        }

        if ( newOrder.value > totalCategories) {
            return CustomResult.Failure(IllegalArgumentException("Invalid category order. Must be between 0.0 and $totalCategories."))
        }

        // Perform update within aggregate to keep invariants and raise events
        categoryToUpdate.update(newName, newOrder)

        return when(val result = categoryRepository.save(categoryToUpdate)) {
            is CustomResult.Success -> {
                EventDispatcher.publish(categoryToUpdate)
                CustomResult.Success(Unit)
            }
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
}

package com.example.domain.usecase.project

import com.example.domain._repository.CategoryRepository // Corrected import path
import javax.inject.Inject

/**
 * 프로젝트 카테고리를 삭제하는 유스케이스 인터페이스
 */
interface DeleteCategoryUseCase {
    suspend operator fun invoke(categoryId: String): Result<Unit>
}

/**
 * DeleteCategoryUseCase의 구현체. This Usecase now relies on CategoryRepository.
 * @param categoryRepository 카테고리 관련 데이터 접근을 위한 Repository.
 */
class DeleteCategoryUseCaseImpl @Inject constructor(
    private val categoryRepository: CategoryRepository
) : DeleteCategoryUseCase {

    /**
     * 유스케이스를 실행하여 프로젝트 카테고리를 삭제합니다.
     * @param categoryId 삭제할 카테고리의 ID
     * @return Result<Unit> 삭제 처리 결과
     */
    override suspend fun invoke(categoryId: String): Result<Unit> {
        return categoryRepository.deleteCategory(categoryId)
    }
}
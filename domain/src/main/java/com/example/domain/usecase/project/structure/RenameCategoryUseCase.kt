package com.example.domain.usecase.project.structure

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.repository.base.CategoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 카테고리 이름을 변경하는 유스케이스
 * TODO: CategoryCollectionRepository 제거 후 실제 구현 필요
 */
interface RenameCategoryUseCase {
    /**
     * 카테고리 이름을 변경합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param newName 새 이름
     * @return 변경 결과를 포함한 CustomResult
     */
    suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId,
        newName: String
    ): CustomResult<Unit, Exception>
}

/**
 * RenameCategoryUseCase 실제 구현체
 * Provider로부터 CategoryRepository를 받아 카테고리 이름을 변경합니다.
 */
class RenameCategoryUseCaseImpl @Inject constructor(
    private val categoryRepository: CategoryRepository
) : RenameCategoryUseCase {
    
    /**
     * 카테고리 이름을 변경합니다.
     */
    override suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId,
        newName: String
    ): CustomResult<Unit, Exception> {
        return try {
            // 카테고리 조회
            val categoryResult = categoryRepository.observe(categoryId).first()
            
            when (categoryResult) {
                is CustomResult.Success -> {
                    val category = categoryResult.data as Category
                    
                    // 카테고리 이름 변경
                    category.changeName(CategoryName.from(newName))
                    
                    // 변경된 카테고리 저장
                    val saveResult = categoryRepository.save(category)
                    
                    when (saveResult) {
                        is CustomResult.Success -> CustomResult.Success(Unit)
                        is CustomResult.Failure -> saveResult
                        else -> CustomResult.Failure(Exception("Unexpected save result"))
                    }
                }
                is CustomResult.Failure -> categoryResult
                else -> CustomResult.Failure(Exception("Failed to load category"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}

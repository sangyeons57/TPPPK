package com.example.domain.usecase.project.structure

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.CategoryRepository
import javax.inject.Inject

/**
 * 카테고리를 삭제하는 유스케이스
 * DDD 방식에 따라 CategoryRepository를 직접 사용합니다.
 */
interface DeleteCategoryUseCase {
    /**
     * 카테고리를 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 삭제할 카테고리 ID
     * @return 삭제 성공 여부를 포함한 CustomResult
     */
    suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId
    ): CustomResult<Unit, Exception>
}

/**
 * DeleteCategoryUseCase 구현체
 * Provider로부터 CategoryRepository를 받아 카테고리를 삭제합니다.
 */
class DeleteCategoryUseCaseImpl @Inject constructor(
    private val categoryRepository: CategoryRepository
) : DeleteCategoryUseCase {
    
    /**
     * 카테고리를 삭제합니다.
     * 
     * @param projectId 프로젝트 ID (사용되지 않음 - Repository에서 이미 프로젝트별로 생성됨)
     * @param categoryId 삭제할 카테고리 ID
     * @return 삭제 성공 여부를 포함한 CustomResult
     */
    override suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId
    ): CustomResult<Unit, Exception> {
        // Repository의 delete() 메서드를 사용하여 카테고리 삭제
        return categoryRepository.delete(categoryId)
    }
}
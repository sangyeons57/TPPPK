package com.example.domain.usecase.project.structure

import com.example.core_common.result.CustomResult
import com.example.domain.repository.collection.CategoryCollectionRepository
import javax.inject.Inject

/**
 * 카테고리 컬렉션 목록에서 카테고리를 삭제하는 유스케이스
 * 
 * 이 유스케이스는 카테고리 컬렉션 목록에서 카테고리를 삭제하고 남은 카테고리들의 순서를 재정렬하는 기능을 제공합니다.
 */
interface DeleteCategoryUseCase {
    /**
     * 카테고리 컬렉션 목록에서 카테고리를 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 삭제할 카테고리 ID
     * @return 삭제 성공 여부를 포함한 CustomResult
     */
    suspend operator fun invoke(
        projectId: String,
        categoryId: String
    ): CustomResult<Unit, Exception>
}

/**
 * DeleteCategoryUseCase 구현체
 */
class DeleteCategoryUseCaseImpl @Inject constructor(
    private val categoryCollectionRepository: CategoryCollectionRepository
) : DeleteCategoryUseCase {
    
    /**
     * 카테고리 컬렉션 목록에서 카테고리를 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 삭제할 카테고리 ID
     * @return 삭제 성공 여부를 포함한 CustomResult
     */
    override suspend operator fun invoke(
        projectId: String,
        categoryId: String
    ): CustomResult<Unit, Exception> {
        // 저장소를 통해 카테고리 삭제
        return categoryCollectionRepository.removeCategory(
            projectId = projectId,
            categoryId = categoryId
        )
    }
}
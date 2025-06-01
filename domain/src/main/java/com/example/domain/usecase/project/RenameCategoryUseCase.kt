package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.model.collection.CategoryCollection
import com.example.domain.repository.CategoryCollectionRepository
import javax.inject.Inject

/**
 * 카테고리 이름을 변경하는 유스케이스
 * 
 * 이 유스케이스는 프로젝트 구조에서 특정 카테고리의 이름을 변경하는 기능을 제공합니다.
 */
interface RenameCategoryUseCase {
    /**
     * 카테고리 이름을 변경합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 이름을 변경할 카테고리 ID
     * @param newName 새로운 카테고리 이름
     * @return 카테고리 이름이 변경된 카테고리 컬렉션을 포함한 CustomResult
     */
    suspend operator fun invoke(
        projectId: String,
        categoryId: String,
        newName: String
    ): CustomResult<CategoryCollection, Exception>
}

/**
 * RenameCategoryUseCase 구현체
 */
class RenameCategoryUseCaseImpl @Inject constructor(
    private val categoryCollectionRepository: CategoryCollectionRepository
) : RenameCategoryUseCase {
    
    /**
     * 카테고리 이름을 변경합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 이름을 변경할 카테고리 ID
     * @param newName 새로운 카테고리 이름
     * @return 카테고리 이름이 변경된 카테고리 컬렉션을 포함한 CustomResult
     */
    override suspend operator fun invoke(
        projectId: String,
        categoryId: String,
        newName: String
    ): CustomResult<CategoryCollection, Exception> {
        // 입력값 검증
        if (newName.isBlank()) {
            return CustomResult.Failure(Exception("New name is blank"))
        }
        
        // 저장소를 통해 카테고리 이름 변경
        return categoryCollectionRepository.renameCategory(
            projectId = projectId,
            categoryId = categoryId,
            newName = newName
        )
    }
}

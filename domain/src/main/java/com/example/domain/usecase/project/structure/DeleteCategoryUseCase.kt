package com.example.domain.usecase.project.structure

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.CategoryRepository
import com.example.domain.repository.factory.context.CategoryRepositoryFactoryContext
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
 */
class DeleteCategoryUseCaseImpl @Inject constructor(
    private val categoryRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<CategoryRepositoryFactoryContext, CategoryRepository>
) : DeleteCategoryUseCase {
    
    /**
     * 카테고리를 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 삭제할 카테고리 ID
     * @return 삭제 성공 여부를 포함한 CustomResult
     */
    override suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId
    ): CustomResult<Unit, Exception> {
        // CategoryRepository 생성
        // repository 생성은 viewmodel 에서 해야함
        // 정확히는 provider 에서 해야함 provider를 viemodel 에서 주입받고
        val categoryRepository = categoryRepositoryFactory.create(
            CategoryRepositoryFactoryContext(
                collectionPath = CollectionPath.projectCategories(projectId.value)
            )
        )
        
        // Repository의 delete() 메서드를 사용하여 카테고리 삭제
        return categoryRepository.delete(categoryId)
    }
}
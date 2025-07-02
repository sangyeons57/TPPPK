package com.example.domain.usecase.project.structure

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
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
 * RenameCategoryUseCase 임시 스텁 구현체
 * TODO: DDD 방식으로 CategoryRepository를 직접 사용하도록 수정 필요
 */
class RenameCategoryUseCaseImpl @Inject constructor() : RenameCategoryUseCase {
    
    /**
     * 임시 스텁 구현 - 항상 성공을 반환합니다.
     */
    override suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId,
        newName: String
    ): CustomResult<Unit, Exception> {
        // TODO: CategoryRepository를 사용한 실제 이름 변경 로직 구현
        return CustomResult.Success(Unit)
    }
}

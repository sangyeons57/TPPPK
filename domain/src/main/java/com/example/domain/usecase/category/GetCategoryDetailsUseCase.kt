package com.example.domain.usecase.category

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.CategoryRepository
import javax.inject.Inject

/**
 * 특정 카테고리의 상세 정보를 가져오는 유스케이스 인터페이스입니다.
 */
interface GetCategoryDetailsUseCase {
    /**
     * 지정된 프로젝트 ID와 카테고리 ID에 해당하는 카테고리의 상세 정보를 가져옵니다.
     *
     * @param projectId 카테고리가 속한 프로젝트의 ID입니다.
     * @param categoryId 가져올 카테고리의 ID입니다.
     * @return 카테고리 상세 정보 또는 실패 결과를 담은 [CustomResult]를 반환합니다.
     */
    suspend operator fun invoke(projectId: String, categoryId: String): CustomResult<Category, Exception>
}

/**
 * [GetCategoryDetailsUseCase]의 구현체입니다.
 *
 * @property categoryRepository 카테고리 데이터 처리를 위한 저장소입니다.
 */
class GetCategoryDetailsUseCaseImpl @Inject constructor(
    private val categoryRepository: CategoryRepository
) : GetCategoryDetailsUseCase {
    /**
     * 지정된 프로젝트 ID와 카테고리 ID에 해당하는 카테고리의 상세 정보를 [categoryRepository]를 통해 가져옵니다.
     *
     * @param projectId 카테고리가 속한 프로젝트의 ID입니다.
     * @param categoryId 가져올 카테고리의 ID입니다.
     * @return 카테고리 상세 정보 또는 실패 결과를 담은 [CustomResult]를 반환합니다.
     */
    override suspend operator fun invoke(projectId: String, categoryId: String): CustomResult<Category, Exception> {
        // 입력 값 유효성 검사 (선택 사항, 필요에 따라 추가)
        if (projectId.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Project ID cannot be blank."))
        }
        if (categoryId.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Category ID cannot be blank."))
        }
        return when (val result = categoryRepository.findById(DocumentId(categoryId))) {
            is CustomResult.Success -> CustomResult.Success(result.data as Category)
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> result
            is CustomResult.Loading -> result
            is CustomResult.Progress -> result
        }
    }
}

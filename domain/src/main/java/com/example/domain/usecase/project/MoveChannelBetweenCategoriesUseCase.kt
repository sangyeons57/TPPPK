package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.model.collection.CategoryCollection
import com.example.domain.repository.collection.CategoryCollectionRepository
import javax.inject.Inject

/**
 * 카테고리 컬렉션 목록 내에서 채널을 이동하는 유스케이스
 * 
 * 이 유스케이스는 채널을 한 카테고리에서 다른 카테고리로 이동하는 기능을 제공합니다.
 * CategoryCollectionRepository를 활용하여 채널 이동 로직을 처리합니다.
 */
interface MoveChannelBetweenCategoriesUseCase {
    /**
     * 채널을 이동합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param channelId 이동할 채널 ID
     * @param sourceCategoryId 원본 카테고리 ID
     * @param targetCategoryId 대상 카테고리 ID
     * @param newOrder 이동할 목표 순서
     * @return 이동 결과를 포함한 CustomResult
     */
    suspend operator fun invoke(
        projectId: String,
        channelId: String,
        sourceCategoryId: String,
        targetCategoryId: String,
        newOrder: Int
    ): CustomResult<List<CategoryCollection>, Exception>
}

/**
 * MoveChannelBetweenCategoriesUseCase 구현체
 * CategoryCollectionRepository를 사용하여 채널 이동 로직을 처리합니다.
 */
class MoveChannelBetweenCategoriesUseCaseImpl @Inject constructor(
    private val categoryCollectionRepository: CategoryCollectionRepository
) : MoveChannelBetweenCategoriesUseCase {
    
    /**
     * 채널을 이동합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param channelId 이동할 채널 ID
     * @param sourceCategoryId 원본 카테고리 ID
     * @param targetCategoryId 대상 카테고리 ID
     * @param newOrder 이동할 목표 순서
     * @return 이동 결과를 포함한 CustomResult
     */
    override suspend operator fun invoke(
        projectId: String,
        channelId: String,
        sourceCategoryId: String,
        targetCategoryId: String,
        newOrder: Int
    ): CustomResult<List<CategoryCollection>, Exception> {
        // 변경 사항이 없는 경우 검증
        if (sourceCategoryId == targetCategoryId) {
            // 같은 카테고리 내에서의 순서 변경은 다른 UseCase에서 처리할 수 있음
            return CustomResult.Failure(Exception("같은 카테고리 내에서의 이동은 지원하지 않습니다. MoveChannelOrderUseCase를 사용하세요."))
        }

        // CategoryCollectionRepository에 채널 이동 작업 위임
        return categoryCollectionRepository.moveChannelBetweenCategories(
            projectId = projectId,
            channelId = channelId,
            sourceCategoryId = sourceCategoryId,
            targetCategoryId = targetCategoryId,
            newOrder = newOrder
        )
    }
}

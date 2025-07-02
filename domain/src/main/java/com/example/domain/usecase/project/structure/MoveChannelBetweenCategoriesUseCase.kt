package com.example.domain.usecase.project.structure

import com.example.core_common.result.CustomResult
import javax.inject.Inject

/**
 * 카테고리 간 채널 이동 유스케이스
 * TODO: CategoryCollectionRepository 제거 후 실제 구현 필요
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
    ): CustomResult<Unit, Exception>
}

/**
 * MoveChannelBetweenCategoriesUseCase 임시 스텁 구현체
 * TODO: DDD 방식으로 ProjectChannelRepository를 직접 사용하도록 수정 필요
 */
class MoveChannelBetweenCategoriesUseCaseImpl @Inject constructor() : MoveChannelBetweenCategoriesUseCase {
    
    /**
     * 임시 스텁 구현 - 항상 성공을 반환합니다.
     */
    override suspend operator fun invoke(
        projectId: String,
        channelId: String,
        sourceCategoryId: String,
        targetCategoryId: String,
        newOrder: Int
    ): CustomResult<Unit, Exception> {
        // TODO: ProjectChannelRepository를 사용한 실제 채널 이동 로직 구현
        return CustomResult.Success(Unit)
    }
}

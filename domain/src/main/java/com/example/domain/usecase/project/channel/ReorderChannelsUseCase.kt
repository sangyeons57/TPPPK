package com.example.domain.usecase.project.channel

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.base.Category
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import com.example.domain.repository.base.ProjectChannelRepository
import javax.inject.Inject

/**
 * 채널들의 순서를 재정렬하는 UseCase
 */
interface ReorderChannelsUseCase {
    /**
     * 지정된 채널 ID 순서에 따라 채널들의 순서를 재정렬합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID (null이면 No_Category)
     * @param channelIds 새로운 순서의 채널 ID 목록
     * @return 재정렬 결과
     */
    suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId?,
        channelIds: List<String>
    ): CustomResult<Unit, Exception>
}

/**
 * ReorderChannelsUseCase 구현체
 */
class ReorderChannelsUseCaseImpl @Inject constructor(
    private val projectChannelRepository: ProjectChannelRepository
) : ReorderChannelsUseCase {

    override suspend fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId?,
        channelIds: List<String>
    ): CustomResult<Unit, Exception> {
        try {
            // 현재 채널들을 가져옴
            val currentChannels = when (val result = projectChannelRepository.findAll()) {
                is CustomResult.Success -> result.data.filterIsInstance<ProjectChannel>()
                is CustomResult.Failure -> return CustomResult.Failure(result.error)
                else -> return CustomResult.Failure(IllegalStateException("Unable to fetch channels"))
            }

            // 해당 카테고리의 채널들만 필터링
            val targetCategoryId = categoryId?.value ?: Category.NO_CATEGORY_ID
            val categoryChannels = currentChannels.filter { it.categoryId.value == targetCategoryId }
            
            // 채널 ID로 매핑
            val channelMap = categoryChannels.associateBy { it.id.value }
            
            // 새로운 순서로 채널들을 재정렬
            val reorderedChannels = channelIds.mapIndexed { index, channelId ->
                val channel = channelMap[channelId] as ProjectChannel?
                    ?: return CustomResult.Failure(IllegalArgumentException("Channel not found: $channelId"))
                
                // 모든 채널은 순서에 따라 0부터 시작하는 order 값을 가짐
                val newOrder = ProjectChannelOrder(index)
                
                // 채널 순서 업데이트
                channel.changeOrder(newOrder)
                channel
            }
            
            // 모든 채널 저장
            reorderedChannels.forEach { channel ->
                when (val saveResult = projectChannelRepository.save(channel)) {
                    is CustomResult.Failure -> return CustomResult.Failure(saveResult.error)
                    else -> { /* 성공 */ }
                }
            }
            
            return CustomResult.Success(Unit)
            
        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }
}
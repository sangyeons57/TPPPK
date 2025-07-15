package com.example.domain.usecase.project.channel

import com.example.core_common.result.CustomResult
import com.example.domain.event.EventDispatcher
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import com.example.domain.repository.base.ProjectChannelRepository
import javax.inject.Inject

/**
 * 프로젝트 채널을 업데이트하는 UseCase입니다.
 */
interface UpdateProjectChannelUseCase {
    /**
     * 채널을 업데이트합니다.
     *
     * @param channelToUpdate 업데이트할 채널 객체
     * @param newName 새로운 채널 이름
     * @param newOrder 새로운 채널 순서
     * @param newCategoryId 새로운 카테고리 ID (선택적)
     * @param newChannelType 새로운 채널 타입 (선택적)
     * @return 업데이트 결과
     */
    suspend operator fun invoke(
        channelToUpdate: ProjectChannel,
        newName: Name,
        newOrder: ProjectChannelOrder,
        newCategoryId: DocumentId? = null,
        newChannelType: ProjectChannelType? = null
    ): CustomResult<Unit, Exception>
}

/**
 * UpdateProjectChannelUseCase의 구현체
 */
class UpdateProjectChannelUseCaseImpl @Inject constructor(
    private val projectChannelRepository: ProjectChannelRepository
) : UpdateProjectChannelUseCase {
    override suspend fun invoke(
        channelToUpdate: ProjectChannel,
        newName: Name,
        newOrder: ProjectChannelOrder,
        newCategoryId: DocumentId?,
        newChannelType: ProjectChannelType?
    ): CustomResult<Unit, Exception> {
        // 유효성 검사
        if (newName.value.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Channel name cannot be blank."))
        }

        // No_Category에 속한 채널 order 검증
        if (channelToUpdate.categoryId.value == com.example.domain.model.base.Category.NO_CATEGORY_ID) {
            // No_Category 채널은 order 0으로 고정
            if (newOrder.value != com.example.domain.model.base.Category.NO_CATEGORY_ORDER) {
                return CustomResult.Failure(IllegalArgumentException("No_Category channel order must be ${com.example.domain.model.base.Category.NO_CATEGORY_ORDER}"))
            }
        } else {
            // 다른 카테고리 채널은 order 1 이상
            if (newOrder.value < com.example.domain.model.base.ProjectChannel.MIN_CHANNEL_ORDER) {
                return CustomResult.Failure(IllegalArgumentException("Channel order must be ${com.example.domain.model.base.ProjectChannel.MIN_CHANNEL_ORDER} or greater (${com.example.domain.model.base.Category.NO_CATEGORY_ORDER} is reserved for No_Category channels)"))
            }
        }

        try {
            // 채널 업데이트 (도메인 메서드 사용)
            channelToUpdate.updateName(newName)
            channelToUpdate.changeOrder(newOrder)
            
            if (newCategoryId != null) {
                channelToUpdate.moveToCategory(newCategoryId)
            }
            
            // channelType은 현재 ProjectChannel에 직접 변경 메서드가 없으므로 
            // 필요시 별도 메서드 추가 필요

            return when (val result = projectChannelRepository.save(channelToUpdate)) {
                is CustomResult.Success -> {
                    EventDispatcher.publish(channelToUpdate)
                    CustomResult.Success(Unit)
                }
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }
} 
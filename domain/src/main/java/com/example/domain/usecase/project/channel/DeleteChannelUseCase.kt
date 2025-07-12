package com.example.domain.usecase.project.channel

import com.example.core_common.result.CustomResult
import com.example.domain.event.EventDispatcher
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.ProjectChannelRepository
import javax.inject.Inject

/**
 * 프로젝트 구조에서 채널을 삭제하는 유스케이스
 * 실제로는 soft delete를 수행하여 채널을 DELETED 상태로 변경합니다.
 */
interface DeleteChannelUseCase {
    /**
     * 프로젝트 구조에서 채널을 soft delete 합니다.
     * 채널을 실제로 삭제하지 않고 DELETED 상태로 변경합니다.
     * 
     * @param channelId 삭제할 채널 ID
     * @return 삭제 결과를 포함한 CustomResult
     */
    suspend operator fun invoke(channelId: DocumentId): CustomResult<Unit, Exception>
}

/**
 * DeleteChannelUseCase 구현체
 */
class DeleteChannelUseCaseImpl @Inject constructor(
    private val projectChannelRepository: ProjectChannelRepository
) : DeleteChannelUseCase {
    
    override suspend operator fun invoke(channelId: DocumentId): CustomResult<Unit, Exception> {
        return try {
            // 채널을 soft delete 처리 (DELETED 상태로 변경)
            when (val getResult = projectChannelRepository.findById(channelId)) {
                is CustomResult.Success -> {
                    val channel = getResult.data as ProjectChannel
                    
                    // 채널을 DELETED 상태로 변경 (soft delete)
                    val deletedChannel = channel.markDeleted()
                    when (val saveResult = projectChannelRepository.save(deletedChannel)) {
                        is CustomResult.Success -> {
                            // soft delete 이벤트 발생
                            EventDispatcher.publish(deletedChannel)
                            CustomResult.Success(Unit)
                        }
                        is CustomResult.Failure -> CustomResult.Failure(saveResult.error)
                        is CustomResult.Loading -> CustomResult.Loading
                        is CustomResult.Initial -> CustomResult.Initial
                        is CustomResult.Progress -> CustomResult.Progress(saveResult.progress)
                    }
                }
                is CustomResult.Failure -> CustomResult.Failure(getResult.error)
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(getResult.progress)
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}
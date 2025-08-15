package com.example.domain_usecase.usecase.project.channel

import com.example.core_common.result.CustomResult
import com.example.domain.event.EventDispatcher
import com.example.domain.model.base.ProjectChannel
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.ProjectChannelRepository
import javax.inject.Inject
import java.time.Instant

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
            // 직접 Firestore 필드 업데이트를 위한 맵 생성
            val updateFields: Map<String, Any?> = mapOf(
                "status" to "DELETED",
                "updatedAt" to Instant.now()
            )

            // Repository를 통해 직접 필드 업데이트
            when (val result = projectChannelRepository.updateFields(channelId, updateFields)) {
                is CustomResult.Success -> CustomResult.Success(Unit)
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                else -> CustomResult.Failure(Exception("Unexpected update result"))
            }

        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}
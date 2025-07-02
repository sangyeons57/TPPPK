package com.example.domain.usecase.project.channel

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import javax.inject.Inject

/**
 * 프로젝트 구조에서 채널을 삭제하는 유스케이스
 * TODO: CategoryCollectionRepository 제거 후 실제 구현 필요
 */
interface DeleteChannelUseCase {
    /**
     * 프로젝트 구조에서 채널을 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 채널이 속한 카테고리 ID
     * @param channelId 삭제할 채널 ID
     * @return 삭제 결과를 포함한 CustomResult
     */
    suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId,
        channelId: DocumentId
    ): CustomResult<Unit, Exception>
}

/**
 * DeleteChannelUseCase 임시 스텁 구현체
 * TODO: DDD 방식으로 ProjectChannelRepository를 직접 사용하도록 수정 필요
 */
class DeleteChannelUseCaseImpl @Inject constructor() : DeleteChannelUseCase {
    
    /**
     * 임시 스텁 구현 - 항상 성공을 반환합니다.
     */
    override suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId,
        channelId: DocumentId
    ): CustomResult<Unit, Exception> {
        // TODO: ProjectChannelRepository를 사용한 실제 삭제 로직 구현
        return CustomResult.Success(Unit)
    }
}
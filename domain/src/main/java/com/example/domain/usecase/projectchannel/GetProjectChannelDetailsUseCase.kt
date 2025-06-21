package com.example.domain.usecase.projectchannel

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectChannel
import com.example.domain.repository.base.ProjectChannelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 특정 프로젝트 채널의 상세 정보를 가져오는 유스케이스 인터페이스입니다.
 */
interface GetProjectChannelDetailsUseCase {
    /**
     * 지정된 프로젝트 ID와 채널 ID에 해당하는 프로젝트 채널의 상세 정보를 스트림으로 가져옵니다.
     *
     * @param projectId 채널이 속한 프로젝트의 ID입니다.
     * @param channelId 가져올 채널의 ID입니다.
     * @return 프로젝트 채널 상세 정보 또는 실패 결과를 담은 [CustomResult]를 방출하는 [Flow]를 반환합니다.
     */
    operator fun invoke(projectId: String, channelId: String): Flow<CustomResult<ProjectChannel, Exception>>
}

/**
 * [GetProjectChannelDetailsUseCase]의 구현체입니다.
 *
 * @property projectChannelRepository 프로젝트 채널 데이터 처리를 위한 저장소입니다.
 */
class GetProjectChannelDetailsUseCaseImpl @Inject constructor(
    private val projectChannelRepository: ProjectChannelRepository
) : GetProjectChannelDetailsUseCase {
    /**
     * 지정된 프로젝트 ID와 채널 ID에 해당하는 프로젝트 채널의 상세 정보를 [projectChannelRepository]를 통해 스트림으로 가져옵니다.
     *
     * @param projectId 채널이 속한 프로젝트의 ID입니다.
     * @param channelId 가져올 채널의 ID입니다.
     * @return 프로젝트 채널 상세 정보 또는 실패 결과를 담은 [CustomResult]를 방출하는 [Flow]를 반환합니다.
     */
    override operator fun invoke(projectId: String, channelId: String): Flow<CustomResult<ProjectChannel, Exception>> {
        // 입력 값 유효성 검사 (선택 사항, 필요에 따라 추가)
        if (projectId.isBlank()) {
            // Flow를 반환해야 하므로, 에러를 Flow로 감싸서 반환하거나, Repository에서 처리하도록 위임할 수 있습니다.
            // 여기서는 Repository가 이미 Flow<CustomResult>를 반환하므로, 빈 ID 전달 시 Repository에서 에러 처리될 것으로 기대합니다.
            // 또는 kotlinx.coroutines.flow.flowOf(CustomResult.Failure(IllegalArgumentException("Project ID cannot be blank."))) 등을 사용할 수 있습니다.
        }
        if (channelId.isBlank()) {
            // 마찬가지로 Repository에서 처리될 것으로 기대
        }
        return projectChannelRepository.getProjectChannelStream(projectId, channelId)
    }
}

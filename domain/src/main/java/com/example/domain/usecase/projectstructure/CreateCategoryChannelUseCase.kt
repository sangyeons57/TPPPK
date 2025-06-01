package com.example.domain.usecase.projectstructure

import com.example.core_common.result.CustomResult
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.base.ProjectChannel
import com.example.domain.repository.ProjectChannelRepository
import com.example.domain.repository.ProjectRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 프로젝트의 특정 카테고리 내에 채널을 생성하는 UseCase입니다.
 *
 * @property projectRepository 프로젝트 관련 기능을 제공하는 Repository
 */
class CreateCategoryChannelUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectChannelRepository: ProjectChannelRepository,
) {
    /**
     * 지정된 프로젝트의 특정 카테고리에 채널을 생성합니다.
     *
     * @param projectId 채널을 생성할 프로젝트의 ID
     * @param categoryId 채널을 생성할 카테고리의 ID
     * @param name 생성할 채널의 이름
     * @param type 생성할 채널의 타입 ([ChannelMode])
     * @return 생성된 [Channel] 정보를 담은 [Result]
     */
    suspend operator fun invoke(projectId: String, categoryId: String, name: String, type: ProjectChannelType, order: Double): CustomResult<Unit, Exception> {
        if (projectId.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("프로젝트 ID는 비어있을 수 없습니다."))
        }
        if (categoryId.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("카테고리 ID는 비어있을 수 없습니다."))
        }
        if (name.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("채널 이름은 비어있을 수 없습니다."))
        }

        return projectChannelRepository.addProjectChannel(projectId = projectId, channel = ProjectChannel(
            channelName = name,
            channelType = type,
            order = order
        ))
    }
} 
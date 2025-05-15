package com.example.domain.usecase.projectstructure

import com.example.domain.model.Channel
import com.example.domain.model.ChannelMode
import com.example.domain.repository.ProjectRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 프로젝트 내에 직속 채널을 생성하는 UseCase입니다.
 *
 * @property projectRepository 프로젝트 관련 기능을 제공하는 Repository
 */
class CreateDirectChannelUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    /**
     * 지정된 프로젝트에 직속 채널을 생성합니다.
     *
     * @param projectId 채널을 생성할 프로젝트의 ID
     * @param name 생성할 채널의 이름
     * @param mode 생성할 채널의 타입 ([ChannelMode])
     * @return 생성된 [Channel] 정보를 담은 [Result]
     */
    suspend operator fun invoke(projectId: String, name: String, mode: ChannelMode, order: Int): Result<Channel> {
        if (projectId.isBlank()) {
            return Result.failure(IllegalArgumentException("프로젝트 ID는 비어있을 수 없습니다."))
        }
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("채널 이름은 비어있을 수 없습니다."))
        }
        // Type은 Enum이므로 별도 blank 체크 불필요

        return projectRepository.createDirectChannel(projectId, name.trim(), mode, order)
    }
} 
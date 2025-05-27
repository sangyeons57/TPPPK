// Required change for ProjectStructureRepository:
// interface ProjectStructureRepository {
//     // ... other methods
//     fun createCategoryChannel(projectId: String, categoryId: String, name: String, mode: ChannelMode, order: Int): Result<Channel> // Or ProjectChannelModel
// }
// TODO: Consider returning a more specific ProjectChannelModel instead of the generic Channel model from the repository.
package com.example.domain.usecase.projectstructure

import com.example.domain.model.Channel // TODO: Replace with ProjectChannelModel if defined by ProjectStructureRepository
import com.example.domain.model.ChannelMode
import com.example.domain.repository.ProjectStructureRepository // Changed from ProjectRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 프로젝트의 특정 카테고리 내에 채널을 생성하는 UseCase입니다.
 *
 * @property projectStructureRepository 프로젝트 구조 관련 기능을 제공하는 Repository
 */
class CreateCategoryChannelUseCase @Inject constructor(
    private val projectStructureRepository: ProjectStructureRepository // Changed from projectRepository
) {
    /**
     * 지정된 프로젝트의 특정 카테고리에 채널을 생성합니다.
     *
     * @param projectId 채널을 생성할 프로젝트의 ID
     * @param categoryId 채널을 생성할 카테고리의 ID
     * @param name 생성할 채널의 이름
     * @param mode 생성할 채널의 타입 ([ChannelMode])
     * @param order 채널의 순서
     * @return 생성된 [Channel] 정보를 담은 [Result] // TODO: Consider returning ProjectChannelModel
     */
    suspend operator fun invoke(projectId: String, categoryId: String, name: String, mode: ChannelMode, order: Int): Result<Channel> { // TODO: Consider Result<ProjectChannelModel>
        if (projectId.isBlank()) {
            return Result.failure(IllegalArgumentException("프로젝트 ID는 비어있을 수 없습니다."))
        }
        if (categoryId.isBlank()) {
            return Result.failure(IllegalArgumentException("카테고리 ID는 비어있을 수 없습니다."))
        }
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("채널 이름은 비어있을 수 없습니다."))
        }

        // Changed from projectRepository.createCategoryChannel to projectStructureRepository.createCategoryChannel
        return projectStructureRepository.createCategoryChannel(projectId, categoryId, name.trim(), mode, order)
    }
}
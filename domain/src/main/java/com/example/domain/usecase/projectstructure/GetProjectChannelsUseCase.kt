package com.example.domain.usecase.projectstructure

import com.example.domain.model.ProjectStructure
import com.example.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 특정 프로젝트의 전체 채널 구조(카테고리 및 직속 채널 포함)를 가져오는 UseCase입니다.
 *
 * @property projectRepository 프로젝트 관련 기능을 제공하는 Repository
 */
class GetProjectChannelsUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    /**
     * 지정된 프로젝트 ID에 대한 전체 채널 구조를 Flow 형태로 반환합니다.
     *
     * @param projectId 채널 구조를 조회할 프로젝트의 ID
     * @return 프로젝트의 전체 채널 구조를 담고 있는 [Flow]<[ProjectStructure]>
     */
    operator fun invoke(projectId: String): Flow<ProjectStructure> {
        return projectRepository.getProjectStructureStream(projectId)
    }
} 
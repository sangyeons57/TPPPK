package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.core_common.util.AuthUtil
import com.example.domain.model.base.Project
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.repository.ProjectRepository
import com.example.domain.repository.ProjectsWrapperRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 사용자가 참여하고 있는 프로젝트 목록을 가져오는 UseCase
 * 개인 일정을 추가할 때 선택 가능한 프로젝트 목록을 표시하는 등의 용도로 사용됩니다.
 */
interface GetUserParticipatingProjectsUseCase {
    /**
     * 현재 로그인한 사용자가 참여 중인 모든 프로젝트 목록을 가져옵니다.
     * @return Flow<CustomResult<List<Project>, Exception>> 프로젝트 목록을 포함한 결과
     */
    operator fun invoke(): Flow<CustomResult<List<Project>, Exception>>
}

/**
 * GetUserParticipatingProjectsUseCase 구현체
 * ProjectsWrapperRepository에서 사용자의 프로젝트 래퍼를 가져와 Project 정보로 변환합니다.
 */
class GetUserParticipatingProjectsUseCaseImpl @Inject constructor(
    private val projectsWrapperRepository: ProjectsWrapperRepository,
    private val projectRepository: ProjectRepository,
    private val authUtil: AuthUtil
) : GetUserParticipatingProjectsUseCase {

    override fun invoke(): Flow<CustomResult<List<Project>, Exception>> {
        val userId = authUtil.getCurrentUserId()

        // ProjectsWrapper에서 프로젝트 ID를 추출하고, ProjectRepository를 통해 전체 프로젝트 정보를 가져옵니다.
        return projectsWrapperRepository.getProjectsWrapperStream(userId)
            .map { wrapperResults ->
                resultTry {
                    // 성공적으로 가져온 프로젝트 래퍼만 필터링
                    val successWrappers = wrapperResults.mapNotNull { result ->
                        when (result) {
                            is CustomResult.Success -> result.data
                            else -> null
                        }
                    }

                    // 프로젝트 ID 리스트 추출
                    val projectIds = successWrappers.map { it.projectId }
                    
                    // 각 프로젝트 ID에 대한 전체 프로젝트 정보 가져오기
                    val projects = projectIds.mapNotNull { projectId ->
                        val projectResult = projectRepository.getProjectDetailsStream(projectId).first()
                        when (projectResult) {
                            is CustomResult.Success -> projectResult.data
                            else -> null
                        }
                    }
                    projects
                }
            }
    }
}


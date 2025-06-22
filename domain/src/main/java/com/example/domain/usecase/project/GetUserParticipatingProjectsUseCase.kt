package com.example.domain.usecase.project

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.domain.event.AggregateRoot
import com.example.domain.model.base.Project
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.repository.base.ProjectRepository
import com.example.domain.repository.base.ProjectsWrapperRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
    suspend operator fun invoke(): Flow<CustomResult<List<Project>, Exception>>
}

/**
 * GetUserParticipatingProjectsUseCase 구현체
 * ProjectsWrapperRepository에서 사용자의 프로젝트 래퍼를 가져와 Project 정보로 변환합니다.
 */
class GetUserParticipatingProjectsUseCaseImpl @Inject constructor(
    private val projectsWrapperRepository: ProjectsWrapperRepository,
    private val projectRepository: ProjectRepository,
) : GetUserParticipatingProjectsUseCase {

    override suspend fun invoke(): Flow<CustomResult<List<Project>, Exception>> {

        // ProjectsWrapper에서 프로젝트 ID를 추출하고, ProjectRepository를 통해 전체 프로젝트 정보를 가져옵니다.
        return when (val wrappersResult : CustomResult<List<AggregateRoot>, Exception> = projectsWrapperRepository.observeAll().first()) {
            is CustomResult.Success -> {
                val wrappers: List<ProjectsWrapper> = wrappersResult.data as List<ProjectsWrapper>
                flowOf (CustomResult.Success(
                    wrappers.map { wrapper ->
                        when(val projectResult = projectRepository.findById(wrapper.id)) {
                            is CustomResult.Success -> projectResult.data as Project
                            is CustomResult.Failure -> return flowOf(CustomResult.Failure(projectResult.error))
                            is CustomResult.Initial -> return flowOf(CustomResult.Initial)
                            is CustomResult.Loading -> return flowOf(CustomResult.Loading)
                            is CustomResult.Progress -> return flowOf(CustomResult.Progress(projectResult.progress))
                        }
                    }
                ))
            }
            is CustomResult.Failure -> flowOf(CustomResult.Failure(wrappersResult.error))
            is CustomResult.Initial -> flowOf(CustomResult.Initial)
            is CustomResult.Loading -> flowOf(CustomResult.Loading)
            is CustomResult.Progress -> flowOf(CustomResult.Progress(wrappersResult.progress))
        }
    }
}


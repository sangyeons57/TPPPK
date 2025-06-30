package com.example.domain.usecase.project.core


import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.domain.model.AggregateRoot
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
        return projectsWrapperRepository.observeAll().map { result ->
            Log.d("GetUserParticipatingProjectsUseCase", "projectsWrapperRepository.observeAll() emitted: $result")
            when (result) {
                is CustomResult.Success -> {
                    val wrappers: List<ProjectsWrapper> = result.data.map { it as ProjectsWrapper }
                    Log.d("GetUserParticipatingProjectsUseCase", "Wrapper ids: ${wrappers.map{it.id}}")
                    val projectResults = wrappers.map { wrapper ->
                        Log.d("GetUserParticipatingProjectsUseCase", "Fetching project id=${wrapper.id}")
                        projectRepository.findById(wrapper.id)
                    }

                    // 프로젝트 개별 조회 결과에 따라 전체 결과 타입 결정
                    when {
                        // 하나라도 Failure 가 있으면 즉시 Failure 반환
                        projectResults.any { it is CustomResult.Failure } -> {
                            val firstFailure = projectResults.first { it is CustomResult.Failure } as CustomResult.Failure<Exception>
                            Log.e("GetUserParticipatingProjectsUseCase", "Failure fetching project: ${firstFailure.error}")
                            return@map CustomResult.Failure(firstFailure.error)
                        }

                        // 진행률이 존재하면 Progress 상태 전달 (가장 첫 Progress 사용)
                        projectResults.any { it is CustomResult.Progress } -> {
                            val firstProgress = projectResults.first { it is CustomResult.Progress } as CustomResult.Progress
                            Log.d("GetUserParticipatingProjectsUseCase", "Encountered Progress state = ${firstProgress.progress}")
                            return@map CustomResult.Progress(firstProgress.progress)
                        }

                        // 로딩 중인 결과가 있으면 Loading 상태 전달
                        projectResults.any { it is CustomResult.Loading } -> {
                            Log.d("GetUserParticipatingProjectsUseCase", "Some project fetch still Loading")
                            return@map CustomResult.Loading
                        }

                        // 초기 상태가 있으면 Initial 상태 전달
                        projectResults.any { it is CustomResult.Initial } -> {
                            Log.d("GetUserParticipatingProjectsUseCase", "Initial state encountered (shouldn't happen)")
                            return@map CustomResult.Initial
                        }

                        // 전부 성공한 경우 리스트로 매핑
                        else -> {
                            val projects = projectResults.map { (it as CustomResult.Success).data as Project }
                            Log.d("GetUserParticipatingProjectsUseCase", "All projects fetched successfully: size=${projects.size}")
                            return@map CustomResult.Success(projects)
                        }
                    }
                }
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }
}


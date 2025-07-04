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
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import com.example.domain.model.vo.DocumentId
import com.google.firebase.firestore.FirebaseFirestoreException

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

        // ProjectsWrapper에서 프로젝트 ID별 CustomResult 를 Map 형태로 구성하여 어떤 ID에서 오류가 발생했는지 추적하기 쉽도록 변경한다.
        return projectsWrapperRepository.observeAll().map { result ->
            Log.d("GetUserParticipatingProjectsUseCase", "projectsWrapperRepository.observeAll() emitted: $result")
            when (result) {
                is CustomResult.Success -> {
                    val wrappers: List<ProjectsWrapper> = result.data.map { it as ProjectsWrapper }
                    Log.d("GetUserParticipatingProjectsUseCase", "Wrapper ids: ${wrappers.map { it.id }}")

                    // id -> 조회 결과 매핑
                    val projectResultsMap: Map<DocumentId, CustomResult<Project, Exception>> = wrappers.associate { wrapper ->
                        Log.d("GetUserParticipatingProjectsUseCase", "Fetching project id=${wrapper.id.value}")
                        (wrapper.id to projectRepository.findById(wrapper.id)) as Pair<DocumentId, CustomResult<Project, Exception>>
                    }

                    // ----- 결과 집계 -----
                    when {
                        // 하나라도 Failure 인 경우 ⇒ 어떤 ID 인지 명확히 로깅 후 Failure 반환
                        projectResultsMap.values.any { it is CustomResult.Failure } -> {
                            val failureEntry = projectResultsMap.entries.first { it.value is CustomResult.Failure }
                            val error = (failureEntry.value as CustomResult.Failure).error
                            Log.e(
                                "GetUserParticipatingProjectsUseCase",
                                "Failure fetching project id=${failureEntry.key.value}: $error"
                            )
                            CustomResult.Failure(error)
                        }

                        // 진행률 존재시 Progress 전달 (첫 번째 Progress 사용)
                        projectResultsMap.values.any { it is CustomResult.Progress } -> {
                            val progress = (projectResultsMap.values.first { it is CustomResult.Progress } as CustomResult.Progress).progress
                            Log.d("GetUserParticipatingProjectsUseCase", "Encountered Progress state = $progress")
                            CustomResult.Progress(progress)
                        }

                        // 로딩 중 결과가 있으면 Loading 전달
                        projectResultsMap.values.any { it is CustomResult.Loading } -> {
                            Log.d("GetUserParticipatingProjectsUseCase", "Some project fetch still Loading")
                            CustomResult.Loading
                        }

                        // 초기 상태가 있으면 Initial 전달
                        projectResultsMap.values.any { it is CustomResult.Initial } -> {
                            Log.d("GetUserParticipatingProjectsUseCase", "Initial state encountered (shouldn't happen)")
                            CustomResult.Initial
                        }

                        // 전부 성공한 경우 Map → List 변환 후 Success 반환
                        else -> {
                            val projects: List<Project> = projectResultsMap.values.map { (it as CustomResult.Success).data }
                            Log.d(
                                "GetUserParticipatingProjectsUseCase",
                                "All projects fetched successfully: size=${projects.size}"
                            )
                            CustomResult.Success(projects)
                        }
                    }
                }
                is CustomResult.Failure -> {
                    // 권한 에러인 경우 특별 처리: 빈 리스트 반환
                    val isPermissionError = result.error is FirebaseFirestoreException &&
                            (result.error as FirebaseFirestoreException).code == FirebaseFirestoreException.Code.PERMISSION_DENIED

                    if (isPermissionError) {
                        Log.w("GetUserParticipatingProjectsUseCase", "Permission denied for projects wrapper - returning empty list (user likely logged out)")
                        CustomResult.Success(emptyList())
                    } else {
                        CustomResult.Failure(result.error)
                    }
                }
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }.catch { exception ->
            // 전체 Flow에서 발생하는 예외를 catch하여 graceful하게 처리
            Log.e("GetUserParticipatingProjectsUseCase", "Unexpected error in flow", exception)
            
            // 권한 에러인 경우 빈 리스트 반환
            val isPermissionError = exception is FirebaseFirestoreException &&
                    exception.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            
            if (isPermissionError) {
                Log.w("GetUserParticipatingProjectsUseCase", "Permission denied - returning empty list (user likely logged out)")
                emit(CustomResult.Success(emptyList()))
            } else {
                emit(CustomResult.Failure(Exception("Unexpected error occurred while loading participating projects", exception)))
            }
        }
    }
}


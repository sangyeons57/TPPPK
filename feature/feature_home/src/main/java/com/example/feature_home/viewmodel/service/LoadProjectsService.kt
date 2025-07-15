package com.example.feature_home.viewmodel.service

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.provider.project.CoreProjectUseCaseProvider
import com.example.domain.provider.project.CoreProjectUseCases
import com.example.feature_home.model.ProjectUiModel
import com.example.feature_home.model.toProjectUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 프로젝트 데이터 로딩을 담당하는 Service
 * Domain UseCase들을 조합하여 UI에 특화된 프로젝트 데이터를 제공합니다.
 */
class LoadProjectsService @Inject constructor(
    private val coreProjectUseCaseProvider: CoreProjectUseCaseProvider
) {
    
    private lateinit var coreProjectUseCases: CoreProjectUseCases

    /**
     * 사용자가 참여한 프로젝트 목록을 UI에 최적화된 형태로 스트림 제공
     */
    fun getUserParticipatingProjectsStream(): Flow<CustomResult<List<ProjectUiModel>, Exception>> = flow {
        Log.d("LoadProjectsService", "Starting to load projects")
        coreProjectUseCases = coreProjectUseCaseProvider.createForCurrentUser()

        try {
            emitAll(
                coreProjectUseCases.getUserParticipatingProjectsUseCase().map { result ->
                    when (result) {
                        is CustomResult.Loading -> {
                            Log.d("LoadProjectsService", "Loading projects...")
                            CustomResult.Loading
                        }

                        is CustomResult.Success -> {
                            val projectWrappers = result.data
                            val mappedProjectUiModels = projectWrappers.map { it.toProjectUiModel() }
                            Log.d("LoadProjectsService", "Projects loaded: ${mappedProjectUiModels.size}")
                            CustomResult.Success(mappedProjectUiModels)
                        }

                        is CustomResult.Failure -> {
                            val errorMessage = result.error.message
                            val isPermissionError = errorMessage?.contains("permission", ignoreCase = true) == true ||
                                    errorMessage?.contains("PERMISSION_DENIED", ignoreCase = true) == true

                            if (isPermissionError) {
                                Log.w("LoadProjectsService", "Permission error - user likely logged out")
                                CustomResult.Success(emptyList())
                            } else {
                                Log.e("LoadProjectsService", "Failed to load projects", result.error)
                                CustomResult.Failure(result.error)
                            }
                        }

                        is CustomResult.Initial -> {
                            Log.d("LoadProjectsService", "Initial state")
                            CustomResult.Loading
                        }

                        is CustomResult.Progress -> {
                            Log.d("LoadProjectsService", "Progress: ${result.progress}%")
                            CustomResult.Loading
                        }
                    }
                }
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                Log.d("LoadProjectsService", "Load projects job was cancelled")
            } else {
                Log.e("LoadProjectsService", "Unexpected error in loadProjects", e)
                emit(CustomResult.Failure(e))
            }
        }
    }
}
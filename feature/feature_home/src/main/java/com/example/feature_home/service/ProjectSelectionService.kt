package com.example.feature_home.viewmodel.service

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Project
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.provider.project.CoreProjectUseCases
import com.example.domain.provider.project.ProjectStructureUseCases
import com.example.feature_home.model.ProjectStructureUiState
import com.example.feature_home.model.toProjectStructureUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map

/**
 * 프로젝트 선택 및 프로젝트 구조 관리를 담당하는 Service
 * Domain UseCase들을 조합하여 프로젝트 선택에 필요한 데이터를 제공합니다.
 */
class ProjectSelectionService(
    private val coreProjectUseCases: CoreProjectUseCases,
    private val projectStructureUseCases: ProjectStructureUseCases?
) {
    
    data class ProjectSelectionData(
        val projectDetails: Project,
        val projectStructure: ProjectStructureUiState
    )
    
    /**
     * 프로젝트 세부사항을 UI에 최적화된 형태로 스트림 제공
     */
    fun getProjectDetailsStream(projectId: DocumentId): Flow<CustomResult<Project, Exception>> = flow {
        Log.d("ProjectSelectionService", "Getting project details for: $projectId")
        
        // coreProjectUseCases는 항상 사용 가능
        
        try {
            emitAll(
                coreProjectUseCases.getProjectDetailsStreamUseCase(projectId).map { result ->
                    when (result) {
                        is CustomResult.Loading -> {
                            Log.d("ProjectSelectionService", "Loading project details...")
                            CustomResult.Loading
                        }

                        is CustomResult.Success -> {
                            Log.d("ProjectSelectionService", "Project details loaded: ${result.data.name}")
                            CustomResult.Success(result.data)
                        }

                        is CustomResult.Failure -> {
                            Log.e("ProjectSelectionService", "Failed to load project details", result.error)
                            CustomResult.Failure(result.error)
                        }

                        is CustomResult.Initial -> {
                            Log.d("ProjectSelectionService", "Initial state")
                            CustomResult.Loading
                        }

                        is CustomResult.Progress -> {
                            Log.d("ProjectSelectionService", "Progress: ${result.progress}%")
                            CustomResult.Loading
                        }
                    }
                }
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                Log.d("ProjectSelectionService", "Project details job was cancelled")
            } else {
                Log.e("ProjectSelectionService", "Unexpected error in getProjectDetails", e)
                emit(CustomResult.Failure(e))
            }
        }
    }
    
    /**
     * 프로젝트 구조를 UI에 최적화된 형태로 스트림 제공
     */
    fun getProjectStructureStream(projectId: DocumentId): Flow<CustomResult<ProjectStructureUiState, Exception>> = flow {
        Log.d("ProjectSelectionService", "Getting project structure for: $projectId")
        
        if (projectStructureUseCases == null) {
            Log.w("ProjectSelectionService", "ProjectStructureUseCases not available")
            emit(CustomResult.Failure(IllegalStateException("Service not available for this context")))
            return@flow
        }
        
        try {
            emitAll(
                projectStructureUseCases.getProjectStructureUseCase(projectId).map { result ->
                    when (result) {
                        is CustomResult.Loading -> {
                            Log.d("ProjectSelectionService", "Loading project structure...")
                            CustomResult.Loading
                        }

                        is CustomResult.Success -> {
                            Log.d("ProjectSelectionService", "Project structure loaded")
                            val structureUiState = result.data.toProjectStructureUiState()
                            CustomResult.Success(structureUiState)
                        }

                        is CustomResult.Failure -> {
                            Log.e("ProjectSelectionService", "Failed to load project structure", result.error)
                            CustomResult.Failure(result.error)
                        }

                        is CustomResult.Initial -> {
                            Log.d("ProjectSelectionService", "Initial state")
                            CustomResult.Loading
                        }

                        is CustomResult.Progress -> {
                            Log.d("ProjectSelectionService", "Progress: ${result.progress}%")
                            CustomResult.Loading
                        }
                    }
                }
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                Log.d("ProjectSelectionService", "Project structure job was cancelled")
            } else {
                Log.e("ProjectSelectionService", "Unexpected error in getProjectStructure", e)
                emit(CustomResult.Failure(e))
            }
        }
    }
    
    /**
     * 프로젝트 구조 새로고침
     */
    suspend fun refreshProjectStructure(projectId: DocumentId): CustomResult<Unit, Exception> {
        Log.d("ProjectSelectionService", "Refreshing project structure for: $projectId")
        
        return try {
            if (projectStructureUseCases == null) {
                Log.w("ProjectSelectionService", "ProjectStructureUseCases not available")
                CustomResult.Failure(IllegalStateException("Service not available for this context"))
            } else {
                // 프로젝트 구조 새로고침 로직
                // 실제로는 repository에서 캐시를 무효화하거나 강제로 다시 로드
                CustomResult.Success(Unit)
            }
        } catch (e: Exception) {
            Log.e("ProjectSelectionService", "Failed to refresh project structure", e)
            CustomResult.Failure(e)
        }
    }
}
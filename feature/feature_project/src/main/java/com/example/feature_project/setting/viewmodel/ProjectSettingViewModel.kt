package com.example.feature_project.setting.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.extension.getRequiredString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
// Domain 계층에서 모델 및 리포지토리 인터페이스 임포트 (올바른 경로)
import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
// import com.example.domain.repository.ProjectSettingRepository // Remove Repo import
import com.example.domain.usecase.project.* // Import project use cases

// --- UI 상태 ---
data class ProjectSettingUiState(
    val projectId: String = "",
    val projectName: String = "",
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// --- 이벤트 ---
sealed class ProjectSettingEvent {
    object NavigateBack : ProjectSettingEvent()
    data class ShowSnackbar(val message: String) : ProjectSettingEvent()
    data class NavigateToEditCategory(val projectId: String, val categoryId: String) : ProjectSettingEvent()
    data class NavigateToCreateCategory(val projectId: String) : ProjectSettingEvent()
    data class NavigateToEditChannel(val projectId: String, val categoryId: String, val channelId: String) : ProjectSettingEvent()
    data class NavigateToCreateChannel(val projectId: String, val categoryId: String) : ProjectSettingEvent()
    data class NavigateToMemberList(val projectId: String) : ProjectSettingEvent()
    data class NavigateToRoleList(val projectId: String) : ProjectSettingEvent()
    data class ShowDeleteCategoryConfirm(val category: Category) : ProjectSettingEvent()
    data class ShowDeleteChannelConfirm(val channel: ProjectChannel) : ProjectSettingEvent()
    object ShowRenameProjectDialog : ProjectSettingEvent()
    object ShowDeleteProjectConfirm : ProjectSettingEvent()
}

@HiltViewModel
class ProjectSettingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    // private val repository: ProjectSettingRepository // Remove Repo injection
    private val getProjectAllCategoriesUseCase: GetProjectAllCategoriesUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val deleteChannelUseCase: DeleteChannelUseCase,
    private val renameProjectUseCase: RenameProjectUseCase,
    private val deleteProjectUseCase: DeleteProjectUseCase
) : ViewModel() {

    val projectId: String = savedStateHandle.getRequiredString(AppRoutes.Project.ARG_PROJECT_ID)

    private val _uiState = MutableStateFlow(ProjectSettingUiState(projectId = projectId, isLoading = true))
    val uiState: StateFlow<ProjectSettingUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ProjectSettingEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadProjectStructure()
    }

    private fun loadProjectStructure() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            println("ViewModel: Loading structure for project $projectId (UseCase)")
            // --- UseCase 호출 ---
            val result = getProjectAllCategoriesUseCase(projectId).first()
            // -------------------
            // delay(800) // Remove temporary delay
            when (result) {
                is CustomResult.Success -> {
                    val projectStructure = result.data // ProjectStructure 객체 받음
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            // projectName은 ProjectStructure에 포함되지 않으므로 현재 상태 유지
                            categories = projectStructure
                        )
                    }
                }

                is CustomResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "프로젝트 정보를 불러오지 못했습니다: ${result.error}"
                        )
                    }
                    _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트 구조 로딩 실패")) // Notify user               }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    // --- 카테고리 관련 액션 ---
    fun requestEditCategory(categoryId: String) {
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.NavigateToEditCategory(projectId, categoryId)) }
    }
    fun requestDeleteCategory(category: Category) {
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.ShowDeleteCategoryConfirm(category)) }
    }
    fun confirmDeleteCategory(categoryId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // Show loading
            // TODO: DeleteCategoryUseCase 호출
            println("Deleting Category: $categoryId (UseCase)")
            val result = deleteCategoryUseCase(categoryId)
            // delay(500) // Remove delay
            if (result.isSuccess) {
                 _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("카테고리가 삭제되었습니다."))
                 loadProjectStructure() // Refresh structure
            } else {
                 _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("카테고리 삭제 실패: ${result.exceptionOrNull()?.message}"))
                 _uiState.update { it.copy(isLoading = false) } // Hide loading on failure
            }
            // isLoading will be turned off by loadProjectStructure on success
        }
    }
    fun requestCreateCategory() {
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.NavigateToCreateCategory(projectId)) }
    }

    // --- 채널 관련 액션 ---
    fun requestEditChannel(categoryId: String, channelId: String) {
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.NavigateToEditChannel(projectId, categoryId, channelId)) }
    }
    fun requestDeleteChannel(channel: ProjectChannel) {
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.ShowDeleteChannelConfirm(channel)) }
    }
    fun confirmDeleteChannel(channelId: String) {
        viewModelScope.launch {
             _uiState.update { it.copy(isLoading = true) } // Show loading
            // TODO: DeleteChannelUseCase 호출
            println("Deleting Channel: $channelId (UseCase)")
             val result = deleteChannelUseCase(channelId)
            // delay(500) // Remove delay
            when (result) {
                is CustomResult.Success -> {
                     _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("채널이 삭제되었습니다."))
                     loadProjectStructure() // Refresh structure
                }
                is CustomResult.Failure -> {
                     _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("채널 삭제 실패: ${result.error}"))
                     _uiState.update { it.copy(isLoading = false) } // Hide loading on failure
                 }
                else ->{
                    Log.e("ProjectSettingViewModel", "Unknown result type: $result")
                }
            }
             // isLoading will be turned off by loadProjectStructure on success
        }
    }
    fun requestCreateChannel(categoryId: String) {
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.NavigateToCreateChannel(projectId, categoryId)) }
    }

    // --- 멤버/역할 관리 네비게이션 ---
    fun requestManageMembers() {
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.NavigateToMemberList(projectId)) }
    }
    fun requestManageRoles() {
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.NavigateToRoleList(projectId)) }
    }

    // --- 프로젝트 이름 변경 ---
    fun requestRenameProject() {
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.ShowRenameProjectDialog) }
    }
    fun confirmRenameProject(newName: String) {
        val trimmedNewName = newName.trim()
        if (trimmedNewName.isBlank()) {
            viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트 이름은 비워둘 수 없습니다.")) }
            return
        }
        if (trimmedNewName == _uiState.value.projectName) {
             viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("현재 이름과 동일합니다.")) }
            return
        }

        viewModelScope.launch {
             _uiState.update { it.copy(isLoading = true) } // Show loading
            // TODO: RenameProjectUseCase 호출
            println("Renaming Project $projectId to '$trimmedNewName' (UseCase)")
            val result = renameProjectUseCase(projectId, trimmedNewName)
            // delay(500) // Remove delay
            if (result.isSuccess) {
                 // Update UI state directly for immediate feedback, structure reload might not be needed
                 _uiState.update { it.copy(projectName = trimmedNewName, isLoading = false) }
                 _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트 이름이 변경되었습니다."))
            } else {
                 _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("이름 변경 실패: ${result.exceptionOrNull()?.message}"))
                 _uiState.update { it.copy(isLoading = false) } // Hide loading on failure
            }
        }
    }

    // --- 프로젝트 삭제 ---
    fun requestDeleteProject() {
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.ShowDeleteProjectConfirm) }
    }
    fun confirmDeleteProject() {
        viewModelScope.launch {
             _uiState.update { it.copy(isLoading = true) } // Show loading
            // TODO: DeleteProjectUseCase 호출
            println("Deleting Project $projectId (UseCase)")
            val result = deleteProjectUseCase(projectId)
            // delay(1000) // Remove delay
            if (result.isSuccess) {
                 _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트가 삭제되었습니다."))
                 _eventFlow.emit(ProjectSettingEvent.NavigateBack) // Navigate back on success
                 // No need to turn off loading as we are navigating away
            } else {
                 _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트 삭제 실패: ${result.exceptionOrNull()?.message}"))
                 _uiState.update { it.copy(isLoading = false) } // Hide loading on failure
            }
        }
    }
}
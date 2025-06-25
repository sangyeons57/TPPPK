package com.example.feature_project_setting_screen.viewmodel.viewmodel

// Domain 계층에서 모델 및 리포지토리 인터페이스 임포트 (올바른 경로)
// import com.example.domain.repository.ProjectSettingRepository // Remove Repo import
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_common.result.exceptionOrNull
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.usecase.project.channel.DeleteChannelUseCase
import com.example.domain.usecase.project.core.DeleteProjectUseCase
import com.example.domain.usecase.project.core.GetProjectDetailsStreamUseCase
import com.example.domain.usecase.project.core.RenameProjectUseCase
import com.example.domain.usecase.project.structure.DeleteCategoryUseCase
import com.example.domain.usecase.project.structure.GetProjectAllCategoriesUseCase
import com.example.feature_model.CategoryUiModel
import com.example.feature_model.ChannelUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI 상태 ---
data class ProjectSettingUiState(
    val projectId: DocumentId,
    val projectName: ProjectName = ProjectName.EMPTY,
    val categories: List<CategoryUiModel> = emptyList(), // Changed to CategoryUiModel
    val isLoading: Boolean = false,
    val error: String? = null,
    val showRenameProjectDialog: Boolean = false,
    val showDeleteProjectDialog: Boolean = false
)

// --- 이벤트 ---
sealed class ProjectSettingEvent {
    object NavigateBack : ProjectSettingEvent()
    data class ShowSnackbar(val message: String) : ProjectSettingEvent()
    data class NavigateToEditCategory(val projectId: DocumentId, val categoryId: String) :
        ProjectSettingEvent()

    data class NavigateToCreateCategory(val projectId: DocumentId) : ProjectSettingEvent()
    data class NavigateToEditChannel(
        val projectId: DocumentId,
        val categoryId: String,
        val channelId: String
    ) : ProjectSettingEvent()

    data class NavigateToCreateChannel(val projectId: DocumentId, val categoryId: String) :
        ProjectSettingEvent()

    data class NavigateToMemberList(val projectId: DocumentId) : ProjectSettingEvent()
    data class NavigateToRoleList(val projectId: DocumentId) : ProjectSettingEvent()
    data class ShowDeleteCategoryConfirm(val category: CategoryUiModel) : ProjectSettingEvent() // Changed
    data class ShowDeleteChannelConfirm(val channel: ChannelUiModel) : ProjectSettingEvent() // Changed
}

@HiltViewModel
class ProjectSettingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    // private val repository: ProjectSettingRepository // Remove Repo injection
    private val getProjectAllCategoriesUseCase: GetProjectAllCategoriesUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val deleteChannelUseCase: DeleteChannelUseCase,
    private val renameProjectUseCase: RenameProjectUseCase,
    private val deleteProjectUseCase: DeleteProjectUseCase,
    private val getProjectStream: GetProjectDetailsStreamUseCase,
) : ViewModel() {

    val projectId: DocumentId = savedStateHandle.getRequiredString(AppRoutes.Project.ARG_PROJECT_ID)
        .let { DocumentId.from((it)) }

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


            when (val result = getProjectAllCategoriesUseCase(projectId).first()) {
                is CustomResult.Success -> {
                    val domainCategories = result.data // This is List<com.example.domain.model.base.Category>
                    val uiCategories = domainCategories.map { domainCategory ->
                        // TODO: Fetch actual channels for this domainCategory.id using another use case if needed.
                        // For now, placeholder:
                        val placeholderChannels = emptyList<ChannelUiModel>() // Empty list for now
                        CategoryUiModel(
                            id = domainCategory.category.id,
                            name = domainCategory.category.name,
                            channels = placeholderChannels
                        )
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            // projectName은 ProjectStructure에 포함되지 않으므로 현재 상태 유지
                            categories = uiCategories // Assign mapped UI models
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
        viewModelScope.launch { _eventFlow.emit(
            ProjectSettingEvent.NavigateToEditCategory(
                projectId,
                categoryId
            )
        ) }
    }
    fun requestDeleteCategory(category: CategoryUiModel) { // Changed to CategoryUiModel
        viewModelScope.launch { _eventFlow.emit(
            ProjectSettingEvent.ShowDeleteCategoryConfirm(
                category
            )
        ) }
    }
    fun confirmDeleteCategory(category: CategoryUiModel) { // Changed to CategoryUiModel
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // Show loading
            // TODO: DeleteCategoryUseCase 호출
            println("Deleting Category: ${category.id} (UseCase)") // Used category.id
            val result = deleteCategoryUseCase(projectId, category.id) // Used category.id
            // delay(500) // Remove delay
            when (result) {
                is CustomResult.Success -> {
                    _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("카테고리가 삭제되었습니다."))
                    loadProjectStructure() // Refresh structure
                }
                is CustomResult.Failure -> {
                    _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("카테고리 삭제 실패: ${result.error.message}"))
                    _uiState.update { it.copy(isLoading = false) } // Hide loading on failure
                }
                else -> {
                    _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("카테고리 삭제 실패: 알수 없는 에러"))
                    _uiState.update { it.copy(isLoading = false) } // Hide loading on failure
                }
            }
            // isLoading will be turned off by loadProjectStructure on success
        }
    }
    fun requestCreateCategory() {
        viewModelScope.launch { _eventFlow.emit(
            ProjectSettingEvent.NavigateToCreateCategory(
                projectId
            )
        ) }
    }

    // --- 채널 관련 액션 ---
    fun requestEditChannel(categoryId: String, channelId: String) {
        viewModelScope.launch { _eventFlow.emit(
            ProjectSettingEvent.NavigateToEditChannel(
                projectId,
                categoryId,
                channelId
            )
        ) }
    }
    fun requestDeleteChannel(channel: ChannelUiModel) { // Changed to ChannelUiModel
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.ShowDeleteChannelConfirm(channel)) }
    }
    fun confirmDeleteChannel(channel: ChannelUiModel) { // Changed to ChannelUiModel
        viewModelScope.launch {
             _uiState.update { it.copy(isLoading = true) } // Show loading
            // TODO: DeleteChannelUseCase 호출
            println("Deleting Channel: ${channel.id} (UseCase)") // Used channel.id
             val result = deleteChannelUseCase(projectId, channel.categoryId, channel.id) // Used channel.id
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
        viewModelScope.launch { _eventFlow.emit(
            ProjectSettingEvent.NavigateToCreateChannel(
                projectId,
                categoryId
            )
        ) }
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
        viewModelScope.launch {
            when (val result = getProjectStream(projectId).first()){
                is CustomResult.Success -> {
                    _uiState.update { it.copy(projectName = result.data.name, showRenameProjectDialog = true) }
                }
                else -> {
                    _uiState.update {
                        it.copy(
                            projectName = ProjectName.EMPTY,
                            showRenameProjectDialog = false
                        )
                    }
                    _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트 정보를 가지고 오는데 실패했습니다."))
                }

            }
        }
    }

    fun confirmRenameProject(newName: ProjectName) {
        dismiss()
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
            println("Renaming Project $projectId to '$trimmedNewName' (UseCase)")
            val result = renameProjectUseCase(projectId, trimmedNewName)
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
        _uiState.update { it.copy(showRenameProjectDialog = true, showDeleteProjectDialog = true) }
    }

    fun confirmDeleteProject() {
        viewModelScope.launch {
             _uiState.update { it.copy(isLoading = true) } // Show loading
            println("Deleting Project $projectId (UseCase)")
            val result = deleteProjectUseCase(projectId)
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

    fun dismiss() {
        _uiState.update { it.copy(showRenameProjectDialog = false, showDeleteProjectDialog = false) }
    }
}
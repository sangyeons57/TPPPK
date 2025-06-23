package com.example.feature_project.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.destination.AppRoutes
import com.example.domain.provider.project.ProjectUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.core_common.result.CustomResult
import com.example.domain.model.enum.ProjectChannelType

// Import the new UI models - assuming they are in com.example.feature_project.model

// Define ChannelMode enum and CreateChannelDialogData data class

data class CreateChannelDialogData(
    val channelName: String = "",
    val categoryId: String? = null, // For which category to add, null for direct
    val channelMode: ProjectChannelType = ProjectChannelType.MESSAGES // Default to TEXT
)

/**
 * UI 상태를 나타내는 데이터 클래스입니다.
 * 프로젝트 상세 화면 (채널 목록 포함)에서 사용됩니다.
 */
data class ProjectDetailUiState(
    val projectId: String,
    val projectName: String = "", // TODO: 프로젝트 이름도 가져오도록 수정
    val categories: List<com.example.feature_model.CategoryUiModel> = emptyList(),
    val directChannels: List<com.example.feature_model.ChannelUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    // 채널 생성 관련 상태
    val showCreateChannelDialog: Boolean = false,
    val createChannelDialogData: CreateChannelDialogData? = null // Added this field
)


@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectUseCaseProvider: ProjectUseCaseProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {

    // 매개변수 추출 - 안전한 방식으로 projectId 획득
    private val projectId: String = savedStateHandle.get<String>(AppRoutes.Project.ARG_PROJECT_ID) 
        ?: error("${AppRoutes.Project.ARG_PROJECT_ID} is required - 프로젝트 ID가 전달되지 않았습니다")

    // ProjectUseCaseProvider를 통해 해당 프로젝트의 UseCases 생성
    private val projectUseCases = projectUseCaseProvider.createForProject(projectId)

    private val _uiState = MutableStateFlow(ProjectDetailUiState(projectId = projectId))
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()
    
    init {
        // 프로젝트 상세 정보 로드
        loadProjectDetails()
    }
    
    private fun loadProjectDetails() {
        viewModelScope.launch {
            // 프로젝트 상세 정보 스트림 구독
            projectUseCases.getProjectDetailsStreamUseCase(projectId).collect { result ->
                when (result) {
                    is CustomResult.Success -> {
                        _uiState.update { 
                            it.copy(
                                projectName = result.data.name,
                                isLoading = false,
                                error = null
                            ) 
                        }
                    }
                    is CustomResult.Failure -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = "프로젝트 정보를 불러올 수 없습니다: ${result.error.message}"
                            ) 
                        }
                    }
                    else -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    fun showCreateDirectChannelDialog() {
        _uiState.update {
            it.copy(
                showCreateChannelDialog = true,
                createChannelDialogData = CreateChannelDialogData(
                    categoryId = null,
                    channelName = "",
                    channelMode = ProjectChannelType.MESSAGES
                )
            )
        }
    }

    fun showCreateCategoryChannelDialog(categoryId: String) {
        _uiState.update {
            it.copy(
                showCreateChannelDialog = true,
                createChannelDialogData = CreateChannelDialogData(
                    categoryId = categoryId,
                    channelName = "",
                    channelMode = ProjectChannelType.MESSAGES
                )
            )
        }
    }

    fun dismissCreateChannelDialog() {
        _uiState.update { it.copy(showCreateChannelDialog = false, createChannelDialogData = null) }
    }

    fun updateCreateChannelDialogName(name: String) {
        _uiState.update { state ->
            state.createChannelDialogData?.let {
                state.copy(createChannelDialogData = it.copy(channelName = name))
            } ?: state
        }
    }

    fun updateCreateChannelDialogChannelMode(mode: ProjectChannelType) {
        _uiState.update { state ->
            state.createChannelDialogData?.let {
                state.copy(createChannelDialogData = it.copy(channelMode = mode))
            } ?: state
        }
    }

    fun confirmCreateChannel() {
        val dialogData = uiState.value.createChannelDialogData ?: return
        if (dialogData.channelName.isBlank()) {
            _uiState.update { it.copy(error = "채널 이름을 입력해주세요.") } // Show error in UI
            return
        }

        // TODO: The dialogData.channelMode (String for TEXT/VOICE) is collected 
        // but the current UseCase signatures (CreateDirectChannelUseCase, CreateCategoryChannelUseCase)
        // expect a ChannelType (PROJECT/CATEGORY) and do not have a parameter for channelMode.
        // This is a potential functional gap. The UseCases/Repository might need to be updated
        // to accept channelMode if it needs to be set at creation.
        // For now, passing ChannelType.PROJECT/CATEGORY and a default order to satisfy the signature.

        viewModelScope.launch {
            val result = if (dialogData.categoryId == null) {
                // Create Direct Channel - Provider의 UseCase 사용
                projectUseCases.createProjectChannelUseCase(projectId, dialogData.channelName, ProjectChannelType.MESSAGES, 0.0)
            } else {
                // Create Category Channel - Provider의 UseCase 사용
                projectUseCases.createProjectChannelUseCase(projectId, dialogData.categoryId, dialogData.channelName, ProjectChannelType.MESSAGES, 0.0)
            }

            when (result) {
                is CustomResult.Success -> {
                    _uiState.update { it.copy(showCreateChannelDialog = false, createChannelDialogData = null, error = null) }
                    // 채널 생성 성공 시 자동으로 해당 채널로 이동
                    val channelId = result.data.value // DocumentId에서 실제 ID 값 추출
                    navigateToChannel(channelId)
                }
                is CustomResult.Failure -> {
                    _uiState.update { it.copy(error = "채널 생성 실패: ${result.error.message}") }
                }
                else -> {
                    _uiState.update { it.copy(error = "채널 생성 실패: Unknown error") }
                }
            }
        }
    }
    
    // === 네비게이션 메서드들 ===
    
    fun navigateToChannel(channelId: String) {
        navigationManger.navigateToChat(channelId)
    }
    
    fun navigateToProjectSettings() {
        navigationManger.navigateToProjectSettings(projectId)
    }
    
    fun navigateBack() {
        navigationManger.navigateBack()
    }
    
    fun navigateToAddProjectMember() {
        // TODO: AddProjectMember 화면 구현 시 추가
        // navigationManger.navigateToAddProjectMember(projectId)
    }
    
    fun navigateToEditCategory(categoryId: String) {
        // TODO: EditCategory 화면 구현 시 추가
        // navigationManger.navigateToEditCategory(projectId, categoryId)
    }
    
    fun navigateToEditChannel(channelId: String) {
        // TODO: EditChannel 화면 구현 시 추가
        // navigationManger.navigateToEditChannel(projectId, channelId)
    }
} 
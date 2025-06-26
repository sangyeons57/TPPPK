package com.example.feature_project.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import com.example.domain.provider.project.CoreProjectUseCaseProvider
import com.example.domain.provider.project.ProjectChannelUseCaseProvider
import com.example.feature_model.CategoryUiModel
import com.example.feature_model.ChannelUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Import the new UI models - assuming they are in com.example.feature_project.model

// Define ChannelMode enum and CreateChannelDialogData data class

data class CreateChannelDialogData(
    val channelName: Name = Name.EMPTY,
    val categoryId: DocumentId? = null, // For which category to add, null for direct
    val channelMode: ProjectChannelType = ProjectChannelType.MESSAGES // Default to TEXT
)

/**
 * UI 상태를 나타내는 데이터 클래스입니다.
 * 프로젝트 상세 화면 (채널 목록 포함)에서 사용됩니다.
 */
data class ProjectDetailUiState(
    val projectId: DocumentId,
    val projectName: ProjectName = ProjectName.EMPTY,
    val categories: List<CategoryUiModel> = emptyList(),
    val directChannels: List<ChannelUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    // 채널 생성 관련 상태
    val showCreateChannelDialog: Boolean = false,
    val createChannelDialogData: CreateChannelDialogData? = null // Added this field
)


@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val coreProjectUseCaseProvider: CoreProjectUseCaseProvider,
    private val projectChannelUseCaseProvider: ProjectChannelUseCaseProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {

    // 매개변수 추출 - 안전한 방식으로 projectId 획득
    private val projectId: DocumentId = DocumentId(savedStateHandle.getRequiredString(RouteArgs.PROJECT_ID))

    // ProjectUseCaseProvider를 통해 해당 프로젝트의 UseCases 생성
    private val projectUseCases = coreProjectUseCaseProvider.createForCurrentUser()
    private val channelUseCases = projectChannelUseCaseProvider.createForProject(projectId)

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
                    channelName = Name.EMPTY,
                    channelMode = ProjectChannelType.MESSAGES
                )
            )
        }
    }

    fun showCreateCategoryChannelDialog(categoryId: DocumentId) {
        _uiState.update {
            it.copy(
                showCreateChannelDialog = true,
                createChannelDialogData = CreateChannelDialogData(
                    categoryId = categoryId,
                    channelName = Name.EMPTY,
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
                state.copy(createChannelDialogData = it.copy(channelName = Name(name)))
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
                channelUseCases.createProjectChannelUseCase(
                    dialogData.channelName,
                    ProjectChannelOrder.DEFAULT,
                    ProjectChannelType.MESSAGES
                )
            } else {
                // Create Category Channel - Provider의 UseCase 사용
                //  TODO:체널 생성 Provider가 아니라 Category관련 사용
                channelUseCases.createProjectChannelUseCase(
                    dialogData.channelName,
                    ProjectChannelOrder.DEFAULT,
                    ProjectChannelType.MESSAGES
                )
            }

            when (result) {
                is CustomResult.Success -> {
                    _uiState.update { it.copy(showCreateChannelDialog = false, createChannelDialogData = null, error = null) }
                    // 채널 생성 성공 시 자동으로 해당 채널로 이동
                    val channelId = result.data.toString() // Convert data to string for navigation
                    navigateToChannel(channelId)
                }
                is CustomResult.Failure -> {
                    _uiState.update { it.copy(error = "채널 생성 실패: ${result.error}") }
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
        navigationManger.navigateToProjectSettings(projectId.value)
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
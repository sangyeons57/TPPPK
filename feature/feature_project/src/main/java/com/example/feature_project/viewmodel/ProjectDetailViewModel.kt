package com.example.feature_project.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.util.DateTimeUtil
import com.example.core_navigation.destination.AppRoutes
import com.example.domain.model.ChannelType
import com.example.domain.model.ProjectStructure
import com.example.domain.usecase.projectstructure.CreateCategoryChannelUseCase
import com.example.domain.usecase.projectstructure.CreateDirectChannelUseCase
import com.example.domain.usecase.projectstructure.GetProjectChannelsUseCase
import com.example.domain.model.ui.CategoryUiModel
import com.example.domain.model.ui.ChannelUiModel
import com.example.domain.model.ui.CreateChannelDialogData
import com.example.domain.model.ui.ProjectDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.core_common.constants.FirestoreConstants
import com.example.domain.model.ChannelMode
import com.example.domain.model.channel.isDirectProjectChannel

/**
 * UI 상태를 나타내는 데이터 클래스입니다.
 * 프로젝트 상세 화면 (채널 목록 포함)에서 사용됩니다.
 */
data class ProjectDetailUiState(
    val projectId: String,
    val projectName: String = "", // TODO: 프로젝트 이름도 가져오도록 수정
    val categories: List<CategoryUiModel> = emptyList(),
    val directChannels: List<ChannelUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    // 채널 생성 관련 상태
    val showCreateChannelDialog: Boolean = false,
    val createChannelDialogData: CreateChannelDialogData? = null // categoryId가 null이면 직속 채널
)

// Domain -> UI Model Mappers
private fun com.example.domain.model.Channel.toUiModel(): ChannelUiModel {
    return ChannelUiModel(
        id = this.id,
        categoryId = this.projectSpecificData?.categoryId,
        projectId = this.projectSpecificData?.projectId ?: "",
        name = this.name,
        type = this.type,
        order = this.projectSpecificData?.order ?: 0,
        isDirect = this.isDirectProjectChannel(),
        lastMessagePreview = this.lastMessagePreview,
        formattedLastMessageTimestamp = DateTimeUtil.formatChatTime(this.lastMessageTimestamp)
    )
}

private fun com.example.domain.model.Category.toUiModel(): CategoryUiModel {
    return CategoryUiModel(
        id = this.id,
        projectId = this.projectId,
        name = this.name,
        order = this.order,
        channels = this.channels.map { it.toUiModel() },
        formattedCreatedAt = DateTimeUtil.formatDate(this.createdAt),
        formattedUpdatedAt = DateTimeUtil.formatDate(this.updatedAt)
    )
}

private fun ProjectStructure.toCategoryUiModels(): List<CategoryUiModel> {
    return this.categories.map { it.toUiModel() }
}

private fun ProjectStructure.toDirectChannelUiModels(): List<ChannelUiModel> {
    return this.directChannels.map { it.toUiModel() }
}

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProjectChannelsUseCase: GetProjectChannelsUseCase,
    private val createCategoryChannelUseCase: CreateCategoryChannelUseCase,
    private val createDirectChannelUseCase: CreateDirectChannelUseCase
) : ViewModel() {

    private val projectId: String = savedStateHandle.get<String>(AppRoutes.Project.ARG_PROJECT_ID) ?: error("${AppRoutes.Project.ARG_PROJECT_ID} is required")

    private val _uiState = MutableStateFlow(ProjectDetailUiState(projectId = projectId))
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    init {
        fetchProjectStructure()
        // TODO: Fetch project name and update uiState.projectName
    }

    private fun fetchProjectStructure() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getProjectChannelsUseCase(projectId)
                .map { projectStructure ->
                    // Map domain models to UI models
                    Pair(projectStructure.toCategoryUiModels(), projectStructure.toDirectChannelUiModels())
                }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = "프로젝트 구조 로드 실패: ${e.message}") }
                }
                .collect { (categories, directChannels) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            categories = categories,
                            directChannels = directChannels,
                            error = null
                        )
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
                    channelMode = ChannelMode.TEXT
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
                    channelMode = ChannelMode.TEXT
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

    fun updateCreateChannelDialogChannelMode(mode: ChannelMode) {
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
                // Create Direct Channel
                createDirectChannelUseCase(projectId, dialogData.channelName, ChannelMode.TEXT, 0) // Use ChannelType.PROJECT, add order
            } else {
                // Create Category Channel
                createCategoryChannelUseCase(projectId, dialogData.categoryId!!, dialogData.channelName, ChannelMode.TEXT, 0) // Use ChannelType.CATEGORY
            }

            if (result.isSuccess) {
                _uiState.update { it.copy(showCreateChannelDialog = false, createChannelDialogData = null, error = null) }
                // Optionally, refresh structure or rely on stream to update
            } else {
                _uiState.update { it.copy(error = "채널 생성 실패: ${result.exceptionOrNull()?.message}") }
            }
        }
    }
    // TODO: Implement navigation to ChatScreen for selected channels
} 
package com.example.feature_main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.Channel
import com.example.domain.model.Project
import com.example.domain.repository.ProjectRepository
import com.example.domain.repository.UserRepository
import com.example.domain.usecase.dm.GetUserDmChannelsUseCase
import com.example.domain.usecase.user.GetCurrentUserUseCase
import com.example.domain.model.ui.DmUiModel
import com.example.domain.model.ui.MainScreenType
import com.example.domain.model.ui.MainUiState
import com.example.domain.model.ui.ProjectUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Mapper functions
private suspend fun Channel.toUiModel(currentUserId: String, userRepository: UserRepository): DmUiModel {

    val partnerId = this.participantIds.find { it != currentUserId } ?: ""
    var partnerName = this.name
    var partnerProfilePic : String? = ""

    if (partnerId.isNotEmpty()) {
        userRepository.getUser(partnerId).getOrNull()?.let {
            partnerName = it.name
            partnerProfilePic = it.profileImageUrl
        }
    }

    return DmUiModel(
        channelId = this.id,
        partnerName = partnerName,
        partnerProfileImageUrl = partnerProfilePic,
        lastMessage = this.lastMessagePreview,
        lastMessageTimestamp = this.updatedAt,
    )
}

private fun Project.toUiModel(): ProjectUiModel {
    return ProjectUiModel(
        id = this.id,
        name = this.name,
        description = this.description,
        imageUrl = this.imageUrl,
        // isPublic can be added if needed in ProjectUiModel and domain Project
    )
}

/**
 * MainViewModel: 메인 화면 (하단 탭 호스트) 전체에 필요한 공통 로직 관리
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val getUserDmChannelsUseCase: GetUserDmChannelsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    init {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().firstOrNull()
            currentUserId = user?.id ?: ""
            
            if (currentUserId.isNotEmpty()) {
                fetchDmChannels()
            } else {
                _uiState.update { it.copy(isLoadingDms = false, dmsError = "User ID not available.") }
            }
            fetchProjects()
        }
    }

    private fun fetchDmChannels() {
        if (currentUserId.isBlank()) {
            _uiState.update { it.copy(isLoadingDms = false, dmsError = "Current User ID is missing, cannot fetch DMs.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDms = true) }
            getUserDmChannelsUseCase(currentUserId)
                .map { domainChannels ->
                    domainChannels.map { channel -> channel.toUiModel(currentUserId, userRepository) }
                }
                .catch { e ->
                    _uiState.update { it.copy(isLoadingDms = false, dmsError = "DM 목록 로드 실패: ${e.message}") }
                }
                .collect { dmUiModels ->
                    _uiState.update { it.copy(isLoadingDms = false, dmConversations = dmUiModels, dmsError = null) }
                }
        }
    }

    private fun fetchProjects() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProjects = true) }
            projectRepository.getProjectListStream()
                .map { domainProjects -> domainProjects.map { it.toUiModel() } }
                .catch { e ->
                    _uiState.update { it.copy(isLoadingProjects = false, projectsError = "프로젝트 목록 로드 실패: ${e.message}") }
                }
                .collect { projectUiModels ->
                    _uiState.update { it.copy(isLoadingProjects = false, projects = projectUiModels, projectsError = null) }
                }
        }
    }

    /**
     * 메인 화면에 표시될 현재 탭/화면을 변경합니다.
     * @param screenType 변경할 화면 타입 ([MainScreenType])
     */
    fun setCurrentScreen(screenType: MainScreenType) {
        _uiState.update { it.copy(currentScreen = screenType) }
    }
}
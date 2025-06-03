package com.example.feature_main.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.usecase.dm.GetUserDmChannelsUseCase
import com.example.domain.model.base.DMChannel // Added import for DMChannel
import com.example.domain.model.base.Project
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.collection.CategoryCollection
import com.example.domain.usecase.project.GetProjectAllCategoriesUseCase
import com.example.domain.usecase.user.GetCurrentUserStreamUseCase
import com.example.domain.usecase.user.GetUserInfoUseCase
import com.example.domain.usecase.project.GetProjectListStreamUseCase // Added

import com.example.domain.usecase.project.GetProjectDetailsStreamUseCase // Added
import com.example.domain.usecase.dm.AddDmChannelUseCase // Added
import com.example.domain.usecase.project.CreateProjectUseCase // Added
import com.example.domain.usecase.project.UpdateProjectStructureUseCase // Added
import com.example.domain.usecase.user.GetUserProjectWrappersUseCase
import com.example.feature_main.ui.DmUiModel
import com.example.feature_main.ui.ProjectUiModel // Added
import com.example.feature_main.ui.project.CategoryUiModel
import com.example.feature_main.ui.project.ChannelUiModel
import com.example.feature_main.ui.project.ProjectStructureUiState
import com.example.feature_main.ui.toProjectUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async // Added for async mapping
import kotlinx.coroutines.awaitAll // Added for async mapping
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import javax.inject.Inject

// 상단 탭 상태
enum class TopSection {
    PROJECTS, DMS
}

// ProjectItem REMOVED (replaced by ProjectUiModel)
// DmItem REMOVED
// ------------------------

// 홈 화면 UI 상태
data class HomeUiState(
    val selectedTopSection: TopSection = TopSection.DMS, // 기본 선택: DMS
    val projects: List<ProjectUiModel> = emptyList(), // Now using ProjectUiModel
    val dms: List<DmUiModel> = emptyList(), // Use DmUiModel
    val isLoading: Boolean = false,
    val errorMessage: String = "default",
    
    // 프로젝트 관련 상태
    val selectedProjectId: String? = null, // 선택된 프로젝트 ID
    val selectedDmId: String? = null, // 선택된 프로젝트 ID
    val projectName: String = "", // 선택된 프로젝트 이름
    val projectDescription: String? = null, // 선택된 프로젝트 설명
    val projectMembers: List<ProjectMember> = emptyList(), // 선택된 프로젝트 멤버
    
    // 카테고리 및 채널 관련 상태
    val projectStructure: ProjectStructureUiState = ProjectStructureUiState(),
    
    // 사용자 프로필 관련 상태
    val userInitial: String = "U", // 사용자 이니셜 (기본값: "U")
    val userProfileImageUrl: String? = null, // 사용자 프로필 이미지 URL
    
    // 표시 관련 상태
    val isDetailFullScreen: Boolean = false // 전체 화면 모드 플래그
)

// 프로젝트 멤버 정보
data class ProjectMember(
    val id: String,
    val name: String,
    val role: String
)

// 홈 화면 이벤트
sealed class HomeEvent {
    data class NavigateToProjectSettings(val projectId: String) : HomeEvent() // 프로젝트 설정 화면
    data class NavigateToDmChat(val dmId: String) : HomeEvent()
    data class NavigateToChannel(val projectId: String, val channelId: String) : HomeEvent() // 채널 화면으로 이동
    object ShowAddProjectDialog : HomeEvent() // 또는 화면 이동
    object ShowAddFriendDialog : HomeEvent() // 또는 화면 이동
    object NavigateToAddProject : HomeEvent() // 프로젝트 추가 화면
    data class ShowSnackbar(val message: String) : HomeEvent()
    object EditProjectStructure : HomeEvent() // 프로젝트 구조 편집
}


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCurrentUserStreamUseCase: GetCurrentUserStreamUseCase,
    private val getUserDmChannelsUseCase: GetUserDmChannelsUseCase,
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val getProjectAllCategoriesUseCase: GetProjectAllCategoriesUseCase,
    private val getUserProjectWrappersUseCase: GetUserProjectWrappersUseCase, // Added
    // private val getDmListStreamUseCase: GetDmListStreamUseCase, // Removed as getUserDmChannelsUseCase seems to cover DM list fetching
    private val getProjectDetailsStreamUseCase: GetProjectDetailsStreamUseCase, // Added
    private val addDmChannelUseCase: AddDmChannelUseCase, // Added
    private val createProjectUseCase: CreateProjectUseCase, // Added
    private val updateProjectStructureUseCase: UpdateProjectStructureUseCase // Added
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<HomeEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // 카테고리 확장 상태 캐시 (프로젝트 ID -> 카테고리 ID -> 확장 상태)
    private val categoryExpandedStates = mutableMapOf<String, MutableMap<String, Boolean>>()
    
    // 선택된 채널 ID
    private var selectedChannelId: String? = null
    
    // 현재 사용자 ID
    private var currentUserId: String = ""
        set(value) {
            Log.d("HomeViewModel", "Setting currentUserId: $value (previous: $field)")
            field = value
            if (value.isNotBlank()) {
                loadDataForUser()
            }
        }

    init {
        Log.d("HomeViewModel", "HomeViewModel initialized")
        viewModelScope.launch {
            Log.d("HomeViewModel", "Starting to collect from getCurrentUserStreamUseCase")
            // userPreferencesRepository 대신 UseCase 사용
            getCurrentUserStreamUseCase()
                .catch { exception ->
                    Log.e("HomeViewModel", "Failed to get current user", exception)
                    _uiState.update { it.copy(isLoading = false, errorMessage = "사용자 정보 로드 실패: ${exception.localizedMessage}") }
                }
                .collectLatest { result : CustomResult<User, Exception> ->
                    result.fold(
                        onSuccess = { user ->
                            Log.d("HomeViewModel", "User received from UseCase: ${user.uid}")
                            currentUserId = user.uid

                            // 사용자 이니셜과 프로필 이미지 URL 업데이트
                            _uiState.update { state ->
                                state.copy(
                                    userInitial = user.name.firstOrNull()?.toString() ?: "U",
                                    userProfileImageUrl = user.profileImageUrl
                                )
                            }
                        },
                        onFailure = { exception ->
                            Log.e("HomeViewModel", "Failed to get current user", exception)
                        },
                    )
                }
        }
    }

    /**
     * 현재 사용자에 대한 모든 데이터를 로드합니다.
     */
    private fun loadDataForUser() {
        Log.d("HomeViewModel", "loadDataForUser called with userId: $currentUserId")
        loadProjects()
        loadDms()
    }

    // Mapper function DMChannel -> DmUiModel
// DMChannel 객체에서 DmUiModel에 필요한 정보를 추출합니다.
private suspend fun toDmUiModel(dmChannel: DMChannel, currentUserId: String): DmUiModel {
    val partnerId = dmChannel.participants.firstOrNull { it != currentUserId }
    var partnerName = "Unknown User"
    var partnerProfileImageUrl: String? = null

    if (partnerId != null && partnerId.isNotEmpty()) {
        // GetUserInfoUseCase returns Flow<CustomResult<User, Exception>>
        // We take the first result from this flow.
        when (val userInfoResult = getUserInfoUseCase(partnerId).first()) {
            is CustomResult.Success -> {
                partnerName = userInfoResult.data.name // Assuming User model has 'name'
                partnerProfileImageUrl = userInfoResult.data.profileImageUrl // Assuming User model has 'profileImageUrl'
                Log.d("HomeViewModel", "Partner info success for $partnerId: Name=$partnerName")
            }
            is CustomResult.Failure -> {
                Log.e("HomeViewModel", "Failed to get partner info for $partnerId", userInfoResult.error)
            }
            else -> { 
                Log.d("HomeViewModel", "Fetching partner info for $partnerId resulted in: $userInfoResult (e.g. Loading/Initial)")
            }
        }
    }

    return DmUiModel(
        channelId = dmChannel.id,
        partnerName = partnerName,
        partnerProfileImageUrl = partnerProfileImageUrl,
        lastMessage = dmChannel.lastMessagePreview,
        lastMessageTimestamp = dmChannel.lastMessageTimestamp!!, // Corrected from updatedAt
        unreadCount = 0 // Placeholder - unread count not in DMChannel model
    )
}

    // 상단 탭 선택 시 호출
    fun onTopSectionSelect(section: TopSection) {
        if (_uiState.value.selectedTopSection == section && !_uiState.value.isLoading) return // 로딩 중이 아닐 때만 중복 선택 무시
        _uiState.update { it.copy(selectedTopSection = section, isLoading = true, errorMessage = "default") }
        loadDataForSelectedSection()
    }

    // 현재 선택된 탭에 맞는 데이터 로드 함수
    private fun loadDataForSelectedSection() {
        loadProjects() // 프로젝트는 currentUserId와 무관하게 로드 가능

        if (_uiState.value.selectedTopSection == TopSection.DMS) {
            loadDms() // currentUserId 체크는 이제 usecase 내부에서 처리
        }
    }

    // 프로젝트 데이터 로드
    private fun loadProjects() {
        Log.d("HomeViewModel", "loadProjects called")
        viewModelScope.launch {
            getUserProjectWrappersUseCase(currentUserId)
                .collectLatest { result ->
                    when (result) {
                        is CustomResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true, errorMessage = "default") }
                        }
                        is CustomResult.Success -> {
                            val projectWrappers = result.data
                            // Assuming com.example.feature_main.ui.toProjectUiModel extension function exists or will be created
                            val mappedProjectUiModels = projectWrappers.map { it.toProjectUiModel() }
                            _uiState.update { state ->
                                state.copy(
                                    projects = mappedProjectUiModels, // Update with mapped models
                                    isLoading = false,
                                    errorMessage = if (mappedProjectUiModels.isEmpty()) "프로젝트가 없습니다." else "default"
                                )
                            }
                            Log.d("HomeViewModel", "Projects loaded: ${mappedProjectUiModels.size}")
                        }
                        is CustomResult.Failure -> {
                            Log.e("HomeViewModel", "Failed to load projects", result.error)
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = result.error.message ?: "알 수 없는 오류가 발생했습니다."
                                )
                            }
                        }
                        is CustomResult.Initial -> {
                            // Optionally handle Initial state, e.g., by showing loading
                            _uiState.update { it.copy(isLoading = true, errorMessage = "default") }
                        }
                        is CustomResult.Progress -> {
                            // Handle progress if applicable, e.g. update a progress bar
                            // For now, we can treat it as loading
                            val progressValue = result.progress
                            Log.d("HomeViewModel", "Project loading progress: $progressValue%")
                            _uiState.update { it.copy(isLoading = true) } // Keep isLoading true during progress
                        }
                    }
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadDms() {
        viewModelScope.launch {
            val currentUserResult = getCurrentUserStreamUseCase().first()
            when (currentUserResult) {
                is CustomResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "사용자 정보를 가져올 수 없습니다.") }
                    Log.e("HomeViewModel", "Current user ID is null or empty in loadDms.")
                    Log.d("HomeViewModel", "loadDms called with currentUserId: $currentUserId")
                    return@launch
                }
                is CustomResult.Success -> {
                    getUserDmChannelsUseCase().collectLatest { result: CustomResult<List<DMChannel>, Exception> ->
                        Log.d("HomeViewModel", "Received DM channels result: $result")
                        when (result) {
                            is CustomResult.Loading -> {
                                _uiState.update { it.copy(isLoading = true, errorMessage = "default") }
                            }
                            is CustomResult.Success -> {
                                Log.d("HomeViewModel", "Successfully fetched DM channels: ${result.data.size} channels.")
                                // Map DMChannel domain models to DmUiModel, fetching partner info
                                val dmUiModels = result.data.map { dmChannel ->
                                    async { toDmUiModel(dmChannel, currentUserId) } // Launch async mapping for each
                                }.awaitAll() // Wait for all mappings to complete
                                _uiState.update { state ->
                                    state.copy(
                                        dms = dmUiModels,
                                        isLoading = false,
                                        errorMessage = if (dmUiModels.isEmpty()) "DM이 없습니다." else "default"
                                    )
                                }
                                Log.d("HomeViewModel", "DMs loaded and UI updated: ${dmUiModels.size}")
                            }
                            is CustomResult.Failure -> {
                                Log.e("HomeViewModel", "Failed to load DMs", result.error)
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        errorMessage = result.error.message ?: "알 수 없는 DM 오류가 발생했습니다."
                                    )
                                }
                            }
                            is CustomResult.Initial -> {
                                _uiState.update { it.copy(isLoading = true, errorMessage = "default") }
                                Log.d("HomeViewModel", "DM loading initial state.")
                            }
                            is CustomResult.Progress -> {
                                val progressValue = result.progress
                                Log.d("HomeViewModel", "DM loading progress: $progressValue%")
                                _uiState.update { it.copy(isLoading = true) }
                            }
                        }
                    }
                }
                else -> {
                    Log.e("HomeViewModel", "Unexpected result type in loadDms.")
                }
            }
        }
    }

    // 프로젝트 아이템 클릭 시
    fun onProjectClick(projectId: String) {
        viewModelScope.launch {
            // 프로젝트 ID가 이미 선택되어 있으면 무시
            if (_uiState.value.selectedProjectId == projectId) return@launch
            
            // 상태 업데이트
            _uiState.update { it.copy(
                selectedProjectId = projectId,
                selectedTopSection = TopSection.PROJECTS, // 프로젝트 탭으로 전환
                isLoading = true, // 로딩 상태로 설정
                projectStructure = ProjectStructureUiState(isLoading = true) // 프로젝트 구조 로딩 상태로 설정
            )}
            
            // 프로젝트 상세 정보 로드
            loadProjectDetails(projectId)
            
            // 프로젝트 구조 (카테고리 및 채널) 로드
            loadProjectStructure(projectId)
        }
    }

    // 프로젝트 상세 정보 로드
private fun loadProjectDetails(projectId: String) { 
    viewModelScope.launch {
        Log.d("HomeViewModel", "loadProjectDetails called for projectId: $projectId")
        getProjectDetailsStreamUseCase(projectId)
            .collectLatest { result: CustomResult<Project, Exception> ->
                Log.d("HomeViewModel", "Received project details result: $result")
                when (result) {
                    is CustomResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, errorMessage = "default") }
                    }
                    is CustomResult.Success -> {
                        val project = result.data
                        _uiState.update { state ->
                            state.copy(
                                projectName = project.name,
                                projectDescription = "Project description placeholder for ${project.name}", 
                                isLoading = false,
                                errorMessage = "default"
                            )
                        }
                        Log.d("HomeViewModel", "Project details loaded: ${project.name}")
                    }
                    is CustomResult.Failure -> {
                        Log.e("HomeViewModel", "Failed to load project details for $projectId", result.error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.error.message ?: "프로젝트 상세 정보를 가져오지 못했습니다."
                            )
                        }
                    }
                    is CustomResult.Initial -> {
                        _uiState.update { it.copy(isLoading = true, errorMessage = "default") }
                        Log.d("HomeViewModel", "Project details loading initial state.")
                    }
                    is CustomResult.Progress -> {
                        val progressValue = result.progress ?: 0f
                        Log.d("HomeViewModel", "Project details loading progress: $progressValue%")
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
    }
}
    
    // 프로젝트 구조 (카테고리 및 채널) 로드
    private fun loadProjectStructure(projectId: String) { 
    viewModelScope.launch {
        Log.d("HomeViewModel", "loadProjectStructure called for projectId: $projectId")
        val projectStructureFlow = getProjectAllCategoriesUseCase(projectId)

        projectStructureFlow.collectLatest { result: CustomResult<List<CategoryCollection>, Exception> ->
            Log.d("HomeViewModel", "Received project structure result: $result")
            when (result) {
                is CustomResult.Loading -> {
                    _uiState.update { state ->
                        state.copy(projectStructure = state.projectStructure.copy(isLoading = true, error = "default"))
                    }
                }
                is CustomResult.Success -> {
                    val categoryCollections = result.data
                    val categoriesMap = categoryExpandedStates.getOrPut(projectId) { mutableMapOf() }

                    val categoryUiModels = categoryCollections.map { categoryCollection ->
                        val categoryDomain = categoryCollection.category
                        val isExpanded = categoriesMap.getOrPut(categoryDomain.id) { true } // Default to expanded
                        CategoryUiModel(
                            id = categoryDomain.id,
                            name = categoryDomain.name,
                            channels = categoryCollection.channels.map { channelDomain ->
                                ChannelUiModel(
                                    id = channelDomain.id,
                                    name = channelDomain.channelName,
                                    // Assuming ProjectChannel has channelMode, otherwise adjust
                                    mode = channelDomain.channelType, // Provide a default or handle null
                                    isSelected = channelDomain.id == selectedChannelId // Use .value for StateFlow
                                )
                            },
                            isExpanded = isExpanded
                        )
                    }

                    _uiState.update { state ->
                        state.copy(
                            projectStructure = ProjectStructureUiState(
                                categories = categoryUiModels,
                                directChannel = emptyList(), // Direct channels not handled by CategoryCollection
                                isLoading = false,
                                error = "default"
                            )
                        )
                    }
                    Log.d("HomeViewModel", "Project structure loaded for $projectId")
                }
                is CustomResult.Failure -> {
                    Log.e("HomeViewModel", "Failed to load project structure for $projectId", result.error)
                    _uiState.update { state ->
                        state.copy(
                            projectStructure = state.projectStructure.copy(
                                isLoading = false,
                                error = result.error.message ?: "프로젝트 구조를 가져오지 못했습니다."
                            )
                        )
                    }
                }
                is CustomResult.Initial -> {
                     _uiState.update { state ->
                        state.copy(projectStructure = state.projectStructure.copy(isLoading = true, error = "default"))
                    }
                    Log.d("HomeViewModel", "Project structure loading initial state.")
                }
                is CustomResult.Progress -> {
                    val progressValue = result.progress ?: 0f
                    Log.d("HomeViewModel", "Project structure loading progress: $progressValue%")
                     _uiState.update { state ->
                        state.copy(projectStructure = state.projectStructure.copy(isLoading = true))
                    }
                }
            }
        }
    }
}

    // 카테고리 클릭 시 (접기/펼치기)
    fun onCategoryClick(category: CategoryUiModel) {
        val projectId = _uiState.value.selectedProjectId ?: return
        
        // 카테고리 확장 상태 토글
        categoryExpandedStates.getOrPut(projectId) { mutableMapOf() }[category.id] = !category.isExpanded
        
        // 프로젝트 구조 UI 상태 업데이트
        _uiState.update { state ->
            val updatedCategories = state.projectStructure.categories.map { cat ->
                if (cat.id == category.id) {
                    cat.copy(isExpanded = !cat.isExpanded)
                } else {
                    cat
                }
            }
            
            state.copy(
                projectStructure = state.projectStructure.copy(
                    categories = updatedCategories
                )
            )
        }
    }
    
    // 채널 클릭 시
    fun onChannelClick(channel: ChannelUiModel) {
        val projectId = _uiState.value.selectedProjectId ?: return
        
        // 이미 선택된 채널이면 무시
        if (selectedChannelId == channel.id) return
        
        // 선택된 채널 ID 업데이트
        selectedChannelId = channel.id
        
        // 프로젝트 구조 UI 상태 업데이트 (선택된 채널 하이라이트)
        _uiState.update { state ->
            val updatedCategories = state.projectStructure.categories.map { category ->
                val updatedChannels = category.channels.map { ch ->
                    ch.copy(isSelected = ch.id == channel.id)
                }
                category.copy(channels = updatedChannels)
            }
            
            val updatedGeneralChannels = state.projectStructure.directChannel.map { ch ->
                ch.copy(isSelected = ch.id == channel.id)
            }
            
            state.copy(
                projectStructure = state.projectStructure.copy(
                    categories = updatedCategories,
                    directChannel = updatedGeneralChannels
                )
            )
        }
        
        // 채널 화면으로 이동 이벤트 발행
        viewModelScope.launch {
            _eventFlow.emit(HomeEvent.NavigateToChannel(projectId, channel.id))
        }
    }

    // 프로젝트 설정 아이콘 클릭 시 (새로 추가)
    fun onProjectSettingsClick(projectId: String) {
        viewModelScope.launch {
            // 설정은 별도 화면으로 네비게이션
            _eventFlow.emit(HomeEvent.NavigateToProjectSettings(projectId))
        }
    }

    // DM 아이템 클릭 시
    fun onDmClick(dmId: String) {
        viewModelScope.launch {
            when (_uiState.value.selectedTopSection) {
                TopSection.PROJECTS -> {
                    println("ViewModel: 프로젝트 추가 버튼 클릭 -> 화면 이동 요청")
                    _eventFlow.emit(HomeEvent.NavigateToAddProject) // 수정: 화면 이동 이벤트 발생
                }
                TopSection.DMS -> {
                    println("ViewModel: 친구 추가/DM 버튼 클릭")
                    _eventFlow.emit(HomeEvent.ShowAddFriendDialog)
                }
            }
        }
    }

    // FAB 클릭 시
    fun onAddButtonClick() {
        viewModelScope.launch {
            when (_uiState.value.selectedTopSection) {
                TopSection.PROJECTS -> {
                    println("ViewModel: 프로젝트 추가 버튼 클릭")
                    _eventFlow.emit(HomeEvent.ShowAddProjectDialog) // 또는 화면 이동 이벤트
                }
                TopSection.DMS -> {
                    println("ViewModel: 친구 추가/DM 버튼 클릭")
                    _eventFlow.emit(HomeEvent.ShowAddFriendDialog) // 또는 화면 이동 이벤트
                }
            }
        }
    }

    // 프로젝트 추가 버튼 클릭 시
    fun onProjectAddButtonClick() {
        viewModelScope.launch {
            println("ViewModel: 프로젝트 추가 버튼 클릭")
            _eventFlow.emit(HomeEvent.NavigateToAddProject) // 또는 화면 이동 이벤트
        }
    }
    
    // 친구 추가/DM 버튼 클릭 시
    fun onAddFriendClick() {
        viewModelScope.launch {
            println("ViewModel: 친구 추가 버튼 클릭")
            _eventFlow.emit(HomeEvent.ShowAddFriendDialog) // 또는 화면 이동 이벤트
        }
    }

    // 프로젝트 구조 편집 버튼 클릭 시
    fun onEditProjectStructureClick() {
        viewModelScope.launch {
            println("ViewModel: 프로젝트 구조 편집 버튼 클릭")
            _eventFlow.emit(HomeEvent.EditProjectStructure)
        }
    }

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = "default") }
    }
    
    // 전체 화면 표시 모드 토글
    fun toggleDetailDisplayMode() {
        _uiState.update { it.copy(isDetailFullScreen = !it.isDetailFullScreen) }
    }
    
    /**
     * 스낵바를 통해 메시지를 표시합니다.
     * @param message 표시할 메시지
     */
    fun showSnackbar(message: String) {
        viewModelScope.launch {
            _eventFlow.emit(HomeEvent.ShowSnackbar(message))
        }
    }
    
    /**
     * 프로젝트 구조를 새로고침합니다.
     * @param projectId 새로고침할 프로젝트 ID
     */
    fun refreshProjectStructure(projectId: String) {
        viewModelScope.launch {
            loadProjectStructure(projectId)
        }
    }
    
    // Domain 모델을 UI 모델로 변환하는 확장 함수
    private fun Project.toProjectUiModel(): ProjectUiModel {
        Log.d("HomeViewModel", "Mapping Project domain to ProjectUiModel: id=${this.id}, name=${this.name}")
        return ProjectUiModel(
            id = this.id,
            name = this.name,
            imageUrl = this.imageUrl,
        )
    }

    private fun formatTimestamp(timestamp: Instant): String {
        val localDateTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        val now = LocalDateTime.now(ZoneId.systemDefault())
        return when {
            ChronoUnit.MINUTES.between(localDateTime, now) < 1 -> "방금 전"
            ChronoUnit.HOURS.between(localDateTime, now) < 1 -> "${ChronoUnit.MINUTES.between(localDateTime, now)}분 전"
            ChronoUnit.DAYS.between(localDateTime, now) < 1 -> localDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
            ChronoUnit.DAYS.between(localDateTime, now) == 1L -> "어제"
            else -> localDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
        }
    }

    // DM 아이템 클릭 시 이벤트 발생
    fun onDmItemClick(dmUiModel: DmUiModel) {
        viewModelScope.launch {
            _eventFlow.emit(HomeEvent.NavigateToDmChat(dmUiModel.channelId))
        }
    }

    /**
     * 저장된 상태로부터 확장된 카테고리 목록을 복원합니다.
     * 화면 전환 시 이전 상태 복원에 사용됩니다.
     *
     * @param expandedCategoryIdsList 복원할 확장된 카테고리 ID 목록
     */
    fun restoreExpandedCategories(expandedCategoryIdsList: List<String>) {
        Log.d("HomeViewModel", "Restoring expanded categories: $expandedCategoryIdsList")
        val projectId = _uiState.value.selectedProjectId
        if (projectId != null) {
            _uiState.update { currentState ->
                val currentProjectStructure = currentState.projectStructure
                val updatedCategories = currentProjectStructure.categories.map { category ->
                    // category.id가 expandedCategoryIdsList에 포함되어 있으면 isExpanded를 true로 설정
                    category.copy(isExpanded = category.id in expandedCategoryIdsList)
                }
                
                // 내부 categoryExpandedStates 캐시도 업데이트
                val categoryMap = categoryExpandedStates.getOrPut(projectId) { mutableMapOf() }
                updatedCategories.forEach { category ->
                    categoryMap[category.id] = category.isExpanded
                }

                currentState.copy(
                    projectStructure = currentProjectStructure.copy(categories = updatedCategories)
                )
            }
        }
    }
    
    /**
     * 에러 메시지를 UI에 표시합니다.
     *
     * @param message 표시할 에러 메시지
     */
    fun showErrorMessage(message: String) {
        viewModelScope.launch {
            _eventFlow.emit(HomeEvent.ShowSnackbar(message))
        }
    }
}
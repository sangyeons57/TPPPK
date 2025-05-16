package com.example.feature_main.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Channel
import com.example.domain.model.Project
import com.example.domain.model.User
import com.example.domain.model.ui.DmUiModel
import com.example.domain.repository.UserRepository
import com.example.domain.usecase.dm.GetUserDmChannelsUseCase
import com.example.domain.usecase.project.FetchProjectListUseCase
import com.example.domain.usecase.project.GetProjectListStreamUseCase
import com.example.domain.usecase.project.GetProjectStructureUseCase
import com.example.domain.usecase.project.GetSchedulableProjectsUseCase
import com.example.domain.usecase.user.GetCurrentUserUseCase
import com.example.domain.usecase.user.GetUserInfoUseCase
import com.example.feature_main.ui.project.CategoryUiModel
import com.example.feature_main.ui.project.ChannelUiModel
import com.example.feature_main.ui.project.ProjectStructureUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

// --- 데이터 모델 (예시) ---
data class ProjectItem(
    val id: String,
    val name: String,
    val description: String,
    val lastUpdated: String // 예시 필드
)

// DmItem REMOVED
// ------------------------

// 홈 화면 UI 상태
data class HomeUiState(
    val selectedTopSection: TopSection = TopSection.DMS, // 기본 선택: DMS
    val projects: List<ProjectItem> = emptyList(),
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
}


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getUserDmChannelsUseCase: GetUserDmChannelsUseCase,
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val getProjectListStreamUseCase: GetProjectListStreamUseCase,
    private val fetchProjectListUseCase: FetchProjectListUseCase,
    private val getSchedulableProjectsUseCase: GetSchedulableProjectsUseCase,
    private val getProjectStructureUseCase: GetProjectStructureUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<HomeEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // 카테고리 확장 상태 캐시 (프로젝트 ID -> 카테고리 ID -> 확장 상태)
    private val categoryExpandedStates = mutableMapOf<String, MutableMap<String, Boolean>>()
    
    // 선택된 채널 ID
    private var selectedChannelId: String? = null
    
    // 현재 사용자 ID (실제로는 인증 서비스 등에서 가져와야 함)
    // 임시로 ""로 설정. 실제 앱에서는 주입받거나, UserRepository 등을 통해 가져와야 합니다.
    private var currentUserId: String = "" 

    init {
        viewModelScope.launch {
            // 현재 사용자 ID 가져오기
            getCurrentUserUseCase() // 파라미터 없이 호출
                .catch { exception -> // Flow 직접 반환 시 여기서 catch
                    _uiState.update { it.copy(isLoading = false, errorMessage = "사용자 정보 로드 실패: ${exception.localizedMessage}") }
                    Log.e("HomeViewModel", "Failed to load current user", exception)
                }
                .collectLatest { user -> // User 객체 직접 받음
                    currentUserId = user.id
                    Log.d("HomeViewModel", "Current User ID set: $currentUserId")
                    
                    // 사용자 이니셜과 프로필 이미지 URL 업데이트
                    _uiState.update { state ->
                        state.copy(
                            userInitial = user.name.firstOrNull()?.toString() ?: "U",
                            userProfileImageUrl = user.profileImageUrl
                        )
                    }
                    
                    // currentUserId 설정 후 DM 로드 (만약 DMS 탭이 선택되어 있고, dms 목록이 비어있다면)
                    if (_uiState.value.selectedTopSection == TopSection.DMS) { // && _uiState.value.dms.isEmpty() 불필요, loadDms에서 로딩 상태 관리
                        loadDms()
                    }
                }
        }

        loadDataForSelectedSection()

        viewModelScope.launch {
            // projectRepository.getProjectListStream() 대신 UseCase 사용
            getProjectListStreamUseCase()
                .catch { e ->
                     _uiState.update { it.copy(isLoading = false, errorMessage = "프로젝트 스트림 오류: ${e.localizedMessage}") }
                }
                .collectLatest { projects ->
                    if (_uiState.value.selectedTopSection == TopSection.PROJECTS) {
                        _uiState.update { state ->
                            state.copy(
                                projects = projects.map { it.toProjectItem() },
                                isLoading = false,
                                errorMessage = if (projects.isEmpty() && state.errorMessage == "default") "프로젝트가 없습니다." else state.errorMessage
                            )
                        }
                    }
                }
        }
    }

    // Helper to format timestamp for UI - Instant를 받도록 수정
    private fun formatDmTimestamp(timestamp: Instant?): String {
        if (timestamp == null) return ""
        val localDateTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        val now = LocalDateTime.now(ZoneId.systemDefault())
        return when {
            ChronoUnit.MINUTES.between(localDateTime, now) < 1 -> "Just now"
            ChronoUnit.HOURS.between(localDateTime, now) < 1 -> "${ChronoUnit.MINUTES.between(localDateTime, now)} min ago"
            ChronoUnit.DAYS.between(localDateTime, now) < 1 -> localDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
            ChronoUnit.DAYS.between(localDateTime, now) == 1L -> "Yesterday"
            else -> localDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
        }
    }

    // Mapper function Channel -> DmUiModel로 변경
    // Channel 객체에서 DmUiModel에 필요한 정보를 추출합니다.
    // partnerUserId, partnerUserName, partnerProfileImageUrl 은 Channel.participantIds 와 UserRepository 를 통해 가져와야 합니다.
    // 이 부분은 DmRepositoryImpl에서 Channel 객체를 어떻게 구성했는지에 따라 달라집니다.
    // 여기서는 Channel.name이 상대방 이름, Channel.coverImageUrl이 상대방 프로필 URL이라고 가정합니다.
    // 또한, currentUserId를 알아야 상대방 ID를 식별할 수 있습니다.
    private suspend fun Channel.toDmUiModel(currentUserId: String): DmUiModel {
        val partnerId = this.participantIds.find { it != currentUserId } ?: ""
        
        var partnerName = "알 수 없는 사용자" 
        var partnerProfilePic = ""

        if (partnerId.isNotEmpty()) {
            // GetUserInfoUseCase를 사용하여 파트너 정보를 가져옵니다.
            getUserInfoUseCase(partnerId).getOrNull()?.let { user -> 
                partnerName = user.name
                partnerProfilePic = user.profileImageUrl ?: ""
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

    // 상단 탭 선택 시 호출
    fun onTopSectionSelect(section: TopSection) {
        if (_uiState.value.selectedTopSection == section && !_uiState.value.isLoading) return // 로딩 중이 아닐 때만 중복 선택 무시
        _uiState.update { it.copy(selectedTopSection = section, isLoading = true, errorMessage = "default") }
        loadDataForSelectedSection()
    }

    // 현재 선택된 탭에 맞는 데이터 로드 함수
    private fun loadDataForSelectedSection() {
        // currentUserId가 필요한 DMS 탭은 init 블록의 collectLatest 이후 또는 onTopSectionSelect에서 로드
        if (_uiState.value.selectedTopSection == TopSection.DMS && currentUserId.isNotBlank()) {
            loadDms()
        } else if (_uiState.value.selectedTopSection == TopSection.PROJECTS) {
            loadProjects() // 프로젝트는 currentUserId와 무관하게 로드 가능
        } else if (_uiState.value.selectedTopSection == TopSection.DMS && currentUserId.isBlank()) {
            // currentUserId가 아직 준비되지 않았음을 알림
            _uiState.update { it.copy(isLoading = false, errorMessage = "사용자 정보를 가져오는 중입니다. 잠시 후 DMS를 로드합니다.") }
            Log.d("HomeViewModel", "DMS load deferred until currentUserId is available.")
        }
    }

    // 프로젝트 데이터 로드
    private fun loadProjects() { 
        viewModelScope.launch {
             _uiState.update { it.copy(isLoading = true, errorMessage = "default") }
            try {
                // projectRepository.fetchProjectList() 대신 UseCase 사용
                fetchProjectListUseCase().onFailure { e -> // Result를 반환하므로 onFailure 처리
                     _uiState.update {
                        it.copy(
                            errorMessage = "프로젝트 목록 동기화 실패: ${e.localizedMessage ?: "알 수 없는 오류"}",
                            // isLoading = false // 실패 시에도 로딩은 끝난 것으로 간주할 수 있음
                        )
                    }
                }
                // 성공 여부와 관계없이 로딩 상태는 아래 스트림에서 처리하거나, 여기서 명시적으로 false로 설정
                 _uiState.update { it.copy(isLoading = false) } 
            } catch (e: Exception) { // UseCase가 Result를 반환하면 이 catch는 불필요할 수 있음
                _uiState.update {
                    it.copy(
                        errorMessage = "프로젝트 목록 동기화 중 예측 못한 오류: ${e.localizedMessage ?: "알 수 없는 오류"}",
                        isLoading = false
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadDms() {
        if (currentUserId.isBlank()) { // 이중 체크
            Log.w("HomeViewModel", "loadDms called but currentUserId is blank.")
             _uiState.update { it.copy(isLoading = false, errorMessage = "DM을 로드하려면 사용자 정보가 필요합니다.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = "default") }
            Log.d("HomeViewModel", "Attempting to load DMs for user: $currentUserId")
            
            getUserDmChannelsUseCase(currentUserId) // Flow<List<Channel>> 반환
                .mapLatest { channels -> // 각 List<Channel>에 대해 List<DmUiModel>로 변환
                    channels.map { channel -> channel.toDmUiModel(currentUserId) }
                }
                .catch { exception ->
                    Log.e("HomeViewModel", "Error loading DMs", exception)
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = "DM 목록 로드 중 오류: ${exception.localizedMessage}"
                        )
                    }
                }
                .collectLatest { dmUiModels ->
                    Log.d("HomeViewModel", "DMs loaded: ${dmUiModels.size}")
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            dms = dmUiModels,
                            errorMessage = if (dmUiModels.isEmpty() && state.errorMessage == "default") "DM이 없습니다." 
                                           else if (state.errorMessage == "default") "" 
                                           else state.errorMessage
                        )
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
    private suspend fun loadProjectDetails(projectId: String) {
        try {
            // projectRepository.getAvailableProjectsForScheduling() 대신 UseCase 사용
            val allProjectsResult = getSchedulableProjectsUseCase()
            
            allProjectsResult.onSuccess { projectsList ->
                // 프로젝트 ID로 프로젝트 찾기
                val project = projectsList.find { it.id == projectId }
                
                if (project != null) {
                    // 프로젝트 찾음
                    _uiState.update { state ->
                        state.copy(
                            projectName = project.name,
                            projectDescription = project.description ?: "",
                            isLoading = false
                        )
                    }
                } else {
                    // 프로젝트를 찾을 수 없음
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = "프로젝트를 찾을 수 없습니다: $projectId"
                    )}
                }
            }.onFailure { error ->
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "프로젝트 정보를 불러오는데 실패했습니다: ${error.message ?: "알 수 없는 오류"}"
                )}
            }
            
            // 프로젝트 멤버 정보는 별도 API 호출이 필요할 수 있음
            // TODO: 멤버 정보 로드 로직 추가
            
        } catch (e: Exception) {
            _uiState.update { it.copy(
                isLoading = false,
                errorMessage = "오류 발생: ${e.message ?: "알 수 없는 오류"}"
            )}
        }
    }
    
    // 프로젝트 구조 (카테고리 및 채널) 로드
    private suspend fun loadProjectStructure(projectId: String) {
        try {
            // projectSettingRepository.getProjectStructure(projectId) 대신 UseCase 사용
            val structureResult = getProjectStructureUseCase(projectId)
            
            structureResult.onSuccess { resultData -> // resultData is Pair<String, List<ProjectCategory>>
                val (projectNameFromStructure, categories) = resultData // 프로젝트 이름도 함께옴
                // 프로젝트 구조 UI 모델로 변환
                val categoriesMap = categoryExpandedStates.getOrPut(projectId) { mutableMapOf() }
                
                val categoryUiModels = categories.map { category ->
                    // 카테고리 확장 상태 가져오기 (저장된 상태가 없으면 기본값 true)
                    val isExpanded = categoriesMap.getOrPut(category.id) { true }
                    
                    CategoryUiModel(
                        id = category.id,
                        name = category.name,
                        channels = category.channels.map { channel ->
                            ChannelUiModel(
                                id = channel.id,
                                name = channel.name,
                                mode = channel.channelMode,
                                isSelected = channel.id == selectedChannelId
                            )
                        },
                        isExpanded = isExpanded
                    )
                }
                
                // 일반 채널 (카테고리에 속하지 않은 채널) - 현재는 미구현
                val generalChannels = emptyList<ChannelUiModel>()
                
                // UI 상태 업데이트
                _uiState.update { state ->
                    state.copy(
                        projectName = projectNameFromStructure, // UseCase에서 프로젝트 이름도 가져오므로 업데이트
                        projectStructure = ProjectStructureUiState(
                            categories = categoryUiModels,
                            generalChannels = generalChannels,
                            isLoading = false
                        )
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        projectStructure = state.projectStructure.copy(
                            isLoading = false,
                            error = "프로젝트 구조를 불러오는데 실패했습니다: ${error.message ?: "알 수 없는 오류"}"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update { state ->
                state.copy(
                    projectStructure = state.projectStructure.copy(
                        isLoading = false,
                        error = "오류 발생: ${e.message ?: "알 수 없는 오류"}"
                    )
                )
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
            
            val updatedGeneralChannels = state.projectStructure.generalChannels.map { ch ->
                ch.copy(isSelected = ch.id == channel.id)
            }
            
            state.copy(
                projectStructure = state.projectStructure.copy(
                    categories = updatedCategories,
                    generalChannels = updatedGeneralChannels
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

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = "default") }
    }
    
    // 전체 화면 표시 모드 토글
    fun toggleDetailDisplayMode() {
        _uiState.update { it.copy(isDetailFullScreen = !it.isDetailFullScreen) }
    }
    
    // Domain 모델을 UI 모델로 변환하는 확장 함수
    private fun Project.toProjectItem(): ProjectItem {
        return ProjectItem(
            id = this.id,
            name = this.name,
            description = this.description ?: "설명 없음",
            lastUpdated = this.updatedAt?.let { formatTimestamp(it) } ?: "알 수 없음"
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
}
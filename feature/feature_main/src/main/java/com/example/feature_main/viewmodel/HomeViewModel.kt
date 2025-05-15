package com.example.feature_main.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Channel
import com.example.domain.model.Project
import com.example.domain.model.ui.DmUiModel
import com.example.domain.repository.ProjectRepository
import com.example.domain.repository.ProjectSettingRepository
import com.example.feature_main.ui.project.CategoryUiModel
import com.example.feature_main.ui.project.ChannelUiModel
import com.example.feature_main.ui.project.ProjectStructureUiState
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val projectName: String = "", // 선택된 프로젝트 이름
    val projectDescription: String? = null, // 선택된 프로젝트 설명
    val projectMembers: List<ProjectMember> = emptyList(), // 선택된 프로젝트 멤버
    
    // 카테고리 및 채널 관련 상태
    val projectStructure: ProjectStructureUiState = ProjectStructureUiState(),
    
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
    private val projectRepository: ProjectRepository,
    private val projectSettingRepository: ProjectSettingRepository,
    private val userRepository: com.example.domain.repository.UserRepository
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
            // 실제로는 AuthRepository 등을 통해 현재 사용자 ID를 가져와야 합니다.
            // 여기서는 임시로 userRepository.getCurrentUserId() (가상) 또는 고정값을 사용합니다.
            // currentUserId = userRepository.getCurrentUserId()?.id ?: "" // 예시
            // 이 부분이 실제 앱의 구조에 맞게 수정되어야 합니다.
            // currentUserId를 설정하는 로직이 필요합니다. 여기서는 DmRepositoryImpl에서 currentUserId를 필요로 하므로,
            // HomeViewModel에서도 이를 알아야 partnerUserId를 식별할 수 있습니다.
            // 지금은 DmRepositoryImpl에서 알아서 처리한다고 가정하고, toDmUiModel에서 currentUserId를 인자로 넘깁니다.
        }

        // 초기 데이터 로드 (선택된 탭 기준)
        loadDataForSelectedSection()
        
        // 프로젝트 목록 관찰
        viewModelScope.launch {
            projectRepository.getProjectListStream()
                .collectLatest { projects ->
                    if (_uiState.value.selectedTopSection == TopSection.PROJECTS) {
                        _uiState.update { state ->
                            state.copy(
                                projects = projects.map { it.toProjectItem() },
                                isLoading = false
                            )
                        }
                    }
                }
        }
        // currentUserId를 얻기 위한 임시 로직 (실제 앱에서는 Hilt를 통해 UserRepository 주입 후 사용자 정보 조회)
        viewModelScope.launch {
            userRepository.getCurrentUserProfileStream().firstOrNull()?.let { user ->
                currentUserId = user.id
                // currentUserId 설정 후 DM 로드 재시도 (이미 loadDataForSelectedSection() 호출됨)
                if (_uiState.value.selectedTopSection == TopSection.DMS && _uiState.value.dms.isEmpty()) {
                     loadDms() // currentUserId가 설정된 후 다시 로드
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
        // DM 채널의 경우 참여자는 2명 (현재 사용자와 상대방)
        val partnerId = this.participantIds.find { it != currentUserId } ?: ""
        
        // 상대방 사용자 정보 조회 (이름, 프로필 이미지) - 실제로는 UserRepository를 통해 비동기 조회 필요
        // 여기서는 Channel의 name과 coverImageUrl을 사용한다고 가정.
        // 만약 Channel.name이 "UserA, UserB" 형태라면 파싱 필요.
        // DmRepositoryImpl에서 name을 상대방 이름으로, coverImageUrl을 상대방 프로필로 설정했다면 더 간단.
        var partnerName = this.name // 기본값으로 채널 이름 사용
        var partnerProfilePic =  ""

        if (partnerId.isNotEmpty()) {
            userRepository.getUser(partnerId).getOrNull()?.let { user ->
                partnerName = user.name
                partnerProfilePic = user.profileImageUrl.toString()
            }
        }


        return DmUiModel(
            channelId = this.id,
            partnerName = partnerName, // Channel.name 또는 UserRepository에서 조회
            partnerProfileImageUrl = partnerProfilePic, // Channel.coverImageUrl 또는 UserRepository에서 조회
            lastMessage = this.lastMessagePreview, // Channel.lastMessageSnippet 사용
            lastMessageTimestamp = this.updatedAt, // Channel.updatedAt (또는 lastActivityAt) 사용, Instant 타입
        )
    }

    // 상단 탭 선택 시 호출
    fun onTopSectionSelect(section: TopSection) {
        if (_uiState.value.selectedTopSection == section) return // 이미 선택된 탭이면 무시
        _uiState.update { it.copy(selectedTopSection = section, isLoading = true, errorMessage = "default") }
        // 선택된 탭에 맞는 데이터 로드
        loadDataForSelectedSection()
    }

    // 현재 선택된 탭에 맞는 데이터 로드 함수
    private fun loadDataForSelectedSection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // 로딩 시작
            when (_uiState.value.selectedTopSection) {
                TopSection.PROJECTS -> loadProjects()
                TopSection.DMS -> loadDms()
            }
        }
    }

    // 프로젝트 데이터 로드
    private suspend fun loadProjects() {
        // 원격 데이터 동기화 시도
        try {
            projectRepository.fetchProjectList()
            // 프로젝트 목록 업데이트는 getProjectListStream() Flow에 의해 자동 처리됨
        } catch (e: Exception) {
            // 원격 데이터 가져오기 실패 처리
            _uiState.update {
                it.copy(
                    errorMessage = "프로젝트 목록을 가져오는 중 오류 발생: ${e.localizedMessage ?: "알 수 없는 오류"}",
                    isLoading = false
                ) 
            }
        }
    }

    private fun loadDms() {
        // currentUserId가 설정되지 않았으면 DM 로드를 시도하지 않음.
        if (currentUserId.isBlank() && _uiState.value.selectedTopSection == TopSection.DMS) {
             // currentUserId를 기다렸다가 다시 시도하도록 로직 추가 가능 (예: collect 이후 currentUserId 변경 시 재시작)
            Log.w("HomeViewModel", "Current User ID is blank. DM loading skipped.")
             _uiState.update { it.copy(isLoading = false, errorMessage = "User ID not available for DMs.") }
            return
        }

        viewModelScope.launch {
            // _uiState.update { it.copy(isLoading = true) } // isLoading is set by loadDataForSelectedSection
            Log.d("HomeViewModel", "Attempting to load DMs for user: $currentUserId")
            // getUserDmChannelsUseCase 호출 (userId 파라미터가 있다면 전달, 없다면 UseCase 내부에서 처리 가정)
            // GetUserDmChannelsUseCase는 userId를 파라미터로 받으므로, currentUserId를 전달해야 합니다.
            getUserDmChannelsUseCase(currentUserId) 
                .map { domainModels ->
                    // Channel을 DmUiModel로 변환 (비동기 매핑 가능성 고려)
                    // toDmUiModel이 suspend 함수이므로 map 내부에서 직접 호출은 부적절할 수 있음.
                    // Flow<List<Channel>> -> Flow<List<DmUiModel>>
                    // 각 Channel에 대해 toDmUiModel을 호출해야 함.
                    // Flow의 transform이나 mapLatest 같은 연산자 사용 고려.
                    // 여기서는 간단히 map으로 처리. toDmUiModel이 suspend이므로 Flow를 처리하는 방식 변경 필요
                    domainModels.map { channel -> channel.toDmUiModel(currentUserId) } // toDmUiModel에 currentUserId 전달
                }
                .catch { e ->
                    Log.e("HomeViewModel", "Error loading DMs", e)
                    _uiState.update { state ->
                        state.copy(
                            errorMessage = "Failed to load DMs: ${e.localizedMessage}", 
                            isLoading = false
                        )
                    }
                }
                .collectLatest { uiModels ->
                    Log.d("HomeViewModel", "Successfully loaded DMs: ${uiModels.size} items")
                    _uiState.update { state ->
                        state.copy(
                            dms = uiModels,
                            isLoading = false,
                            errorMessage = if (uiModels.isEmpty() && currentUserId.isNotBlank()) "No DMs found." else state.errorMessage
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
            // 일정 추가용 프로젝트 목록을 이용하여 프로젝트 상세 정보 로드
            val allProjectsResult = projectRepository.getAvailableProjectsForScheduling()
            
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
            // 프로젝트 구조 로드
            val structureResult = projectSettingRepository.getProjectStructure(projectId)
            
            // onSuccess/onFailure 호출을 별도 변수로 분리하여 타입 추론 이슈 해결
            val result = structureResult
            
            result.onSuccess { resultData ->
                val (_, categories) = resultData
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
                                mode = channel.type,
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

    fun onProjectAddButtonClick() {
        viewModelScope.launch {
            println("ViewModel: 프로젝트 추가 버튼 클릭")
            _eventFlow.emit(HomeEvent.NavigateToAddProject) // 또는 화면 이동 이벤트
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
            description = this.description ?: "",
            lastUpdated = "최근" // 실제 업데이트 시간이 있으면 포맷팅
        )
    }

    // DM 아이템 클릭 시 이벤트 발생
    fun onDmItemClick(dmUiModel: DmUiModel) {
        viewModelScope.launch {
            _eventFlow.emit(HomeEvent.NavigateToDmChat(dmUiModel.channelId))
        }
    }
}
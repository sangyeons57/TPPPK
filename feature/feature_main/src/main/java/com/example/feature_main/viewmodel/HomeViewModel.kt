package com.example.feature_main.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Project
import com.example.domain.model.ProjectCategory
import com.example.domain.model.ProjectChannel
import com.example.domain.repository.ProjectRepository
import com.example.domain.repository.ProjectSettingRepository
import com.example.feature_main.ui.project.CategoryUiModel
import com.example.feature_main.ui.project.ChannelUiModel
import com.example.feature_main.ui.project.ProjectStructureUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

data class DmItem(
    val id: String, // DM 방 ID 또는 상대방 User ID
    val partnerName: String,
    val lastMessage: String?,
    val unreadCount: Int,
    val partnerProfileUrl: String? // 예시 필드
)
// ------------------------

// 홈 화면 UI 상태
data class HomeUiState(
    val selectedTopSection: TopSection = TopSection.PROJECTS, // 기본 선택: 프로젝트
    val projects: List<ProjectItem> = emptyList(),
    val dms: List<DmItem> = emptyList(),
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
    private val projectSettingRepository: ProjectSettingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<HomeEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // 카테고리 확장 상태 캐시 (프로젝트 ID -> 카테고리 ID -> 확장 상태)
    private val categoryExpandedStates = mutableMapOf<String, MutableMap<String, Boolean>>()
    
    // 선택된 채널 ID
    private var selectedChannelId: String? = null

    init {
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

    private suspend fun loadDms() {
        println("ViewModel: DM 목록 로드 시도")
        // val result = dmRepository.getDmList()
        delay(500) // 임시 딜레이
        val success = true // 임시 성공
        if (success) {
            _uiState.update {
                it.copy(
                    dms = List(5) { i -> DmItem("dm$i", "친구 ${i+1}", "마지막 메시지 ${i+1}", i % 3, null) },
                    isLoading = false
                )
            }
        } else {
            _uiState.update { it.copy(errorMessage = "DM 로드 실패", isLoading = false) }
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
                                type = channel.type,
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
}
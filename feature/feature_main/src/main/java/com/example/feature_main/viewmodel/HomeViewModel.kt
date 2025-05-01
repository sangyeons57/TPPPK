package com.example.teamnovapersonalprojectprojectingkotlin.feature_main.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val errorMessage: String = "default"
)

// 홈 화면 이벤트
sealed class HomeEvent {
    data class NavigateToProjectDetails(val projectId: String) : HomeEvent()
    data class NavigateToDmChat(val dmId: String) : HomeEvent()
    object ShowAddProjectDialog : HomeEvent() // 또는 화면 이동
    object ShowAddFriendDialog : HomeEvent() // 또는 화면 이동
    object NavigateToAddProject : HomeEvent() // 또는 화면 이동
    data class ShowSnackbar(val message: String) : HomeEvent()
}


@HiltViewModel
class HomeViewModel @Inject constructor(
    // TODO: private val projectRepository: ProjectRepository,
    // TODO: private val dmRepository: DmRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<HomeEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        // 초기 데이터 로드 (선택된 탭 기준)
        loadDataForSelectedSection()
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
            _uiState.update { it.copy(isLoading = false) } // 로딩 종료 (성공/실패 처리는 각 함수 내에서)
        }
    }

    // --- TODO: 실제 데이터 로딩 함수 구현 ---
    private suspend fun loadProjects() {
        println("ViewModel: 프로젝트 목록 로드 시도")
        // val result = projectRepository.getProjectList()
        kotlinx.coroutines.delay(500) // 임시 딜레이
        val success = true // 임시 성공
        if (success) {
            _uiState.update {
                it.copy(
                    projects = List(5) { i -> ProjectItem("p$i", "프로젝트 ${i + 1}", "프로젝트 설명 ${i + 1}", "어제") },
                    isLoading = false // 여기서 로딩 종료해도 됨
                )
            }
        } else {
            _uiState.update { it.copy(errorMessage = "프로젝트 로드 실패", isLoading = false) }
        }
    }

    private suspend fun loadDms() {
        println("ViewModel: DM 목록 로드 시도")
        // val result = dmRepository.getDmList()
        kotlinx.coroutines.delay(500) // 임시 딜레이
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
    // ---------------------------------------

    // 프로젝트 아이템 클릭 시
    fun onProjectClick(projectId: String) {
        viewModelScope.launch {
            println("ViewModel: 프로젝트 클릭 - $projectId")
            // TODO: 실제 프로젝트 상세 화면 이동 로직
            // _eventFlow.emit(HomeEvent.NavigateToProjectDetails(projectId))
            _eventFlow.emit(HomeEvent.ShowSnackbar("프로젝트 $projectId 클릭됨 (이동 구현 필요)"))
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
}
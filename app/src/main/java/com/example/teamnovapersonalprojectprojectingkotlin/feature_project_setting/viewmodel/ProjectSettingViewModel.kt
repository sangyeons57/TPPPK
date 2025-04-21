package com.example.teamnovapersonalprojectprojectingkotlin.feature_project_setting.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ProjectStructure ViewModel에서 사용된 enum 재사용 또는 별도 정의
enum class ChannelType { TEXT, VOICE }

// --- 데이터 모델 ---
data class ProjectChannel(
    val id: String,
    val name: String,
    val type: ChannelType
)

data class ProjectCategory(
    val id: String,
    val name: String,
    val channels: List<ProjectChannel> = emptyList()
)

// --- UI 상태 ---
data class ProjectSettingUiState(
    val projectId: String = "",
    val projectName: String = "",
    val categories: List<ProjectCategory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// --- 이벤트 ---
sealed class ProjectSettingEvent {
    object NavigateBack : ProjectSettingEvent()
    data class ShowSnackbar(val message: String) : ProjectSettingEvent()
    data class NavigateToEditCategory(val projectId: String, val categoryId: String) : ProjectSettingEvent()
    data class NavigateToCreateCategory(val projectId: String) : ProjectSettingEvent()
    data class NavigateToEditChannel(val projectId: String, val categoryId: String, val channelId: String) : ProjectSettingEvent()
    data class NavigateToCreateChannel(val projectId: String, val categoryId: String) : ProjectSettingEvent()
    data class NavigateToMemberList(val projectId: String) : ProjectSettingEvent()
    data class NavigateToRoleList(val projectId: String) : ProjectSettingEvent()
    data class ShowDeleteCategoryConfirm(val category: ProjectCategory) : ProjectSettingEvent()
    data class ShowDeleteChannelConfirm(val channel: ProjectChannel) : ProjectSettingEvent()
    object ShowRenameProjectDialog : ProjectSettingEvent()
    object ShowDeleteProjectConfirm : ProjectSettingEvent()

}

// --- Repository 인터페이스 (가상) ---
interface ProjectSettingRepository { // 별도 또는 ProjectStructureRepository 확장
    suspend fun getProjectStructure(projectId: String): Result<Pair<String, List<ProjectCategory>>> // 이름과 구조 반환
    suspend fun deleteCategory(categoryId: String): Result<Unit>
    suspend fun deleteChannel(channelId: String): Result<Unit>
    suspend fun renameProject(projectId: String, newName: String): Result<Unit>
    suspend fun deleteProject(projectId: String): Result<Unit>
    // ... (카테고리/채널 생성, 수정 관련 함수는 다른 Repo 또는 여기에 포함)
}

@HiltViewModel
class ProjectSettingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    // TODO: private val repository: ProjectSettingRepository
    // TODO: private val structureRepository: ProjectStructureRepository // 필요 시
) : ViewModel() {

    val projectId: String = savedStateHandle["projectId"] ?: error("projectId가 전달되지 않았습니다.")

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
            println("ViewModel: Loading structure for project $projectId")
            // --- TODO: 실제 프로젝트 구조 로드 (repository.getProjectStructure) ---
            delay(800) // 임시 딜레이
            val success = true
            // val result = repository.getProjectStructure(projectId)
            // --------------------------------------------------------------
            if (success /*result.isSuccess*/) {
                // 임시 데이터
                val projectName = "프로젝트 $projectId"
                val categories = listOf(
                    ProjectCategory("c1", "공지사항", listOf(
                        ProjectChannel("ch1", "전체 공지", ChannelType.TEXT)
                    )),
                    ProjectCategory("c2", "팀 채널", listOf(
                        ProjectChannel("ch2", "일반 대화", ChannelType.TEXT),
                        ProjectChannel("ch3", "아이디어 공유", ChannelType.TEXT),
                        ProjectChannel("ch4", "주간 회의", ChannelType.VOICE)
                    )),
                    ProjectCategory("c3", "자료실", listOf(
                        ProjectChannel("ch5", "디자인 자료", ChannelType.TEXT)
                    ))
                )
                // val (projectName, categories) = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        projectName = projectName,
                        categories = categories
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "프로젝트 정보를 불러오지 못했습니다." // result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    // --- 카테고리 관련 액션 ---
    fun requestEditCategory(categoryId: String) {
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.NavigateToEditCategory(projectId, categoryId)) }
    }
    fun requestDeleteCategory(category: ProjectCategory) {
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.ShowDeleteCategoryConfirm(category)) }
    }
    fun confirmDeleteCategory(categoryId: String) {
        viewModelScope.launch {
            // TODO: repository.deleteCategory(categoryId) 호출 및 결과 처리
            println("Deleting Category: $categoryId")
            delay(500) // 임시
            _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("카테고리가 삭제되었습니다."))
            loadProjectStructure() // 구조 새로고침
        }
    }
    fun requestCreateCategory() {
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.NavigateToCreateCategory(projectId)) }
    }

    // --- 채널 관련 액션 ---
    fun requestEditChannel(categoryId: String, channelId: String) {
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.NavigateToEditChannel(projectId, categoryId, channelId)) }
    }
    fun requestDeleteChannel(channel: ProjectChannel) {
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.ShowDeleteChannelConfirm(channel)) }
    }
    fun confirmDeleteChannel(channelId: String) {
        viewModelScope.launch {
            // TODO: repository.deleteChannel(channelId) 호출 및 결과 처리
            println("Deleting Channel: $channelId")
            delay(500) // 임시
            _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("채널이 삭제되었습니다."))
            loadProjectStructure() // 구조 새로고침
        }
    }
    fun requestCreateChannel(categoryId: String) {
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.NavigateToCreateChannel(projectId, categoryId)) }
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
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.ShowRenameProjectDialog) }
    }
    fun confirmRenameProject(newName: String) {
        viewModelScope.launch {
            // TODO: repository.renameProject(projectId, newName) 호출 및 결과 처리
            println("Renaming Project $projectId to '$newName'")
            delay(500) // 임시
            _uiState.update { it.copy(projectName = newName) } // UI 즉시 반영
            _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트 이름이 변경되었습니다."))
        }
    }

    // --- 프로젝트 삭제 ---
    fun requestDeleteProject() {
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.ShowDeleteProjectConfirm) }
    }
    fun confirmDeleteProject() {
        viewModelScope.launch {
            // TODO: repository.deleteProject(projectId) 호출 및 결과 처리
            println("Deleting Project $projectId")
            delay(1000) // 임시
            _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트가 삭제되었습니다."))
            _eventFlow.emit(ProjectSettingEvent.NavigateBack) // 설정 화면 나가기
        }
    }
}
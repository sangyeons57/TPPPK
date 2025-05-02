package com.example.feature_project.members.viewmodel // 경로 확인!

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ProjectMember
import com.example.domain.repository.ProjectMemberRepository
// Domain 요소 Import
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI 모델 ---
data class ProjectMemberItem( // UI 표시에 필요한 정보
    val userId: String,
    val userName: String,
    val profileImageUrl: String?,
    val rolesText: String // 역할 목록을 UI에 표시할 문자열
)

// Domain -> UI 모델 변환
fun ProjectMember.toUiModel(): ProjectMemberItem {
    return ProjectMemberItem(
        userId = this.userId,
        userName = this.userName,
        profileImageUrl = this.profileImageUrl,
        rolesText = this.roleNames.joinToString(", ") // 예: "관리자, 팀원"
    )
}

// --- UI 상태 ---
data class MemberListUiState(
    val members: List<ProjectMemberItem> = emptyList(), // UI 모델 사용
    val isLoading: Boolean = false,
    val error: String? = null
)

// --- 이벤트 ---
sealed class MemberListEvent {
    data class NavigateToEditMember(val projectId: String, val userId: String) : MemberListEvent()
    data class ShowAddMemberDialog(val projectId: String) : MemberListEvent() // 멤버 추가 다이얼로그 표시
    data class ShowSnackbar(val message: String) : MemberListEvent()
}

// --- ViewModel ---
@HiltViewModel
class MemberListViewModel @Inject constructor( // 클래스 이름 오타 수정 확인!
    private val savedStateHandle: SavedStateHandle,
    private val projectMemberRepository: ProjectMemberRepository // ★ Domain Repository 주입
) : ViewModel() {

    val projectId: String = savedStateHandle["projectId"] ?: error("projectId가 필요합니다.")

    private val _uiState = MutableStateFlow(MemberListUiState(isLoading = true))
    val uiState: StateFlow<MemberListUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<MemberListEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        observeMembers()
        refreshMembers()
    }

    private fun observeMembers() {
        viewModelScope.launch {
            projectMemberRepository.getProjectMembersStream(projectId) // ★ Repository 호출 (Stream)
                .map { domainMembers -> domainMembers.map { it.toUiModel() } } // Domain -> UI 모델 변환
                .catch { e -> _uiState.update { it.copy(error = "멤버 목록 스트림 오류: ${e.message}", isLoading = false) } }
                .collect { uiMembers ->
                    _uiState.update { it.copy(members = uiMembers, isLoading = false, error = null) }
                }
        }
    }

    fun refreshMembers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = projectMemberRepository.fetchProjectMembers(projectId) // ★ Repository 호출 (Fetch)
            if (result.isFailure) {
                _uiState.update { it.copy(error = "멤버 목록 새로고침 실패", isLoading = false) }
            }
            // 성공 시 Flow가 처리, 로딩 상태만 해제
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onMemberClick(userId: String) {
        viewModelScope.launch {
            _eventFlow.emit(MemberListEvent.NavigateToEditMember(projectId, userId))
        }
    }

    fun onAddMemberClick() {
        viewModelScope.launch {
            // TODO: 멤버 추가 권한 확인 로직 필요 시 추가
            _eventFlow.emit(MemberListEvent.ShowAddMemberDialog(projectId))
        }
    }

    // --- 내부 Repository 및 데이터 클래스 정의 삭제됨 ---
}
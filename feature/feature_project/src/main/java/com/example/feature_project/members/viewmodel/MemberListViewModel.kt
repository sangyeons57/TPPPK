package com.example.feature_project.members.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ProjectMember
import com.example.domain.repository.ProjectMemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 프로젝트 멤버 아이템 UI 모델
 * UI 표시에 필요한 정보를 담고 있는 데이터 클래스
 *
 * @param userId 사용자 ID
 * @param userName 사용자 이름
 * @param profileImageUrl 프로필 이미지 URL (nullable)
 * @param rolesText 역할 목록을 표시할 문자열
 */
data class ProjectMemberItem(
    val userId: String,
    val userName: String,
    val profileImageUrl: String?,
    val rolesText: String
)

/**
 * 멤버 목록 화면의 UI 상태
 *
 * @param members 멤버 UI 아이템 목록
 * @param isLoading 로딩 중 여부
 * @param error 오류 메시지 (nullable)
 */
data class MemberListUiState(
    val members: List<ProjectMemberItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 멤버 목록 화면에서 발생하는 이벤트 정의
 */
sealed class MemberListEvent {
    /**
     * 멤버 편집 화면으로 이동 이벤트
     */
    data class NavigateToEditMember(val projectId: String, val userId: String) : MemberListEvent()
    
    /**
     * 멤버 추가 다이얼로그 표시 이벤트
     */
    data class ShowAddMemberDialog(val projectId: String) : MemberListEvent()
    
    /**
     * 스낵바 메시지 표시 이벤트
     */
    data class ShowSnackbar(val message: String) : MemberListEvent()
}

/**
 * 멤버 목록 화면의 ViewModel
 * 프로젝트 멤버 데이터를 관리하고 UI 상태를 제공합니다.
 *
 * @param savedStateHandle 상태 저장 핸들러 (프로젝트 ID 전달용)
 * @param projectMemberRepository 프로젝트 멤버 저장소 인터페이스
 */
@HiltViewModel
class MemberListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val projectMemberRepository: ProjectMemberRepository
) : ViewModel() {

    /**
     * 현재 보고 있는 프로젝트 ID
     */
    val projectId: String = savedStateHandle["projectId"] ?: error("projectId가 필요합니다.")

    /**
     * UI 상태 (내부 Mutable 버전)
     */
    private val _uiState = MutableStateFlow(MemberListUiState(isLoading = true))
    
    /**
     * UI 상태 (외부 노출용 읽기 전용 버전)
     */
    val uiState: StateFlow<MemberListUiState> = _uiState.asStateFlow()

    /**
     * 이벤트 Flow (내부 Mutable 버전)
     */
    private val _eventFlow = MutableSharedFlow<MemberListEvent>()
    
    /**
     * 이벤트 Flow (외부 노출용 읽기 전용 버전)
     */
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        observeMembers()
        refreshMembers()
    }

    /**
     * Domain 모델을 UI 모델로 변환하는 확장 함수
     */
    private fun ProjectMember.toUiModel(): ProjectMemberItem {
        return ProjectMemberItem(
            userId = this.userId,
            userName = this.userName,
            profileImageUrl = this.profileImageUrl,
            rolesText = this.roleNames.joinToString(", ") // 예: "관리자, 팀원"
        )
    }

    /**
     * 멤버 목록 실시간 관찰 함수
     * 멤버 목록 변화를 실시간으로 감지하여 UI 상태를 업데이트합니다.
     */
    private fun observeMembers() {
        viewModelScope.launch {
            projectMemberRepository.getProjectMembersStream(projectId)
                .map { domainMembers -> domainMembers.map { it.toUiModel() } }
                .catch { e -> 
                    _uiState.update { it.copy(
                        error = "멤버 목록 스트림 오류: ${e.message}", 
                        isLoading = false
                    ) }
                }
                .collect { uiMembers ->
                    _uiState.update { it.copy(
                        members = uiMembers, 
                        isLoading = false, 
                        error = null
                    ) }
                }
        }
    }

    /**
     * 멤버 목록 새로고침 함수
     * 서버에서 최신 멤버 목록을 가져와 로컬 캐시를 업데이트합니다.
     */
    fun refreshMembers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = projectMemberRepository.fetchProjectMembers(projectId)
            if (result.isFailure) {
                _uiState.update { it.copy(
                    error = "멤버 목록 새로고침 실패", 
                    isLoading = false
                ) }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 멤버 클릭 이벤트 처리 함수
     * 멤버 편집 화면으로 이동하는 이벤트를 발생시킵니다.
     *
     * @param userId 클릭한 멤버의 사용자 ID
     */
    fun onMemberClick(userId: String) {
        viewModelScope.launch {
            _eventFlow.emit(MemberListEvent.NavigateToEditMember(projectId, userId))
        }
    }

    /**
     * 멤버 추가 버튼 클릭 이벤트 처리 함수
     * 멤버 추가 다이얼로그를 표시하는 이벤트를 발생시킵니다.
     */
    fun onAddMemberClick() {
        viewModelScope.launch {
            _eventFlow.emit(MemberListEvent.ShowAddMemberDialog(projectId))
        }
    }
} 
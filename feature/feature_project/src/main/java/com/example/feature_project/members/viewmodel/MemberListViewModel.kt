package com.example.feature_project.members.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_navigation.extension.getRequiredString
import com.example.core_navigation.destination.AppRoutes
import com.example.domain.model.ProjectMember
import com.example.domain.usecase.project.DeleteProjectMemberUseCase
import com.example.domain.usecase.project.FetchProjectMembersUseCase
import com.example.domain.usecase.project.ObserveProjectMembersUseCase
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
 * @param searchQuery 검색 쿼리
 * @param selectedMember 선택된 멤버 (멤버 편집/삭제 등을 위한 상태)
 */
data class MemberListUiState(
    val members: List<ProjectMemberItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedMember: ProjectMember? = null
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
     * 멤버 삭제 확인 다이얼로그 표시 이벤트
     */
    data class ShowDeleteConfirm(val member: ProjectMember) : MemberListEvent()
    
    /**
     * 스낵바 메시지 표시 이벤트
     */
    data class ShowSnackbar(val message: String) : MemberListEvent()

    /**
     * 멤버 추가 다이얼로그 표시 이벤트
     */
    data class ShowAddMemberDialog(val projectId: String) : MemberListEvent()
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
    savedStateHandle: SavedStateHandle,
    private val observeProjectMembersUseCase: ObserveProjectMembersUseCase,
    private val fetchProjectMembersUseCase: FetchProjectMembersUseCase,
    private val deleteProjectMemberUseCase: DeleteProjectMemberUseCase
) : ViewModel() {

    private val projectId: String = savedStateHandle.getRequiredString(AppRoutes.Project.ARG_PROJECT_ID)

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
            rolesText = this.roles.joinToString(", ") { it.name }
        )
    }

    /**
     * 멤버 목록 실시간 관찰 및 검색 필터링 함수
     */
    private fun observeMembers() {
        viewModelScope.launch {
            // Combine search query and member stream
            uiState.map { it.searchQuery }.distinctUntilChanged()
                .combine(observeProjectMembersUseCase(projectId)) { query, members ->
                    members.filter { member ->
                        member.userName.contains(query, ignoreCase = true)
                    }.map { it.toUiModel() } // Map to UI model after filtering
                }
                .catch { e ->
                    _uiState.update { it.copy(error = "멤버 목록 스트림 오류: ${e.message}", isLoading = false) }
                }
                .collect { filteredUiMembers ->
                    _uiState.update { it.copy(members = filteredUiMembers, isLoading = false, error = null) }
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
            val result = fetchProjectMembersUseCase(projectId)
            if (result.isFailure) {
                _uiState.update { it.copy(
                    error = "멤버 목록 새로고침 실패",
                    isLoading = false
                ) }
            }
            // If successful, observeMembers will handle the update. Just turn off loading.
             _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * ★ 검색 쿼리 변경 처리 함수 추가
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        // The combine operator in observeMembers will automatically react to this state change.
    }

    /**
     * 멤버 클릭 이벤트 처리 함수
     * 멤버 편집 화면으로 이동하는 이벤트를 발생시킵니다.
     *
     * @param member 클릭한 멤버 객체 (Domain 모델)
     */
    fun onMemberClick(member: ProjectMember) {
        viewModelScope.launch {
            _eventFlow.emit(MemberListEvent.NavigateToEditMember(projectId, member.userId))
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

    /**
     * ★ 멤버 삭제 요청 처리 함수 추가
     */
    fun requestDeleteMember(member: ProjectMember) {
        viewModelScope.launch {
            _eventFlow.emit(MemberListEvent.ShowDeleteConfirm(member))
        }
    }

    /**
     * ★ 멤버 삭제 확정 처리 함수 추가
     */
    fun confirmDeleteMember(member: ProjectMember) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = deleteProjectMemberUseCase(projectId, member.userId)
            if (result.isSuccess) {
                _eventFlow.emit(MemberListEvent.ShowSnackbar("${member.userName}님을 내보냈습니다."))
            } else {
                _eventFlow.emit(MemberListEvent.ShowSnackbar("멤버 내보내기 실패: ${result.exceptionOrNull()?.message ?: "알 수 없는 오류"}"))
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
} 
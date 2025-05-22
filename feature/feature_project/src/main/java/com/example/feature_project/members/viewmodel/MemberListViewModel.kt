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
import com.example.domain.usecase.project.GetProjectRolesUseCase // Added import
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
// import com.example.core_common.util.DateTimeUtil // Already imported if needed, or remove if not used in this diff


data class MemberListUiState(
    val members: List<ProjectMember> = emptyList(), // Changed from ProjectMemberUiItem
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedMember: ProjectMember? = null,
    val projectId: String = ""
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
    private val deleteProjectMemberUseCase: DeleteProjectMemberUseCase,
    private val getProjectRolesUseCase: GetProjectRolesUseCase // Added
) : ViewModel() {

    private val projectId: String = savedStateHandle.getRequiredString(AppRoutes.Project.ARG_PROJECT_ID)

    /**
     * UI 상태 (내부 Mutable 버전)
     */
    private val _uiState = MutableStateFlow(MemberListUiState(isLoading = true, projectId = projectId))
    
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
     * 멤버 목록 실시간 관찰 및 검색 필터링 함수
     */
    private fun observeMembers() {
        viewModelScope.launch {
            uiState.map { it.searchQuery }.distinctUntilChanged()
                .combine(observeProjectMembersUseCase(projectId)) { query, members ->
                    members.filter { member ->
                        member.userName.contains(query, ignoreCase = true)
                    }
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
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * ★ 검색 쿼리 변경 처리 함수 추가
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
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
package com.example.feature_project.members.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.extension.getRequiredString
import com.example.core_navigation.destination.AppRoutes
import com.example.domain.model.base.Member
import com.example.domain.usecase.project.DeleteProjectMemberUseCase
import com.example.domain.usecase.project.ObserveProjectMembersUseCase
import com.example.domain.usecase.project.role.GetProjectRolesUseCase
import com.example.domain.model.ui.data.MemberUiModel // Added import
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
    val members: List<MemberUiModel> = emptyList(), // Changed
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    // val selectedMember: Member? = null, // Type will be MemberUiModel if used - Removed for now as per plan
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
    data class ShowDeleteConfirm(val member: MemberUiModel) : MemberListEvent() // Changed
    
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
    private val deleteProjectMemberUseCase: DeleteProjectMemberUseCase,
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
                .combine(observeProjectMembersUseCase(projectId)) { query, membersResult -> // membersResult is CustomResult<List<Member>>
                    Pair(query, membersResult) // Pass both to the next stage
                }
                .catch { e -> // Catch errors from observeProjectMembersUseCase or combine itself
                    _uiState.update { it.copy(error = "멤버 목록 스트림 오류: ${e.message}", members = emptyList(), isLoading = false) }
                }
                .collect { (query, membersResult) ->
                    when (membersResult) {
                        is CustomResult.Success -> {
                            val domainMembers = membersResult.data
                            val uiMembers = domainMembers.map { domainMember ->
                                MemberUiModel(
                                    userId = domainMember.userId,
                                    userName = "User ${domainMember.userId.take(4)}", // Placeholder from previous step
                                    profileImageUrl = null, // Placeholder
                                    roleNames = domainMember.roleIds.map { "Role_$it" }, // Placeholder
                                    joinedAt = domainMember.joinedAt
                                )
                            }
                            val filteredList = if (query.isBlank()) {
                                uiMembers
                            } else {
                                uiMembers.filter { it.userName.contains(query, ignoreCase = true) }
                            }
                            _uiState.update { it.copy(members = filteredList, isLoading = false, error = null) }
                        }
                        is CustomResult.Failure -> {
                            _uiState.update { it.copy(error = membersResult.error.toString(), members = emptyList(), isLoading = false) }
                        }
                        CustomResult.Initial -> {
                             //isLoading should be true if observeProjectMembersUseCase emits Loading first.
                             //If it directly emits Initial then success/failure, this might be okay.
                            _uiState.update { it.copy(isLoading = false, error = "초기 상태") }
                        }
                        CustomResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                        is CustomResult.Progress -> {
                            // Handle progress if needed
                            _uiState.update { it.copy(isLoading = true) } // Or update progress: e.g. it.copy(progress = membersResult.progress)
                        }
                    }
                }
        }
    }

    /**
     * 멤버 목록 새로고침 함수
     * 서버에서 최신 멤버 목록을 가져와 로컬 캐시를 업데이트합니다.
     */
    fun refreshMembers() {
        /** stream을 사용해서 불필요할 것으로 예상된는데 한번 살펴봐야함
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = fetchProjectMembersUseCase(projectId) // This is a suspend function call
            when (result) {
                is CustomResult.Success -> {
                    // Data is observed by observeMembersUseCase, so just turn off loading for the refresh action itself.
                    // Error state, if any, from this explicit fetch can be cleared if needed,
                    // but observeMembers should provide the source of truth for data errors.
                    // _uiState.update { it.copy(isLoading = false, error = null) } // isLoading handled by observeMembers flow
                }
                is CustomResult.Failure -> {
                    _uiState.update { it.copy(
                        error = "멤버 목록 새로고침 실패: ${result.exceptionOrNull()?.message}",
                        isLoading = false // Explicitly set isLoading false on failure of refresh action
                    ) }
                }
                CustomResult.Initial -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
                CustomResult.Loading -> {
                    // This should ideally not happen if fetchProjectMembersUseCase is a one-shot suspend fun.
                    // If it can emit Loading, keep isLoading true.
                    _uiState.update { it.copy(isLoading = true) }
                }
                is CustomResult.Progress -> {
                     _uiState.update { it.copy(isLoading = true) } // Or handle progress
                }
            }
            // isLoading state will be ultimately managed by the observeMembers flow when it processes
            // the (potentially) new data emitted as a result of this refresh trigger.
            // However, if fetch itself fails, we stop its own loading indicator.
        }
        **/
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
     * @param member 클릭한 멤버 객체 (UI 모델)
     */
    fun onMemberClick(member: MemberUiModel) { // Changed
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
    fun requestDeleteMember(member: MemberUiModel) { // Changed
        viewModelScope.launch {
            _eventFlow.emit(MemberListEvent.ShowDeleteConfirm(member))
        }
    }

    /**
     * ★ 멤버 삭제 확정 처리 함수 추가
     */
    fun confirmDeleteMember(member: MemberUiModel) { // Changed
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = deleteProjectMemberUseCase(projectId, member.userId)
            when (result){
                is CustomResult.Success -> {
                    _eventFlow.emit(MemberListEvent.ShowSnackbar("${member.userName}님을 내보냈습니다.")) // Used MemberUiModel.userName
                }
                is CustomResult.Failure -> {
                    _eventFlow.emit(MemberListEvent.ShowSnackbar("멤버 내보내기 실패: ${result.error}"))
                }
                else -> {
                    _eventFlow.emit(MemberListEvent.ShowSnackbar("멤버 내보내기 실패: 알 수 없는 오류"))
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
} 
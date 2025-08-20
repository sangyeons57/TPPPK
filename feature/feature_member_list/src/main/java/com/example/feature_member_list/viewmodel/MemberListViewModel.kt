package com.example.feature_member_list.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.base.Role
import com.example.domain.model.ui.data.MemberUiModel
import com.example.domain.vo.DocumentId
import com.example.domain.vo.Name
import com.example.domain.vo.UserId
import com.example.domain.vo.user.UserName
import com.example.domain_usecase.provider.auth.AuthSessionUseCaseProvider
import com.example.domain_usecase.provider.project.ProjectMemberUseCaseProvider
import com.example.domain_usecase.provider.project.ProjectRoleUseCaseProvider
import com.example.domain_usecase.provider.user.UserUseCaseProvider
import com.example.feature_member_list.service.MemberImagePreloader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class MemberListUiState(
    val members: List<MemberUiModel> = emptyList(), // Changed
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    // val selectedMember: Member? = null, // Type will be MemberUiModel if used - Removed for now as per plan
    val projectId: DocumentId = DocumentId.EMPTY,
    val currentUserId: UserId? = null // 현재 로그인한 사용자 ID 추가 👈
)

/**
 * 멤버 목록 화면에서 발생하는 이벤트 정의
 */
sealed class MemberListEvent {
    /**
     * 멤버 삭제 확인 다이얼로그 표시 이벤트
     */
    data class ShowDeleteConfirm(val member: MemberUiModel) : MemberListEvent()
    
    /**
     * 스낵바 메시지 표시 이벤트
     */
    data class ShowSnackbar(val message: String) : MemberListEvent()

    /**
     * 멤버 추가 다이얼로그 표시 이벤트
     */
    object ShowAddMemberDialog : MemberListEvent()
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
    private val projectMemberUseCaseProvider: ProjectMemberUseCaseProvider,
    private val userUseCaseProvider: UserUseCaseProvider,
    private val projectRoleUseCaseProvider: ProjectRoleUseCaseProvider,
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider,
    private val navigationManger: NavigationManger,
    private val memberImagePreloader: MemberImagePreloader
) : ViewModel() {

    private val projectId: DocumentId =
        savedStateHandle.getRequiredString(RouteArgs.PROJECT_ID)
            .let { DocumentId.from(it) }
    
    // Provider를 통해 생성된 UseCase 그룹
    private val projectMemberUseCases = projectMemberUseCaseProvider.createForProject(projectId)
    private val userUseCases = userUseCaseProvider.createForUser()
    private val projectRoleUseCases = projectRoleUseCaseProvider.createForProject(projectId)
    private val authSessionUseCases = authSessionUseCaseProvider.create()

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
        loadCurrentUser()
        observeMembers()
    }

    fun navigateBack() {
        navigationManger.navigateBack()
    }

    /**
     * 현재 로그인한 사용자 정보를 로드합니다.
     */
    private fun loadCurrentUser() {
        viewModelScope.launch {
            when (val sessionResult = authSessionUseCases.checkSessionUseCase()) {
                is CustomResult.Success -> {
                    val currentUserId = sessionResult.data.userId
                    _uiState.update { it.copy(currentUserId = currentUserId) }
                }
                is CustomResult.Failure -> {
                    // 세션 정보를 가져올 수 없음 - 로그인 상태가 아님
                    _uiState.update { it.copy(error = "사용자 인증 정보를 확인할 수 없습니다.") }
                }
                else -> {
                    // Initial, Loading, Progress 상태는 무시
                }
            }
        }
    }

    /**
     * 멤버 목록 실시간 관찰 및 검색 필터링 함수
     */
    private fun observeMembers() {
        viewModelScope.launch {
            uiState.map { it.searchQuery }.distinctUntilChanged()
                .combine(projectMemberUseCases.observeProjectMembersUseCase()) { query, membersResult -> // membersResult is CustomResult<List<Member>>
                    membersResult.onSuccess { members ->
                        Log.d("MemberListViewModel", "Loaded members: ${members.size}")
                    }
                    Pair(query, membersResult) // Pass both to the next stage
                }
                .catch { e -> // Catch errors from observeProjectMembersUseCase or combine itself
                    _uiState.update { it.copy(error = "멤버 목록 스트림 오류: ${e.message}", members = emptyList(), isLoading = false) }
                }
                .collect { (query, membersResult) ->
                    when (membersResult) {
                        is CustomResult.Success -> {
                            val domainMembers = membersResult.data
                            Log.d("MemberListViewModel", "Loaded members: $domainMembers")
                            
                            // 각 멤버에 대해 사용자 정보와 역할 정보를 비동기로 가져오기
                            val uiMembers = domainMembers.map { domainMember ->
                                async {
                                    try {
                                        // 사용자 정보 가져오기 - UseCase 사용
                                        val userResult = userUseCases.getUserByIdUseCase(domainMember.id)
                                        val userName = when (userResult) {
                                            is CustomResult.Success -> {
                                                userResult.data.name
                                            }
                                            else -> UserName("사용자 ${domainMember.id.value.take(4)}") // 백업값
                                        }

                                        // 역할 정보들 가져오기
                                        val roleNames = domainMember.roleIds.map { roleId ->
                                            val roleResult = projectRoleUseCases.getProjectRoleUseCase(roleId.value)
                                            when (roleResult) {
                                                is CustomResult.Success -> roleResult.data.name
                                                else -> Name("역할 ${roleId.value.take(4)}") // 백업값
                                            }
                                        }

                                        MemberUiModel(
                                            userId = UserId.from(domainMember.id),
                                            userName = userName,
                                            roleNames = roleNames,
                                            joinedAt = domainMember.createdAt
                                        )
                                    } catch (e: Exception) {
                                        // 에러 발생 시 백업 데이터 사용
                                        MemberUiModel(
                                            userId = UserId.from(domainMember.id),
                                            userName = UserName("사용자 ${domainMember.id.value.take(4)}"),
                                            roleNames = listOf(Name("역할 로딩 실패")),
                                            joinedAt = domainMember.createdAt
                                        )
                                    }
                                }
                            }.awaitAll() // 모든 비동기 작업 완료 대기

                            // 🔄 멤버 정렬: 시스템 역할(OWNER)을 가진 멤버를 맨 위로 우선 정렬
                            val sortedMembers = uiMembers.sortedWith(compareBy<MemberUiModel> { member ->
                                // 시스템 역할을 가진 멤버인지 확인 (OWNER 등)
                                val hasSystemRole = member.roleNames.any { roleName ->
                                    Role.isSystemRole(roleName.value)
                                }
                                !hasSystemRole // false가 먼저 오므로 시스템 역할이 위로
                            }.thenBy { member ->
                                // 두 번째 정렬 기준: 이름 순
                                member.userName.value.lowercase()
                            })
                            
                            val filteredList = if (query.isBlank()) {
                                sortedMembers
                            } else {
                                sortedMembers.filter { it.userName.value.contains(query, ignoreCase = true) }
                            }
                            _uiState.update { it.copy(members = filteredList, isLoading = false, error = null) }

                            // 멤버 목록이 업데이트되면 프로필 이미지 프리로딩 시작
                            memberImagePreloader.preloadMemberImages(filteredList, viewModelScope)
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
     * ★ 검색 쿼리 변경 처리 함수 추가
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * 멤버 클릭 이벤트 처리 함수
     * 멤버 편집 화면으로 이동합니다.
     *
     * @param member 클릭한 멤버 객체 (UI 모델)
     */
    fun onMemberClick(member: MemberUiModel) {
        navigationManger.navigateToEditMember(projectId.value, member.userId.value)
    }

    /**
     * 멤버 추가 버튼 클릭 이벤트 처리 함수
     * 멤버 추가 다이얼로그를 표시하는 이벤트를 발생시킵니다.
     */
    fun onAddMemberClick() {
        viewModelScope.launch {
            _eventFlow.emit(MemberListEvent.ShowAddMemberDialog)
        }
    }

    /**
     * ★ 멤버 삭제 요청 처리 함수 추가
     */
    fun requestDeleteMember(member: MemberUiModel) { // Changed
        viewModelScope.launch {
            // 🚨 자기 자신 삭제 방지 체크
            val currentUserId = _uiState.value.currentUserId
            if (currentUserId != null && currentUserId.value == member.userId.value) {
                _eventFlow.emit(MemberListEvent.ShowSnackbar("자기 자신은 내보낼 수 없습니다. 프로젝트 나가기 기능을 사용해주세요."))
                return@launch
            }

            _eventFlow.emit(MemberListEvent.ShowDeleteConfirm(member))
        }
    }

    /**
     * ★ 멤버 삭제 확정 처리 함수 추가
     */
    fun confirmDeleteMember(member: MemberUiModel) { // Changed
        viewModelScope.launch {
            // 🚨 안전장치: 자기 자신 삭제 재확인
            val currentUserId = _uiState.value.currentUserId
            if (currentUserId != null && currentUserId.value == member.userId.value) {
                _eventFlow.emit(MemberListEvent.ShowSnackbar("자기 자신은 내보낼 수 없습니다."))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            val result = projectMemberUseCases.removeMemberUseCase(projectId, member.userId)
            when (result){
                is CustomResult.Success -> {
                    _eventFlow.emit(MemberListEvent.ShowSnackbar("${member.userName.value}님을 내보냈습니다.")) // Used MemberUiModel.userName
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

    /**
     * 프로젝트 나가기 처리 함수
     */
    fun leaveProject() {
        viewModelScope.launch {
            val currentUserId = _uiState.value.currentUserId
            if (currentUserId == null) {
                _eventFlow.emit(MemberListEvent.ShowSnackbar("사용자 정보를 확인할 수 없습니다."))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            val result = projectMemberUseCases.leaveProjectUseCase(projectId)
            when (result) {
                is CustomResult.Success -> {
                    _eventFlow.emit(MemberListEvent.ShowSnackbar("프로젝트에서 나갔습니다."))
                    navigationManger.navigateBack()
                }
                is CustomResult.Failure -> {
                    _eventFlow.emit(MemberListEvent.ShowSnackbar("프로젝트 나가기 실패: ${result.error}"))
                }
                else -> {
                    _eventFlow.emit(MemberListEvent.ShowSnackbar("프로젝트 나가기 실패: 알 수 없는 오류"))
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * 소유권 전달 처리 함수
     */
    fun transferOwnership(newOwnerId: String) {
        viewModelScope.launch {
            val currentUserId = _uiState.value.currentUserId
            if (currentUserId == null) {
                _eventFlow.emit(MemberListEvent.ShowSnackbar("사용자 정보를 확인할 수 없습니다."))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            val result = projectMemberUseCases.transferOwnershipUseCase(projectId, newOwnerId)
            when (result) {
                is CustomResult.Success -> {
                    _eventFlow.emit(MemberListEvent.ShowSnackbar("소유권이 전달되었습니다."))
                }
                is CustomResult.Failure -> {
                    _eventFlow.emit(MemberListEvent.ShowSnackbar("소유권 전달 실패: ${result.error}"))
                }
                else -> {
                    _eventFlow.emit(MemberListEvent.ShowSnackbar("소유권 전달 실패: 알 수 없는 오류"))
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * 현재 사용자가 OWNER인지 확인하는 함수
     */
    fun isCurrentUserOwner(): Boolean {
        val currentUserId = _uiState.value.currentUserId
        return currentUserId != null && _uiState.value.members.any { member ->
            member.userId.value == currentUserId.value && 
            member.roleNames.any { it.value == "OWNER" }
        }
    }

    /**
     * 현재 사용자가 자기 자신인지 확인하는 함수
     */
    fun isCurrentUser(userId: String): Boolean {
        val currentUserId = _uiState.value.currentUserId
        return currentUserId != null && currentUserId.value == userId
    }

    /**
     * 멤버를 차단하는 함수
     */
    fun blockMember(member: MemberUiModel) {
        viewModelScope.launch {
            // 🚨 자기 자신 차단 방지 체크
            val currentUserId = _uiState.value.currentUserId
            if (currentUserId != null && currentUserId.value == member.userId.value) {
                _eventFlow.emit(MemberListEvent.ShowSnackbar("자기 자신은 차단할 수 없습니다."))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            val result =
                projectMemberUseCases.blockMemberUseCase.blockMember(projectId, member.userId.value)
            when (result) {
                is CustomResult.Success -> {
                    _eventFlow.emit(MemberListEvent.ShowSnackbar("${member.userName.value}님을 영구 차단했습니다."))
                }

                is CustomResult.Failure -> {
                    _eventFlow.emit(MemberListEvent.ShowSnackbar("멤버 차단 실패: ${result.error}"))
                }

                else -> {
                    _eventFlow.emit(MemberListEvent.ShowSnackbar("멤버 차단 실패: 알 수 없는 오류"))
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

} 

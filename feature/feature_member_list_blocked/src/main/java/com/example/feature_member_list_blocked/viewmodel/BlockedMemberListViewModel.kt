package com.example.feature_member_list_blocked.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.ui.data.MemberUiModel
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.user.UserName
import com.example.domain_usecase.provider.project.ProjectMemberUseCaseProvider
import com.example.domain_usecase.provider.user.UserUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 차단된 멤버 목록 화면의 ViewModel
 */
@HiltViewModel
class BlockedMemberListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectMemberUseCaseProvider: ProjectMemberUseCaseProvider,
    private val userUseCaseProvider: UserUseCaseProvider,
    private val navigationManger: NavigationManger,
) : ViewModel() {

    private val projectId: DocumentId =
        savedStateHandle.getRequiredString("projectId").let(DocumentId::from)

    private val projectMemberUseCases = projectMemberUseCaseProvider.createForProject(projectId)
    private val userUseCases = userUseCaseProvider.createForUser()

    private val _uiState = MutableStateFlow(BlockedMemberListUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<BlockedMemberListEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        observeBlockedMembers()
    }

    /**
     * 차단된 멤버 목록 실시간 관찰 및 검색 필터링
     */
    private fun observeBlockedMembers() {
        viewModelScope.launch {
            uiState.map { it.searchQuery }.distinctUntilChanged()
                .combine(projectMemberUseCases.observeBlockedProjectMembersUseCase(projectId)) { query, membersResult ->
                    Pair(query, membersResult)
                }
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            error = "차단된 멤버 목록 스트림 오류: ${e.message}",
                            blockedMembers = emptyList(),
                            isLoading = false
                        )
                    }
                }
                .collect { (_, membersResult) ->
                    when (membersResult) {
                        is CustomResult.Success -> {
                            val domainMembers = membersResult.data
                            Log.d(
                                "BlockedMemberListViewModel",
                                "Loaded blocked members: ${domainMembers.size}"
                            )

                            // 각 멤버에 대해 사용자 정보를 가져와서 MemberUiModel로 변환
                            val memberUiModels = domainMembers.mapNotNull { member ->
                                when (val userResult = userUseCases.getUserByIdUseCase(member.id)) {
                                    is CustomResult.Success -> {
                                        MemberUiModel(
                                            userId = UserId.from(member.id.value),
                                            userName = UserName(userResult.data.name.value),
                                            roleNames = emptyList(),
                                            joinedAt = member.createdAt,
                                        )
                                    }

                                    else -> {
                                        Log.w(
                                            "BlockedMemberListViewModel",
                                            "Failed to load user for member: ${member.id}"
                                        )
                                        null
                                    }
                                }
                            }

                            _uiState.update {
                                it.copy(
                                    blockedMembers = memberUiModels,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }

                        is CustomResult.Failure -> {
                            Log.e(
                                "BlockedMemberListViewModel",
                                "Failed to load blocked members: ${membersResult.error}"
                            )
                            _uiState.update {
                                it.copy(
                                    error = "차단된 멤버 목록을 불러오는데 실패했습니다: ${membersResult.error.message}",
                                    blockedMembers = emptyList(),
                                    isLoading = false
                                )
                            }
                        }

                        else -> {
                            // Loading 상태는 유지
                        }
                    }
                }
        }
    }

    /**
     * 멤버 차단 해제
     * 권한은 이미 프로젝트 세팅 화면 진입 시점에서 확인되었으므로 별도 확인하지 않음
     */
    fun unblockMember(member: MemberUiModel) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 차단 해제 실행
            when (val result = projectMemberUseCases.unblockMemberUseCase.unblockMember(
                projectId,
                member.userId.value
            )) {
                is CustomResult.Success -> {
                    _eventFlow.emit(BlockedMemberListEvent.ShowSnackbar("${member.userName.value}님의 차단을 해제했습니다"))
                    _uiState.update { it.copy(isLoading = false) }
                }

                is CustomResult.Failure -> {
                    Log.e("BlockedMemberListViewModel", "Failed to unblock member: ${result.error}")
                    _eventFlow.emit(BlockedMemberListEvent.ShowSnackbar("차단 해제에 실패했습니다: ${result.error}"))
                    _uiState.update { it.copy(isLoading = false) }
                }

                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    /**
     * 검색어 변경
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * 멤버 클릭 (프로필 보기 등)
     */
    fun onMemberClick(member: MemberUiModel) {
        // TODO: Navigate to member profile or do nothing for blocked members
        Log.d("BlockedMemberListViewModel", "Member clicked: ${member.userName.value}")
    }


    /**
     * 뒤로 가기
     */
    fun navigateBack() {
        navigationManger.navigateBack()
    }
}

/**
 * 차단된 멤버 목록 화면의 UI 상태
 */
data class BlockedMemberListUiState(
    val blockedMembers: List<MemberUiModel> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val filteredMembers: List<MemberUiModel>
        get() = if (searchQuery.isBlank()) {
            blockedMembers
        } else {
            blockedMembers.filter { member ->
                member.userName.value.contains(searchQuery, ignoreCase = true)
            }
        }
}

/**
 * 차단된 멤버 목록 화면의 이벤트
 */
sealed class BlockedMemberListEvent {
    data class ShowSnackbar(val message: String) : BlockedMemberListEvent()
}
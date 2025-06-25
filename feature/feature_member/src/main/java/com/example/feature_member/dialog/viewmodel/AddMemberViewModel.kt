package com.example.feature_member.dialog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.user.UserName
import com.example.domain.provider.project.ProjectMemberUseCaseProvider
import com.example.domain.provider.user.UserUseCaseProvider
import com.example.feature_member.dialog.ui.UserSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddMemberDialogUiState(
    val username: UserName = UserName.EMPTY,
    val searchResults: UserSearchResult? = null,
    val selectedUsers: Set<UserId> = emptySet(), // Set of user IDs
    val isLoading: Boolean = false,
    val error: String? = null,
    val addSuccess: Boolean = false
)

sealed class AddMemberDialogEvent {
    data class ShowSnackbar(val message: String) : AddMemberDialogEvent()
    object DismissDialog : AddMemberDialogEvent() // To close dialog on success/error
    object MembersAddedSuccessfully : AddMemberDialogEvent() // To notify parent
}

@HiltViewModel
class AddMemberViewModel @Inject constructor(
    private val projectMemberUseCaseProvider: ProjectMemberUseCaseProvider,
    private val userUseCaseProvider: UserUseCaseProvider
) : ViewModel() {

    // Provider를 통해 생성된 UseCase 그룹
    private val userUseCases = userUseCaseProvider.createForUser()
    private var projectMemberUseCases: com.example.domain.provider.project.ProjectMemberUseCases? =
        null

    private val _uiState = MutableStateFlow(AddMemberDialogUiState())
    val uiState: StateFlow<AddMemberDialogUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddMemberDialogEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var searchJob: Job? = null

    fun onSearchQueryChanged(name: UserName) {
        _uiState.update { it.copy(username = name, error = null) }
        searchJob?.cancel() // Cancel previous search
        if (name.length < 2) { // Minimum query length
            _uiState.update { it.copy(searchResults = null, isLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            _uiState.update { it.copy(isLoading = true) }
            userUseCases.searchUserByNameUseCase(name).collect { userResult ->
                when(userResult) {
                    is CustomResult.Success -> {
                        val users = userResult.data
                        val uiResults = users.let { user -> // Map Domain User to UserSearchResult
                            UserSearchResult(
                                userId = UserId.from(user.id), // Adjust field names based on actual User model
                                userName = user.name,
                                userEmail = user.email.value,
                                profileImageUrl = user.profileImageUrl?.value
                            )
                        }
                        _uiState.update { it.copy(searchResults = uiResults, isLoading = false) }
                    }

                    is CustomResult.Failure ->{
                        _uiState.update { it.copy(isLoading = false, error = "검색 실패: ${userResult.error.message}") }
                    }
                    else -> {
                        _uiState.update { it.copy(isLoading = false, error = "검색 실패: $userResult") }
                    }
                }
            }
        }
    }

    fun onUserSelectionChanged(userId: UserId, isSelected: Boolean) {
        _uiState.update { currentState ->
            val newSelectedUsers = currentState.selectedUsers.toMutableSet()
            if (isSelected) {
                newSelectedUsers.add(userId)
            } else {
                newSelectedUsers.remove(userId)
            }
            currentState.copy(selectedUsers = newSelectedUsers)
        }
    }

    fun addSelectedMembers(projectId: DocumentId, defaultRoleIds: List<DocumentId> = emptyList()) {
        val selectedUserIds = _uiState.value.selectedUsers
        if (selectedUserIds.isEmpty()) {
            viewModelScope.launch { _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("추가할 멤버를 선택해주세요.")) }
            return
        }

        // 프로젝트별 UseCase 그룹 생성
        if (projectMemberUseCases == null) {
            projectMemberUseCases = projectMemberUseCaseProvider.createForProject(projectId)
        }
        val useCases = projectMemberUseCases ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            var allSuccess = true
            var successCount = 0
            // Attempt to add all selected users
            for (userId in selectedUserIds) {
                val result = useCases.addProjectMemberUseCase(
                    userId = userId,
                    initialRoleIds = defaultRoleIds
                )
                if (result.isSuccess) {
                    successCount++
                } else if (result.isFailure){
                    allSuccess = false
                    // Optionally collect individual errors or stop on first error
                    _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("${_uiState.value.searchResults?.userName}님 추가 실패"))
                    // break // Uncomment to stop on first error
                }
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    addSuccess = allSuccess,
                    selectedUsers = emptySet(),
                    username = UserName.EMPTY
                )
            }

            if (successCount > 0) {
                _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("$successCount 명의 멤버를 추가했습니다."))
                _eventFlow.emit(AddMemberDialogEvent.MembersAddedSuccessfully)
            }
            if (!allSuccess) {
                // General error if any failed, specific errors shown above
                _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("일부 멤버 추가에 실패했습니다."))
            }
            // _eventFlow.emit(AddMemberDialogEvent.DismissDialog) // Dialog dismissal controlled by Screen
        }
    }
}

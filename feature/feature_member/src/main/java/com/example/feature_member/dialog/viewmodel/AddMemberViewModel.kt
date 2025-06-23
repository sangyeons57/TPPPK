package com.example.feature_member.dialog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.domain.usecase.project.member.AddProjectMemberUseCase
import com.example.domain.usecase.user.SearchUsersByNameUseCase
import com.example.feature_member.dialog.ui.UserSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddMemberDialogUiState(
    val searchQuery: String = "",
    val searchResults: List<UserSearchResult> = emptyList(),
    val selectedUsers: Set<String> = emptySet(), // Set of user IDs
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
    private val searchUsersByNameUseCase: SearchUsersByNameUseCase, // Or SearchUsersUseCase
    private val addProjectMemberUseCase: AddProjectMemberUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMemberDialogUiState())
    val uiState: StateFlow<AddMemberDialogUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddMemberDialogEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var searchJob: Job? = null

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, error = null) }
        searchJob?.cancel() // Cancel previous search
        if (query.length < 2) { // Minimum query length
            _uiState.update { it.copy(searchResults = emptyList(), isLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            _uiState.update { it.copy(isLoading = true) }
            searchUsersByNameUseCase(query, 10).collect{ userResult ->
                when(userResult) {
                    is CustomResult.Success -> {
                        val users = userResult.data
                        val uiResults = users.map { user -> // Map Domain User to UserSearchResult
                            UserSearchResult(
                                userId = user.uid.value, // Adjust field names based on actual User model
                                userName = user.name.value,
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

    fun onUserSelectionChanged(userId: String, isSelected: Boolean) {
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

    fun addSelectedMembers(projectId: String, defaultRoleIds: List<String> = emptyList()) {
        val selectedUserIds = _uiState.value.selectedUsers
        if (selectedUserIds.isEmpty()) {
            viewModelScope.launch { _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("추가할 멤버를 선택해주세요.")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            var allSuccess = true
            var successCount = 0
            // Attempt to add all selected users
            for (userId in selectedUserIds) {
                val result = addProjectMemberUseCase(projectId, userId, defaultRoleIds)
                if (result.isSuccess) {
                    successCount++
                } else if (result.isFailure){
                    allSuccess = false
                    // Optionally collect individual errors or stop on first error
                    _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("${_uiState.value.searchResults.find { it.userId == userId }?.userName ?: userId}님 추가 실패"))
                    // break // Uncomment to stop on first error
                }
            }
            _uiState.update { it.copy(isLoading = false, addSuccess = allSuccess, selectedUsers = emptySet(), searchQuery = "") }

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

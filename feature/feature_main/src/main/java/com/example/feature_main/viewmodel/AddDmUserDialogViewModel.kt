package com.example.feature_main.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// import com.example.domain.usecase.user.SearchUserUseCase // Placeholder
// import com.example.domain.usecase.dm.AddDmChannelUseCase // Placeholder
// import com.example.domain.common.CustomResult // Placeholder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Add DM User dialog.
 */
@HiltViewModel
class AddDmUserDialogViewModel @Inject constructor(
    // private val searchUserUseCase: SearchUserUseCase, // TODO: Inject actual use case
    // private val addDmChannelUseCase: AddDmChannelUseCase // TODO: Inject actual use case
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDmUserUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddDmUserEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username, errorMessage = null) }
    }

    fun searchUser() {
        viewModelScope.launch {
            val currentUsername = _uiState.value.username
            if (currentUsername.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Please enter a username to search.") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            // TODO: Implement user search logic with searchUserUseCase
            // For now, simulate a delay and then either success or failure
            kotlinx.coroutines.delay(1000) // Simulate network call
            
            // Example: if (searchResult is CustomResult.Success) {
            //    _uiState.update { it.copy(isLoading = false, searchResults = mapToUiModel(searchResult.data)) }
            //    // If a user is found and selected, then attempt to create DM
            //    // createDmChannelWithUser(foundUserId) 
            // } else if (searchResult is CustomResult.Failure) {
            //    _uiState.update { it.copy(isLoading = false, errorMessage = "User not found or error occurred.") }
            // }
            _uiState.update { it.copy(isLoading = false, errorMessage = "Search not implemented yet.") }
        }
    }

    // This function would be called after a user is successfully found and selected from search results
    private fun createDmChannelWithUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // val result = addDmChannelUseCase(userId) // TODO: Use actual AddDmChannelUseCase
            // when (result) {
            //     is CustomResult.Success -> {
            //         _eventFlow.emit(AddDmUserEvent.NavigateToDmChat(result.data))
            //     }
            //     is CustomResult.Failure -> {
            //         _uiState.update { it.copy(isLoading = false, errorMessage = result.exception.message ?: "Failed to create DM channel.") }
            //     }
            //     else -> { /* Handle other states if necessary */ }
            // }
             _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun dismiss() {
        viewModelScope.launch {
            _eventFlow.emit(AddDmUserEvent.DismissDialog)
        }
    }
}

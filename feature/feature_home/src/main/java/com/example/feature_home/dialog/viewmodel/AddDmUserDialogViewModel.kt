package com.example.feature_home.dialog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.user.UserName
import com.example.domain.provider.dm.DMUseCaseProvider
import com.example.domain.provider.user.UserUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.first // Import .first()
import java.util.NoSuchElementException // Import for exception handling

// UiState와 Event 정의가 별도 파일에 있다면 해당 파일을 참고해야 합니다.
// 여기서는 ViewModel 내에서 추론 가능한 구조로 가정합니다.
data class AddDmUserUiState(
    val username: UserName = UserName.EMPTY,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // val searchResults: List<User> = emptyList() // 검색 기능 구현 시 필요
)

sealed interface AddDmUserEvent {
    data class NavigateToDmChat(val channelId: DocumentId) : AddDmUserEvent
    data class ShowSnackbar(val message: String) : AddDmUserEvent
    object DismissDialog : AddDmUserEvent
}


/**
 * ViewModel for the Add DM User dialog.
 */
@HiltViewModel
class AddDmUserDialogViewModel @Inject constructor(
    private val dmUseCaseProvider: DMUseCaseProvider,
    private val userUseCaseProvider: UserUseCaseProvider
) : ViewModel() {

    // Provider를 통해 생성된 UseCase 그룹들
    private val userUseCases = userUseCaseProvider.createForUser()
    private var dmUseCases: com.example.domain.provider.dm.DMUseCases? = null

    private val _uiState = MutableStateFlow(AddDmUserUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddDmUserEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        // Initialize DM UseCases when current user is available
        viewModelScope.launch {
            userUseCases.getCurrentUserStreamUseCase().collect { result ->
                when (result) {
                    is CustomResult.Success -> {
                        dmUseCases = dmUseCaseProvider.createForUser(UserId.from(result.data.id))
                    }

                    else -> {
                        // Handle user loading or error states if needed
                    }
                }
            }
        }
    }

    fun onUsernameChange(username: UserName) {
        _uiState.update { it.copy(username = username, errorMessage = null) }
    }

    // 현재 searchUser는 바로 createDmChannelWithUser를 호출합니다.
    // 실제로는 사용자 검색 -> 선택 -> DM 생성 순서가 될 것입니다.
    // 지금은 createDmChannelWithUser가 Flow를 잘 처리하는 데 집중합니다.
    fun searchUser() {
        viewModelScope.launch {
            val currentUsername = _uiState.value.username
            if (currentUsername.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Please enter a username to search.") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, errorMessage = null) } // Set loading before calling use case
            createDmChannelWithUser(currentUsername)
        }
    }

    private fun createDmChannelWithUser(username: UserName) {
        viewModelScope.launch {
            val currentDmUseCases = dmUseCases
            if (currentDmUseCases == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "User not initialized"
                    )
                }
                return@launch
            }
            
            // Use .first to take only the first terminal result (Success or Failure)
            val result = try {
                currentDmUseCases.addDmChannelUseCase(username)
                    .first { it is CustomResult.Success || it is CustomResult.Failure }
            } catch (e: NoSuchElementException) {
                // This catch block handles the case where the Flow completes without emitting
                // an item that satisfies the .first() predicate (e.g., user not found and flow emits nothing or only Loading).
                CustomResult.Failure(Exception("User '$username' not found or no definitive response.", e))
            } catch (e: Exception) {
                // Catch any other exceptions during the Flow collection or .first() operation
                CustomResult.Failure(Exception("An error occurred while processing DM for '$username': ${e.message}", e))
            }

            // Process the single result
            when (result) {
                is CustomResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            username = UserName.EMPTY // Clear username on success
                        )
                    }
                    _eventFlow.emit(AddDmUserEvent.NavigateToDmChat(result.data))
                }
                is CustomResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message ?: "Failed to create DM channel with '$username'."
                        )
                    }
                }
                // Only Success or Failure will be the 'result' due to the .first() predicate.
                // Other CustomResult types like Loading, Initial, Progress, if emitted before a terminal state,
                // would not be passed here unless they satisfied the predicate (which they don't).
                // An else branch can handle unexpected CustomResult types if the predicate were different.
                else -> { // Should ideally not be reached if predicate is strict
                     _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "An unexpected result type was received for '$username'."
                        )
                    }
                }
            }
        }
    }
    
    fun dismiss() {
        viewModelScope.launch {
            // Optional: Reset parts of the UI state when the dialog is explicitly dismissed by the user,
            // though Hilt ViewModel scoping might mean a new instance or this state is fine on reopen.
            // _uiState.update { it.copy(username = "", errorMessage = null, isLoading = false) } 
            _eventFlow.emit(AddDmUserEvent.DismissDialog)
        }
    }
}

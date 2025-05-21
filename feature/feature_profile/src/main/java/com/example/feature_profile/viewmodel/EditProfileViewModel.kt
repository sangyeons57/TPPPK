package com.example.feature_profile.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.dispatcher.DispatcherProvider
import com.example.domain.model.User
import com.example.domain.usecase.user.GetCurrentUserUseCase
import com.example.domain.usecase.user.UpdateUserProfileParams
import com.example.domain.usecase.user.UpdateUserProfileUseCase
import com.example.domain.usecase.user.UploadProfileImageUseCase
import com.example.feature_profile.viewmodel.EditProfileEvent.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

// --- 이벤트 ---
sealed interface EditProfileEvent {
    object NavigateBack : EditProfileEvent
    object RequestImagePick : EditProfileEvent
    data class ShowSnackbar(val message: String) : EditProfileEvent
}

// --- ViewModel ---
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val uploadProfileImageUseCase: UploadProfileImageUseCase,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState(isLoading = true))
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EditProfileEvent>()
    val eventFlow: SharedFlow<EditProfileEvent> = _eventFlow.asSharedFlow()

    init {
        loadUserProfile()
    }

    /** 사용자 프로필 정보 로드 */
    private fun loadUserProfile() {
        viewModelScope.launch(dispatcherProvider.io) {
            getCurrentUserUseCase()
                .collectLatest { result ->
                    _uiState.update { currentState ->
                        result.fold(
                            onSuccess = { loadedUser ->
                                currentState.copy(
                                    user = loadedUser,
                                    isLoading = false,
                                    errorMessage = null
                                )
                            },
                            onFailure = { exception ->
                                currentState.copy(
                                    user = null,
                                    isLoading = false,
                                    errorMessage = exception.message ?: "Failed to load profile"
                                )
                            }
                        )
                    }
                    if (result.isFailure) {
                        _eventFlow.emit(ShowSnackbar("Profile load failed: ${result.exceptionOrNull()?.message}"))
                    }
                }
        }
    }

    fun onNameChanged(newName: String) {
        _uiState.update { currentState ->
            currentState.copy(user = currentState.user?.copy(name = newName))
        }
    }

    fun onProfileImageClicked() {
        viewModelScope.launch {
            _eventFlow.emit(EditProfileEvent.RequestImagePick)
        }
    }

    fun handleImageSelection(uri: Uri?) {
        if (uri == null) {
            viewModelScope.launch {
                _eventFlow.emit(EditProfileEvent.ShowSnackbar("Image selection cancelled."))
            }
            return
        }

        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.update { it.copy(isLoading = true) }
            uploadProfileImageUseCase(uri)
                .onSuccess {
                    val newImageUrl = it
                    _uiState.update { currentState ->
                        currentState.copy(
                            user = currentState.user?.copy(profileImageUrl = newImageUrl),
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    _eventFlow.emit(ShowSnackbar("Image uploaded successfully."))
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            errorMessage = it.message ?: "Image upload failed",
                            isLoading = false
                        )
                    }
                    _eventFlow.emit(ShowSnackbar("Image upload failed: ${it.message}"))
                }
        }
    }

    fun onSaveProfileClicked() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value.user?.let { currentUser ->
                if (currentUser.name.isBlank()) {
                    _eventFlow.emit(ShowSnackbar("Name cannot be empty."))
                    return@launch
                }

                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val params = UpdateUserProfileParams(
                    name = currentUser.name,
                    profileImageUrl = currentUser.profileImageUrl
                )
                updateUserProfileUseCase(params)
                    .onSuccess {
                        _uiState.update { it.copy(isLoading = false) }
                        _eventFlow.emit(ShowSnackbar("Profile updated successfully"))
                        _eventFlow.emit(EditProfileEvent.NavigateBack)
                    }
                    .onFailure { exception ->
                        _uiState.update {
                            it.copy(
                                errorMessage = exception.message ?: "Failed to update profile",
                                isLoading = false
                            )
                        }
                        _eventFlow.emit(ShowSnackbar("Failed to update profile: ${exception.message}"))
                    }
            } ?: run {
                _eventFlow.emit(EditProfileEvent.ShowSnackbar("Cannot save, user data is missing."))
            }
        }
    }

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
package com.example.feature_main.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.dispatcher.DispatcherProvider
import com.example.domain.model.Result // Import actual Result
import com.example.domain.model.User // Changed from UserProfileData to User
import com.example.domain.usecase.user.GetMyProfileUseCase // Import actual GetMyProfileUseCase
import com.example.domain.usecase.user.UpdateUserProfileParams // Import actual UpdateUserProfileParams
import com.example.domain.usecase.user.UpdateUserProfileUseCase // Import actual UpdateUserProfileUseCase
import com.example.domain.usecase.user.UploadProfileImageUseCase // Import actual UploadProfileImageUseCase
import com.example.feature_main.viewmodel.EditProfileEvent.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
// UserProfileData import is removed
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI State for EditProfileScreen
data class EditProfileUiState(
    val user: User? = null, // Changed from name and profileImageUrl fields
    val isLoading: Boolean = false,
    val errorMessage: String? = null
    // newProfileImageUriForUpload is not explicitly needed in UiState if newImageToUploadUrl is handled internally
    // and uiState.user.profileImageUrl is updated for preview.
)

// Events for EditProfileScreen
sealed interface EditProfileEvent {
    object NavigateBack : EditProfileEvent
    object RequestImagePick : EditProfileEvent // For requesting image picker
    data class ShowSnackbar(val message: String) : EditProfileEvent
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getMyProfileUseCase: GetMyProfileUseCase, // Now using actual domain use case
    private val updateUserProfileUseCase: UpdateUserProfileUseCase, // Now using actual domain use case
    private val uploadProfileImageUseCase: UploadProfileImageUseCase, // Now using actual domain use case
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EditProfileEvent>()
    val eventFlow: SharedFlow<EditProfileEvent> = _eventFlow.asSharedFlow()

    // This variable will hold the URL of a newly uploaded image that hasn't been saved with the profile yet.
    // It's used to ensure that if the user picks an image, then changes name, then saves,
    // the correct (newly uploaded) image URL is used for saving, not just what's in uiState.user.profileImageUrl
    // if uiState.user.profileImageUrl is only for *persisted* data.
    // However, the subtask implies uiState.user.profileImageUrl should be updated for preview.
    // So, this separate variable might be redundant if uiState.user.profileImageUrl is THE source of truth for the save operation.
    // Let's simplify and assume uiState.user.profileImageUrl will hold the latest URL (either fetched or from new upload).
    // private var newImageToUploadUrl: String? = null // Removed for simplification, will use uiState.user.profileImageUrl

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getMyProfileUseCase()) { // getMyProfileUseCase now returns Result<User>
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            user = result.data, // Store the whole User object
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = result.message ?: "Failed to load profile",
                            isLoading = false
                        )
                    }
                }
                is Result.Loading -> TODO()
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
            when (val result = uploadProfileImageUseCase(uri)) {
                is Result.Success -> {
                    val newImageUrl = result.data
                    _uiState.update { currentState ->
                        currentState.copy(
                            user = currentState.user?.copy(profileImageUrl = newImageUrl), // Update User object
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = result.message ?: "Image upload failed",
                            isLoading = false
                        )
                    }
                }
                is Result.Loading -> TODO()
            }
        }
    }

    fun onSaveProfileClicked() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value.user?.let { currentUser ->
                _uiState.update { it.copy(isLoading = true) }
                val params = UpdateUserProfileParams(
                    name = currentUser.name,
                    profileImageUrl = currentUser.profileImageUrl // This is now the potentially new URL
                )
                when (val result = updateUserProfileUseCase(params)) {
                    is Result.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                        // Optionally reload profile or assume local state is source of truth post-save
                        // loadUserProfile() // To get completely fresh data from server if needed
                        _eventFlow.emit(ShowSnackbar("Profile updated successfully"))
                        _eventFlow.emit(EditProfileEvent.NavigateBack)
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                errorMessage = result.message ?: "Failed to update profile",
                                isLoading = false
                            )
                        }
                    }
                    is Result.Loading -> TODO()
                }
            } ?: run {
                // Handle case where user is null, though should ideally not happen if save is enabled
                _eventFlow.emit(EditProfileEvent.ShowSnackbar("Cannot save, user data is missing."))
            }
        }
    }

    fun errorMessageShown() { // Renamed from onErrorShown to match convention
        _uiState.update { it.copy(errorMessage = null) }
    }
}

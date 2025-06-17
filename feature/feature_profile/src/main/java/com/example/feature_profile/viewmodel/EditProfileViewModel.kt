package com.example.feature_profile.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.dispatcher.DispatcherProvider
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.vo.user.UserName
import com.example.domain.usecase.user.GetCurrentUserStreamUseCase
import com.example.domain.usecase.user.UpdateUserProfileParams
import com.example.domain.usecase.user.UpdateUserProfileUseCase
import com.example.domain.usecase.user.UploadProfileImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 프로필 편집 화면의 UI 상태
 */
data class EditProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 프로필 편집 화면의 이벤트
 */
sealed interface EditProfileEvent {
    object NavigateBack : EditProfileEvent
    object RequestImagePick : EditProfileEvent
    data class ShowSnackbar(val message: String) : EditProfileEvent
}

/**
 * 프로필 편집 화면을 위한 ViewModel
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserStreamUseCase,
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

    /**
     * 사용자 프로필 정보를 로드합니다.
     */
    private fun loadUserProfile() {
        viewModelScope.launch(dispatcherProvider.io) {
            getCurrentUserUseCase().collect { result ->
                when (result) {
                    is CustomResult.Success -> {
                        val loadedUser = result.data
                        _uiState.update { currentState ->
                            currentState.copy(
                                user = loadedUser,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    }
                    is CustomResult.Failure -> {
                        val exception = result.error
                        _uiState.update { currentState ->
                            currentState.copy(
                                user = null,
                                isLoading = false,
                                errorMessage = exception.message ?: "Failed to load profile"
                            )
                        }
                        _eventFlow.emit(EditProfileEvent.ShowSnackbar("Profile load failed: ${exception.message}"))
                    }
                    else -> {
                        // 로딩 상태 등 무시
                    }
                }
            }
        }
    }

    /**
     * 사용자가 이름을 변경할 때 호출됩니다.
     */
    fun onNameChanged(newName: String) {
        uiState.value.user?.changeName(UserName(newName))
    }

    /**
     * 프로필 이미지 클릭 이벤트 처리
     */
    fun onProfileImageClicked() {
        viewModelScope.launch {
            _eventFlow.emit(EditProfileEvent.RequestImagePick)
        }
    }

    /**
     * 사용자가 선택한 이미지 처리
     */
    fun handleImageSelection(uri: Uri?) {
        if (uri == null) {
            viewModelScope.launch {
                _eventFlow.emit(EditProfileEvent.ShowSnackbar("Image selection cancelled."))
            }
            return
        }

        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.update { it.copy(isLoading = true) }
            val result = uploadProfileImageUseCase(uri)
            
            when (result) {
                is CustomResult.Success -> {
                    val user = result.data
                    _uiState.update { currentState ->
                        currentState.user?.changeProfileImage(user.profileImageUrl)
                        currentState.copy(
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    _eventFlow.emit(EditProfileEvent.ShowSnackbar("Image uploaded successfully."))
                }
                is CustomResult.Failure -> {
                    val exception = result.error
                    _uiState.update { currentState ->
                        currentState.copy(
                            errorMessage = exception.message ?: "Image upload failed",
                            isLoading = false
                        )
                    }
                    _eventFlow.emit(EditProfileEvent.ShowSnackbar("Image upload failed: ${exception.message}"))
                }
                else -> {
                    // 기타 상태 무시
                }
            }
        }
    }

    /**
     * 프로필 저장 버튼 클릭 이벤트 처리
     */
    fun onSaveProfileClicked() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value.user?.let { currentUser ->
                if (currentUser.name.value.isBlank()) {
                    _eventFlow.emit(EditProfileEvent.ShowSnackbar("Name cannot be empty."))
                    return@launch
                }

                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val params = UpdateUserProfileParams(
                    name = currentUser.name.value,
                    profileImageUrl = currentUser.profileImageUrl?.value
                )
                
                val result = updateUserProfileUseCase(params)
                when (result) {
                    is CustomResult.Success -> {
                        val updatedUser = result.data
                        _uiState.update { it.copy(
                            user = updatedUser,
                            isLoading = false
                        )}
                        _eventFlow.emit(EditProfileEvent.ShowSnackbar("Profile updated successfully"))
                        _eventFlow.emit(EditProfileEvent.NavigateBack)
                    }
                    is CustomResult.Failure -> {
                        val exception = result.error
                        _uiState.update {
                            it.copy(
                                errorMessage = exception.message ?: "Failed to update profile",
                                isLoading = false
                            )
                        }
                        _eventFlow.emit(EditProfileEvent.ShowSnackbar("Failed to update profile: ${exception.message}"))
                    }
                    else -> {
                        // 기타 상태 무시
                    }
                }
            } ?: run {
                _eventFlow.emit(EditProfileEvent.ShowSnackbar("Cannot save, user data is missing."))
            }
        }
    }
    
    /**
     * 에러 메시지가 표시된 후 호출됩니다.
     * 에러 메시지를 초기화합니다.
     */
    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
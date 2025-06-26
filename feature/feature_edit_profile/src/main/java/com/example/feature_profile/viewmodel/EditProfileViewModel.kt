package com.example.feature_profile.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.dispatcher.DispatcherProvider
import com.example.core_common.result.CustomResult
import com.example.core_navigation.core.NavigationManger
import com.example.domain.model.base.User
import com.example.domain.model.vo.user.UserName
import com.example.domain.provider.user.UserUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    object RequestImagePick : EditProfileEvent
    data class ShowSnackbar(val message: String) : EditProfileEvent
}

/**
 * 프로필 편집 화면을 위한 ViewModel
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userUseCaseProvider: UserUseCaseProvider,
    private val dispatcherProvider: DispatcherProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {

    // Provider를 통해 생성된 UseCase 그룹
    private val userUseCases = userUseCaseProvider.createForUser()

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
            userUseCases.getCurrentUserStreamUseCase().collect { result ->
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
        _uiState.update { currentState ->
            currentState.user?.let { user ->
                user.changeName(UserName(newName))
                currentState.copy(user = user)
            } ?: currentState
        }
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
            val result = userUseCases.uploadProfileImageUseCase(uri)
            
            when (result) {
                is CustomResult.Success -> {
                    // Firebase Functions가 자동으로 Firestore를 업데이트하므로
                    // getCurrentUserStreamUseCase의 실시간 리스너가 자동으로 UI를 업데이트함
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    _eventFlow.emit(EditProfileEvent.ShowSnackbar("Image uploaded successfully. Processing..."))
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
                // Update user profile via Firebase Functions
                when (val result = userUseCases.updateUserProfileUseCase(
                    name = currentUser.name.value
                )) {
                    is CustomResult.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _eventFlow.emit(EditProfileEvent.ShowSnackbar("Profile updated successfully"))
                        navigateBack()
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

    /**
     * 뒤로가기 네비게이션 처리
     */
    fun navigateBack() {
        navigationManger.navigateBack()
    }
}
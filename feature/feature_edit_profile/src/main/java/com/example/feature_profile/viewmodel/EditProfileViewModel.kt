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
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 프로필 편집 화면의 UI 상태
 */
data class EditProfileUiState(
    val user: User? = null,
    val originalUser: User? = null, // 변경사항 비교를 위한 원본 사용자 정보
    val selectedImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasChanges: Boolean = false
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
        viewModelScope.launch {
            userUseCases.getCurrentUserStreamUseCase().collect { result ->
                when (result) {
                    is CustomResult.Success -> {
                        val loadedUser = result.data
                        _uiState.update { currentState ->
                            currentState.copy(
                                user = loadedUser,
                                originalUser = loadedUser, // 원본 사용자 정보 저장
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
                val hasNameChanged = currentState.originalUser?.let { original ->
                    newName != original.name.value
                } ?: false
                val hasImageChanged = currentState.selectedImageUri != null
                currentState.copy(
                    user = user,
                    hasChanges = hasNameChanged || hasImageChanged
                )
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
     * 이미지를 즉시 업로드하지 않고 임시 저장합니다.
     */
    fun handleImageSelection(uri: Uri?) {
        if (uri == null) {
            viewModelScope.launch {
                _eventFlow.emit(EditProfileEvent.ShowSnackbar("이미지 선택이 취소되었습니다."))
            }
            return
        }

        _uiState.update { currentState ->
            val hasNameChanged = currentState.user?.let { user ->
                currentState.originalUser?.let { original ->
                    user.name.value != original.name.value
                } ?: false
            } ?: false
            
            currentState.copy(
                selectedImageUri = uri,
                hasChanges = true, // 이미지가 선택되면 변경사항이 있음
                errorMessage = null
            )
        }
        
        viewModelScope.launch {
            _eventFlow.emit(EditProfileEvent.ShowSnackbar("이미지가 선택되었습니다. 저장 버튼을 눌러 적용하세요."))
        }
    }

    /**
     * 프로필 저장 버튼 클릭 이벤트 처리
     * 이름 변경과 이미지 업로드를 동시에 처리합니다.
     */
    fun onSaveProfileClicked() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val currentUser = currentState.user
            val selectedImageUri = currentState.selectedImageUri
            
            if (currentUser == null) {
                _eventFlow.emit(EditProfileEvent.ShowSnackbar("사용자 정보를 찾을 수 없습니다."))
                return@launch
            }

            if (currentUser.name.value.isBlank()) {
                _eventFlow.emit(EditProfileEvent.ShowSnackbar("이름은 비어있을 수 없습니다."))
                return@launch
            }

            if (!currentState.hasChanges) {
                _eventFlow.emit(EditProfileEvent.ShowSnackbar("변경된 내용이 없습니다."))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // 1. 이미지 업로드 (선택된 이미지가 있는 경우)
                if (selectedImageUri != null) {
                    val imageResult = withContext(dispatcherProvider.io) {
                        userUseCases.uploadProfileImageUseCase(selectedImageUri)
                    }
                    when (imageResult) {
                        is CustomResult.Success -> {
                            _eventFlow.emit(EditProfileEvent.ShowSnackbar("이미지 업로드 완료"))
                        }
                        is CustomResult.Failure -> {
                            _uiState.update { it.copy(isLoading = false) }
                            _eventFlow.emit(EditProfileEvent.ShowSnackbar("이미지 업로드 실패: ${imageResult.error.message}"))
                            return@launch
                        }
                        else -> {
                            // 로딩 상태 등 무시
                        }
                    }
                }

                // 2. 이름 업데이트 (이름이 변경된 경우)
                val hasNameChanged = currentState.originalUser?.let { original ->
                    currentUser.name.value != original.name.value
                } ?: false

                if (hasNameChanged) {
                    val nameResult = withContext(dispatcherProvider.io) {
                        userUseCases.updateUserProfileUseCase(name = currentUser.name.value)
                    }
                    when (nameResult) {
                        is CustomResult.Success -> {
                            _eventFlow.emit(EditProfileEvent.ShowSnackbar("프로필이 성공적으로 업데이트되었습니다"))
                        }
                        is CustomResult.Failure -> {
                            _uiState.update { it.copy(isLoading = false) }
                            _eventFlow.emit(EditProfileEvent.ShowSnackbar("이름 업데이트 실패: ${nameResult.error.message}"))
                            return@launch
                        }
                        else -> {
                            // 로딩 상태 등 무시
                        }
                    }
                }

                // 성공 시 상태 초기화 및 뒤로가기
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        selectedImageUri = null,
                        hasChanges = false
                    ) 
                }
                
                if (selectedImageUri != null && hasNameChanged) {
                    _eventFlow.emit(EditProfileEvent.ShowSnackbar("이미지와 이름이 모두 업데이트되었습니다"))
                } else if (selectedImageUri != null) {
                    _eventFlow.emit(EditProfileEvent.ShowSnackbar("이미지가 업데이트되었습니다"))
                } else if (hasNameChanged) {
                    _eventFlow.emit(EditProfileEvent.ShowSnackbar("이름이 업데이트되었습니다"))
                }
                
                navigateBack()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "프로필 업데이트 중 오류가 발생했습니다",
                        isLoading = false
                    )
                }
                _eventFlow.emit(EditProfileEvent.ShowSnackbar("프로필 업데이트 실패: ${e.message}"))
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
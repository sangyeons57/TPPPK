package com.example.feature_settings.viewmodel

import android.net.Uri // 이미지 처리를 위해 Uri 사용
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.UserProfileData
import com.example.domain.usecase.user.GetUserUseCase
import com.example.domain.usecase.user.RemoveProfileImageUseCase
import com.example.domain.usecase.user.UpdateImageUseCase
// Domain UseCase Import
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI 상태 ---
data class EditProfileUiState(
    val userProfile: UserProfileData? = null, // 로드된 사용자 프로필 정보
    val selectedImageUri: Uri? = null, // 사용자가 갤러리에서 선택한 이미지 URI (업로드 전 임시 저장)
    val isLoading: Boolean = false,
    val isUploading: Boolean = false, // 이미지 업로드 중 상태
    val error: String? = null,
    val updateSuccess: Boolean = false // 프로필 업데이트 성공 플래그
)

// --- 이벤트 ---
sealed class EditProfileEvent {
    object NavigateBack : EditProfileEvent()
    data class ShowSnackbar(val message: String) : EditProfileEvent()
    object RequestImagePicker : EditProfileEvent() // 이미지 선택기 실행 요청
    object RequestProfileImageRemoveConfirm : EditProfileEvent() // 이미지 삭제 확인 요청
}

// --- ViewModel ---
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getUserProfileUseCase: GetUserUseCase,
    private val updateProfileImageUseCase: UpdateImageUseCase,
    private val removeProfileImageUseCase: RemoveProfileImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState(isLoading = true))
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EditProfileEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadUserProfile()
    }

    /** 사용자 프로필 정보 로드 */
    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            println("ViewModel: Loading user profile")
            
            // 현재 로그인된 사용자의 ID 또는 특정 ID 가져오기
            val userId = savedStateHandle.get<String>("userId") ?: "current"
            val result = getUserProfileUseCase() // ★ UseCase 호출

            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, userProfile = result.getOrThrow()) }
            } else {
                val errorMsg = "프로필 정보를 불러오지 못했습니다: ${result.exceptionOrNull()?.message}"
                _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                _eventFlow.emit(EditProfileEvent.ShowSnackbar(errorMsg))
            }
        }
    }

    /** 이미지 선택 버튼 클릭 시 */
    fun onSelectImageClick() {
        viewModelScope.launch {
            _eventFlow.emit(EditProfileEvent.RequestImagePicker)
        }
    }

    /** 이미지 선택기에서 이미지 선택 완료 시 호출 */
    fun onImagePicked(uri: Uri?) {
        if (uri != null) {
            _uiState.update { it.copy(selectedImageUri = uri) }
            // 선택된 이미지를 바로 업로드
            uploadProfileImage(uri)
        }
    }

    /** 프로필 이미지 업로드 */
    private fun uploadProfileImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null) }
            _eventFlow.emit(EditProfileEvent.ShowSnackbar("프로필 이미지 업로드 중..."))

            val result = updateProfileImageUseCase(uri) // ★ UseCase 호출

            if (result.isSuccess) {
                val newImageUrl = result.getOrThrow()
                // 성공 시 UI 상태의 userProfile 업데이트 및 selectedImageUri 초기화
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        userProfile = it.userProfile?.copy(profileImageUrl = newImageUrl), // 새 URL 반영
                        selectedImageUri = null, // 업로드 완료 후 선택 해제
                        updateSuccess = true // 성공 플래그 (선택적)
                    )
                }
                _eventFlow.emit(EditProfileEvent.ShowSnackbar("프로필 이미지가 업데이트되었습니다."))
            } else {
                val errorMsg = "이미지 업로드 실패: ${result.exceptionOrNull()?.message}"
                _uiState.update { it.copy(isUploading = false, error = errorMsg, selectedImageUri = null) } // 실패 시 선택 해제
                _eventFlow.emit(EditProfileEvent.ShowSnackbar(errorMsg))
            }
        }
    }

    /** 프로필 이미지 제거 버튼 클릭 시 */
    fun onRemoveImageClick() {
        // 이미지가 있을 때만 제거 확인 요청
        if (uiState.value.userProfile?.profileImageUrl != null) {
            viewModelScope.launch {
                _eventFlow.emit(EditProfileEvent.RequestProfileImageRemoveConfirm)
            }
        } else {
            viewModelScope.launch {
                _eventFlow.emit(EditProfileEvent.ShowSnackbar("제거할 프로필 이미지가 없습니다."))
            }
        }
    }

    /** 프로필 이미지 제거 확인 시 호출 */
    fun confirmRemoveProfileImage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null) } // 로딩 상태 사용
            _eventFlow.emit(EditProfileEvent.ShowSnackbar("프로필 이미지 제거 중..."))

            val result = removeProfileImageUseCase() // ★ UseCase 호출

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        userProfile = it.userProfile?.copy(profileImageUrl = null), // 이미지 URL 제거
                        updateSuccess = true
                    )
                }
                _eventFlow.emit(EditProfileEvent.ShowSnackbar("프로필 이미지가 제거되었습니다."))
            } else {
                val errorMsg = "이미지 제거 실패: ${result.exceptionOrNull()?.message}"
                _uiState.update { it.copy(isUploading = false, error = errorMsg) }
                _eventFlow.emit(EditProfileEvent.ShowSnackbar(errorMsg))
            }
        }
    }
}
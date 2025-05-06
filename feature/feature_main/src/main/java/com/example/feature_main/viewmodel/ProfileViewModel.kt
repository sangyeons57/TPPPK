package com.example.feature_main.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.UserProfileData
import com.example.domain.usecase.user.GetUserProfileUseCase
import com.example.domain.usecase.auth.LogoutUseCase
import com.example.domain.usecase.user.UpdateUserProfileImageUseCase
import com.example.domain.usecase.user.UpdateUserStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


// 프로필 화면 UI 상태
data class ProfileUiState(
    val isLoading: Boolean = false,
    val userProfile: UserProfileData? = null, // 사용자 프로필 데이터 (null 가능)
    val errorMessage: String? = null
)

// 프로필 화면 이벤트
sealed class ProfileEvent {
    object NavigateToSettings : ProfileEvent() // 설정 화면으로 이동
    object NavigateToStatus: ProfileEvent()
    object NavigateToFriends: ProfileEvent()
    object ShowEditStatusDialog : ProfileEvent() // 상태 메시지 변경 다이얼로그 표시
    object PickProfileImage : ProfileEvent() // 이미지 선택기 실행 요청
    object LogoutCompleted : ProfileEvent() // 로그아웃 완료 알림 -> 화면 전환용
    data class ShowSnackbar(val message: String) : ProfileEvent()
}


@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val updateUserStatusUseCase: UpdateUserStatusUseCase,
    private val updateUserProfileImageUseCase: UpdateUserProfileImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true)) // 초기 로딩 상태
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ProfileEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadUserProfile()
    }

    // 프로필 정보 로드
    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            println("ViewModel: 사용자 프로필 로드 시도 (UseCase 사용)")

            // --- UseCase 호출 ---
            val profileResult: Result<UserProfileData> = getUserProfileUseCase() // Call UseCase

            profileResult.fold(
                onSuccess = { userProfileData ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userProfile = userProfileData,
                            errorMessage = null
                        )
                    }
                    println("ViewModel: 프로필 로드 성공 - ${userProfileData.name}")
                },
                onFailure = { exception ->
                    println("ViewModel: 프로필 로드 실패 - ${exception.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userProfile = null, // Or keep existing value based on policy
                            errorMessage = "프로필을 불러오는데 실패했습니다."
                        )
                    }
                    // SentryUtil.captureException(exception, "Failed to load user profile") // Error logging
                }
            )
            // --------------------------
        }
    }

    // 프로필 이미지 변경 버튼 클릭
    fun onEditProfileImageClick() {
        viewModelScope.launch {
            _eventFlow.emit(ProfileEvent.PickProfileImage)
        }
    }

    // 상태 메시지 변경 버튼 클릭
    fun onEditStatusClick() {
        viewModelScope.launch {
            _eventFlow.emit(ProfileEvent.ShowEditStatusDialog)
        }
    }

    // 설정 버튼 클릭
    fun onStatusClick() {
        viewModelScope.launch {
            _eventFlow.emit(ProfileEvent.NavigateToStatus)
        }
    }
    // 설정 버튼 클릭
    fun onFriendsClick() {
        viewModelScope.launch {
            _eventFlow.emit(ProfileEvent.NavigateToFriends)
        }
    }
    // 설정 버튼 클릭
    fun onSettingsClick() {
        viewModelScope.launch {
            _eventFlow.emit(ProfileEvent.NavigateToSettings)
        }
    }

    // 로그아웃 버튼 클릭
    fun onLogoutClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            println("ViewModel: 로그아웃 시도 (UseCase 사용)")
            // val result = authRepository.logout() // Remove direct repository call
            val result = logoutUseCase() // Call UseCase
            result.onSuccess {
                _eventFlow.emit(ProfileEvent.LogoutCompleted)
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
                _eventFlow.emit(ProfileEvent.ShowSnackbar("로그아웃 실패"))
            }
        }
    }

    // --- 상태 메시지 변경, 프로필 이미지 변경 처리 함수 ---
    fun changeStatusMessage(newStatus: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            println("ViewModel: 상태 메시지 변경 시도 (UseCase 사용) - $newStatus")
            // val result = userRepository.updateStatusMessage(newStatus) // Remove direct repository call
            val result = updateUserStatusUseCase(newStatus) // Call UseCase
            result.onSuccess {
                // UseCase 성공 시, 프로필을 다시 로드하여 최신 상태 반영
                loadUserProfile()
                _eventFlow.emit(ProfileEvent.ShowSnackbar("상태 메시지 변경됨"))
            }.onFailure { exception ->
                 _uiState.update { it.copy(isLoading = false) }
                _eventFlow.emit(ProfileEvent.ShowSnackbar("상태 메시지 변경 실패: ${exception.message}"))
            }
            // delay(300) // Remove temporary delay
            // _uiState.update { it.copy(isLoading = false, userProfile = it.userProfile?.copy(statusMessage = newStatus)) } // Remove temporary UI update
            // _eventFlow.emit(ProfileEvent.ShowSnackbar("상태 메시지 변경됨 (임시)")) // Remove temporary snackbar
        }
    }

    fun changeProfileImage(imageUri: Uri?) {
        if (imageUri == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            println("ViewModel: 프로필 이미지 변경 시도 (UseCase 사용) - $imageUri")
            // --- UseCase 호출 ---
            // val result = userRepository.updateProfileImage(imageUri) // Remove direct repository call
            val result = updateUserProfileImageUseCase(imageUri) // Call UseCase
            result.onSuccess { newImageUrl ->
                // UseCase 성공 시, 프로필을 다시 로드하여 최신 상태 반영
                loadUserProfile()
                _eventFlow.emit(ProfileEvent.ShowSnackbar("프로필 이미지 변경됨"))
            }.onFailure { exception ->
                 _uiState.update { it.copy(isLoading = false) }
                _eventFlow.emit(ProfileEvent.ShowSnackbar("프로필 이미지 변경 실패: ${exception.message}"))
            }
            // ----------------------------------------------
            // delay(1000) // Remove temporary delay
            // val newImageUrl = "https://picsum.photos/seed/${System.currentTimeMillis()}/100" // Remove temporary URL generation
            // _uiState.update { it.copy(isLoading = false, userProfile = it.userProfile?.copy(profileImageUrl = newImageUrl)) } // Remove temporary UI update
            // _eventFlow.emit(ProfileEvent.ShowSnackbar("프로필 이미지 변경됨 (임시)")) // Remove temporary snackbar
        }
    }
    // -----------------------------------------------------

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * User(Domain Model) -> UserProfileData(UI Model) 변환 확장 함수
     * (이 함수는 UseCase로 이동되었으므로 ViewModel에서 제거합니다)
     */
    // private fun User.toUserProfileData(): UserProfileData { ... } // Remove this function
}
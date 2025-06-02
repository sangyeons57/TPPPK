package com.example.feature_main.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.enum.UserStatus
import com.example.domain.usecase.user.GetCurrentUserStreamUseCase
import com.example.domain.usecase.user.UpdateUserImageUseCase
import com.example.domain.usecase.user.UpdateUserStatusUseCase
import com.example.domain.usecase.auth.LogoutUseCase
import com.example.domain.usecase.user.ChangeMemoUseCase // Placeholder
import com.example.feature_main.ui.UserProfileData // Added
import com.example.feature_main.ui.toUserProfileData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


// 프로필 화면 UI 상태
data class ProfileUiState(
    val isLoading: Boolean = false,
    val userProfile: UserProfileData? = null, // 사용자 프로필 데이터 (null 가능)
    val errorMessage: String? = null,
    val showChangeStatusDialog: Boolean = false // Dialog visibility state
)

// 프로필 화면 이벤트
sealed class ProfileEvent {
    object NavigateToSettings : ProfileEvent() // 설정 화면으로 이동
    // NavigateToStatus is now handled by showChangeStatusDialog in UiState
    object NavigateToFriends: ProfileEvent()
    object NavigateToEditProfile : ProfileEvent() // 프로필 수정 화면으로 이동
    object PickProfileImage : ProfileEvent() // 이미지 선택기 실행 요청
    object LogoutCompleted : ProfileEvent() // 로그아웃 완료 알림 -> 화면 전환용
    data class ShowSnackbar(val message: String) : ProfileEvent()
}


@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserStreamUseCase: GetCurrentUserStreamUseCase,
    private val updateUserStatusUseCase: UpdateUserStatusUseCase,
    private val updateUserProfileImageUseCase: UpdateUserImageUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val changeMemoUseCase: ChangeMemoUseCase // Placeholder
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true)) // 초기 로딩 상태
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ProfileEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadUserProfile()
    }

    // 프로필 수정 버튼 클릭
    fun onEditProfileClicked() {
        viewModelScope.launch {
            _eventFlow.emit(ProfileEvent.NavigateToEditProfile)
        }
    }

    // 프로필 정보 로드
    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            println("ViewModel: 사용자 프로필 로드 시도 (UseCase 사용)")

            // --- UseCase 호출 ---
            getCurrentUserStreamUseCase()
                .catch { exception ->
                    val errorMsg = "프로필 정보를 불러오지 못했습니다: ${exception.message}"
                    _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                    _eventFlow.emit(ProfileEvent.ShowSnackbar(errorMsg))
                    println("ViewModel: 프로필 로드 중 예외 발생 - ${exception.message}")
                }
                .collectLatest { customResult: CustomResult<User, Exception> ->
                    when (customResult) {
                        is CustomResult.Success -> {
                            val user = customResult.data
                            _uiState.update { it.copy(isLoading = false, userProfile = user.toUserProfileData()) }
                            println("ViewModel: 프로필 로드 성공 - ${user.name}")
                        }
                        is CustomResult.Failure -> {
                            val errorMsg = "프로필 로드 실패: ${customResult.error.message}"
                            _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                            _eventFlow.emit(ProfileEvent.ShowSnackbar(errorMsg))
                            println("ViewModel: 프로필 로드 실패 - $errorMsg")
                        }
                        else -> {
                            val errorMsg = "프로필 로드 실패: Unknown"
                            _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                            _eventFlow.emit(ProfileEvent.ShowSnackbar(errorMsg))
                            println("ViewModel: 프로필 로드 실패 - $errorMsg")
                        }
                    }
                }
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
        // No longer emits ShowEditStatusDialog. 
        // This function can be used if any specific ViewModel logic is needed when editing starts,
        // but for now, it might not be strictly necessary as ProfileScreen will handle the edit state.
        // Keeping it for now in case future logic needs it.
        viewModelScope.launch {
            // Placeholder for any future logic if needed when status editing begins.
        }
    }

    // "상태 표시" 메뉴 아이템 클릭 시 (기존 onStatusClick)
    fun onChangeStatusClick() { // Renamed for clarity
        _uiState.update { it.copy(showChangeStatusDialog = true) }
    }

    fun onDismissChangeStatusDialog() {
        _uiState.update { it.copy(showChangeStatusDialog = false) }
    }

    fun onChangeStatusSuccess(statusName: String) {
        // Dismiss the dialog first
        _uiState.update { it.copy(showChangeStatusDialog = false) }
        // Then show snackbar and reload profile
        viewModelScope.launch {
            _eventFlow.emit(ProfileEvent.ShowSnackbar("상태가 '$statusName'(으)로 변경되었습니다."))
            loadUserProfile() // Refresh user profile to show updated status
        }
    }

    // 친구 목록 버튼 클릭 (기존 onFriendsClick)
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
            when (val result = logoutUseCase()) { // Assign to val for smart casting
                is CustomResult.Success -> {
                    _eventFlow.emit(ProfileEvent.LogoutCompleted)
                    // isLoading state will be managed by the screen navigating away or resetting.
                }
                is CustomResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(ProfileEvent.ShowSnackbar("로그아웃 실패: ${result.error.message}"))
                    println("ViewModel: 로그아웃 실패 - ${result.error.message}")
                }
                is CustomResult.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                    println("ViewModel: 로그아웃 로딩 중...")
                }
                is CustomResult.Initial -> {
                    _uiState.update { it.copy(isLoading = false) } // Or true if initial implies start of a process
                    println("ViewModel: 로그아웃 초기 상태 - $result")
                }
                is CustomResult.Progress -> {
                    _uiState.update { it.copy(isLoading = true) } // Keep loading true, potentially update UI with progress
                    println("ViewModel: 로그아웃 진행 중 (${result.progress}%) - $result")
                }
            }
        }
    }

    // --- 상태 메시지 변경, 프로필 이미지 변경 처리 함수 ---
    fun changeStatus(newStatus: UserStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            println("ViewModel: 상태 메시지 변경 시도 (UseCase 사용) - $newStatus")
            // This updateUserStatusUseCase is for the status *message*, not UserStatus (ONLINE/OFFLINE)
            // Ensure this is not confused with the one for UserStatus.
            val result = updateUserStatusUseCase(newStatus)
            result.onSuccess {
                viewModelScope.launch {
                    // UseCase 성공 시, 프로필을 다시 로드하여 최신 상태 반영
                    loadUserProfile() // This reloads the whole profile, including user status and status message
                    _eventFlow.emit(ProfileEvent.ShowSnackbar("상태 메시지 변경됨"))
                }
            }.onFailure { exception ->
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(ProfileEvent.ShowSnackbar("상태 메시지 변경 실패: ${exception.message}"))
                }
            }
            // delay(300) // Remove temporary delay
            // _uiState.update { it.copy(isLoading = false, userProfile = it.userProfile?.copy(statusMessage = newStatus)) } // Remove temporary UI update
            // _eventFlow.emit(ProfileEvent.ShowSnackbar("상태 메시지 변경됨 (임시)")) // Remove temporary snackbar
        }
    }

    fun changeMemo(newMemo: String) {
        // TODO: Implement actual call to changeMemoUseCase once its dependencies (UserRepository.updateUserMemo) are ready
        // changeMemoUseCase(newMemo)
        println("ProfileViewModel.changeMemo called with: $newMemo - UseCase call commented out for now.")
        // For now, simulate success or handle as needed for UI development
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Simulate a call and success
            when (val result = changeMemoUseCase(newMemo)) { // Assign to val and use actual use case call
                is CustomResult.Success -> {
                    // Optionally update UI state if memo is directly displayed and needs immediate refresh
                    // _uiState.update { it.copy(userProfile = it.userProfile?.copy(memo = newMemo), isLoading = false) }
                    _uiState.update { it.copy(isLoading = false) } // Stop loading
                    _eventFlow.emit(ProfileEvent.ShowSnackbar("메모가 업데이트되었습니다."))
                    println("ViewModel: 메모 변경 성공")
                }
                is CustomResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(ProfileEvent.ShowSnackbar("메모 변경 실패: ${result.error.message}"))
                    println("ViewModel: 메모 변경 실패 - ${result.error.message}")
                }
                is CustomResult.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                    println("ViewModel: 메모 변경 로딩 중...")
                }
                is CustomResult.Initial -> {
                    _uiState.update { it.copy(isLoading = false) } // Or true if initial implies start of a process
                    println("ViewModel: 메모 변경 초기 상태 - $result")
                }
                is CustomResult.Progress -> {
                    _uiState.update { it.copy(isLoading = true) } // Keep loading true, potentially update UI with progress
                    println("ViewModel: 메모 변경 진행 중 (${result.progress}%) - $result")
                }
            }
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
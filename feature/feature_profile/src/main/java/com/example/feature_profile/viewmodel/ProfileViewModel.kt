package com.example.feature_profile.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.core.NavigationManger
import com.example.domain.model.base.User
import com.example.domain.model.enum.UserStatus
import com.example.domain.model.vo.user.UserMemo
import com.example.domain.provider.auth.AuthSessionUseCaseProvider
import com.example.domain.provider.user.UserUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * UI-specific data class for displaying user profile information.
 */
data class UserProfileData(
    val uid: String,
    val name: String,
    val email: String?,
    val memo: String?, // Mapped from User.memo
    val userStatus: UserStatus
)

fun User.toUserProfileData(): UserProfileData {
    Log.d("ProfileViewModel", "️ ProfileViewModel: Converting User to UserProfileData")
    Log.d("ProfileViewModel", "🖼️ ProfileViewModel: User ID = ${this.id.value}")
    // profile image url logging removed

    return UserProfileData(
        uid = this.id.value,
        name = this.name.value,
        email = this.email.value.ifEmpty { null },
        memo = this.memo?.value, // Mapping 'memo' to 'statusMessage'
        userStatus = this.userStatus
    )
}

// 프로필 화면 UI 상태
data class ProfileUiState(
    val isLoading: Boolean = false,
    val userProfile: UserProfileData? = null, // 사용자 프로필 데이터 (null 가능)
    val errorMessage: String? = null,
    val showChangeStatusDialog: Boolean = false, // Dialog visibility state
    val tempSelectedStatus: UserStatus? = null // Holds status selected in dialog before confirmation
)

// 프로필 화면 이벤트
sealed class ProfileEvent {
    // NavigateToStatus is now handled by showChangeStatusDialog in UiState
    object PickProfileImage : ProfileEvent() // 이미지 선택기 실행 요청
    object LogoutCompleted : ProfileEvent() // 로그아웃 완료 알림 -> 화면 전환용
    data class ShowSnackbar(val message: String) : ProfileEvent()
}


@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider,
    private val userUseCaseProvider: UserUseCaseProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {

    // Provider를 통해 생성된 UseCase 그룹들
    private val authUseCases = authSessionUseCaseProvider.create()
    private val userUseCases = userUseCaseProvider.createForUser()

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true)) // 초기 로딩 상태
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ProfileEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadUserProfile()
    }

    // 프로필 수정 버튼 클릭
    fun onEditProfileClicked() {
        navigationManger.navigateToEditProfile()
    }

    // 프로필 정보 로드
    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            Log.d("ProfileViewModel", "🔄 ProfileViewModel: 사용자 프로필 로드 시도 (UseCase 사용)")

            // --- UseCase 호출 ---
            userUseCases.getCurrentUserStreamUseCase()
                .catch { exception ->
                    val errorMsg = "프로필 정보를 불러오지 못했습니다: ${exception.message}"
                    _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                    _eventFlow.emit(ProfileEvent.ShowSnackbar(errorMsg))
                    Log.d("ProfileViewModel", "❌ ProfileViewModel: 프로필 로드 중 예외 발생 - ${exception.message}")
                    Log.d("ProfileViewModel", "❌ ProfileViewModel: Exception stack trace: ${exception.stackTraceToString()}")
                }
                .collectLatest { customResult: CustomResult<User, Exception> ->
                    Log.d("ProfileViewModel", "📦 ProfileViewModel: CustomResult received - ${customResult.javaClass.simpleName}")
                    when (customResult) {
                        is CustomResult.Success -> {
                            val user = customResult.data
                            
                            val userProfileData = user.toUserProfileData()
                            Log.d("ProfileViewModel", "✅ ProfileViewModel: Converted to UserProfileData")
                            
                            _uiState.update { it.copy(isLoading = false, userProfile = userProfileData) }
                            Log.d("ProfileViewModel", "✅ ProfileViewModel: UI State updated with new profile data")
                        }
                        is CustomResult.Failure -> {
                            val errorMsg = "프로필 로드 실패: ${customResult.error.message}"
                            _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                            _eventFlow.emit(ProfileEvent.ShowSnackbar(errorMsg))
                            Log.d("ProfileViewModel", "❌ ProfileViewModel: 프로필 로드 실패 - $errorMsg")
                            Log.d("ProfileViewModel", "❌ ProfileViewModel: Error details: ${customResult.error.stackTraceToString()}")
                        }
                        else -> {
                            val errorMsg = "프로필 로드 실패: Unknown"
                            _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                            _eventFlow.emit(ProfileEvent.ShowSnackbar(errorMsg))
                            Log.d("ProfileViewModel", "❌ ProfileViewModel: 프로필 로드 실패 - Unknown result type: ${customResult.javaClass.simpleName}")
                        }
                    }
                }
        }
    }

    fun onProfileImageClick() {
        /** 프로필 이미지 클릭 은 지금  상태표시나 다른걸로 이동할 가능성 있음
        viewModelScope.launch {
            _eventFlow.emit(ProfileEvent.PickProfileImage)
        }
        **/
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
    fun onChangeStatusClick() {
        // Initialize tempSelectedStatus with current user's status when dialog is opened
        _uiState.update { currentState ->
            currentState.copy(
                showChangeStatusDialog = true,
                tempSelectedStatus = currentState.userProfile?.userStatus
            )
        }
    }

    fun onDismissChangeStatusDialog() {
        val currentProfileStatus = _uiState.value.userProfile?.userStatus
        val tempStatus = _uiState.value.tempSelectedStatus

        if (tempStatus != null && tempStatus != currentProfileStatus) {
            changeStatus(tempStatus) // Persist the change
        }
        // Reset dialog state
        _uiState.update { it.copy(showChangeStatusDialog = false, tempSelectedStatus = null) }
    }

    fun onStatusSelectedInDialog(status: UserStatus) {
        _uiState.update { it.copy(tempSelectedStatus = status) }
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
        navigationManger.navigateToFriends()
    }
    // 설정 버튼 클릭
    fun onSettingsClick() {
        navigationManger.navigateToSettings()
    }

    // 로그아웃 버튼 클릭
    fun onLogoutClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            println("ViewModel: 로그아웃 시도 (UseCase 사용)")
            
            // LogoutUseCase는 suspend 함수이므로 invoke()를 호출해야 함
            when (val result = authUseCases.logoutUseCase.invoke()) {
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
            val result = userUseCases.updateUserStatusUseCase(newStatus)
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
            when (val result = userUseCases.updateUserMemoUseCase(UserMemo(newMemo))) { // Assign to val and use actual use case call
                is CustomResult.Success -> {
                    // 서버에 저장 성공 후 프로필을 다시 로드하여 최신 메모 반영
                    _uiState.update { it.copy(isLoading = false) }
                    //loadUserProfile() // 최신 값 반영
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
                // Adding a general else to handle any other unhandled CustomResult states if they exist
                // Or if your CustomResult is a sealed class with exhaustive when, this might not be strictly needed
                // but can be a good fallback.
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                    println("ViewModel: 메모 변경, 알 수 없거나 처리되지 않은 상태: $result")
                }
            }
        }
    }

    fun changeProfileImage(imageUri: Uri?) {
        if (imageUri == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            println("ViewModel: 프로필 이미지 변경 시도 (UseCase 사용) - $imageUri")
            
            // UploadProfileImageUseCase를 사용하여 Firebase Storage에 업로드
            when (val result = userUseCases.uploadProfileImageUseCase(imageUri)) {
                is CustomResult.Success -> {
                    // 업로드 성공 시, 프로필을 다시 로드하여 최신 상태 반영
                    loadUserProfile()
                    _eventFlow.emit(ProfileEvent.ShowSnackbar("프로필 이미지 변경됨"))
                }
                is CustomResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(ProfileEvent.ShowSnackbar("프로필 이미지 변경 실패: ${result.error.message}"))
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(ProfileEvent.ShowSnackbar("프로필 이미지 변경 중 알 수 없는 오류가 발생했습니다."))
                }
            }
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
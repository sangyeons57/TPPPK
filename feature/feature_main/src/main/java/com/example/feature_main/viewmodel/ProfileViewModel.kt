package com.example.teamnovapersonalprojectprojectingkotlin.feature_main.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.User
import com.example.teamnovapersonalprojectprojectingkotlin.domain.repository.AuthRepository
import com.example.teamnovapersonalprojectprojectingkotlin.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- 데이터 모델 (예시) ---
data class UserProfileData(
    val userId: String,
    val name: String,
    val email: String, // 이전 ProfileViewModel과 병합
    val statusMessage: String,
    val profileImageUrl: String?
)
// ------------------------

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
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) } // 로딩 시작 및 이전 에러 메시지 초기화
            println("ViewModel: 사용자 프로필 로드 시도 (실제 로직)")

            // --- 실제 프로필 로드 로직 ---
            val profileResult: Result<User> = userRepository.getUser() // Repository 호출

            profileResult.fold(
                onSuccess = { user ->
                    // 성공 시: User 모델 -> UserProfileData 모델로 변환
                    val userProfileData = user.toUserProfileData() // 매핑 함수 사용
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userProfile = userProfileData, // 로드된 프로필 데이터 업데이트
                            errorMessage = null // 성공 시 에러 메시지 없음
                        )
                    }
                    println("ViewModel: 프로필 로드 성공 - ${userProfileData.name}")
                },
                onFailure = { exception ->
                    // 실패 시: 에러 처리
                    println("ViewModel: 프로필 로드 실패 - ${exception.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userProfile = null, // 실패 시 프로필 정보 null 처리 (또는 기존 값 유지)
                            errorMessage = "프로필을 불러오는데 실패했습니다." // 에러 메시지 설정
                        )
                    }
                    // SentryUtil.captureException(exception, "Failed to load user profile") // 에러 로깅 (필요 시)
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
            _uiState.update { it.copy(isLoading = true) } // 로딩 표시 (선택 사항)
            println("ViewModel: 로그아웃 시도")
            val result = authRepository.logout()
            result.onSuccess {
                _eventFlow.emit(ProfileEvent.LogoutCompleted) // 로그아웃 완료 이벤트 발생
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
                _eventFlow.emit(ProfileEvent.ShowSnackbar("로그아웃 실패"))
            }
        }
    }

    // --- TODO: 상태 메시지 변경, 프로필 이미지 변경 처리 함수 ---
    fun changeStatusMessage(newStatus: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            println("ViewModel: 상태 메시지 변경 시도 - $newStatus")
            // val result = userRepository.updateStatusMessage(newStatus)
            // result.onSuccess { loadUserProfile() } // 성공 시 프로필 다시 로드
            // .onFailure { _eventFlow.emit(...) }
            kotlinx.coroutines.delay(300)
            _uiState.update { it.copy(isLoading = false, userProfile = it.userProfile?.copy(statusMessage = newStatus)) } // 임시로 UI만 업데이트
            _eventFlow.emit(ProfileEvent.ShowSnackbar("상태 메시지 변경됨 (임시)"))
        }
    }

    fun changeProfileImage(imageUri: Uri?) {
        if (imageUri == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            println("ViewModel: 프로필 이미지 변경 시도 - $imageUri")
            // --- TODO: 실제 이미지 업로드 및 URL 업데이트 로직 ---
            // val result = userRepository.updateProfileImage(imageUri)
            // result.onSuccess { newImageUrl -> loadUserProfile() }
            // .onFailure { _eventFlow.emit(...) }
            // ----------------------------------------------
            kotlinx.coroutines.delay(1000)
            val newImageUrl = "https://picsum.photos/seed/${System.currentTimeMillis()}/100" // 임시 URL
            _uiState.update { it.copy(isLoading = false, userProfile = it.userProfile?.copy(profileImageUrl = newImageUrl)) } // 임시로 UI만 업데이트
            _eventFlow.emit(ProfileEvent.ShowSnackbar("프로필 이미지 변경됨 (임시)"))
        }
    }
    // -----------------------------------------------------

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * User(Domain Model) -> UserProfileData(UI Model) 변환 확장 함수
     * (이 함수는 ViewModel 파일 내부 또는 별도의 Mapper 파일에 위치할 수 있습니다)
     */
    private fun User.toUserProfileData(): UserProfileData {
        // User 모델에는 statusMessage 필드가 없으므로, 임시값 또는 다른 로직 필요
        // 여기서는 임시로 빈 문자열 또는 기본 메시지 사용
        val tempStatusMessage = "상태 메시지 없음" // TODO: 상태 메시지 로드/관리 로직 추가 필요

        return UserProfileData(
            userId = this.userId,
            name = this.name,
            email = this.email,
            statusMessage = tempStatusMessage, // User 모델에 없는 정보 처리
            profileImageUrl = this.profileImageUrl
        )
    }
}
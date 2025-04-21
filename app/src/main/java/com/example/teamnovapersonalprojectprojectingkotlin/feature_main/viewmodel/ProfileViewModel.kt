package com.example.teamnovapersonalprojectprojectingkotlin.feature_main.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    object ShowEditStatusDialog : ProfileEvent() // 상태 메시지 변경 다이얼로그 표시
    object PickProfileImage : ProfileEvent() // 이미지 선택기 실행 요청
    object LogoutCompleted : ProfileEvent() // 로그아웃 완료 알림 -> 화면 전환용
    data class ShowSnackbar(val message: String) : ProfileEvent()
}


@HiltViewModel
class ProfileViewModel @Inject constructor(
    // TODO: private val userRepository: UserRepository,
    // TODO: private val authRepository: AuthRepository
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
            _uiState.update { it.copy(isLoading = true) }
            println("ViewModel: 사용자 프로필 로드 시도")
            // --- TODO: 실제 프로필 로드 로직 (userRepository.getUserProfile()) ---
            kotlinx.coroutines.delay(500) // 임시 딜레이
            val success = true // 임시 성공
            val profileData = if (success) {
                UserProfileData(
                    userId = "user123",
                    name = "홍길동",
                    email = "gildong@example.com",
                    statusMessage = "오늘도 파이팅!",
                    profileImageUrl = null // "https://example.com/profile.jpg" // null 또는 URL
                )
            } else null
            // ----------------------------------------------------------------
            if (profileData != null) {
                _uiState.update { it.copy(isLoading = false, userProfile = profileData, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "프로필 로드 실패") }
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
        viewModelScope.launch {
            _eventFlow.emit(ProfileEvent.ShowEditStatusDialog)
        }
    }

    // 설정 버튼 클릭
    fun onStatusClick() {
        viewModelScope.launch {
            _eventFlow.emit(ProfileEvent.NavigateToSettings)
        }
    }
    // 설정 버튼 클릭
    fun onFriendsClick() {
        viewModelScope.launch {
            _eventFlow.emit(ProfileEvent.NavigateToSettings)
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
            // --- TODO: 실제 로그아웃 로직 (authRepository.logout(), 로컬 데이터 삭제 등) ---
            kotlinx.coroutines.delay(500) // 임시 딜레이
            val success = true // 임시 성공
            // val result = authRepository.logout()
            // result.onSuccess { ... }.onFailure { ... }
            // --------------------------------------------------------------------
            if (success) {
                _eventFlow.emit(ProfileEvent.LogoutCompleted) // 로그아웃 완료 이벤트 발생
            } else {
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
}
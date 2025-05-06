package com.example.feature_user.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_navigation.routes.AppRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 사용자 프로필 화면 UI 상태
data class UserProfileUiState(
    val userId: String? = null,
    val userName: String = "사용자 이름 로딩 중...",
    val userEmail: String = "",
    val profileImageUrl: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
    // 추가적인 사용자 정보 필드들
)

// 사용자 프로필 화면 이벤트
sealed class UserProfileEvent {
    // data class NavigateToEditProfile(val userId: String) : UserProfileEvent() // 예시
}

/**
 * 사용자 프로필 정보를 관리하는 ViewModel.
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
    // 필요한 UseCase 주입 (예: GetUserProfileUseCase)
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = savedStateHandle.get<String>(AppRoutes.User.ARG_USER_ID)
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, error = "사용자 ID를 찾을 수 없습니다.") }
                return@launch
            }
            _uiState.update { it.copy(userId = userId, isLoading = true) }

            // TODO: 실제 사용자 프로필 정보 로드 로직 (UseCase 사용)
            // 예시: val userProfile = getUserProfileUseCase(userId)
            // 성공: _uiState.update { it.copy(userName = userProfile.name, userEmail = userProfile.email, isLoading = false) }
            // 실패: _uiState.update { it.copy(error = "프로필 정보 로드 실패", isLoading = false) }

            // 임시 데이터
            kotlinx.coroutines.delay(1000)
            _uiState.update {
                it.copy(
                    userName = "사용자 $userId (임시)",
                    userEmail = "$userId@example.com",
                    isLoading = false
                )
            }
        }
    }
} 
package com.example.feature_settings.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.core.NavigationManger
import com.example.domain.provider.auth.AuthAccountUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authAccountUseCaseProvider: AuthAccountUseCaseProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {

    // Provider를 통해 생성된 UseCase 그룹
    private val authAccountUseCases = authAccountUseCaseProvider.create()

    private val _uiEvent = Channel<WithdrawalUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val _showWithdrawalDialog = MutableStateFlow(false)
    val showWithdrawalDialog: StateFlow<Boolean> = _showWithdrawalDialog.asStateFlow()

    fun onWithdrawAccountClick() {
        _showWithdrawalDialog.value = true
    }

    fun dismissWithdrawalDialog() {
        _showWithdrawalDialog.value = false
    }

    fun confirmWithdrawal() {
        viewModelScope.launch {
            // Immediately hide the dialog when confirmation starts
            _showWithdrawalDialog.value = false

            when (val result = authAccountUseCases.withdrawMembershipUseCase()) {
                is CustomResult.Success -> {
                    Log.d("SettingsViewModel", "Withdrawal successful. Navigating to auth screen.")
                    _uiEvent.send(WithdrawalUiEvent.Success("회원 탈퇴가 완료되었습니다."))
                    // Navigate to the auth screen, clearing the back stack
                    navigationManger.navigateToSplash()
                }
                is CustomResult.Failure -> {
                    Log.e("SettingsViewModel", "Withdrawal failed.", result.error)
                    _uiEvent.send(WithdrawalUiEvent.Error("회원 탈퇴에 실패했습니다. 다시 시도해주세요."))
                }
                else -> {
                    Log.e("SettingsViewModel", "Unknown result: $result")
                    _uiEvent.send(WithdrawalUiEvent.Error("알 수 없는 오류가 발생했습니다."))
                }
            }
        }
    }

    sealed class WithdrawalUiEvent {
        data class Success(val message: String) : WithdrawalUiEvent()
        data class Error(val message: String) : WithdrawalUiEvent()
    }
}
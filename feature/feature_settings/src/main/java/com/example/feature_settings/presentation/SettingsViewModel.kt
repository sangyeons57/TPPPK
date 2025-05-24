package com.example.feature_settings.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.usecase.user.WithdrawUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val withdrawUserUseCase: WithdrawUserUseCase
) : ViewModel() {

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
            try {
                withdrawUserUseCase().getOrThrow()
                Log.d("SettingsViewModel", "User withdrawal successful.")
                // TODO: Add navigation to a "logged out" screen or login screen
                // TODO: Show success message (e.g., via a new StateFlow for UI events)
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "User withdrawal failed.", e)
                // TODO: Show error message (e.g., via a new StateFlow for UI events)
            } finally {
                dismissWithdrawalDialog() // Dismiss dialog regardless of outcome
            }
        }
    }
}

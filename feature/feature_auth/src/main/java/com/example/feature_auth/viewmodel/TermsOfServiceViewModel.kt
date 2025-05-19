package com.example.feature_auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 서비스 이용약관 화면의 이벤트 정의
 */
sealed class TermsOfServiceEvent {
    data object NavigateBack : TermsOfServiceEvent()
    data class ShowSnackbar(val message: String) : TermsOfServiceEvent()
}

/**
 * 서비스 이용약관 화면의 ViewModel
 */
@HiltViewModel
class TermsOfServiceViewModel @Inject constructor() : ViewModel() {

    private val _eventFlow = MutableSharedFlow<TermsOfServiceEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    /**
     * 뒤로가기 버튼 클릭 시 호출
     */
    fun onBackClick() {
        viewModelScope.launch {
            _eventFlow.emit(TermsOfServiceEvent.NavigateBack)
        }
    }
} 
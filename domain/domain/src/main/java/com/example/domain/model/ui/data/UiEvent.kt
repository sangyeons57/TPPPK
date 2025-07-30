package com.example.domain.model.ui.data

import com.example.domain.model.ui.enum.LoginFormFocusTarget
import com.example.domain.model.ui.enum.SignUpFormFocusTarget

/**
 * UI 이벤트를 정의하는 sealed class
 * UI 이벤트는 ViewModel에서 UI로 전달되는 일회성 이벤트입니다.
 */
sealed class UiEvent {
    /**
     * 포커스 요청 이벤트
     * 특정 필드에 대한 포커스를 요청할 때 사용합니다.
     */
    data class RequestFocus<T>(val target: T) : UiEvent()
    
    /**
     * 스낵바 메시지 표시 이벤트
     * 사용자에게 짧은 메시지를 표시할 때 사용합니다.
     */
    data class ShowSnackbar(val message: String) : UiEvent()
    
    /**
     * 네비게이션 이벤트
     * 다른 화면으로 이동할 때 사용합니다.
     */
    data class Navigate(val route: String) : UiEvent()
    
    /**
     * 뒤로 가기 이벤트
     * 현재 화면에서 뒤로 가기를 요청할 때 사용합니다.
     */
    object NavigateBack : UiEvent()
}

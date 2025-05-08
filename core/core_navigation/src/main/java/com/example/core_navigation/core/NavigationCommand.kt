package com.example.core_navigation.core

import android.os.Bundle
import androidx.navigation.NavOptions
import com.example.core_navigation.destination.NavDestination

/**
 * 네비게이션 명령을 정의하는 sealed 클래스
 * 
 * 앱의 네비게이션 관련 작업을 나타내는 이벤트로,
 * NavigationHandler(또는 구현체인 NavigationManager)에 의해 발행되고
 * AppNavigationGraph에서 처리됩니다.
 */
sealed class NavigationCommand {
    /**
     * 특정 경로로 이동하는 명령
     * 
     * @param route 이동할 경로 문자열
     * @param navOptions 네비게이션 옵션 (선택적)
     */
    data class NavigateToRoute(
        val route: String,
        val navOptions: NavOptions? = null
    ) : NavigationCommand()
    
    /**
     * 인자와 함께 특정 목적지로 이동하는 명령
     * 
     * @param destination 이동할 목적지
     * @param args 전달할 인자
     * @param navOptions 네비게이션 옵션
     */
    data class NavigateWithArguments(
        val destination: NavDestination,
        val args: Map<String, Any> = emptyMap(),
        val navOptions: NavOptions? = null
    ) : NavigationCommand()
    
    /**
     * 이전 화면으로 돌아가는 명령
     */
    object NavigateBack : NavigationCommand()
    
    /**
     * 논리적 상위 화면으로 이동하는 명령
     * (Up 버튼과 유사)
     */
    object NavigateUp : NavigationCommand()
    
    /**
     * 중첩된 그래프로 이동하는 명령
     * 
     * @param parentRoute 부모 그래프 경로
     * @param childRoute 자식 경로
     */
    data class NavigateToNestedGraph(
        val parentRoute: String,
        val childRoute: String
    ) : NavigationCommand()
    
    /**
     * 특정 탭으로 이동하는 명령
     * 
     * @param route 이동할 탭 경로
     * @param saveState 현재 상태를 저장할지 여부
     * @param restoreState 이전 상태를 복원할지 여부
     */
    data class NavigateToTab(
        val route: String,
        val saveState: Boolean,
        val restoreState: Boolean
    ) : NavigationCommand()
    
    /**
     * 백스택을 모두 비우고 특정 경로로 이동하는 명령
     * 
     * @param route 이동할 경로 문자열
     */
    data class NavigateClearingBackStack(
        val route: String
    ) : NavigationCommand()
} 
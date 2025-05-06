package com.example.core_navigation.core

import android.os.Bundle
import androidx.navigation.NavOptions
import com.example.core_navigation.destination.NavDestination

/**
 * 네비게이션 명령을 정의하는 sealed 클래스
 * 다양한 네비게이션 패턴을 지원합니다.
 */
sealed class NavigationCommand {
    /**
     * 특정 경로로 이동하는 명령
     * 
     * @param route 이동할 경로
     * @param navOptions 네비게이션 옵션 (애니메이션, 백스택 처리 등)
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
     * 탭으로 이동하는 명령
     * (백스택 처리 포함)
     * 
     * @param route 탭 경로
     * @param saveState 현재 상태 저장 여부
     * @param restoreState 이전 상태 복원 여부
     */
    data class NavigateToTab(
        val route: String,
        val saveState: Boolean = true,
        val restoreState: Boolean = true
    ) : NavigationCommand()
    
    /**
     * 백스택을 모두 지우고 이동하는 명령
     * 
     * @param route 이동할 경로
     */
    data class NavigateClearingBackStack(
        val route: String
    ) : NavigationCommand()
} 
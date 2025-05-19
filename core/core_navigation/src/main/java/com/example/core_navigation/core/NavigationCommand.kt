package com.example.core_navigation.core

import android.os.Bundle
import androidx.navigation.NavOptions

/**
 * 네비게이션 목적지를 정의하는 인터페이스
 * 모든 화면 목적지 객체가 구현해야 합니다.
 */
interface NavDestination {
    /**
     * 이 목적지의 고유 경로
     */
    val route: String
    
    /**
     * 네비게이션 인자들의 목록
     */
    val arguments: List<String>
        get() = emptyList()
    
    /**
     * 인자들을 포함한 전체 경로 생성
     * 
     * @param args 경로에 추가할 인자 맵
     * @return 인자를 포함한 완전한 경로 문자열
     */
    fun createRoute(args: Map<String, Any> = emptyMap()): String {
        if (args.isEmpty()) return route
        
        // 기본 경로에 인자 추가
        val argValues = arguments.map { argName ->
            args[argName]?.toString() ?: "null"
        }
        
        return buildRouteWithArgs(argValues)
    }
    
    /**
     * 인자 값들을 사용하여 경로 생성
     * 
     * @param argValues 인자 값 목록
     * @return 인자 값을 포함한 경로
     */
    fun buildRouteWithArgs(argValues: List<String>): String {
        if (argValues.isEmpty()) return route
        
        return route + argValues.joinToString(prefix = "/", separator = "/")
    }
    
    /**
     * 문자열 경로로부터 NavDestination을 생성하는 간단한 팩토리 메소드
     */
    companion object {
        /**
         * 간단한 경로 문자열로부터 NavDestination 객체 생성
         * 
         * @param routePath 경로 문자열
         * @return 해당 경로를 가진 NavDestination 객체
         */
        fun fromRoute(routePath: String): NavDestination {
            return object : NavDestination {
                override val route: String = routePath
            }
        }
    }
}

/**
 * 네비게이션 명령을 정의하는 sealed 클래스
 * 
 * 앱의 네비게이션 관련 작업을 나타내는 이벤트로,
 * AppNavigator에 의해 발행되고 AppNavigationGraph에서 처리됩니다.
 */
sealed class NavigationCommand {
    /**
     * 특정 목적지로 이동하는 명령
     * 
     * @param destination 이동할 목적지
     * @param args 전달할 인자
     * @param navOptions 네비게이션 옵션
     */
    data class NavigateToRoute(
        val destination: NavDestination,
        val args: Map<String, Any> = emptyMap(),
        val navOptions: NavOptions? = null
    ) : NavigationCommand() {
        /**
         * 단순 문자열 경로로부터 NavigateToRoute 객체 생성
         */
        companion object {
            fun fromRoute(route: String, args :Map<String, Any> = emptyMap(), navOptions: NavOptions? = null): NavigateToRoute {
                return NavigateToRoute(
                    destination = NavDestination.fromRoute(route),
                    args = args,
                    navOptions = navOptions
                )
            }
        }
        
        /**
         * 최종 경로 문자열 반환
         */
        val route: String
            get() = destination.createRoute(args)
    }
    
    /**
     * 이전 화면으로 돌아가는 명령
     */
    object NavigateBack : NavigationCommand()
    
    /**
     * 백스택을 모두 비우고 특정 경로로 이동하는 명령
     * 
     * @param destination 이동할 목적지
     */
    data class NavigateClearingBackStack(
        val destination: NavDestination
    ) : NavigationCommand() {
        /**
         * 단순 문자열 경로로부터 NavigateClearingBackStack 객체 생성
         */
        companion object {
            fun fromRoute(route: String): NavigateClearingBackStack {
                return NavigateClearingBackStack(
                    destination = NavDestination.fromRoute(route)
                )
            }
        }
        
        /**
         * 경로 문자열 반환
         */
        val route: String
            get() = destination.route
    }
} 
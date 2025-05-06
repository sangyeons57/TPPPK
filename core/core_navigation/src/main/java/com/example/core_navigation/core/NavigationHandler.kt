package com.example.core_navigation.core

import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.Flow

/**
 * 네비게이션 명령을 실행하는 인터페이스
 * 
 * 다양한 네비게이션 구현체(Compose, XML 등)가 이 인터페이스를 구현하여
 * 실제 네비게이션 작업을 수행합니다.
 */
interface NavigationHandler {
    /**
     * 네비게이션 명령 Flow
     * 구현체는 이 Flow를 통해 명령을 수신하고 처리합니다.
     */
    val navigationCommands: Flow<NavigationCommand>
    
    /**
     * 명령에 따라 네비게이션을 실행합니다.
     * 
     * @param command 실행할 네비게이션 명령
     */
    fun navigate(command: NavigationCommand)
    
    /**
     * 이전 화면으로 돌아갑니다.
     * 
     * @return 성공적으로 뒤로 이동했는지 여부
     */
    fun navigateBack(): Boolean
    
    /**
     * 결과 값을 설정합니다.
     * 화면 간 데이터 전달에 사용됩니다.
     * 
     * @param key 결과를 식별하는 키
     * @param result 전달할 결과 값
     */
    fun <T> setResult(key: String, result: T)
    
    /**
     * 저장된 결과 값을 가져옵니다.
     * 화면 간 데이터 전달에 사용됩니다.
     * 
     * @param key 결과를 식별하는 키
     * @return 저장된 결과 값 또는 null
     */
    fun <T> getResult(key: String): T?
    
    /**
     * 특정 탭으로 이동합니다.
     * 
     * @param route 탭 경로
     * @param saveState 현재 상태 저장 여부
     * @param restoreState 이전 상태 복원 여부
     */
    fun navigateToTab(route: String, saveState: Boolean = true, restoreState: Boolean = true) {
        navigate(NavigationCommand.NavigateToTab(route, saveState, restoreState))
    }
    
    /**
     * 백스택을 모두 비우고 특정 경로로 이동합니다.
     * 
     * @param route 이동할 경로
     */
    fun navigateClearingBackStack(route: String) {
        navigate(NavigationCommand.NavigateClearingBackStack(route))
    }
    
    /**
     * 중첩된 그래프로 이동합니다.
     * 
     * @param parentRoute 부모 그래프 경로
     * @param childRoute 자식 경로
     */
    fun navigateToNestedGraph(parentRoute: String, childRoute: String) {
        navigate(NavigationCommand.NavigateToNestedGraph(parentRoute, childRoute))
    }
    
    // 유틸리티 확장 함수
    companion object {
        /**
         * 간단하게 경로로 이동합니다.
         * 
         * @param handler 네비게이션 핸들러
         * @param route 이동할 경로
         */
        fun navigateTo(handler: NavigationHandler, route: String) {
            handler.navigate(NavigationCommand.NavigateToRoute(route))
        }
    }
} 
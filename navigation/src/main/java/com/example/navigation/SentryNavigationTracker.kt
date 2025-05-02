package com.example.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.example.core_logging.SentryUtil
import io.sentry.ITransaction

/**
 * Sentry를 사용한 네비게이션 이벤트 추적기
 * 
 * 화면 이동을 자동으로 추적하여 사용자 경로와 성능을 모니터링합니다.
 */
object SentryNavigationTracker {
    
    private var currentScreen: String? = null
    private var previousScreen: String? = null
    private var currentTransaction: ITransaction? = null
    
    /**
     * NavController에 네비게이션 리스너를 등록하여 모든 화면 전환을 자동으로 추적합니다.
     * 
     * @param navController 추적할 NavController
     */
    fun registerNavigationListener(navController: NavController) {
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            trackScreenChange(destination, arguments)
        }
        
        // 초기 설정
        SentryUtil.addBreadcrumb("navigation", "Navigation tracking started")
    }
    
    /**
     * 화면 변경 시 Sentry에 이벤트를 기록합니다.
     * 
     * @param destination 현재 네비게이션 목적지
     * @param arguments 네비게이션 인자
     */
    private fun trackScreenChange(destination: NavDestination, arguments: android.os.Bundle?) {
        val destinationRoute = destination.route ?: return
        val destinationId = destination.id
        
        // 이전 트랜잭션 종료
        currentTransaction?.finish()
        
        // 이전 화면 정보 태그 설정
        SentryUtil.setCustomTag("navigation.previous_screen", currentScreen ?: "none")
        
        // 이전 화면 업데이트
        previousScreen = currentScreen
        
        // 현재 화면 이름 추출 (route에서 인자 부분 제거)
        val screenName = destinationRoute.split('/')[0]
        currentScreen = screenName
        
        // Sentry에 화면 이동 기록
        SentryUtil.trackScreenNavigation(screenName, previousScreen)
        
        // 트랜잭션 시작 및 태그 설정
        startScreenTransaction(screenName, arguments)
        
        // 기본 태그 설정
        SentryUtil.setCustomTag("screen_id", destinationId.toString())
        SentryUtil.setCustomTag("screen_route", destinationRoute)
        
        // 원격 로깅
        println("SentryNavigationTracker: 화면 전환 - $previousScreen -> $screenName")
    }
    
    /**
     * 새 화면 전환에 대한 성능 트랜잭션을 시작합니다.
     * 
     * @param screenName 화면 이름
     * @param arguments 네비게이션 인자
     */
    private fun startScreenTransaction(screenName: String, arguments: android.os.Bundle?) {
        // 트랜잭션 시작
        currentTransaction = SentryUtil.startTransaction(
            name = "navigation.screen.$screenName",
            operation = "navigation",
            tags = mapOf("screen_name" to screenName)
        )
        
        // 인자가 있으면 태그로 추가
        arguments?.keySet()?.forEach { key ->
            val value = arguments.get(key)?.toString() ?: "null"
            SentryUtil.setCustomTag("nav.arg.$key", value)
        }
    }
    
    /**
     * 세션 종료 시 호출하여 마지막 트랜잭션 정리합니다.
     */
    fun finishTracking() {
        // 마지막 트랜잭션 종료
        currentTransaction?.finish()
        currentTransaction = null
        
        // 종료 로그 추가
        SentryUtil.addBreadcrumb("navigation", "Navigation tracking finished")
    }
    
    /**
     * 사용자 액션(버튼 클릭 등)을 추적합니다.
     * 
     * @param actionType 액션 유형 (예: "button_click", "swipe")
     * @param actionDescription 액션 설명
     */
    fun trackUserAction(actionType: String, actionDescription: String) {
        SentryUtil.trackUserAction(actionType, actionDescription, currentScreen ?: "unknown")
    }
    
    /**
     * 현재 화면에서 로딩이나 처리 중인 작업의 성능을 측정합니다.
     * 
     * @param operationName 작업 이름 (예: "data_load", "rendering")
     * @param block 측정할 코드 블록
     * @return 코드 블록의 실행 결과
     */
    fun <T> trackOperation(operationName: String, block: () -> T): T {
        val screenName = currentScreen ?: "unknown"
        val currentTrans = currentTransaction
        
        return if (currentTrans != null) {
            // 트랜잭션이 있으면 스팬으로 측정
            SentryUtil.withSpan(currentTrans, "ui.operation.$operationName", "$screenName - $operationName") {
                block()
            }
        } else {
            // 없으면 그냥 실행
            block()
        }
    }
    
    /**
     * 현재 화면 이름을 반환합니다.
     * @return 현재 화면 이름 또는 null
     */
    fun getCurrentScreen(): String? = currentScreen
} 
package com.example.navigation

import androidx.navigation.NavController
import com.example.core_logging.SentryUtil

/**
 * Sentry 네비게이션 추적을 위한 도우미 클래스
 * 
 * 간소화된 버전의 네비게이션 추적기입니다.
 */
object SentryNavigationHelper {
    
    private var currentScreen: String? = null
    
    /**
     * NavController에 네비게이션 리스너를 등록하여 화면 전환을 자동으로 추적합니다.
     * 
     * @param navController 추적할 NavController
     */
    fun setupTracking(navController: NavController) {
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            val destinationRoute = destination.route ?: return@addOnDestinationChangedListener
            
            // 현재 화면 이름 추출
            val screenName = destinationRoute.split('/')[0]
            val previousScreen = currentScreen
            currentScreen = screenName
            
            // Sentry에 화면 이동 기록
            SentryUtil.trackScreenNavigation(screenName, previousScreen)
            
            // 태그 설정
            SentryUtil.setCustomTag("screen_id", destination.id.toString())
            SentryUtil.setCustomTag("screen_route", destinationRoute)
        }
        
        // 초기 설정
        SentryUtil.addBreadcrumb("navigation", "Navigation tracking started")
    }
    
    /**
     * 세션 종료 시 호출하여 추적을 종료합니다.
     */
    fun finishTracking() {
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
     * 현재 화면 이름을 반환합니다.
     * @return 현재 화면 이름 또는 null
     */
    fun getCurrentScreen(): String? = currentScreen
} 
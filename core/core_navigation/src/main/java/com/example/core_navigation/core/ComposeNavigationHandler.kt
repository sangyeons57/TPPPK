package com.example.core_navigation.core

import android.os.Bundle
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Jetpack Compose 네비게이션을 위한 네비게이션 핸들러 인터페이스
 * 
 * Compose의 NavHostController를 사용한 네비게이션 구현을 추상화합니다.
 */
interface ComposeNavigationHandler : NavigationHandler {
    /**
     * Compose 네비게이션 컨트롤러 설정
     * 
     * @param navController 사용할 네비게이션 컨트롤러
     */
    fun setNavController(navController: NavHostController)
    
    /**
     * 특정 경로로 이동 (일반 String 경로)
     * 
     * @param routePath 이동할 경로 문자열
     * @param navOptions 네비게이션 옵션
     */
    fun navigateTo(routePath: String, navOptions: NavOptions? = null) {
        navigate(NavigationCommand.NavigateToRoute(routePath, navOptions))
    }
    
    /**
     * 네비게이션 결과를 전달하며 이전 화면으로 이동
     * 
     * @param key 결과 키
     * @param result 결과 값
     */
    fun navigateBackWithResult(key: String, result: Any?) {
        setResult(key, result)
        navigateBack()
    }
}

// ComposeNavigationHandlerImpl 클래스 정의 제거됨
// NavigationManager 클래스가 해당 역할을 수행함 
package com.example.core_navigation.util

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.core_navigation.core.NavigationManager
import kotlinx.coroutines.flow.Flow

/**
 * 네비게이션 관련 확장 함수들을 모아둔 파일
 */

/**
 * 특정 경로의 인자를 가져오는 확장 함수
 * 
 * @param key 인자 키
 * @return 인자 값 또는 null
 */
fun <T> NavController.getArgument(key: String): T? {
    return previousBackStackEntry?.savedStateHandle?.get<T>(key)
}

/**
 * 특정 경로의 인자를 Flow로 관찰하는 확장 함수
 * 
 * @param key 인자 키
 * @return 인자 값 Flow
 */
fun <T> NavController.getArgumentFlow(key: String): Flow<T> {
    return previousBackStackEntry?.savedStateHandle?.get(key)
        ?: throw IllegalStateException("No previous backstack entry found")
}

/**
 * 인자를 설정하는 확장 함수
 * 
 * @param key 인자 키
 * @param value 인자 값
 */
fun <T> NavController.setResult(key: String, value: T) {
    previousBackStackEntry?.savedStateHandle?.set(key, value)
}

/**
 * 네비게이션 결과를 관찰하는 컴포저블
 * 
 * @param key 결과 키
 * @param onResult 결과 처리 콜백
 */
@Composable
fun <T> NavController.ObserveResult(key: String, onResult: (T) -> Unit) {
    val currentOnResult = remember(key, onResult) { onResult }
    
    LaunchedEffect(key) {
        val flow = currentBackStackEntry?.savedStateHandle?.getStateFlow<T?>(key, null)
            ?: return@LaunchedEffect
        
        flow.collect { value ->
            if (value != null) {
                currentOnResult(value)
                currentBackStackEntry?.savedStateHandle?.set(key, null)
            }
        }
    }
}

/**
 * 네비게이션 매니저의 결과를 관찰하는 컴포저블
 * 
 * @param navigationManager 네비게이션 매니저
 * @param key 결과 키
 * @param onResult 결과 처리 콜백
 */
@Composable
fun <T> ObserveNavigationResult(
    navigationManager: NavigationManager,
    key: String,
    onResult: (T) -> Unit
) {
    val currentOnResult = remember(key, onResult) { onResult }
    
    LaunchedEffect(key) {
        navigationManager.getResultFlow<T>(key).collect { value ->
            currentOnResult(value)
        }
    }
}

/**
 * NavType에 맞는 navArgument를 생성하는 함수
 * 
 * @param name 인자 이름
 * @param navType 네비게이션 타입
 * @param nullable null 허용 여부
 * @param defaultValue 기본값
 * @return NavArgument
 */
fun createNavArgument(
    name: String,
    navType: NavType<*>,
    nullable: Boolean = false,
    defaultValue: Any? = null
) = navArgument(name) {
    type = navType
    this.nullable = nullable
    defaultValue?.let { this.defaultValue = it }
} 
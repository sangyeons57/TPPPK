package com.example.core_navigation.core

import android.os.Bundle
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.Flow

/**
 * 앱 전체 네비게이션을 관리하는 통합 인터페이스
 * 
 * 이 인터페이스는 기존의 여러 네비게이션 인터페이스(NavigationHandler, ComposeNavigationHandler, 
 * NavigationResultListener)를 통합하여 단일 진입점을 제공합니다.
 */
interface AppNavigator {
    // ===== 기본 네비게이션 기능 =====
    
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
     * NavigationCommand를 사용하여 백스택을 모두 비우고 특정 경로로 이동합니다.
     * 
     * @param command 백스택 비우기 명령 객체
     */
    fun navigateClearingBackStack(command: NavigationCommand.NavigateClearingBackStack)
    
    // ===== Compose 특화 기능 =====
    
    /**
     * NavController 설정
     * 일반적으로 앱 시작 시 AppNavigationGraph에서 호출됩니다.
     *
     * @param navController 설정할 NavController
     */
    fun setNavController(navController: NavHostController)

    /*
     * NavController 반환
     */
    fun getNavController(): NavHostController?
    
    /**
     * 자식 NavController 설정
     * 중첩된 그래프를 가진 화면에서 자신의 NavController를 등록하기 위해 호출합니다.
     *
     * @param navController 등록할 자식 NavController (null이면 등록 해제)
     */
    fun setChildNavController(navController: NavHostController?)
    
    /**
     * 현재 활성화된 자식 NavController 반환
     * (없는 경우 null)
     */
    fun getChildNavController(): NavHostController?
    
    // ===== 결과 및 상태 관리 =====
    
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
     * 결과 데이터 Flow를 가져옵니다.
     * 화면에서 결과를 지속적으로 관찰할 때 사용합니다.
     *
     * @param key 결과를 식별하는 키
     * @return 결과 데이터의 Flow
     */
    fun <T> getResultFlow(key: String): Flow<T>
    
    /**
     * 화면 상태 저장
     * 탭 전환 시 화면 상태를 저장하는 데 사용됩니다.
     *
     * @param screenRoute 상태를 저장할 화면의 라우트
     * @param state 저장할 상태 번들
     */
    fun saveScreenState(screenRoute: String, state: Bundle)
    
    /**
     * 화면 상태 복원
     * 탭 전환 시 이전에 저장된 화면 상태를 복원하는 데 사용됩니다.
     *
     * @param screenRoute 상태를 복원할 화면의 라우트
     * @return 저장된 상태 번들 (없는 경우 null)
     */
    fun getScreenState(screenRoute: String): Bundle?
    
    // ===== 특정 화면 이동 편의 메서드 =====

    /**
     * NavigationCommand를 사용하여 탭 내부에서 프로젝트 상세 화면으로 이동
     *
     * @param projectId 이동할 프로젝트의 ID
     * @param command 프로젝트 상세 화면 이동 명령 객체
     */
    fun navigateToProjectDetailsNested(projectId: String, command: NavigationCommand.NavigateToRoute)

    /**
     * 경로가 유효한지 확인
     * 
     * @param route 확인할 경로
     * @return 경로 유효 여부
     */
    fun isValidRoute(route: String): Boolean
} 
package com.example.core_navigation.core

import android.os.Bundle
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.example.core_navigation.destination.AppRoutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow

/**
 * Compose Navigation 관련 기능을 제공하는 인터페이스
 *
 * 이 인터페이스는 Compose 기반 네비게이션에 필요한 메서드를 정의하며,
 * NavigationManager에 의해 구현됩니다.
 */
interface ComposeNavigationHandler : NavigationHandler, NavigationResultListener {

    /**
     * 네비게이션 명령 Flow
     * 이 Flow를 통해 네비게이션 이벤트를 감지하고 처리합니다.
     */
    override val navigationCommands: Flow<NavigationCommand>

    /**
     * 최상위 NavController 설정
     * 일반적으로 앱 시작 시 AppNavigationGraph에서 호출됩니다.
     *
     * @param navController 설정할 NavController
     */
    fun setNavController(navController: NavHostController)

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

    /**
     * 특정 탭으로 이동
     * MainScreen의 하단 탐색에서 사용됩니다.
     *
     * @param route 이동할 탭 경로
     * @param saveState 현재 상태를 저장할지 여부 (기본값: true)
     * @param restoreState 이전 상태를 복원할지 여부 (기본값: true)
     */
    override fun navigateToTab(
        route: String,
        saveState: Boolean,
        restoreState: Boolean
    )

    /**
     * 프로젝트 상세 화면으로 이동
     * @param projectId 이동할 프로젝트의 ID
     */
    fun navigateToProjectDetails(projectId: String)

    fun navigateToChat(channelId: String, messageId: String?)

    /**
     * 탭 내부에서 프로젝트 상세 화면으로 이동
     * MainScreen > Home 탭 내부의 네비게이션을 사용합니다.
     * 
     * @param projectId 이동할 프로젝트의 ID
     */
    fun navigateToProjectDetailsNested(projectId: String)
}

// ComposeNavigationHandlerImpl 클래스 정의 제거됨
// NavigationManager 클래스가 해당 역할을 수행함 
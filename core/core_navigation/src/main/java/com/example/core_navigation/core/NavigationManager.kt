package com.example.core_navigation.core

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.example.core_navigation.destination.AppRoutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 전체 네비게이션을 관리하는 통합 클래스
 * 
 * 단순 네비게이션, 중첩 네비게이션, 결과 처리를 모두 담당합니다.
 * Compose 환경에 특화되어 있으며, NavHostController를 직접 제어합니다.
 */
@Singleton
class NavigationManager @Inject constructor() : ComposeNavigationHandler {

    // 최상위 네비게이션 컨트롤러 (Activity 레벨 NavHost)
    private var parentNavController: NavHostController? = null
    
    // 현재 활성화된 자식(중첩) 네비게이션 컨트롤러 (NestedNavHost 내부)
    private var activeChildNavController: NavHostController? = null
    
    // 네비게이션 커맨드를 전달하는 Flow (UI 또는 ViewModel에서 방출)
    private val _navigationCommands = MutableSharedFlow<NavigationCommand>(extraBufferCapacity = 1)
    override val navigationCommands: Flow<NavigationCommand> = _navigationCommands.asSharedFlow()
    
    // 결과 데이터 저장소
    private val resultStore = mutableMapOf<String, Any?>()
    
    // 결과 발행 Flow 저장소
    private val resultFlows = mutableMapOf<String, MutableSharedFlow<Any?>>()
    
    // 내부 작업을 위한 CoroutineScope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * 최상위(부모) 네비게이션 컨트롤러 설정.
     * 앱의 메인 NavHost에서 한 번만 호출되어야 합니다.
     * 이미 설정된 경우 다시 설정하지 않습니다.
     */
    override fun setNavController(navController: NavHostController) {
        if (parentNavController == null) {
            this.parentNavController = navController
            println("NavigationManager: Parent NavController set.") // 로그 추가
        } else {
            println("NavigationManager: Parent NavController already set.") // 로그 추가
        }
    }

    /**
     * 현재 활성화된 자식 네비게이션 컨트롤러 설정.
     * 중첩된 NavHost에서 자신의 NavController를 등록하기 위해 호출합니다.
     */
    override fun setChildNavController(navController: NavHostController?) {
        this.activeChildNavController = navController
        println("NavigationManager: Active Child NavController updated: ${navController?.graph?.route}") // 로그 추가
    }

    /**
     * 현재 활성화된 자식 네비게이션 컨트롤러를 반환합니다.
     */
    override fun getChildNavController(): NavHostController? {
        return activeChildNavController
    }

    /**
     * 네비게이션 명령 실행
     * ViewModel 등에서 호출되어 네비게이션 이벤트를 내부 스코프에서 발생시킵니다.
     * 실제 이동은 UI 레벨(NavHost)에서 navigationCommands를 구독하여 처리합니다.
     */
    override fun navigate(command: NavigationCommand) {
        scope.launch {
            _navigationCommands.emit(command)
        }
    }
    
    /**
     * 특정 경로로 이동
     */
    fun navigateTo(routePath: String, navOptions: NavOptions?) {
        navigate(NavigationCommand.NavigateToRoute(routePath, navOptions))
    }

    /**
     * 이전 화면으로 돌아가기
     * 자식 컨트롤러 -> 부모 컨트롤러 순서로 백스택 처리 시도
     * @return 이동 성공 여부
     */
    override fun navigateBack(): Boolean {
        // 1. 자식 네비게이션 백스택 시도
        if (activeChildNavController?.popBackStack() == true) {
            println("NavigationManager: Popped back stack on Active Child NavController.")
            return true
        }
        
        // 2. 부모 네비게이션 백스택 시도
        if (parentNavController?.popBackStack() == true) {
            println("NavigationManager: Popped back stack on Parent NavController.")
            // 부모에서 뒤로 갔다면, 자식 컨트롤러 참조는 더 이상 유효하지 않음
            setChildNavController(null) // 명시적 해제
            return true
        }
        // 3. 둘 다 실패
        println("NavigationManager: navigateBack failed - no back stack entries.")
        return false
    }
    
    /**
     * 네비게이션 결과를 전달하며 이전 화면으로 이동
     */
    fun navigateBackWithResult(key: String, result: Any?) {
        setResult(key, result)
        navigateBack()
    }
    
    /**
     * 특정 탭으로 이동
     * 
     * 두 가지 시나리오를 처리합니다:
     * 1. 자식 NavController가 있는 경우: 자식 NavController로 직접 탭 이동을 실행합니다.
     * 2. 자식 NavController가 없는 경우: NavigationCommand.NavigateToTab을 발행하여 
     *    AppNavigationGraph가 먼저 Main 화면으로 이동 후 탭 이동을 준비하도록 합니다.
     */
    override fun navigateToTab(route: String, saveState: Boolean, restoreState: Boolean) {
        // 이미 중첩 NavController가 있는 경우 (MainScreen이 활성화됨)
        activeChildNavController?.let { childNav ->
            println("NavigationManager: Navigating to tab '$route' using childNavController")
            try {
                childNav.navigate(route) {
                    // 중첩 네비게이션에서의 탭 전환 로직
                    val startDestinationId = childNav.graph.findStartDestination().id
                    popUpTo(startDestinationId) {
                        this.saveState = saveState
                    }
                    launchSingleTop = true
                    this.restoreState = restoreState
                }
                return // 성공적으로 이동했으므로 함수 종료
            } catch (e: Exception) {
                println("NavigationManager Warning: Failed to navigate to tab '$route' with child controller: ${e.message}")
                // 실패 시 아래 로직으로 fallback
            }
        }
        
        // 위의 직접 이동이 실패했거나, 중첩 NavController가 없는 경우
        // (MainScreen으로 먼저 이동해야 하는 경우)
        println("NavigationManager: Emitting NavigateToTab command for '$route'")
        navigate(NavigationCommand.NavigateToTab(route, saveState, restoreState))
    }

    /**
     * 백스택을 모두 비우고 특정 경로로 이동
     */
    override fun navigateClearingBackStack(route: String) {
        navigate(NavigationCommand.NavigateClearingBackStack(route))
    }
    
    /**
     * 중첩된 그래프로 이동
     */
    override fun navigateToNestedGraph(parentRoute: String, childRoute: String) {
        navigate(NavigationCommand.NavigateToNestedGraph(parentRoute, childRoute))
    }

    /**
     * 특정 목적지가 현재 활성화되어 있는지 확인
     * 자식 -> 부모 순서로 확인합니다.
     */
    fun isDestinationActive(routePath: String): Boolean {
        val currentChildRoute = activeChildNavController?.currentDestination?.route
        if (currentChildRoute == routePath) return true
        
        val currentParentRoute = parentNavController?.currentDestination?.route
        return currentParentRoute == routePath
    }

    /**
     * 결과 데이터 설정
     * 이전 화면의 SavedStateHandle 또는 내부 저장소에 저장하고 Flow 발행
     */
    override fun <T> setResult(key: String, result: T) {
        println("NavigationManager: Setting result for key '$key'")
        // 1. 내부 저장소에 저장 (getResult로 즉시 가져갈 경우 대비)
        resultStore[key] = result
        
        // 2. 이전 화면의 SavedStateHandle에 저장 (화면 복원 시 유지)
        val targetNavController = activeChildNavController ?: parentNavController
        try {
            targetNavController?.previousBackStackEntry?.savedStateHandle?.set(key, result)
        } catch (e: IllegalStateException) {
            // previousBackStackEntry가 없을 경우 발생 가능 (e.g., 첫 화면)
            println("NavigationManager Warning: Could not set result to previousBackStackEntry's SavedStateHandle for key '$key'. ${e.message}")
        }
        
        // 3. Flow 발행 (현재 화면에서 즉시 결과를 수신해야 하는 경우)
        scope.launch { // 안전하게 emit
            val resultFlow = resultFlows.getOrPut(key) { MutableSharedFlow(replay = 1) } as MutableSharedFlow<Any?>
            resultFlow.emit(result) 
        }
    }

    /**
     * 결과 데이터 즉시 가져오기 (일회성)
     * Flow를 사용하지 않고 직접 결과를 가져올 때 사용합니다.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> getResult(key: String): T? {
        println("NavigationManager: Getting result for key '$key'")
        // 내부 저장소 우선 확인
        if (resultStore.containsKey(key)) {
            val result = resultStore[key] as? T
            resultStore.remove(key) // 일회성
            println("NavigationManager: Found result in internal store for key '$key'.")
            return result
        }
        // SavedStateHandle 확인 (화면 전환 후 복원된 경우)
        val targetNavController = activeChildNavController ?: parentNavController
        return try {
            val result = targetNavController?.currentBackStackEntry?.savedStateHandle?.remove<T>(key)
            if (result != null) {
                println("NavigationManager: Found result in SavedStateHandle for key '$key'.")
            }
            result
        } catch (e: IllegalStateException) {
             println("NavigationManager Warning: Could not get result from currentBackStackEntry's SavedStateHandle for key '$key'. ${e.message}")
             null
        }
    }

    /**
     * 결과 데이터 Flow 가져오기 (구독용)
     * 화면에서 결과를 지속적으로 관찰할 때 사용합니다.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> getResultFlow(key: String): Flow<T> {
        println("NavigationManager: Getting result flow for key '$key'")
        return resultFlows.getOrPut(key) { MutableSharedFlow(replay = 1) }.asSharedFlow() as Flow<T>
    }

    /**
     * 프로젝트 상세 화면으로 이동
     */
    override fun navigateToProjectDetails(projectId: String) {
        // 자식 NavController가 있는지 확인 (MainScreen 내부인지)
        activeChildNavController?.let { childNav ->
            println("NavigationManager: Child NavController 사용하여 프로젝트 상세로 이동: $projectId")
            try {
                // Home 탭 내부의 프로젝트 상세로 이동
                childNav.navigate("project_detail/$projectId")
                return // 성공적으로 이동했으므로 함수 종료
            } catch (e: Exception) {
                println("NavigationManager: 자식 NavController로 이동 실패, 부모 NavController 사용: ${e.message}")
                // 실패 시 최상위 NavController로 이동 (아래 로직)
            }
        }
        
        // 자식 NavController가 없거나 이동 실패 시 최상위 NavController로 이동
        println("NavigationManager: 최상위 NavController 사용하여 프로젝트 상세로 이동: $projectId")
        navigate(NavigationCommand.NavigateToRoute(AppRoutes.Project.detail(projectId)))
    }
    
    /**
     * 채팅 화면으로 이동
     * AppRoutes의 chat channel 경로를 사용합니다.
     *
     * @param channelId 이동할 채널의 ID
     * @param messageId 스크롤할 메시지 ID (옵션)
     */
    override fun navigateToChat(channelId: String, messageId: String?) {
        val route = if (messageId != null) {
            // messageId가 있는 경우 쿼리 파라미터로 추가
            AppRoutes.Chat.channel(channelId, messageId)
        } else {
            AppRoutes.Chat.channel(channelId)
        }
        navigate(NavigationCommand.NavigateToRoute(route))
    }

    /**
     * 탭 내부에서 프로젝트 상세 화면으로 이동
     * 
     * 참고: HomeScreen이 상태 기반으로 업데이트되어 이 메서드는 더 이상 활발히 사용되지 않습니다.
     * HomeViewModel.onProjectClick은 네비게이션 대신 상태 업데이트를 사용합니다.
     * 
     * 이 메서드는 하위 호환성 및 다른 컴포넌트에서의 사용을 위해 유지됩니다.
     * 
     * @param projectId 이동할 프로젝트의 ID
     */
    override fun navigateToProjectDetailsNested(projectId: String) {
        println("NavigationManager: navigateToProjectDetailsNested는 현재 HomeScreen의 상태 기반 업데이트로 대체되었습니다.")
        // 이전 구현: HomeScreen 내부의 중첩 네비게이션 사용
        // 현재: 호환성을 위해 유지하되, 표준 네비게이션으로 전환
        navigateToProjectDetails(projectId)
    }
} 
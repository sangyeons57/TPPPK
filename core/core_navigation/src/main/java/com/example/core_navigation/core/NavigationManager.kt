package com.example.core_navigation.core

import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.example.core_navigation.routes.AppRoutes
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
class NavigationManager @Inject constructor() : ComposeNavigationHandler, NavigationResultListener {

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
    fun setActiveChildNavController(navController: NavHostController?) { // Nullable 허용
        this.activeChildNavController = navController
        println("NavigationManager: Active Child NavController updated: ${navController?.graph?.route}") // 로그 추가
    }

    /**
     * 현재 활성화된 자식 네비게이션 컨트롤러를 반환합니다.
     */
    fun getActiveChildNavController(): NavHostController? {
        return activeChildNavController
    }

    /**
     * 네비게이션 명령 실행 (Non-suspend)
     * ViewModel 등에서 호출되어 네비게이션 이벤트를 내부 스코프에서 발생시킵니다.
     * 실제 이동은 UI 레벨(NavHost)에서 navigationCommands를 구독하여 처리합니다.
     */
    override fun navigate(command: NavigationCommand) { // suspend 제거
        // 내부 scope를 사용하여 비동기적으로 Flow에 emit
        scope.launch {
            _navigationCommands.emit(command)
        }
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
        // 자식에서 더 이상 뒤로 갈 수 없으면, 자식 컨트롤러 참조 해제 고려
        // setActiveChildNavController(null)
        
        // 2. 부모 네비게이션 백스택 시도
        if (parentNavController?.popBackStack() == true) {
            println("NavigationManager: Popped back stack on Parent NavController.")
            // 부모에서 뒤로 갔다면, 자식 컨트롤러 참조는 더 이상 유효하지 않음
            setActiveChildNavController(null) // 명시적 해제
            return true
        }
        // 3. 둘 다 실패
        println("NavigationManager: navigateBack failed - no back stack entries.")
        return false
    }
    
    /**
     * 특정 탭으로 이동 (주로 Bottom Navigation)
     * 부모 NavController를 사용합니다.
     * NavigationHandler 인터페이스의 기본 구현을 사용하도록 suspend 제거
     */
    // override suspend fun navigateToTab(route: String, saveState: Boolean, restoreState: Boolean) {
    //     navigate(NavigationCommand.NavigateToTab(route, saveState, restoreState))
    // }

    /**
     * 백스택을 모두 비우고 특정 경로로 이동
     * 부모 NavController를 사용합니다.
     * NavigationHandler 인터페이스의 기본 구현을 사용하도록 suspend 제거
     */
    // override suspend fun navigateClearingBackStack(route: String) {
    //     navigate(NavigationCommand.NavigateClearingBackStack(route))
    // }
    
    /**
     * 중첩된 그래프로 이동
     * 부모 NavController를 사용하여 중첩 그래프 경로로 이동합니다.
     * NavigationHandler 인터페이스의 기본 구현을 사용하도록 suspend 제거
     */
    // override suspend fun navigateToNestedGraph(parentRoute: String, childRoute: String) {
    //     navigate(NavigationCommand.NavigateToNestedGraph(parentRoute, childRoute))
    // }

    /**
     * 탭 내에서 특정 경로로 이동
     * 현재 활성화된 자식 NavController를 사용합니다.
     */
    fun navigateWithinTab(route: String) {
        if (activeChildNavController == null) {
            println("NavigationManager Warning: navigateWithinTab called but activeChildNavController is null.")
            // 부모 컨트롤러로 시도하거나 오류 처리
            // parentNavController?.navigate(route)
            return
        }
        activeChildNavController?.navigate(route)
    }

    /* 주석 처리: 새 AppRoutes에 SelectType이 정의되어 있지 않음. AppRoutes.Project.ADD로 대체될 수 있음.
    fun navigateToSelectProjectType() {
        // navigate(NavigationCommand.NavigateToRoute(AppRoutes.Project.SelectType.path)) // 예전 경로
        navigate(NavigationCommand.NavigateToRoute(com.example.core_navigation.routes.AppRoutes.Project.ADD))
    }
    */

    /**
     * 프로젝트 메인 화면으로 이동합니다.
     * 백스택을 클리어하고 Main 화면으로 이동하며, 선택적으로 projectId를 전달합니다.
     *
     * @param projectId 이동할 프로젝트의 ID. null이면 projectId 없이 메인으로 이동합니다.
     */
    fun navigateToProjectMain(projectId: String? = null) {
        if (projectId != null) {
            // 이 projectId를 MainScreen 또는 관련 ViewModel/상태 관리자가 인지하도록 하는 로직 필요.
            // 예: 특정 이벤트 버스에 projectId 발행, 공유 ViewModel 상태 업데이트 등.
            // 이 NavigationManager는 단순히 "main" 프레임으로 이동하는 역할만 수행.
            println("NavigationManager: Navigating to Main (Host) for projectId '$projectId'. Project context needs to be handled by MainScreen/ViewModel.")
        }
        // AppRoutes.MainScreens.Host.path ("main") 경로로 이동하고 백스택 클리어 -> 새 AppRoutes.Main.ROOT로 변경
        navigate(NavigationCommand.NavigateClearingBackStack(AppRoutes.MainScreens.ROOT))
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

    // The following methods are obsolete and were causing compilation errors
    // They referenced ScreenRoute and ScreenRouteWithArgs which are no longer part of the
    // ComposeNavigationHandler interface or the new navigation system.

    // override fun navigateTo(screenRoute: ScreenRoute, navOptions: NavOptions?) {
    //     navigate(NavigationCommand.NavigateToRoute(screenRoute.path, navOptions))
    // }

    // override fun <Args : Parcelable> navigateTo(
    //     screenRouteWithArgs: ScreenRouteWithArgs<Args>,
    //     args: Args,
    //     navOptions: NavOptions?
    // ) {
    //     val route = screenRouteWithArgs.buildPath(args)
    //     navigate(NavigationCommand.NavigateToRoute(route, navOptions))
    // }

    override fun navigateBackWithResult(key: String, result: Any?) {
        super.navigateBackWithResult(key, result) // ComposeNavigationHandler의 기본 구현 사용
    }
} 
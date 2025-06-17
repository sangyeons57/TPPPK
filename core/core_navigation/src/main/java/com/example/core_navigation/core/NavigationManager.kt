package com.example.core_navigation.core

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.collection.forEach
import androidx.collection.valueIterator
import androidx.navigation.ActivityNavigator
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
class NavigationManager @Inject constructor() : AppNavigator {

    // 최상위 네비게이션 컨트롤러 (Activity 레벨 NavHost)
    private var parentNavController: NavHostController? = null
    
    // 현재 활성화된 자식(중첩) 네비게이션 컨트롤러 (NestedNavHost 내부)
    internal var activeChildNavController: NavHostController? = null
    
    // 결과 데이터 저장소
    private val resultStore = mutableMapOf<String, Any?>()
    
    // 결과 발행 Flow 저장소
    private val resultFlows = mutableMapOf<String, MutableSharedFlow<Any?>>()
    
    // 화면 상태 저장소
    private val screenStates = mutableMapOf<String, Bundle>()
    
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
            //this.activeChildNavController = navController
            println("NavigationManager: Parent NavController set.") // 로그 추가
        } else {
            println("NavigationManager: Parent NavController already set.") // 로그 추가
        }
    }

    override fun getNavController(): NavHostController? {
        return parentNavController
    }

    /**
     * 현재 활성화된 자식 네비게이션 컨트롤러 설정.
     * 중첩된 NavHost에서 자신의 NavController를 등록하기 위해 호출합니다.
     */
    override fun setChildNavController(navController: NavHostController?) {
        this.activeChildNavController = navController
        // 안전한 로깅으로 변경
        val graphRoute = try {
            navController?.graph?.route
        } catch (e: IllegalStateException) {
            // 그래프가 아직 설정되지 않았을 때 발생하는 예외 처리
            null 
        }
        println("NavigationManager: Active Child NavController updated. Current graph route: $graphRoute")
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
     * 직접 NavController를 사용하여 네비게이션을 처리합니다.
     */
    override fun navigate(command: NavigationCommand) {
        Log.d("NavigationManager", "navigate(command: NavigationCommand) called with: $command  ActiveChildNavController: $activeChildNavController ParentNavController: $parentNavController ")
        // 자식 NavController가 있으면 먼저 시도
        activeChildNavController?.let { childNav ->
            Log.d("NavigationManager", "child \n $activeChildNavController \n $childNav ")
            if (processCommandInternal(childNav, command)) {
                Log.d("NavigationManager", "Command processed by activeChildNavController")
                return
            }
            Log.d("NavigationManager", "Command not processed by activeChildNavController, trying parent.")
        }

        // 부모 NavController 사용
        parentNavController?.let { parentNav ->
            Log.d("NavigationManager", "parent \n $parentNavController \n $parentNav ")
            if (processCommandInternal(parentNav, command)) {
                Log.d("NavigationManager", "Command processed by parentNavController")
                return
            }
            Log.e("NavigationManager", "Command not processed by parentNavController either. This might be an issue.")
        } ?: run {
            Log.e("NavigationManager", "No NavController available to process command: $command")
        }
    }
    
    /**
     * NavigationCommand 처리를 실행하는 내부 메서드
     */
    private fun processCommandInternal(navController: NavHostController, command: NavigationCommand): Boolean {
        Log.d("NavigationManager", "processCommandInternal for ${navController.graph.route ?: "unknown graph"} with command: $command")
        try {
            when (command) {
                is NavigationCommand.NavigateToRoute -> {
                    val routeToNavigate = command.route
                    val navOptions = command.navOptions
                    return handleRouteNavigation(navController, routeToNavigate, navOptions)
                }
                is NavigationCommand.NavigateBack -> {
                    return navController.popBackStack()
                }
                is NavigationCommand.NavigateClearingBackStack -> {
                    if (parentNavController == navController) {
                        navController.navigate(command.route) { 
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                        return true
                    }
                    Log.w("NavigationManager", "NavigateClearingBackStack attempted on non-parent controller.")
                    return false
                }
            }
        } catch (e: IllegalStateException) {
            Log.e("NavigationManager", "IllegalStateException in processCommandInternal for ${navController.graph.route ?: "unknown graph"}: ${e.message}", e)
            return false
        } catch (e: IllegalArgumentException) {
            Log.e("NavigationManager", "IllegalArgumentException in processCommandInternal for ${navController.graph.route ?: "unknown graph"}: ${e.message}", e)
            return false
        } catch (e: Exception) {
            Log.e("NavigationManager", "Exception in processCommandInternal for ${navController.graph.route ?: "unknown graph"}: ${e.message}", e)
            return false
        }
    }

    // 경로 이동의 핵심 로직을 담당하는 새로운 private 함수
    private fun handleRouteNavigation(
        navController: NavHostController,
        route: String,
        navOptions: NavOptions?
    ): Boolean {
        Log.d("NavigationManager", "handleRouteNavigation for ${navController.graph.route ?: "unknown graph"} to $route")

        // 1. 현재 NavController에서 해당 route로 직접 이동 시도 (가장 먼저)
        // NavController.navigate()는 경로를 resolving 할 수 있으면 성공함 (중첩 그래프 내부 경로 포함)
        try {
            // navigate 함수는 IllegalArgumentException을 던질 수 있음 (경로 못찾으면)
            navController.navigate(route, navOptions)
            Log.d("NavigationManager", "Successfully navigated to $route with ${navController.graph.route ?: "current controller"}.")
            return true
        } catch (e: IllegalArgumentException) {
            Log.w("NavigationManager", "Direct navigation to $route failed with ${navController.graph.route ?: "current controller"}: ${e.message}")
            // 실패 시 아래의 그래프 전환 로직으로 넘어감
        } catch (e: IllegalStateException) {
            Log.e("NavigationManager", "IllegalStateException during direct navigation to $route: ${e.message}")
            return false // NavController 상태 문제일 수 있으므로 더 이상 진행 안 함
        }

        // 2. 직접 이동 실패했고, 현재 NavController가 parentNavController일 때, 다른 "최상위" 그래프로의 전환 시도
        if (navController == parentNavController) {
            val targetGraphRoot = getGraphRootForRoute(route) // 예: "auth_graph" 또는 "main_host"
            val currentTopLevelGraph = navController.graph.route // 예: "app_root_graph"

            if (targetGraphRoot != null && targetGraphRoot != currentTopLevelGraph) {
                val currentNestedGraph = navController.currentDestination?.parent?.route
                if (targetGraphRoot != currentNestedGraph) {
                    Log.d("NavigationManager", "Transitioning to graph $targetGraphRoot for route $route.")
                    setResult("pending_navigation", route) // 최종 목적지 저장
                    try {
                        navController.navigate(targetGraphRoot, navOptions) // 새 그래프(예: "auth_graph")의 시작점으로 이동
                        return true
                    } catch (e: Exception) {
                        Log.e("NavigationManager", "Failed to navigate to targetGraphRoot $targetGraphRoot: ${e.message}")
                    }
                } else {
                    Log.w("NavigationManager", "Already in target graph $targetGraphRoot, but direct navigation to $route failed earlier.")
                }
            }
        }

        Log.w("NavigationManager", "${navController.graph.route ?: "unknown graph"} cannot handle route $route.")
        return false
    }
    
    // 현재 NavController의 그래프 식별자를 가져오는 보조 함수 (parentNavController.graph.route가 null일 경우 대비)
    private fun getCurrentGraphIdentifier(navController: NavHostController): String? {
        // navController.currentDestination?.parent?.route 또는 다른 방식으로 현재 그래프의 대표 경로를 찾을 수 있음
        // 여기서는 AppRoutes의 구조를 알아야 더 정확하게 구현 가능
        // 임시로 null 반환
        return navController.currentDestination?.parent?.route // 가장 가까운 그래프의 루트일 수 있음
    }

    private fun bundleToMap(bundle: Bundle): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        for (key in bundle.keySet()) {
            bundle.get(key)?.let { map[key] = it }
        }
        return map
    }

    @SuppressLint("RestrictedApi")
    private fun canNavigateDirectly(navController: NavHostController, route: String): Boolean {
        return try {
            val cleanRoute = route.substringBefore("?")
            // 1. ID로 직접 찾아보기 (플레이스홀더 없는 기본 경로에 유용)
            if (navController.graph.findNode(cleanRoute) != null) return true
            Log.d("NavigationManager", "cleanRoute: $cleanRoute")
            Log.d("NavigationManager", "navController.graph.route:  ${navController.graph.route}")
            Log.d("NavigationManager", "navController.graph.nodes: ${navController.graph.nodes}")

            // 2. 그래프 내 모든 Destination의 route와 비교 (플레이스홀더 포함 경로 비교에 필요)
            // NavGraph.nodes는 SparseArrayCompat<NavDestination> 타입
            navController.graph.nodes.forEach { i , destination ->
                if (destination.route == cleanRoute) {
                    return true
                }
            }

            // 중첩된 그래프 안의 destination도 찾아야 할 수 있음
            // 현재 NavController의 그래프에 직접 속한 노드들만 검사함
            // 필요하다면 재귀적으로 모든 중첩 그래프를 탐색하는 로직 추가 가능
            false
        } catch (e: Exception) {
            Log.w("NavigationManager", "Exception in canNavigateDirectly for route $route: ${e.message}")
            false
        }
    }

    private fun getGraphRootForRoute(route: String): String? {
        // AppRoutes.kt 파일 내용에 따라 매우 정교하게 작성되어야 함.
        // 각 최상위 그래프(Auth, Main, Project, Chat 등)의 대표 경로(시작점)를 반환해야 함.
        val cleanRoute = route.substringBefore("?") // 쿼리 파라미터 제거

        if (cleanRoute.startsWith("auth")) return AppRoutes.Auth.Graph.path
        // Main 그래프는 여러 진입점이 있을 수 있으므로, 경로 패턴을 잘 확인해야 함.
        // 예: "home", "friends_list", "calendar_24h/2023/10/26" 등은 모두 Main 그래프 소속일 수 있음.
        val mainGraphPrefixes = listOf("main_host", "home", "friends", "profile", "schedule") // AppRoutes.Main 하위 경로들의 대표 prefix
        if (mainGraphPrefixes.any { cleanRoute.startsWith(it) }) return AppRoutes.Main.ROOT
        
        if (cleanRoute.startsWith("project")) return AppRoutes.Project.Graph.path 
        if (cleanRoute.startsWith("chat")) return AppRoutes.Chat.Graph.path
        // TODO: 다른 최상위 그래프들에 대한 매핑 추가 (예: Settings, User Profile 등)
        
        Log.w("NavigationManager", "Cannot determine graph root for route: $route. Add mapping in getGraphRootForRoute.")
        return null // 매핑되는 그래프가 없으면 null 반환
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
     * NavigationCommand를 사용하여 백스택을 모두 비우고 특정 경로로 이동
     */
    override fun navigateClearingBackStack(command: NavigationCommand.NavigateClearingBackStack) {
        navigate(command as NavigationCommand)
    }

    /**
     * 특정 목적지가 현재 활성화되어 있는지 확인
     * 자식 -> 부모 순서로 확인합니다.
     */
    fun isDestinationActive(destination: NavDestination): Boolean {
        val routePath = destination.route
        
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
        resultStore[key] = result
        val targetNavController = activeChildNavController ?: parentNavController
        try {
            targetNavController?.previousBackStackEntry?.savedStateHandle?.set(key, result)
        } catch (e: IllegalStateException) {
            println("NavigationManager Warning: Could not set result to previousBackStackEntry's SavedStateHandle for key '$key'. ${e.message}")
        }
        scope.launch {
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
        if (resultStore.containsKey(key)) {
            val result = resultStore[key] as? T
            resultStore.remove(key)
            println("NavigationManager: Found result in internal store for key '$key'.")
            return result
        }
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
     * NavigationCommand를 사용하여 탭 내부에서 프로젝트 상세 화면으로 이동
     */
    override fun navigateToProjectDetailsNested(projectId: String, command: NavigationCommand.NavigateToRoute) {
        navigate(command as NavigationCommand)
    }

    /**
     * 화면 상태 저장
     * 탭 전환 시 화면 상태를 저장하는 데 사용됩니다.
     *
     * @param screenRoute 상태를 저장할 화면의 라우트
     * @param state 저장할 상태 번들
     */
    override fun saveScreenState(screenRoute: String, state: Bundle) {
        println("NavigationManager: Saving screen state for route: $screenRoute")
        screenStates[screenRoute] = state
    }
    
    /**
     * 화면 상태 복원
     * 탭 전환 시 이전에 저장된 화면 상태를 복원하는 데 사용됩니다.
     *
     * @param screenRoute 상태를 복원할 화면의 라우트
     * @return 저장된 상태 번들 (없는 경우 null)
     */
    override fun getScreenState(screenRoute: String): Bundle? {
        println("NavigationManager: Getting screen state for route: $screenRoute")
        return screenStates[screenRoute]
    }

    /**
     * 경로가 유효한지 확인합니다.
     * 현재 등록된 NavController에서 가능한 경로인지 검사합니다.
     * 
     * 참고: 이 메서드는 정확한 유효성 검사가 아닌 기본적인 검사만 수행합니다.
     * 실제 네비게이션은 navController.navigate 호출 시 검증됩니다.
     */
    override fun isValidRoute(routePath: String): Boolean {
        if (routePath.isBlank()) return false
        // canNavigateDirectly 또는 getGraphRootForRoute 등을 활용하여 더 정교한 검증 가능
        // 임시로 true 반환 (실제로는 parentNavController나 activeChildNavController에서 경로 존재 여부 확인 필요)
        return parentNavController?.let { 
            canNavigateDirectly(it, routePath) || 
            getGraphRootForRoute(routePath) != null 
        } ?: false || activeChildNavController?.let {
             canNavigateDirectly(it, routePath) 
        } ?: false
    }

}
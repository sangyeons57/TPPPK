package com.example.teamnovapersonalprojectprojectingkotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.core_logging.SentryUtil
import com.example.data.util.FirebaseUtil
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.core_navigation.core.NavigationCommand
import com.example.core_navigation.graph.AppNavigationGraph
import com.example.core_navigation.destination.AppRoutes
import dagger.hilt.android.AndroidEntryPoint
import io.sentry.ITransaction
import io.sentry.Sentry
import io.sentry.SpanStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint // Hilt 사용 시 Activity에 추가
class MainActivity : ComponentActivity() {
    
    // 앱 성능 측정용 트랜잭션
    private var appStartTransaction: ITransaction? = null
    
    @Inject
    lateinit var navigationHandler: ComposeNavigationHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        // 앱 시작 성능 측정 시작
        appStartTransaction = Sentry.startTransaction("app.startup", "startup")
        
        super.onCreate(savedInstanceState)
        FirebaseUtil.initializeApp(this) // 명시적 초기화
        enableEdgeToEdge() // Edge-to-edge 디스플레이 활성화 (선택적)
        
        // Sentry 기본 테스트 예외 전송
        try {
             throw Exception("Sentry Basic Test Exception from MainActivity onCreate")
        } catch (e: Exception) {
             Sentry.captureException(e)
             println(">>> Sent Sentry test exception") // Logcat 확인용
        }
        
        // 앱 시작 이벤트 기록
        SentryUtil.addBreadcrumb("lifecycle", "MainActivity onCreate")
        
        setContent {
            // UI 렌더링 성능 측정
            val uiRenderSpan = appStartTransaction?.startChild("ui.render", "Initial UI Rendering")
            
            TeamnovaPersonalProjectProjectingKotlinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    navigationHandler.setNavController(navController) // Main NavController 등록
                    setupNavigationTracking(navController)
                    
                    // Collect navigation commands
                    LaunchedEffect(navController, navigationHandler) {
                        navigationHandler.navigationCommands.collectLatest { command ->
                            // 현재 활성 컨트롤러 결정 (자식 우선)
                            val controllerToUse = navigationHandler.getActiveChildNavController() ?: navController
                            
                            try {
                                when (command) {
                                    is NavigationCommand.NavigateToRoute -> {
                                        // TODO: Determine target controller more precisely if needed
                                        // For now, assume parent controller or specific tab nav handles child nav
                                        navController.navigate(command.route, command.navOptions)
                                    }
                                    is NavigationCommand.NavigateUp -> {
                                        navigationHandler.navigateBack() // Use manager's back logic
                                    }
                                    is NavigationCommand.PopBackStack -> {
                                        controllerToUse.popBackStack()
                                    }
                                    is NavigationCommand.NavigateToTab -> {
                                        // Tab navigation typically handled within MainScreen's NavHost
                                        // Or needs specific logic targeting the nested NavController
                                        // For now, attempt navigation on the main controller
                                        // This might need refinement based on how tabs are structured
                                        navController.navigate(command.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = command.saveState
                                            }
                                            launchSingleTop = true
                                            restoreState = command.restoreState
                                        }
                                    }
                                    is NavigationCommand.NavigateClearingBackStack -> {
                                        navController.navigate(command.route) {
                                            popUpTo(navController.graph.id) { inclusive = true }
                                        }
                                    }
                                    is NavigationCommand.NavigateToNestedGraph -> {
                                        // Assumes parentRoute is a destination in the main graph
                                        // that hosts the nested graph
                                        navController.navigate(command.parentRoute)
                                        // Navigation to childRoute happens within the nested graph
                                    }
                                }
                            } catch (e: Exception) {
                                // Log navigation errors
                                SentryUtil.logWarning("Navigation Error: ${e.message}", mapOf("command" to command.toString()))
                                println("Navigation Error: ${e.message} for command: $command")
                            }
                        }
                    }
                    
                    AppNavigationGraph(
                        navController = navController,
                        navigationManager = navigationHandler,
                        startDestination = decideStartDestination()
                    )
                }
            }
            
            uiRenderSpan?.finish(SpanStatus.OK)
            
            lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    appStartTransaction?.finish(SpanStatus.OK)
                }
            }
        }
    }
    
    /**
     * 네비게이션 컨트롤러에 Sentry 추적 설정
     */
    private fun setupNavigationTracking(navController: NavController) {
        // 네비게이션 변경 추적
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Sentry에 화면 전환 기록
            SentryUtil.addBreadcrumb(
                "navigation", 
                "Screen changed to: ${destination.route ?: "unknown"}"
            )
        }
        
        // 세션 추적 시작
        SentryUtil.startSessionReplay()
    }
    
    override fun onResume() {
        super.onResume()
        // 앱이 다시 활성화될 때 세션 추적 시작
        SentryUtil.addBreadcrumb("lifecycle", "MainActivity onResume")
    }
    
    override fun onPause() {
        // 앱이 백그라운드로 갈 때 이벤트 기록
        SentryUtil.addBreadcrumb("lifecycle", "MainActivity onPause")
        super.onPause()
    }
    
    override fun onDestroy() {
        // 앱 종료 시 마지막 트랜잭션 정리
        SentryUtil.addBreadcrumb("lifecycle", "MainActivity onDestroy")
        SentryUtil.stopSessionReplay()
        super.onDestroy()
    }

    /**
     * 앱의 시작 목적지를 결정합니다.
     * TODO: 실제 앱 로직(예: 로그인 상태)에 따라 수정 필요
     */
    private fun decideStartDestination(): String {
        // val isLoggedIn = false // 예시: 사용자 로그인 상태 확인 로직
        // return if (isLoggedIn) AppRoutes.Main.ROOT else AppRoutes.Auth.SPLASH // 새 AppRoutes 사용
        return AppRoutes.Auth.SPLASH // 인증 플로우의 스플래시 화면으로 시작
    }
}
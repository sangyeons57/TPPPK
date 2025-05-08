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
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.core_logging.SentryUtil
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.teamnovapersonalprojectprojectingkotlin.navigation.AppNavigationGraph
import com.example.core_navigation.destination.AppRoutes
import dagger.hilt.android.AndroidEntryPoint
import io.sentry.ITransaction
import io.sentry.Sentry
import io.sentry.SpanStatus
import javax.inject.Inject
import androidx.compose.runtime.LaunchedEffect

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
                    
                    // NavigationCommand 처리는 AppNavigationGraph에서 담당하므로 여기서 제거
                    
                    AppNavigationGraph(
                        navController = navController,
                        navigationHandler = navigationHandler,
                        startDestination = decideStartDestination()
                    )
                }
            }
            
            uiRenderSpan?.finish(SpanStatus.OK)

            // LaunchedEffect를 사용하여 생명주기 인식 코루틴 관리
            LaunchedEffect(key1 = lifecycle) { // lifecycle을 키로 사용하여 Activity 생명주기와 연동
                // 이 코루틴은 LaunchedEffect가 컴포지션에 있는 동안 실행됩니다.
                // lifecycle은 Activity에서 가져옵니다.
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    appStartTransaction?.finish(SpanStatus.OK)
                    // Sentry 트랜잭션의 finish()는 여러 번 호출되어도 일반적으로 안전하지만,
                    // 필요하다면 한 번만 호출되도록 플래그 관리를 추가할 수 있습니다.
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
                "Screen changed to: ${destination.toString()}"
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
        // return if (isLoggedIn) AppRoutes.Main.ROOT else AppRoutes.Auth.Graph.path
        return AppRoutes.Auth.Graph.path // Auth 네비게이션 그래프 자체를 시작점으로 지정
    }
}
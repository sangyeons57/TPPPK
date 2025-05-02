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
import com.example.navigation.SentryNavigationTracker
import dagger.hilt.android.AndroidEntryPoint
import io.sentry.ITransaction
import io.sentry.Sentry
import io.sentry.SpanStatus
import kotlinx.coroutines.launch

@AndroidEntryPoint // Hilt 사용 시 Activity에 추가
class MainActivity : ComponentActivity() {
    
    // 앱 성능 측정용 트랜잭션
    private var appStartTransaction: ITransaction? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // 앱 시작 성능 측정 시작
        appStartTransaction = Sentry.startTransaction("app.startup", "startup")
        
        super.onCreate(savedInstanceState)
        FirebaseUtil.initializeApp(this) // 명시적 초기화
        enableEdgeToEdge() // Edge-to-edge 디스플레이 활성화 (선택적)
        
        // 앱 시작 이벤트 기록
        SentryUtil.addBreadcrumb("lifecycle", "MainActivity onCreate")
        
        setContent {
            // UI 렌더링 성능 측정
            val uiRenderSpan = appStartTransaction?.startChild("ui.render", "Initial UI Rendering")
            
            TeamnovaPersonalProjectProjectingKotlinTheme {
                // 앱 전체 배경색을 가진 Surface (선택적)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // 네비게이션 상태 변경 추적
                    setupNavigationTracking(navController)
                    
                    // 앱 전체 네비게이션 시작
                    AppNavigation(navController)
                }
            }
            
            // UI 렌더링 완료 표시
            uiRenderSpan?.finish(SpanStatus.OK)
            
            lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                    appStartTransaction?.finish(SpanStatus.OK)
                }
            }
        }
    }
    
    /**
     * 네비게이션 컨트롤러에 Sentry 추적 설정
     */
    private fun setupNavigationTracking(navController: NavController) {
        // Sentry 네비게이션 추적 설정 (성능 측정 및 사용자 경로)
        SentryNavigationTracker.registerNavigationListener(navController)
        
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
        SentryNavigationTracker.finishTracking()
        SentryUtil.stopSessionReplay()
        super.onDestroy()
    }
}
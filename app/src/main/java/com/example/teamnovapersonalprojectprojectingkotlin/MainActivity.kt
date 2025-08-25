package com.example.teamnovapersonalprojectprojectingkotlin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.core_common.constants.Constants
import com.example.core_navigation.core.DevMenuRoute
import com.example.core_navigation.core.MainContainerRoute
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.core.SplashRoute
import com.example.core_navigation.core.TypeSafeRouteCompat.toAppRoutePath
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.teamnovapersonalprojectprojectingkotlin.fcm.FcmTokenManager
import com.example.teamnovapersonalprojectprojectingkotlin.navigation.AppNavigationGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// FCM 알림 데이터를 담는 데이터 클래스
data class FcmNotificationData(
    val notificationType: String,
    val messageId: String,
    val channelId: String,
    val projectId: String? = null
)

@AndroidEntryPoint // Hilt 사용 시 Activity에 추가
class MainActivity : ComponentActivity() {
    
    
    @Inject
    lateinit var navigationManger: NavigationManger
    
    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    private var backPressedTime: Long = 0
    private var backToast: Toast? = null

    // FCM 알림으로부터 추출된 데이터를 저장
    private var pendingFcmData: FcmNotificationData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // 앱 시작 성능 측정 시작
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Edge-to-edge 디스플레이 활성화 (선택적)

        // 초기 Intent 처리 (앱이 처음 시작될 때)
        handleIntent(intent)
        
        setupBackPressHandler()

        // FCM 초기화는 Application 클래스에서 처리됨 (ANR 방지)
        // fcmTokenManager.initialize() - 제거됨
        
        setContent {

            // NavController 생성 및 AppNavigator에 설정
            val navController = rememberNavController()

            // NavigationHandler에 최상위 NavController 설정 (한 번만 호출)
            LaunchedEffect(navController) {
                navigationManger.setNavController(navController)
                setupNavigationTracking(navController)
            }

            // FCM 알림 데이터 처리를 위한 LaunchedEffect
            LaunchedEffect(pendingFcmData) {
                pendingFcmData?.let { fcmData ->
                    try {
                        when (fcmData.notificationType) {
                            "mention" -> {
                                // 채널 ID 유효성 검사
                                if (fcmData.channelId.isBlank()) {
                                    return@LaunchedEffect
                                }

                                // 딥링크를 사용하여 채팅방으로 직접 이동
                                val deepLinkUri = Uri.parse("app://channel/${fcmData.channelId}")

                                // 네비게이션 시도 및 결과 확인
                                try {
                                    navController.navigate(deepLinkUri)
                                } catch (e: Exception) {
                                    // 네비게이션 실패 시 메인 화면으로 이동
                                    navController.navigate("main") {
                                        popUpTo("main") { inclusive = true }
                                    }
                                }
                            }

                            else -> {
                                // 알 수 없는 FCM 알림 타입
                            }
                        }
                    } catch (e: Exception) {
                        // FCM 알림 데이터 처리 실패
                    } finally {
                        // FCM 데이터 처리 완료 후 항상 초기화
                        pendingFcmData = null
                    }
                }
            }

            TeamnovaPersonalProjectProjectingKotlinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationGraph(
                        navController = navController,
                        navigationManger = navigationManger,
                        startDestination = decideStartDestination()
                    )
                }
            }
            

            // LaunchedEffect를 사용하여 생명주기 인식 코루틴 관리
            LaunchedEffect(key1 = lifecycle) { // lifecycle을 키로 사용하여 Activity 생명주기와 연동
                // 이 코루틴은 LaunchedEffect가 컴포지션에 있는 동안 실행됩니다.
                // lifecycle은 Activity에서 가져옵니다.
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    // App resumed - no performance tracking needed
                }
            }
        }
    }

    private fun setupBackPressHandler() {
        val callback = object : OnBackPressedCallback(true) {
            private var lastBackNavigateTime: Long = 0L // 추가: 연속 뒤로가기 방지용 타임스탬프

            override fun handleOnBackPressed() {
                val now = System.currentTimeMillis()

                // 최근에 처리된 뒤로가기라면 무시 (모든 화면 공통)
                if (now - lastBackNavigateTime < Constants.Navigation.DEBOUNCE_TIMEOUT_MS) {
                    return // 입력 무시 → 연속 팝 방지
                }
                lastBackNavigateTime = now // 타임스탬프 갱신

                val currentRoute = navigationManger.getNavController()?.currentDestination?.route
                val isMainScreen = currentRoute?.startsWith(MainContainerRoute.toAppRoutePath()) ?: false

                if (isMainScreen) {
                    // 메인 화면에서는 "두 번 눌러서 종료" 로직 유지
                    if (now > backPressedTime + Constants.Navigation.EXIT_APP_TIMEOUT_MS) {
                        backPressedTime = now
                        backToast?.cancel()
                        backToast = Toast.makeText(this@MainActivity, "한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT)
                        backToast?.show()
                        return
                    }
                    if (now <= backPressedTime + Constants.Navigation.EXIT_APP_TIMEOUT_MS) {
                        backToast?.cancel()
                        finish()
                    }
                } else {
                    // 메인 이외 화면 → 한 번만 popBackStack 수행 (위의 debounce로 연속 입력 방지)
                    if (!navigationManger.navigateBack()) {
                        // 더 이상 뒤로 갈 곳이 없을 때의 예외 처리
                        // 인증/스플래시가 아닌데 back stack 이 비었으면 앱 종료
                        if (currentRoute != SplashRoute.toAppRoutePath() && currentRoute != "auth") {
                            finish()
                        } else {
                            // Splash, Auth 화면이라면 그대로 종료
                            finish()
                        }
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.let { handleIntent(it) }
    }
    
    /**
     * Intent를 처리하여 FCM 알림 데이터를 추출합니다.
     */
    private fun handleIntent(intent: Intent) {
        // FCM 알림 데이터 처리
        try {
            extractFcmNotificationData(intent)?.let { fcmData ->
                pendingFcmData = fcmData
                // NavigationManger가 초기화된 후 네비게이션 수행
                if (::navigationManger.isInitialized) {
                    navigateToFcmTarget(fcmData)
                }
            }
        } catch (e: Exception) {
            // FCM 데이터 처리 실패 로깅
        }
    }
    
    /**
     * Intent에서 FCM 알림 데이터를 추출합니다.
     */
    private fun extractFcmNotificationData(intent: Intent): FcmNotificationData? {
        return try {
            val notificationType = intent.getStringExtra("notification_type")
            val messageId = intent.getStringExtra("message_id")
            val channelId = intent.getStringExtra("channel_id")
            val projectId = intent.getStringExtra("project_id")

            // 필수 데이터 유효성 검사
            if (notificationType.isNullOrBlank() ||
                messageId.isNullOrBlank() ||
                channelId.isNullOrBlank()
            ) {
                return null
            }

            FcmNotificationData(
                notificationType = notificationType,
                messageId = messageId,
                channelId = channelId,
                projectId = projectId
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * FCM 알림 목적지로 네비게이션합니다.
     * LaunchedEffect 내부에서 사용하기 위해 pending 데이터로 처리합니다.
     */
    private fun navigateToFcmTarget(fcmData: FcmNotificationData) {
        // 이 메소드는 NavigationManger가 아직 초기화되지 않았을 때 호출될 수 있으므로
        // 실제 네비게이션은 LaunchedEffect에서 pendingFcmData를 확인하여 처리합니다.
        // 여기서는 로깅만 수행
    }
    
    /**
     * Navigation controller setup
     */
    private fun setupNavigationTracking(navController: NavController) {
        // Navigation change tracking (placeholder)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Navigation tracking logic can be added here
        }
    }
    
    override fun onResume() {
        super.onResume()
        // App resumed
    }
    
    override fun onPause() {
        // App paused
        super.onPause()
    }
    
    override fun onDestroy() {
        // Cleanup FCM token management
        fcmTokenManager.cleanup()
        
        // App destroyed
        super.onDestroy()
    }

    /**
     * 앱의 시작 목적지를 결정합니다.
     * FCM 알림 데이터는 LaunchedEffect에서 별도 처리하므로 기본 로직만 사용합니다.
     */
    private fun decideStartDestination(): String {
        // 기본 로직: 로그인 상태에 따라 결정
        // val isLoggedIn = false // 예시: 사용자 로그인 상태 확인 로직
        // return if (isLoggedIn) "main" else "auth"

        // 임시: WebSocket 테스트를 위해 DevMenu로 시작
        val destination = DevMenuRoute.toAppRoutePath()
        return destination
        // return "auth" // Auth 네비게이션 그래프 자체를 시작점으로 지정
    }
}
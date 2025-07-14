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
import com.example.teamnovapersonalprojectprojectingkotlin.navigation.AppNavigationGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint // Hilt 사용 시 Activity에 추가
class MainActivity : ComponentActivity() {
    
    
    @Inject
    lateinit var navigationManger: NavigationManger

    private var backPressedTime: Long = 0
    private var backToast: Toast? = null
    
    // 딥링크로부터 추출된 초대 코드를 저장
    private var pendingInviteCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // 앱 시작 성능 측정 시작
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Edge-to-edge 디스플레이 활성화 (선택적)

        // 초기 Intent 처리 (앱이 처음 시작될 때)
        handleIntent(intent)
        
        setupBackPressHandler()
        
        
        setContent {
            
            // NavController 생성 및 AppNavigator에 설정
            val navController = rememberNavController()
            
            // NavigationHandler에 최상위 NavController 설정 (한 번만 호출)
            LaunchedEffect(navController) {
                navigationManger.setNavController(navController)
                setupNavigationTracking(navController)
            }

            TeamnovaPersonalProjectProjectingKotlinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationGraph(
                        navController = navController,
                        navigationManger = navigationManger,
                        startDestination = decideStartDestination(),
                        pendingInviteCode = pendingInviteCode
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
     * Intent를 처리하여 딥링크에서 초대 코드를 추출합니다.
     */
    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                val inviteCode = extractInviteCodeFromUri(uri)
                if (inviteCode != null) {
                    pendingInviteCode = inviteCode
                    // NavigationManger가 초기화된 후 네비게이션 수행
                    if (::navigationManger.isInitialized) {
                        navigateToJoinProject(inviteCode)
                    }
                }
            }
        }
    }
    
    /**
     * URI에서 초대 코드를 추출합니다.
     * 
     * 지원하는 URI 형식:
     * - https://tpppk.app/invite/{code}
     * - tpppk://invite/{code}
     */
    private fun extractInviteCodeFromUri(uri: Uri): String? {
        return when {
            // HTTPS 딥링크: https://tpppk.app/invite/{code}
            uri.scheme == "https" && uri.host == "tpppk.app" -> {
                val pathSegments = uri.pathSegments
                if (pathSegments.size >= 2 && pathSegments[0] == "invite") {
                    pathSegments[1]
                } else null
            }
            // 커스텀 스킴: tpppk://invite/{code}
            uri.scheme == "tpppk" && uri.host == "invite" -> {
                val pathSegments = uri.pathSegments
                if (pathSegments.isNotEmpty()) {
                    pathSegments[0]
                } else {
                    // Query parameter로 전달된 경우: tpppk://invite?code={code}
                    uri.getQueryParameter("code")
                }
            }
            else -> null
        }
    }
    
    /**
     * 초대 코드를 사용하여 프로젝트 참여 화면으로 네비게이션합니다.
     */
    private fun navigateToJoinProject(inviteCode: String) {
        navigationManger.navigateToJoinProjectWithInviteCode(inviteCode)
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
        // App destroyed
        super.onDestroy()
    }

    /**
     * 앱의 시작 목적지를 결정합니다.
     * TODO: 실제 앱 로직(예: 로그인 상태)에 따라 수정 필요
     */
    private fun decideStartDestination(): String {
        // val isLoggedIn = false // 예시: 사용자 로그인 상태 확인 로직
        // return if (isLoggedIn) "main" else "auth"
//        return DevMenuRoute.toAppRoutePath()
         return "auth" // Auth 네비게이션 그래프 자체를 시작점으로 지정
    }
}
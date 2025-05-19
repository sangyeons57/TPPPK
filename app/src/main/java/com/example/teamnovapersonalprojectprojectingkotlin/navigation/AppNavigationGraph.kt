package com.example.teamnovapersonalprojectprojectingkotlin.navigation

import android.util.Log
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core_navigation.destination.AppRoutes
import com.example.feature_auth.ui.LoginScreen
import com.example.feature_auth.ui.SplashScreen
import com.example.feature_auth.viewmodel.LoginViewModel
import com.example.feature_auth.viewmodel.SplashViewModel
import com.example.feature_main.MainContainerScreen
import com.example.feature_main.viewmodel.HomeViewModel
import com.example.feature_project.ui.AddProjectScreen
import com.example.feature_project.ui.JoinProjectScreen
import com.example.feature_project.viewmodel.AddProjectViewModel
import com.example.feature_project.viewmodel.JoinProjectViewModel
import com.example.core_navigation.core.AppNavigator
import com.example.core_navigation.core.NavDestination
import com.example.core_navigation.core.NavigationCommand
import com.example.feature_auth.ui.FindPasswordScreen
import com.example.feature_auth.ui.PrivacyPolicyScreen
import com.example.feature_auth.ui.SignUpScreen
import com.example.feature_auth.ui.TermsOfServiceScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

/**
 * 앱의 최상위 네비게이션 그래프
 * 
 * 앱의 모든 최상위 경로들을 정의하고, NavigationManager와 연결하여 탐색을 처리합니다.
 * 중첩 네비게이션 구조를 사용하여 기능별로 화면들을 그룹화합니다.
 */
@Composable
fun AppNavigationGraph(
    navController: NavHostController,
    appNavigator: AppNavigator,
    startDestination: String = AppRoutes.Auth.Graph.path
) {
    // NavigationManager는 navController를 직접 사용하여 네비게이션 처리
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 인증 관련 네비게이션 그래프 (로그인, 가입 등)
        authGraph(appNavigator)
        
        // 메인 화면 (MainContainerScreen이 자체적으로 탭 네비게이션 처리)
        mainGraph(appNavigator)
        
        // 독립적인 화면들의 그래프 - 메인 탭 외부에서 접근하는 화면들
        projectGraph(appNavigator)
        chatGraph(appNavigator)
        scheduleGraph(appNavigator)
    }
}

/**
 * 인증 관련 화면들의 네비게이션 그래프
 */
fun NavGraphBuilder.authGraph(appNavigator: AppNavigator) {
    navigation(
        route = AppRoutes.Auth.Graph.path,
        startDestination = AppRoutes.Auth.Splash.path
    ) {

        composable(AppRoutes.Auth.Splash.path) {
            Log.d("Splash", "Splash Screen")
            // Register this NavController when this NavHost is active
            SplashScreen(appNavigator = appNavigator)
        }
        
        composable(AppRoutes.Auth.Login.path) {
            Log.d("Login", "Login Screen")
            LoginScreen(appNavigator = appNavigator)
        }
        
        // 회원가입 화면 추가
        composable(AppRoutes.Auth.SignUp.path) {
            Log.d("SignUp", "SignUp Screen")
            // viewModel은 나중에 추가
            SignUpScreen(
                appNavigator = appNavigator
            )
        }
        
        // 비밀번호 찾기 화면 추가
        composable(AppRoutes.Auth.FindPassword.path) {
            Log.d("FindPassword", "FindPassword Screen")
            FindPasswordScreen(appNavigator = appNavigator)
        }

        // 서비스 이용약관 화면 추가
        composable(AppRoutes.Auth.TermsOfService.path) {
            Log.d("TermsOfService", "TermsOfService Screen")
            TermsOfServiceScreen(appNavigator = appNavigator)
        }

        // 개인정보 처리방침 화면 추가
        composable(AppRoutes.Auth.PrivacyPolicy.path) {
            Log.d("PrivacyPolicy", "PrivacyPolicy Screen")
            PrivacyPolicyScreen(appNavigator = appNavigator)
        }
    }
}


/**
 * 메인 화면 그래프 - MainContainerScreen이 자체적으로 탭 네비게이션 처리
 */
fun NavGraphBuilder.mainGraph(appNavigator: AppNavigator) {
    composable(AppRoutes.Main.ROOT) {
        MainContainerScreen(appNavigator = appNavigator)
    }
}

/**
 * 프로젝트 관련 화면들의 네비게이션 그래프
 */
fun NavGraphBuilder.projectGraph(appNavigator: AppNavigator) {
    navigation(
        route = AppRoutes.Project.Graph.path,
        startDestination = AppRoutes.Project.ADD
    ) {
        // 프로젝트 생성 화면
        composable(AppRoutes.Project.ADD) {
            val viewModel = hiltViewModel<AddProjectViewModel>()
            AddProjectScreen(appNavigator)
        }
        
        // 프로젝트 참가 화면
        composable(
            route = AppRoutes.Project.JOIN,
            arguments = listOf(
                navArgument(AppRoutes.Project.ARG_PROJECT_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            val viewModel = hiltViewModel<JoinProjectViewModel>()
            JoinProjectScreen(appNavigator)
        }
        
        // 프로젝트 상세 화면 - 메인 탭 외부에서도 접근 가능
        composable(
            route = AppRoutes.Project.detailRoute(),
            arguments = listOf(
                navArgument(AppRoutes.Project.ARG_PROJECT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(AppRoutes.Project.ARG_PROJECT_ID) ?: ""
            
            // HomeViewModel을 Composable 컨텍스트에서 가져옵니다.
            val homeViewModelStoreOwner = remember(backStackEntry) {
                appNavigator.getNavController()!!.getBackStackEntry(AppRoutes.Main.ROOT)
            }
            val homeViewModel = hiltViewModel<HomeViewModel>(homeViewModelStoreOwner)

            LaunchedEffect(projectId, homeViewModel) {
                // 먼저 메인 화면으로 이동 (Home 탭이 기본 선택됨)
                appNavigator.navigateClearingBackStack(NavigationCommand.NavigateClearingBackStack(
                    NavDestination.fromRoute(AppRoutes.Main.ROOT)))
                
                // 지연을 주어 UI가 업데이트될 시간을 확보
                delay(100)
                
                // Home 화면에서 프로젝트 선택
                homeViewModel.onProjectClick(projectId)
            }
            
            // 리다이렉트 중 로딩 표시
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

/**
 * 채팅 관련 화면들의 네비게이션 그래프
 */
fun NavGraphBuilder.chatGraph(appNavigator: AppNavigator) {
    navigation(
        route = AppRoutes.Chat.Graph.path,
        startDestination = AppRoutes.Chat.route
    ) {
        // 채팅 화면
        composable(
            route = AppRoutes.Chat.route,
            arguments = AppRoutes.Chat.arguments
        ) {
            // TODO: 실제 ChatScreen Composable 구현 필요
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chat Screen Placeholder\nChannel ID: ${it.arguments?.getString(AppRoutes.Chat.ARG_CHANNEL_ID)}\nMessage ID: ${it.arguments?.getString(AppRoutes.Chat.ARG_MESSAGE_ID)}")
            }
        }
    }
}

/**
 * 일정 관련 화면들의 네비게이션 그래프
 */
fun NavGraphBuilder.scheduleGraph(appNavigator: AppNavigator) {
    navigation(
        route = AppRoutes.Main.Calendar.Graph.path,
        startDestination = AppRoutes.Main.Calendar.calendar24HourRoute()
    ) {
        // 24시간 캘린더 화면
        composable(
            route = AppRoutes.Main.Calendar.calendar24HourRoute(),
            arguments = AppRoutes.Main.Calendar.calendar24HourArguments
        ) { backStackEntry ->
            val year = backStackEntry.arguments?.getInt(AppRoutes.Main.Calendar.ARG_YEAR) ?: 0
            val month = backStackEntry.arguments?.getInt(AppRoutes.Main.Calendar.ARG_MONTH) ?: 0
            val day = backStackEntry.arguments?.getInt(AppRoutes.Main.Calendar.ARG_DAY) ?: 0
            
            com.example.feature_schedule.ui.Calendar24HourScreen(
                appNavigator = appNavigator,
            )
        }
        
        // 일정 추가 화면
        composable(
            route = AppRoutes.Main.Calendar.addScheduleRoute(),
            arguments = AppRoutes.Main.Calendar.addScheduleArguments
        ) { backStackEntry ->
            val year = backStackEntry.arguments?.getInt(AppRoutes.Main.Calendar.ARG_YEAR) ?: 0
            val month = backStackEntry.arguments?.getInt(AppRoutes.Main.Calendar.ARG_MONTH) ?: 0
            val day = backStackEntry.arguments?.getInt(AppRoutes.Main.Calendar.ARG_DAY) ?: 0
            
            com.example.feature_schedule.ui.AddScheduleScreen(
                appNavigator = appNavigator
            )
        }
        
        // 일정 상세 화면
        composable(
            route = AppRoutes.Main.Calendar.scheduleDetailRoute(),
            arguments = AppRoutes.Main.Calendar.scheduleDetailArguments
        ) { backStackEntry ->
            val scheduleId = backStackEntry.arguments?.getString(AppRoutes.Main.Calendar.ARG_SCHEDULE_ID) ?: ""
            
            com.example.feature_schedule.ui.ScheduleDetailScreen(
                appNavigator = appNavigator,
            )
        }
        
        // 일정 수정 화면
        composable(
            route = AppRoutes.Main.Calendar.editScheduleRoute(),
            arguments = AppRoutes.Main.Calendar.editScheduleArguments
        ) { backStackEntry ->
            val scheduleId = backStackEntry.arguments?.getString(AppRoutes.Main.Calendar.ARG_SCHEDULE_ID) ?: ""
            
            com.example.feature_schedule.ui.EditScheduleScreen(
                appNavigator = appNavigator,
            )
        }
    }
} 
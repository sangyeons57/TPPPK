package com.example.teamnovapersonalprojectprojectingkotlin.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
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
import androidx.navigation.navArgument
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.core_navigation.core.NavigationCommand
import com.example.feature_auth.ui.LoginScreen
import com.example.feature_auth.ui.SplashScreen
import com.example.feature_auth.viewmodel.LoginViewModel
import com.example.feature_auth.viewmodel.SplashViewModel
import com.example.feature_main.MainScreen
import com.example.feature_main.viewmodel.HomeViewModel
import com.example.feature_project.ui.AddProjectScreen
import com.example.feature_project.ui.JoinProjectScreen
import com.example.feature_project.viewmodel.AddProjectViewModel
import com.example.feature_project.viewmodel.JoinProjectViewModel
import androidx.compose.material3.Text

// Screen Composable imports will be needed here from all feature modules
// Example:
// import com.example.feature_auth.ui.SplashScreen
// import com.example.feature_main.MainScreen
// ... etc.

/**
 * 앱의 최상위 네비게이션 그래프
 * 
 * 앱의 모든 최상위 경로들을 정의하고, NavigationManager와 연결하여 탐색을 처리합니다.
 */
@Composable
fun AppNavigationGraph(
    navController: NavHostController,
    navigationHandler: ComposeNavigationHandler,
    startDestination: String = AppRoutes.Auth.Graph.path
) {
    // NavigationHandler에 최상위 NavController 설정 (한 번만 호출)
    LaunchedEffect(Unit) {
        navigationHandler.setNavController(navController)
    }
    
    // NavigationHandler로부터 명령을 수신하고 처리
    LaunchedEffect(navigationHandler) {
        navigationHandler.navigationCommands.collect { command ->
            processNavigationCommand(command, navController)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 인증 관련 네비게이션 그래프 (로그인, 가입 등)
        authGraph(navController, navigationHandler)
        
        // 메인 화면 (중첩 네비게이션 사용)
        composable(AppRoutes.Main.ROOT) {
            MainScreen(navigationHandler = navigationHandler)
        }
        
        // 독립적인 화면들 - 메인 탭 외부에서 접근하는 화면들
        standaloneScreensGraph(navController, navigationHandler)
    }
}

/**
 * 인증 관련 화면들의 네비게이션 그래프
 */
private fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    navigationHandler: ComposeNavigationHandler
    ) {
        navigation(
            route = AppRoutes.Auth.Graph.path,
            startDestination = AppRoutes.Auth.Splash.path
        ) {
            composable(AppRoutes.Auth.Splash.path) { 
            val viewModel = hiltViewModel<SplashViewModel>()
            SplashScreen(
                navigationHandler = navigationHandler,
                viewModel = viewModel
            )
        }
        
        composable(AppRoutes.Auth.Login.path) {
            val viewModel = hiltViewModel<LoginViewModel>()
            LoginScreen(
                navigationHandler = navigationHandler,
                viewModel = viewModel
            )
        }
        
        // 다른 인증 화면들 추가 (회원가입, 비밀번호 찾기 등)
    }
}

/**
 * 독립적인 화면들의 네비게이션 그래프 (메인 탭 외부에서 접근)
 */
private fun NavGraphBuilder.standaloneScreensGraph(
    navController: NavHostController,
    navigationHandler: ComposeNavigationHandler
) {
    // 프로젝트 생성 화면
    composable(AppRoutes.Project.ADD) {
        val viewModel = hiltViewModel<AddProjectViewModel>()
        AddProjectScreen(navigationHandler)
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
        JoinProjectScreen(navigationHandler)
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
        
        // ProjectDetailViewModel 대신 HomeScreen으로 리다이렉트
        // 메인 화면의 Home 탭으로 이동 후 프로젝트 상세 표시

        // HomeViewModel을 Composable 컨텍스트에서 가져옵니다.
        // navController.getBackStackEntry(AppRoutes.Main.ROOT)가 유효한 시점에 호출되도록 주의해야 합니다.
        // 이 로직은 MainScreen이 이미 백스택에 존재하고 해당 ViewModel이 초기화되었음을 가정합니다.
        // 만약 MainScreen이 아직 생성되지 않았다면 여기서 크래시가 발생할 수 있습니다.
        // 보다 안전한 접근 방식은 navigationHandler를 통해 HomeViewModel에 접근하거나,
        // projectId를 MainScreen으로 전달하고 MainScreen 내부에서 HomeViewModel이 이를 처리하도록 하는 것입니다.
        // 현재 구조에서는 MainScreen으로 먼저 이동한 후 ViewModel을 가져오므로,
        // getBackStackEntry가 유효할 가능성이 높습니다.
        val homeViewModelStoreOwner = remember(backStackEntry) {
            navController.getBackStackEntry(AppRoutes.Main.ROOT)
        }
        val homeViewModel = hiltViewModel<HomeViewModel>(homeViewModelStoreOwner)

        LaunchedEffect(projectId, homeViewModel) {
            // 먼저 메인 화면으로 이동 (Home 탭이 기본 선택됨)
            navigationHandler.navigateClearingBackStack(AppRoutes.Main.ROOT)
            
            // 지연을 주어 UI가 업데이트될 시간을 확보
            // UI가 업데이트되고 homeViewModel이 MainScreen의 컨텍스트에서 완전히 준비될 시간을 기대합니다.
            kotlinx.coroutines.delay(100) // 이 지연이 항상 충분하다고 보장할 수는 없습니다.
            
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

    // 채팅 화면
    composable(
        route = AppRoutes.Chat.route, // "chat/{channelId}?messageId={messageId}"
        arguments = AppRoutes.Chat.arguments // channelId는 필수, messageId는 선택적 쿼리 파라미터
    ) {
        // TODO: 실제 ChatScreen Composable 구현 필요
        // 예시:
        // val channelId = it.arguments?.getString(AppRoutes.Chat.ARG_CHANNEL_ID) ?: return@composable
        // val messageId = it.arguments?.getString(AppRoutes.Chat.ARG_MESSAGE_ID) // nullable
        // ChatScreen(navigationHandler = navigationHandler, channelId = channelId, initialMessageId = messageId)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chat Screen Placeholder\nChannel ID: ${it.arguments?.getString(AppRoutes.Chat.ARG_CHANNEL_ID)}\nMessage ID: ${it.arguments?.getString(AppRoutes.Chat.ARG_MESSAGE_ID)}")
        }
    }
    
    // 추가 독립 화면들을 여기에 정의 (설정, 검색 결과 등)
}

/**
 * NavigationCommand를 처리하여 적절한 네비게이션 작업을 수행합니다.
 * 이 함수는 LaunchedEffect 내에서 호출되어 NavigationManager의 명령을 처리합니다.
 * 
 * @param command 처리할 네비게이션 명령
 * @param navController 사용할 NavController (일반적으로 최상위 NavController)
 */
private fun processNavigationCommand(
    command: NavigationCommand,
    navController: NavHostController
) {
    when (command) {
        // 특정 경로로 이동
        is NavigationCommand.NavigateToRoute -> {
            val route = command.route
            val navOptions = command.navOptions
            navController.navigate(route, navOptions ?: androidx.navigation.NavOptions.Builder().build())
        }
        
        // 백스택을 지우고 이동
        is NavigationCommand.NavigateClearingBackStack -> {
            val route = command.route
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
        
        // 중첩 그래프로 이동
        is NavigationCommand.NavigateToNestedGraph -> {
            val nestedRoute = "${command.parentRoute}/${command.childRoute}"
            navController.navigate(nestedRoute)
        }
        
        // 특정 탭으로 이동 (MainScreen 내부의 NavHost에서 처리됨)
        is NavigationCommand.NavigateToTab -> {
            // 만약 우리가 Main 화면에 있지 않다면, 먼저 Main으로 이동해야 합니다
            if (navController.currentDestination?.route != AppRoutes.Main.ROOT) {
                navController.navigate(AppRoutes.Main.ROOT) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            
            // 참고: 실제 탭 이동은 NavigationManager.navigateToTab() 내부에서
            // 등록된 중첩 NavController를 찾아 처리합니다.
        }
        
        // 뒤로 가기
        is NavigationCommand.NavigateBack -> {
            navController.popBackStack()
        }
        
        // 논리적 위로 이동
        is NavigationCommand.NavigateUp -> {
            navController.navigateUp()
        }
        
        else -> {
            // 기타 명령 처리
        }
    }
} 
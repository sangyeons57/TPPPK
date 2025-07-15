package com.example.teamnovapersonalprojectprojectingkotlin.navigation

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.core_navigation.core.*
import com.example.core_navigation.core.TypeSafeRouteCompat.toAppRoutePath
import com.example.domain.model.vo.DocumentId
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.extractProjectArguments
import com.example.core_navigation.extension.safeComposable
import com.example.feature_add_project.ui.AddProjectScreen
import com.example.feature_auth.ui.SplashScreen
import com.example.feature_calendar_24hour.ui.Calendar24HourScreen
import com.example.feature_edit_schedule.ui.EditScheduleScreen
import com.example.feature_find_password.ui.FindPasswordScreen
import com.example.feature_accept_friend.ui.AcceptFriendsScreen
import com.example.feature_dev.ui.DevMenuScreen
import com.example.feature_friends.ui.FriendsScreen
import com.example.feature_home.viewmodel.HomeViewModel
import com.example.feature_join_project.ui.JoinProjectScreen
import com.example.feature_login.ui.LoginScreen
import com.example.feature_main.MainContainerScreen
import com.example.feature_privacy_policy.ui.PrivacyPolicyScreen
import com.example.feature_profile.ui.EditProfileScreen
import com.example.feature_project_setting_screen.viewmodel.ui.ProjectSettingScreen
import com.example.feature_schedule_detail.ui.ScheduleDetailScreen
import com.example.feature_settings.ui.SettingsScreen
import com.example.feature_signup.ui.SignUpScreen
import com.example.feature_terms_of_service.ui.TermsOfServiceScreen
import com.example.feature_member_list.ui.MemberListScreen
import com.example.feature_role_list.ui.RoleListScreen
import com.example.feature_add_role.ui.AddRoleScreen
import com.example.feature_edit_role.ui.EditRoleScreen
import com.example.feature_edit_member.ui.EditMemberScreen
import com.example.feature_task.ui.TaskListScreen
import com.example.feature_task.ui.TaskDetailScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 앱의 최상위 네비게이션 그래프
 * 
 * 앱의 모든 최상위 경로들을 정의하고, NavigationManager와 연결하여 탐색을 처리합니다.
 * 중첩 네비게이션 구조를 사용하여 기능별로 화면들을 그룹화합니다.
 * 
 * REFACTORING NOTES:
 * This file has been partially modernized to demonstrate new type-safe navigation patterns.
 * 
 * NEW PATTERNS AVAILABLE:
 * 1. Use extractProjectArguments(), extractCalendarArguments(), extractChatArguments()
 *    instead of manual argument extraction
 * 2. Use safeComposable() for automatic error handling of missing arguments
 * 3. Use projectArguments(), calendarArguments() instead of manual argument lists
 * 4. Use NavigationResultManager for result handling instead of deprecated methods
 * 
 * MIGRATION EXAMPLES:
 * 
 * BEFORE (Old Pattern):
 * composable(
 *     route = AppRoutes.Project.settingsRoute(),
 *     arguments = AppRoutes.Project.settingsArguments
 * ) { backStackEntry ->
 *     val projectId = backStackEntry.arguments?.getString(AppRoutes.Project.ARG_PROJECT_ID) ?: ""
 *     ProjectSettingScreen(appNavigator = appNavigator)
 * }
 * 
 * AFTER (New Pattern):
 * safeComposable(
 *     route = AppRoutes.Project.settingsRoute(),
 *     arguments = projectArguments()
 * ) { backStackEntry ->
 *     val args = backStackEntry.extractProjectArguments()
 *     ProjectSettingScreen(
 *         projectId = args.projectId,
 *         appNavigator = appNavigator
 *     )
 * }
 */
@SuppressLint("ContextCastToActivity")
@Composable
fun AppNavigationGraph(
    navController: NavHostController,
    navigationManger: NavigationManger,
    startDestination: String = "auth",
    pendingInviteCode: String? = null
) {

    val activity = (LocalContext.current as? Activity)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() } // 스낵바 사용 시
    var backPressedTime by remember { mutableStateOf(0L) }
    LocalContext.current // Toast 사용 시

    // 딥링크로부터 초대 코드가 있을 경우 자동으로 프로젝트 참여 화면으로 이동
    LaunchedEffect(pendingInviteCode) {
        pendingInviteCode?.let { inviteCode ->
            // 약간의 지연을 주어 네비게이션이 초기화되도록 함
            delay(500)
            navigationManger.navigateToJoinProjectWithInviteCode(inviteCode)
        }
    }

    BackHandler(enabled = navController.previousBackStackEntry == null) {
        if ((System.currentTimeMillis() - backPressedTime) < 2000L) { // 2초 안에 다시 누르면
            activity?.finish()
        } else {
            backPressedTime = System.currentTimeMillis()
            // 스낵바 또는 토스트 메시지 표시
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "한 번 더 누르면 종료됩니다.",
                    duration = SnackbarDuration.Short
                )
            }
            // 또는 Toast.makeText(context, "한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
        }
    }


    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        // 인증 관련 네비게이션 그래프 (로그인, 가입 등)
        authGraph(navigationManger)
        
        // 메인 화면 (MainContainerScreen이 자체적으로 탭 네비게이션 처리)
        mainGraph(navigationManger)
        
        // 친구 화면 그래프 추가
        friendsGraph(navigationManger)
        
        // 독립적인 화면들의 그래프 - 메인 탭 외부에서 접근하는 화면들
        projectGraph(navigationManger)
        chatGraph(navigationManger)
        taskGraph(navigationManger)
        scheduleGraph(navigationManger)

        // 프로필 수정 화면 (Settings Route)
        composable(EditMyProfileRoute.toAppRoutePath()) {
            EditProfileScreen(navigationManger = navigationManger)
        }
        composable(AppSettingsRoute.toAppRoutePath()) {
            SettingsScreen(navigationManger = navigationManger)
        }
        composable(DevMenuRoute.toAppRoutePath()) {
            DevMenuScreen(navigationManger = navigationManger)
        }
    }
}

/**
 * 인증 관련 화면들의 네비게이션 그래프
 */
fun NavGraphBuilder.authGraph(navigationManger: NavigationManger) {
    navigation(
        route = "auth",
        startDestination = SplashRoute.toAppRoutePath()
    ) {

        composable(SplashRoute.toAppRoutePath()) {
            Log.d("Splash", "Splash Screen")
            // Register this NavController when this NavHost is active
            SplashScreen()
        }
        
        composable(LoginRoute.toAppRoutePath()) {
            Log.d("Login", "Login Screen")
            LoginScreen()
        }
        
        // 회원가입 화면 추가
        composable(SignUpRoute.toAppRoutePath()) {
            Log.d("SignUp", "SignUp Screen")
            // viewModel은 나중에 추가
            SignUpScreen( )
        }
        
        // 비밀번호 찾기 화면 추가
        composable(FindPasswordRoute.toAppRoutePath()) {
            Log.d("FindPassword", "FindPassword Screen")
            FindPasswordScreen(navigationManger = navigationManger)
        }

        // 서비스 이용약관 화면 추가
        composable(TermsOfServiceRoute.toAppRoutePath()) {
            Log.d("TermsOfService", "TermsOfService Screen")
            TermsOfServiceScreen(navigationManger = navigationManger)
        }

        // 개인정보 처리방침 화면 추가
        composable(PrivacyPolicyRoute.toAppRoutePath()) {
            Log.d("PrivacyPolicy", "PrivacyPolicy Screen")
            PrivacyPolicyScreen(navigationManger = navigationManger)
        }
    }
}


/**
 * 메인 화면 그래프 - MainContainerScreen이 자체적으로 탭 네비게이션 처리
 */
fun NavGraphBuilder.mainGraph(navigationManger: NavigationManger) {
    composable(MainContainerRoute.toAppRoutePath()) {
        MainContainerScreen(navigationManger = navigationManger)
    }
}

/**
 * 친구 관련 화면들의 네비게이션 그래프
 */
fun NavGraphBuilder.friendsGraph(navigationManger: NavigationManger) {
    navigation(
        route = "friends",
        startDestination = FriendsListRoute.toAppRoutePath()
    ) {
        composable(FriendsListRoute.toAppRoutePath()) {
            FriendsScreen(navigationManger = navigationManger)
        }
        composable(AcceptFriendsRoute.toAppRoutePath()) {
            AcceptFriendsScreen(navigationManger = navigationManger)
        }
        // TODO: 친구 요청 수락 화면 등 추가 경로 정의
        // composable(AppRoutes.Friends.ACCEPT_REQUESTS) { ... }
    }
}

/**
 * 프로젝트 관련 화면들의 네비게이션 그래프
 */
fun NavGraphBuilder.projectGraph(navigationManger: NavigationManger) {
    navigation(
        route = "project",
        startDestination = AddProjectRoute.toAppRoutePath()
    ) {
        // 프로젝트 생성 화면
        composable(AddProjectRoute.toAppRoutePath()) {
          // TODO: AddRoleScreen/EditRoleScreen for adding roles
            AddProjectScreen()
        }

        // Project Settings Screen - MODERNIZED EXAMPLE
        safeComposable(
            route = ProjectSettingsRoute.ROUTE_PATTERN,
            arguments = ProjectSettingsRoute.arguments
        ) { backStackEntry ->
            val args = backStackEntry.extractProjectArguments()
            ProjectSettingScreen()
        }
        
        // 프로젝트 참가 화면
        composable(
            route = JoinProjectRoute.toAppRoutePath(),
        ) {
            JoinProjectScreen()
        }
        
        // 멤버 관리 화면
        composable(
            route = MemberListRoute.ROUTE_PATTERN,
            arguments = MemberListRoute.arguments
        ) {
            MemberListScreen(navigationManger = navigationManger)
        }
        
        // 역할 관리 화면
        composable(
            route = RoleListRoute.ROUTE_PATTERN,
            arguments = RoleListRoute.arguments
        ) {
            RoleListScreen(navigationManger = navigationManger)
        }
        
        // 역할 추가 화면
        composable(
            route = AddRoleRoute.ROUTE_PATTERN,
            arguments = AddRoleRoute.arguments
        ) {
            AddRoleScreen(navigationManger = navigationManger)
        }
        
        // 역할 편집 화면
        composable(
            route = EditRoleRoute.ROUTE_PATTERN,
            arguments = EditRoleRoute.arguments
        ) {
            EditRoleScreen(navigationManger = navigationManger)
        }
        
        // 멤버 편집 화면
        composable(
            route = EditMemberRoute.ROUTE_PATTERN,
            arguments = EditMemberRoute.arguments
        ) {
            EditMemberScreen(navigationManger = navigationManger)
        }
        
        // 프로젝트 상세 화면 - 메인 탭 외부에서도 접근 가능
        composable(
            route = ProjectDetailRoute.ROUTE_PATTERN,
            arguments = ProjectDetailRoute.arguments
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(RouteArgs.PROJECT_ID) ?: ""
            
            // HomeViewModel을 Composable 컨텍스트에서 가져옵니다.
            val homeViewModelStoreOwner = remember(backStackEntry) {
                navigationManger.getNavController()!!.getBackStackEntry(MainContainerRoute.toAppRoutePath())
            }
            val homeViewModel = hiltViewModel<HomeViewModel>(homeViewModelStoreOwner)

            LaunchedEffect(projectId, homeViewModel) {
                // 먼저 메인 화면으로 이동 (Home 탭이 기본 선택됨)
                navigationManger.navigateToClearingBackStack(MainContainerRoute)
                
                // 지연을 주어 UI가 업데이트될 시간을 확보
                delay(100)
                
                // Home 화면에서 프로젝트 선택
                homeViewModel.onProjectClick(DocumentId.from(projectId))
            }
            
            // 리다이렉트 중 로딩 표시
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // 카테고리 수정 화면
        composable(
            route = EditCategoryRoute.ROUTE_PATTERN,
            arguments = EditCategoryRoute.arguments
        ) {
            // EditCategoryScreen 에는 projectId 와 categoryId 가 필요하며,
            // ViewModel 이 hiltViewModel() 로 주입되므로 SavedStateHandle 을 통해 자동으로 받습니다.
            com.example.feature_edit_category.ui.EditCategoryScreen()
        }

        // 채널 수정 화면
        composable(
            route = EditChannelRoute.ROUTE_PATTERN,
            arguments = EditChannelRoute.arguments
        ) {
            // EditChannelScreen 에는 projectId 와 channelId 가 필요하며,
            // ViewModel 이 hiltViewModel() 로 주입되므로 SavedStateHandle 을 통해 자동으로 받습니다.
            com.example.feature_edit_channel.ui.EditChannelScreen()
        }
    }
}

/**
 * 채팅 관련 화면들의 네비게이션 그래프
 */
fun NavGraphBuilder.chatGraph(navigationManger: NavigationManger) {
    navigation(
        route = "chat",
        startDestination = ChatRoute.ROUTE_PATTERN
    ) {
        // 채팅 화면
        composable(
            route = ChatRoute.ROUTE_PATTERN,
            arguments = ChatRoute.arguments
        ) {
            // TODO: 실제 ChatScreen Composable 구현 필요
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chat Screen Placeholder\nChannel ID: ${it.arguments?.getString(RouteArgs.CHANNEL_ID)}\nMessage ID: ${it.arguments?.getString(RouteArgs.MESSAGE_ID)}")
            }
        }
    }
}

/**
 * 일정 관련 화면들의 네비게이션 그래프
 */
fun NavGraphBuilder.scheduleGraph(navigationManger: NavigationManger) {
    navigation(
        route = "calendar",
        startDestination = Calendar24HourRoute.ROUTE_PATTERN
    ) {
        // 24시간 캘린더 화면 - MODERNIZED EXAMPLE
        safeComposable(
            route = Calendar24HourRoute.ROUTE_PATTERN,
            arguments = Calendar24HourRoute.arguments
        ) { backStackEntry ->

            Calendar24HourScreen(
                navigationManger = navigationManger,
            )
        }
        
        // 일정 추가 화면
        composable(
            route = AddScheduleRoute.ROUTE_PATTERN,
            arguments = AddScheduleRoute.arguments
        ) {
            com.example.feature_add_schedule.ui.AddScheduleScreen(
                navigationManger = navigationManger
            )
        }
        
        // 일정 상세 화면
        composable(
            route = ScheduleDetailRoute.ROUTE_PATTERN,
            arguments = ScheduleDetailRoute.arguments
        ) {
            ScheduleDetailScreen(
                navigationManger = navigationManger,
            )
        }
        
        // 일정 수정 화면
        composable(
            route = EditScheduleRoute.ROUTE_PATTERN,
            arguments = EditScheduleRoute.arguments
        ) {
            EditScheduleScreen(
                navigationManger = navigationManger,
            )
        }

    }
}

/**
 * 작업 관리 관련 화면들의 네비게이션 그래프
 */
fun NavGraphBuilder.taskGraph(navigationManger: NavigationManger) {
    navigation(
        route = "task",
        startDestination = TaskListRoute.ROUTE_PATTERN
    ) {
        // 작업 목록 화면
        composable(
            route = TaskListRoute.ROUTE_PATTERN,
            arguments = TaskListRoute.arguments
        ) {
            TaskListScreen(navigationManger = navigationManger)
        }
        
        // 작업 상세 화면
        composable(
            route = TaskDetailRoute.ROUTE_PATTERN,
            arguments = TaskDetailRoute.arguments
        ) {
            TaskDetailScreen(navigationManger = navigationManger)
        }
    }
} 
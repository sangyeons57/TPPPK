package com.example.feature_main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.core_navigation.core.NavigationCommand
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.core_navigation.destination.mainBottomNavItems
import com.example.core_navigation.destination.AppRoutes
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_main.ui.ProfileScreen
import com.example.feature_main.ui.calendar.CalendarScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ui.MainScreenType
import com.example.domain.model.ui.MainUiState
import com.example.feature_main.ui.DmListScreen
import com.example.feature_main.ui.ProjectListScreen
import kotlinx.coroutines.launch

/**
 * 메인 화면 관련 상수
 */
private object MainScreenConstants {
    // 인셋 값들
    const val ZERO_INSET = 0
    
    // 미리보기 크기 상수
    val PREVIEW_WIDTH = 100.dp
    val PREVIEW_HEIGHT = 2000.dp
}

/**
 * 메인 화면: 하단 탭 네비게이션과 각 탭의 콘텐츠를 표시합니다.
 * 각 탭은 별도의 백스택을 유지하여 탭 전환 시에도 상태가 보존됩니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navigationHandler: ComposeNavigationHandler,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    // 현재 선택된 탭
    var selectedTab by rememberSaveable { mutableStateOf(AppRoutes.Main.Home.GRAPH_ROOT) }

    // 각 탭마다 별도의 NavController를 생성하여 탭별 백스택 유지
    val homeNavController = rememberNavController()
    val calendarNavController = rememberNavController()
    val profileNavController = rememberNavController()
    
    // 탭 라우트 -> NavController 맵핑
    val navControllers = remember {
        mapOf(
            AppRoutes.Main.Home.GRAPH_ROOT to homeNavController,
            AppRoutes.Main.Calendar.GRAPH_ROOT to calendarNavController,
            AppRoutes.Main.Profile.GRAPH_ROOT to profileNavController
        )
    }

    // 현재 선택된 탭의 NavController
    val currentNavController = navControllers[selectedTab] ?: homeNavController

    // 탭 선택 시 해당 탭의 시작 목적지로 이동하는 로직
    LaunchedEffect(selectedTab) {
        val controller = navControllers[selectedTab]
        val startDestination = getTabStartDestination(selectedTab)
        if (controller?.currentDestination?.route != startDestination) {
            controller?.navigate(startDestination) {
                popUpTo(controller.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
            ) {
                mainBottomNavItems.forEach { screen ->
                    val selected = selectedTab == screen.route
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = { selectedTab = screen.route }
                    )
                }
            }
        }
    ) { innerPadding ->
        // 현재 선택된 탭에 해당하는 NavHost만 표시
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                AppRoutes.Main.Home.GRAPH_ROOT -> {
                    HomeTabNavHost(
                        navController = homeNavController,
                        navigationHandler = navigationHandler,
                        mainViewModel = mainViewModel
                    )
                }
                AppRoutes.Main.Calendar.GRAPH_ROOT -> {
                    CalendarTabNavHost(
                        navController = calendarNavController,
                        navigationHandler = navigationHandler
                    )
                }
                AppRoutes.Main.Profile.GRAPH_ROOT -> {
                    ProfileTabNavHost(
                        navController = profileNavController,
                        navigationHandler = navigationHandler
                    )
                }
            }
        }
    }
}

/**
 * 탭 경로에 해당하는 시작 목적지를 반환
 */
private fun getTabStartDestination(tabRoute: String): String {
    return when (tabRoute) {
        AppRoutes.Main.Home.GRAPH_ROOT -> AppRoutes.Main.Home.ROOT_CONTENT
        AppRoutes.Main.Calendar.GRAPH_ROOT -> AppRoutes.Main.Calendar.ROOT_CONTENT
        AppRoutes.Main.Profile.GRAPH_ROOT -> AppRoutes.Main.Profile.ROOT_CONTENT
        else -> AppRoutes.Main.Home.ROOT_CONTENT
    }
}

/**
 * Home 탭의 네비게이션 호스트
 */
@Composable
private fun HomeTabNavHost(
    navController: NavHostController,
    navigationHandler: ComposeNavigationHandler,
    mainViewModel: MainViewModel
) {
    // Register this NavController when this NavHost is active
    DisposableEffect(navController, navigationHandler) {
        navigationHandler.setChildNavController(navController)
        onDispose {
            // Only clear if this is still the active one,
            // though MainScreen's logic might make this redundant if it switches.
            // For safety, good to clear.
            if (navigationHandler.getChildNavController() == navController) {
                navigationHandler.setChildNavController(null)
            }
        }
    }

        NavHost(
        navController = navController,
        startDestination = AppRoutes.Main.Home.ROOT_CONTENT
        ) {
        composable(AppRoutes.Main.Home.ROOT_CONTENT) {
            HomeContentScreen(
                viewModel = mainViewModel,
                    navigationHandler = navigationHandler
                )
            }
    }
}

/**
 * HomeContentScreen: Home 탭의 실제 콘텐츠 (DM 목록 또는 프로젝트 목록)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContentScreen(
    viewModel: MainViewModel,
    navigationHandler: ComposeNavigationHandler
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // DM / Project Toggle
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                onClick = { viewModel.setCurrentScreen(MainScreenType.DMS) },
                selected = uiState.currentScreen == MainScreenType.DMS
            ) {
                Text("DM")
            }
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                onClick = { viewModel.setCurrentScreen(MainScreenType.PROJECTS) },
                selected = uiState.currentScreen == MainScreenType.PROJECTS
            ) {
                Text("프로젝트")
            }
        }

        // Display based on current screen type
        when (uiState.currentScreen) {
            MainScreenType.DMS -> {
                // TODO: Handle loading and error states for DMs
                DmListScreen(
                    dms = uiState.dmConversations,
                    onDmClick = {
                        // Navigate to ChatScreen with DM parameters
                        navigationHandler.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Chat.screen(it)))
                    },
                    modifier = Modifier.weight(1f) // Fill remaining space
                )
            }
            MainScreenType.PROJECTS -> {
                // TODO: Handle loading and error states for Projects
                ProjectListScreen(
                    projects = uiState.projects,
                    onProjectClick = { projectId ->
                        // Navigate to Project Detail Screen
                        navigationHandler.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Project.detail(projectId = projectId)))
                    },
                    modifier = Modifier.weight(1f) // Fill remaining space
                )
            }
            else -> {
                // Handle other cases if MainScreenType expands (e.g., Calendar, Profile handled by separate NavHosts)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("선택된 화면 유형 오류: ${uiState.currentScreen}")
                }
            }
        }
    }
}

/**
 * Calendar 탭의 네비게이션 호스트
 */
@Composable
private fun CalendarTabNavHost(
    navController: NavHostController,
    navigationHandler: ComposeNavigationHandler
) {
    // Register this NavController when this NavHost is active
    DisposableEffect(navController, navigationHandler) {
        navigationHandler.setChildNavController(navController)
        onDispose {
            if (navigationHandler.getChildNavController() == navController) {
                navigationHandler.setChildNavController(null)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.Main.Calendar.ROOT_CONTENT
    ) {
        composable(AppRoutes.Main.Calendar.ROOT_CONTENT) {
            CalendarScreen(navigationHandler = navigationHandler)
        }
    }
}

/**
 * Profile 탭의 네비게이션 호스트
 */
@Composable
private fun ProfileTabNavHost(
    navController: NavHostController,
    navigationHandler: ComposeNavigationHandler
) {
    // Register this NavController when this NavHost is active
    DisposableEffect(navController, navigationHandler) {
        navigationHandler.setChildNavController(navController)
        onDispose {
            if (navigationHandler.getChildNavController() == navController) {
                navigationHandler.setChildNavController(null)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.Main.Profile.ROOT_CONTENT
    ) {
        composable(AppRoutes.Main.Profile.ROOT_CONTENT) {
            ProfileScreen(navigationHandler = navigationHandler)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeContentProjectsPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        val nestedNavController = rememberNavController()
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = { 
                PreviewBottomNavigation(nestedNavController)
            }
        ) { innerPadding -> // Scaffold의 content 람다, innerPadding 전달받음
            Box(modifier = Modifier.padding(innerPadding)
                .width(MainScreenConstants.PREVIEW_WIDTH)
                .height(MainScreenConstants.PREVIEW_HEIGHT)
                .background(Color.Red)
            )
        }
    }
}

/**
 * 미리보기용 하단 탭 네비게이션
 */
@Composable
private fun PreviewBottomNavigation(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
                NavigationBar(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    mainBottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                    // 프리뷰에서는 직접 NavController 사용
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
        }
    }
}

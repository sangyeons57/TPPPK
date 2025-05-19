package com.example.feature_main

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.lifecycle.ViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.core_navigation.core.NavigationCommand
import com.example.core_navigation.core.AppNavigator
import com.example.core_navigation.destination.mainBottomNavItems
import com.example.core_navigation.destination.AppRoutes
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_main.ui.HomeScreen
import com.example.feature_main.ui.ProfileScreen
import com.example.feature_main.ui.calendar.CalendarScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ui.MainScreenType
import com.example.domain.model.ui.MainUiState
import com.example.domain.model.ui.ProjectUiModel
import com.example.feature_main.ui.DmListScreen
import com.example.feature_main.ui.ProjectListScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

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
 * 메인 화면: 하단 탭 네비게이션과 각 탭의 콘텐츠를 표시하는 컨트롤러
 * 각 탭은 별도의 백스택을 유지하여 탭 전환 시에도 상태가 보존됩니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    appNavigator: AppNavigator,
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
        modifier = Modifier.fillMaxSize().offset(0.dp,0.dp),
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
        Box(
            modifier = Modifier.padding(innerPadding),
        ) {
            when (selectedTab) {
                AppRoutes.Main.Home.GRAPH_ROOT -> {
                    HomeTabNavHost(
                        navController = homeNavController,
                        appNavigator = appNavigator
                    )
                }
                AppRoutes.Main.Calendar.GRAPH_ROOT -> {
                    CalendarTabNavHost(
                        navController = calendarNavController,
                        appNavigator = appNavigator
                    )
                }
                AppRoutes.Main.Profile.GRAPH_ROOT -> {
                    ProfileTabNavHost(
                        navController = profileNavController,
                        appNavigator = appNavigator
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
    appNavigator: AppNavigator
) {
    // Register this NavController when this NavHost is active
    DisposableEffect(navController, appNavigator) {
        appNavigator.setChildNavController(navController)
        onDispose {
            if (appNavigator.getChildNavController() == navController) {
                appNavigator.setChildNavController(null)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.Main.Home.ROOT_CONTENT
    ) {
        composable(AppRoutes.Main.Home.ROOT_CONTENT) {
            HomeScreen(appNavigator = appNavigator)
        }
    }
}


/**
 * Calendar 탭의 네비게이션 호스트
 */
@Composable
private fun CalendarTabNavHost(
    navController: NavHostController,
    appNavigator: AppNavigator
) {
    // Register this NavController when this NavHost is active
    DisposableEffect(navController, appNavigator) {
        appNavigator.setChildNavController(navController)
        onDispose {
            if (appNavigator.getChildNavController() == navController) {
                appNavigator.setChildNavController(null)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.Main.Calendar.ROOT_CONTENT
    ) {
        composable(AppRoutes.Main.Calendar.ROOT_CONTENT) {
            CalendarScreen(appNavigator = appNavigator)
        }
        
        // 참고: 이전에 여기에 등록했던 Calendar24Hour, AddSchedule, ScheduleDetail, EditSchedule 화면들은
        // 이제 앱 최상위 네비게이션 그래프(AppNavigationGraph.kt)의 standaloneScreensGraph 함수에서
        // 독립적인 화면으로 등록되었습니다.
    }
}

/**
 * Profile 탭의 네비게이션 호스트
 */
@Composable
private fun ProfileTabNavHost(
    navController: NavHostController,
    appNavigator: AppNavigator
) {
    // Register this NavController when this NavHost is active
    DisposableEffect(navController, appNavigator) {
        appNavigator.setChildNavController(navController)
        onDispose {
            if (appNavigator.getChildNavController() == navController) {
                appNavigator.setChildNavController(null)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.Main.Profile.ROOT_CONTENT
    ) {
        composable(AppRoutes.Main.Profile.ROOT_CONTENT) {
            ProfileScreen(appNavigator = appNavigator)
        }
    }
}


/**
 * ProjectListScreen용 프리뷰
 */
@Preview(showBackground = true, widthDp = 120, heightDp = 640)
@Composable
fun ProjectListScreenPreview() {
    // 더미 프로젝트 데이터 생성
    val sampleProjects = listOf(
        ProjectUiModel(id = "1", name = "프로젝트 Alpha", description = "알파 프로젝트 설명", imageUrl = null),
        ProjectUiModel(id = "2", name = "프로젝트 Beta", description = "베타 프로젝트 설명", imageUrl = null),
        ProjectUiModel(id = "3", name = "프로젝트 Gamma", description = "감마 프로젝트 설명", imageUrl = null),
        ProjectUiModel(id = "4", name = "프로젝트 Delta", description = "델타 프로젝트 설명", imageUrl = null),
        ProjectUiModel(id = "5", name = "프로젝트 Epsilon", description = "엡실론 프로젝트 설명", imageUrl = null)
    )
    
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ) {
            ProjectListScreen(
                projects = sampleProjects,
                selectedProjectId = "2", // 두 번째 프로젝트가 선택된 상태
                isDmSelected = false,
                onProfileClick = {},
                onProjectClick = {}
            )
        }
    }
}

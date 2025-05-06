package com.example.feature_main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.*
import com.example.core_navigation.core.NavigationCommand
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.core_navigation.destination.mainBottomNavItems
import com.example.core_navigation.destination.AppRoutes
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_main.ui.HomeScreen
import com.example.feature_main.ui.ProfileScreen
import com.example.feature_main.ui.calendar.CalendarScreen
import androidx.hilt.navigation.compose.hiltViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navigationManager: ComposeNavigationHandler,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    // 중첩된 NavHost를 위한 별도의 NavController 생성
    val nestedNavController = rememberNavController()

    // DisposableEffect를 사용하여 MainScreen이 컴포지션에 추가/제거될 때
    // NavigationManager에 activeChildNavController를 등록/해제합니다.
    DisposableEffect(navigationManager, nestedNavController) {
        navigationManager.setActiveChildNavController(nestedNavController)
        onDispose {
            if (navigationManager.getActiveChildNavController() == nestedNavController) {
                 navigationManager.setActiveChildNavController(null)
            }
        }
    }
    
    // // 일정 추가 후 돌아올 때 캘린더 갱신을 위한 코드 -> CalendarScreen에서 직접 처리하도록 변경
    // val calendarRefreshKey = "refresh_calendar"
    // LaunchedEffect(navigationManager, calendarRefreshKey) {
    //     navigationManager.getResultFlow<Boolean>(calendarRefreshKey).collect { refresh ->
    //         if (refresh) {
    //             // CalendarScreen에서 직접 처리
    //         }
    //     }
    // }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // mainScreenBottomBar는 이제 navigationManager와 nestedNavController를 사용
        bottomBar = mainScreenBottomBar(navigationManager = navigationManager, nestedNavController = nestedNavController),
    ) { innerPadding ->
        NavHost(
            navController = nestedNavController,
            startDestination = AppRoutes.Main.Home.ROOT,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoutes.Main.Home.ROOT) {
                HomeScreen(
                    navigationManager = navigationManager
                )
            }
            
            composable(AppRoutes.Main.Calendar.ROOT) {
                CalendarScreen(
                    navigationManager = navigationManager
                    // shouldRefreshCalendar 파라미터 제거
                )
            }
            
            composable(AppRoutes.Main.Profile.ROOT) {
                ProfileScreen(
                    navigationManager = navigationManager
                )
            }
        }
    }
}


@Composable
fun mainScreenBottomBar(
    navigationManager: ComposeNavigationHandler, // NavigationManager 주입
    nestedNavController: NavHostController // NavOptions 구성에 필요
) : @Composable () -> Unit = {
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val navBackStackEntry by nestedNavController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        mainBottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(nestedNavController.graph.findStartDestination().id, inclusive = false, saveState = true)
                        .setLaunchSingleTop(true)
                        .setRestoreState(true)
                        .build()
                    
                    navigationManager.navigate(NavigationCommand.NavigateToRoute(screen.route, navOptions))
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeContentProjectsPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        val nestedNavController = rememberNavController()
        Scaffold(
            modifier = Modifier.fillMaxSize()
            ,
            contentWindowInsets = WindowInsets(0, 0, 0, 0), // 윈도우 인셋 제거
            bottomBar = { mainScreenBottomBarPreview(nestedNavController) } // Preview용 별도 bottomBar 또는 수정
        ) { innerPadding -> // Scaffold의 content 람다, innerPadding 전달받음
            Box(modifier = Modifier.padding(innerPadding)
                .width(100.dp)
                .height(2000.dp)
                .background(Color.Red)
            ) {

            }
        }
    }
}

// Preview용 mainScreenBottomBar (NavigationManager 없이)
@Composable
fun mainScreenBottomBarPreview(nestedNavController: NavHostController) : @Composable () -> Unit = {
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val navBackStackEntry by nestedNavController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        mainBottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    nestedNavController.navigate(screen.route) {
                        popUpTo(nestedNavController.graph.findStartDestination().id) {
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

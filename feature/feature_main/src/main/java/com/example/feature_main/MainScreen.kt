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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_main.ui.calendar.CalendarScreen
import com.example.feature_main.ui.HomeScreen
import com.example.feature_main.ui.ProfileScreen
import com.example.navigation.MainBottomNavDestination
import com.example.navigation.mainBottomNavItems
import java.time.LocalDate


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    // navController 파라미터 제거
    modifier: Modifier = Modifier,
    // 외부 네비게이션을 위한 간소화된 람다들
    onNavigate: (String) -> Unit,
    onNavigateWithArgs: (String, androidx.navigation.NavOptions?) -> Unit,
    shouldRefreshCalendar: Boolean = false
) {
    // 중첩된 NavHost를 위한 별도의 NavController 생성 (기존과 동일)
    val nestedNavController = rememberNavController()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // mainScreenBottomBar는 nestedNavController를 사용하므로 변경 없음
        bottomBar = mainScreenBottomBar(nestedNavController),
    ) { innerPadding ->
        NavHost(
            navController = nestedNavController,
            startDestination = MainBottomNavDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainBottomNavDestination.Home.route) {
                HomeScreen(
                    onNavigateToAddProject = { onNavigate(com.example.navigation.AddProject.route) }
                )
            }
            
            composable(MainBottomNavDestination.Calendar.route) {
                CalendarScreen(
                    onClickFAB = { route -> onNavigate(route) },
                    onNavigateToScheduleDetail = { scheduleId -> 
                        onNavigate(com.example.navigation.ScheduleDetail.createRoute(scheduleId))
                    },
                    onNavigateToCalendar24Hour = { year, month, day ->
                        onNavigate(com.example.navigation.Calendar24Hour.createRoute(year, month, day))
                    },
                    shouldRefreshCalendar = shouldRefreshCalendar // 일정 추가 후 갱신 플래그 전달
                )
            }
            composable(MainBottomNavDestination.Profile.route) {
                ProfileScreen(
                    // ProfileScreen이 필요로 하는 네비게이션 람다 전달
                    onLogout = { /* TODO: Implement logout */ },
                    onClickSettings = { /* TODO: Implement settings navigation */ },
                    onClickFriends = { /* TODO: Implement friends navigation */ },
                    onClickStatus = { /* TODO: Implement status change dialog or lambda */ }
                )
            }
        }
    }
}


@Composable
fun mainScreenBottomBar(nestedNavController: NavHostController) : @Composable () -> Unit = {
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val navBackStackEntry by nestedNavController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        // 정의된 하단 네비 아이템들로 NavigationBarItem 생성
        mainBottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                // 현재 경로가 해당 아이템의 경로 또는 하위 경로에 포함되는지 확인
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    // 아이템 클릭 시 해당 경로로 이동
                    nestedNavController.navigate(screen.route) {
                        // 백 스택 맨 위까지 pop하여 동일한 목적지 중복 생성 방지
                        popUpTo(nestedNavController.graph.findStartDestination().id) {
                            saveState = true // 상태 저장
                        }
                        // 이미 스택에 있으면 재생성 대신 상태 복원
                        launchSingleTop = true
                        // 이전 상태 복원
                        restoreState = true
                    }
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
            bottomBar = mainScreenBottomBar(nestedNavController)
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

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
    // --- 외부 네비게이션을 위한 람다 파라미터 추가 ---
    onNavigateToAddProject: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToSettings: () -> Unit, // 예시: 설정 화면 이동
    onLogout: () -> Unit,
    onNavigateToScheduleDetail: (String) -> Unit, // 예시: Schedule ID 필요
    onNavigateToAddSchedule: (Int, Int, Int) -> Unit, // 예시: Year, Month, Day 필요
    onNavigateToCalendar24Hour: (Int, Int, Int) -> Unit, // 예시: Year, Month, Day 필요
    // 필요한 다른 외부 네비게이션 람다 추가...
    // -----------------------------------------
    // TODO: MainViewModel 필요 시 주입
) {
    // 중첩된 NavHost를 위한 별도의 NavController 생성 (기존과 동일)
    val nestedNavController = rememberNavController()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // mainScreenBottomBar는 nestedNavController를 사용하므로 변경 없음
        bottomBar = mainScreenBottomBar(nestedNavController),
    ) { innerPadding ->

        // 중첩된 NavHost: 하단 네비게이션
        NavHost(
            navController = nestedNavController, // 중첩 NavController 사용
            startDestination = MainBottomNavDestination.Home.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()) // 하단 패딩 적용
        ) {
            composable(MainBottomNavDestination.Home.route) {
                HomeScreen(
                    // HomeScreen이 필요로 하는 네비게이션 람다 전달
                    onNavigateToAddProject = onNavigateToAddProject // MainScreen이 받은 람다 전달
                    // HomeScreen 내에서 다른 화면으로 이동 필요 시 추가 람다 전달
                    // navController = nestedNavController 제거
                )
            }
            composable(MainBottomNavDestination.Calendar.route) {
                CalendarScreen(
                    // CalendarScreen이 필요로 하는 네비게이션 람다 전달
                    onClickFAB = { route ->
                        // route 분석 또는 직접 람다 전달 방식 수정 필요
                        // 예: onNavigateToAddSchedule 람다 직접 사용
                        val today = LocalDate.now() // 예시 날짜
                        onNavigateToAddSchedule(today.year, today.monthValue, today.dayOfMonth)
                    },
                    onNavigateToScheduleDetail = onNavigateToScheduleDetail,
                    onNavigateToCalendar24Hour = { year, month, day -> 
                        // 24시간 캘린더 뷰로 이동
                        onNavigateToCalendar24Hour(year, month, day)
                    }
                )
            }
            composable(MainBottomNavDestination.Profile.route) {
                ProfileScreen(
                    // ProfileScreen이 필요로 하는 네비게이션 람다 전달
                    onLogout = onLogout, // MainScreen이 받은 람다 전달
                    onClickSettings = onNavigateToSettings, // MainScreen이 받은 람다 전달
                    onClickFriends = onNavigateToFriends, // MainScreen이 받은 람다 전달
                    onClickStatus = { /* TODO: 상태 변경 다이얼로그 등 내부 처리 또는 람다 */ }
                    // navController = nestedNavController 제거
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

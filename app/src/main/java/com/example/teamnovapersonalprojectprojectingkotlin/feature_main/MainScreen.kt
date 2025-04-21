package com.example.teamnovapersonalprojectprojectingkotlin.feature_main

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
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.ui.CalendarScreen
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.ui.HomeContent
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.ui.HomeScreen
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.ui.ProfileScreen
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.viewmodel.HomeUiState
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.viewmodel.ProjectItem
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.viewmodel.TopSection
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project.ui.AddProjectScreen
import com.example.teamnovapersonalprojectprojectingkotlin.navigation.AddProject
import com.example.teamnovapersonalprojectprojectingkotlin.navigation.Login
import com.example.teamnovapersonalprojectprojectingkotlin.navigation.Main
import com.example.teamnovapersonalprojectprojectingkotlin.navigation.MainBottomNavDestination
import com.example.teamnovapersonalprojectprojectingkotlin.navigation.mainBottomNavItems
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme

@OptIn(ExperimentalMaterial3Api::class) // Scaffold 등 사용
@Composable
fun MainScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    // TODO: MainViewModel 필요 시 주입
    // viewModel: MainViewModel = hiltViewModel()
) {
    // 중첩된 NavHost를 위한 별도의 NavController 생성
    val nestedNavController = rememberNavController()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // 윈도우 인셋 제거
        bottomBar = mainScreenBottomBar(nestedNavController),
    ) { innerPadding -> // Scaffold의 content 람다, innerPadding 전달받음

        // 중첩된 NavHost: 하단 네비게이션에 따라 화면 전환
        NavHost(
            navController = nestedNavController,
            startDestination = MainBottomNavDestination.Home.route, // 시작 화면은 홈
            modifier = Modifier.padding(
                bottom = innerPadding.calculateBottomPadding()
            )
        ) {
            composable(MainBottomNavDestination.Home.route) {
                HomeScreen(
                    navController= nestedNavController,
                    onNavigateToAddProject= {navController.navigate(AddProject.route)} // AddProject 화면으로 이동
                )
            }
            composable(MainBottomNavDestination.Calendar.route) { CalendarScreen(navController= nestedNavController) }
            composable(MainBottomNavDestination.Profile.route) {
                ProfileScreen(
                    navController = nestedNavController,
                    onLogout = {
                        navController.navigate(Login.route) { // Login 경로는 AppDestination에 정의
                            popUpTo(Main.route) { inclusive = true } // Main 그래프까지 스택에서 제거
                            launchSingleTop = true // 로그인 화면 중복 생성 방지
                        }
                    },
                )
            }
            // TODO: 필요한 경우 다른 composable 목적지 추가 (설정 화면 등)
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

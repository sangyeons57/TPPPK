package com.example.feature_main

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.core_navigation.core.AppNavigator
import com.example.core_navigation.core.NavControllerSaver
import com.example.feature_friends.ui.FriendsScreen // Added import
import com.example.core_navigation.core.NavControllerState
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.destination.mainBottomNavItems
import com.example.feature_main.ui.HomeScreen
import com.example.feature_main.ui.ProfileScreen
import com.example.feature_main.ui.calendar.CalendarScreen

/**
 * 메인 컨테이너 화면: 하단 탭 네비게이션과 각 탭의 콘텐츠를 표시하는 컨트롤러
 * 각 탭은 별도의 백스택을 유지하며, 탭 전환 시에도 상태가 보존됩니다.
 * 
 * 기존 MainScreen에서 발전된 버전으로, NavControllerState를 사용하여
 * 각 탭의 상태를 명시적으로 저장하고 복원합니다.
 */
@Composable
fun MainContainerScreen(
    appNavigator: AppNavigator
) {
    val TAG = "MainContainerScreen"
    
    // 현재 선택된 탭
    var selectedTab by rememberSaveable { 
        mutableStateOf(AppRoutes.Main.Home.GRAPH_ROOT) 
    }

    // 각 탭마다 별도의 NavController를 생성하여 탭별 백스택 유지
    val homeNavController = rememberNavController()
    val calendarNavController = rememberNavController()
    val profileNavController = rememberNavController()
    val friendsNavController = rememberNavController() // Added
    
    // 탭 라우트 -> NavController 맵핑
    val navControllers = remember {
        mapOf(
            AppRoutes.Main.Home.GRAPH_ROOT to homeNavController,
            AppRoutes.Main.Calendar.GRAPH_ROOT to calendarNavController,
            AppRoutes.Main.Profile.GRAPH_ROOT to profileNavController,
            AppRoutes.Friends.ROOT to friendsNavController // Added
        )
    }

    // 각 탭의 NavController 상태를 저장하는 맵
    val navControllerStates = rememberSaveable(saver = NavControllerSaver) {
        mutableStateMapOf(
            AppRoutes.Main.Home.GRAPH_ROOT to NavControllerState(),
            AppRoutes.Main.Calendar.GRAPH_ROOT to NavControllerState(),
            AppRoutes.Main.Profile.GRAPH_ROOT to NavControllerState(),
            AppRoutes.Friends.ROOT to NavControllerState() // Added
        )
    }

    // 현재 선택된 탭의 NavController
    val currentNavController = navControllers[selectedTab] ?: homeNavController
    
    // NavigationManager에서 pending_tab_navigation 결과를 확인하여 탭 전환
    LaunchedEffect(Unit) {
        appNavigator.getResult<String>("pending_tab_navigation")?.let { pendingTabRoute ->
            Log.d(TAG, "NavigationManager에서 보류중인 탭 이동 발견: $pendingTabRoute")
            
            // 유효한 탭 경로인지 확인하고 해당 탭으로 전환
            val validTabRoutes = mainBottomNavItems.map { it.route }
            if (validTabRoutes.contains(pendingTabRoute)) {
                Log.d(TAG, "보류중인 탭 이동 실행: $pendingTabRoute")
                selectedTab = pendingTabRoute
            } else {
                Log.w(TAG, "유효하지 않은 탭 경로: $pendingTabRoute")
            }
        }
    }

    // 탭 선택 시 해당 탭의 시작 목적지로 이동하는 로직
    LaunchedEffect(selectedTab) {
        val controller = navControllers[selectedTab]
        val startDestination = getTabStartDestination(selectedTab)
        
        // 현재 탭의 상태 복원
        val currentState = navControllerStates[selectedTab]
        Log.d(TAG, "탭 전환: $selectedTab, 복원할 상태: ${currentState?.backStackState}")
        
        // NavController가 올바른 화면으로 이동
        if (controller?.currentDestination?.route != startDestination) {
            controller?.navigate(startDestination) {
                popUpTo(controller.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    
    // NavigationHandler에 현재 TabNavController 등록
    DisposableEffect(currentNavController) {
        appNavigator.setChildNavController(currentNavController)
        onDispose {
            // 현재 탭이 비활성화될 때 상태 저장
            val currentState = navControllerStates[selectedTab] ?: NavControllerState()
            currentState.backStackState = currentNavController.currentDestination?.route
            navControllerStates[selectedTab] = currentState
            
            // NavigationHandler에서 현재 컨트롤러 제거(다른 화면으로 이동 시)
            if (appNavigator.getChildNavController() == currentNavController) {
                appNavigator.setChildNavController(null)
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
                        onClick = { 
                            if (selectedTab != screen.route) {
                                // 현재 탭의 상태 저장
                                val currentState = navControllerStates[selectedTab] ?: NavControllerState()
                                currentState.backStackState = currentNavController.currentDestination?.route
                                navControllerStates[selectedTab] = currentState
                                Log.d(TAG, "현재 탭($selectedTab) 상태 저장: ${currentState.backStackState}")
                                
                                // 새 탭으로 전환
                                selectedTab = screen.route
                            }
                        }
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
                        appNavigator = appNavigator,
                        savedState = navControllerStates[selectedTab]?.screenState
                    )
                }
                AppRoutes.Main.Calendar.GRAPH_ROOT -> {
                    CalendarTabNavHost(
                        navController = calendarNavController,
                        appNavigator = appNavigator,
                        savedState = navControllerStates[selectedTab]?.screenState
                    )
                }
                AppRoutes.Main.Profile.GRAPH_ROOT -> {
                    ProfileTabNavHost(
                        navController = profileNavController,
                        appNavigator = appNavigator,
                        savedState = navControllerStates[selectedTab]?.screenState
                    )
                }
                AppRoutes.Friends.ROOT -> { // Added case
                    FriendsTabNavHost(
                        navController = friendsNavController,
                        appNavigator = appNavigator,
                        savedState = navControllerStates[selectedTab]?.screenState
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
        AppRoutes.Friends.ROOT -> AppRoutes.Friends.LIST // Added
        else -> AppRoutes.Main.Home.ROOT_CONTENT
    }
}

/**
 * Home 탭의 네비게이션 호스트
 */
@Composable
private fun HomeTabNavHost(
    navController: NavHostController,
    appNavigator: AppNavigator,
    savedState: android.os.Bundle? = null
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
            HomeScreen(
                appNavigator = appNavigator,
                savedState = savedState
            )
        }
    }
}

/**
 * Friends 탭의 네비게이션 호스트
 */
@Composable
private fun FriendsTabNavHost(
    navController: NavHostController,
    appNavigator: AppNavigator,
    savedState: android.os.Bundle? = null
) {
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
        startDestination = AppRoutes.Friends.LIST
    ) {
        composable(AppRoutes.Friends.LIST) {
            FriendsScreen(
                appNavigator = appNavigator
            )
        }
        // TODO: Add other destinations for the Friends tab if needed,
        // e.g., AppRoutes.Friends.ACCEPT_REQUESTS
        // composable(AppRoutes.Friends.ACCEPT_REQUESTS) { ... }
    }
}

/**
 * Calendar 탭의 네비게이션 호스트
 */
@Composable
private fun CalendarTabNavHost(
    navController: NavHostController,
    appNavigator: AppNavigator,
    savedState: android.os.Bundle? = null
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
            CalendarScreen(
                appNavigator = appNavigator,
            )
        }
    }
}

/**
 * Profile 탭의 네비게이션 호스트
 */
@Composable
private fun ProfileTabNavHost(
    navController: NavHostController,
    appNavigator: AppNavigator,
    savedState: android.os.Bundle? = null
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
            ProfileScreen(
                appNavigator = appNavigator,
            )
        }
    }
} 
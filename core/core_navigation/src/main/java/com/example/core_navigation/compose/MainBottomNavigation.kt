package com.example.core_navigation.compose

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
// import com.example.core_navigation.destination.MainBottomNavDestination // REMOVE
import com.example.core_navigation.destination.mainBottomNavItems // CORRECTED IMPORT

/**
 * 메인 화면 하단 네비게이션 바 컴포저블
 * 
 * 앱의 주요 섹션 간 이동을 위한 하단 네비게이션 바를 제공합니다.
 * 
 * @param navController 네비게이션 컨트롤러
 * @param modifier 컴포즈 수정자
 */
@Composable
fun MainBottomNavigation(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    // 현재 백스택 엔트리 가져오기
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        // 각 네비게이션 항목에 대한 반복
        mainBottomNavItems.forEach { destination ->
            // 현재 선택된 항목인지 확인
            val selected = currentDestination?.hierarchy?.any { 
                it.route == destination.route 
            } ?: false
            
            // 네비게이션 바 항목 생성
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.title
                    )
                },
                label = { Text(text = destination.title) },
                selected = selected,
                onClick = {
                    // 선택된 항목으로 이동
                    navController.navigate(destination.route) {
                        // 백스택 동작 설정
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // 상태 저장 설정
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
} 
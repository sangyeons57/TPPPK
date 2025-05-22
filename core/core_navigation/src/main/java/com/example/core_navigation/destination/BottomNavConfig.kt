package com.example.core_navigation.destination

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People // Added import
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 데이터 클래스로 각 하단 네비게이션 아이템을 정의합니다.
 * @param route 해당 아이템 선택 시 네비게이션 될 경로 (AppRoutes 사용)
 * @param title 아이템의 제목
 * @param icon 아이템의 아이콘
 */
data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

/**
 * 앱에서 사용될 하단 네비게이션 아이템들의 목록입니다.
 */
val mainBottomNavItems: List<BottomNavItem> = listOf(
    BottomNavItem(
        route = AppRoutes.Main.Home.GRAPH_ROOT,
        title = "홈",
        icon = Icons.Default.Home
    ),
    BottomNavItem(
        route = AppRoutes.Main.Calendar.GRAPH_ROOT,
        title = "캘린더",
        icon = Icons.Default.DateRange
    ),
    BottomNavItem(
        route = AppRoutes.Main.Profile.GRAPH_ROOT,
        title = "프로필",
        icon = Icons.Default.Person
    )
)
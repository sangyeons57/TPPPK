package com.example.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.navArgument

// 앱 내 모든 네비게이션 목적지를 정의하는 인터페이스
sealed interface AppDestination {
    val route: String
}

// --- 주요 화면 목적지 ---
object Splash : AppDestination { override val route = "splash" }
object Login : AppDestination { override val route = "login" }
object SignUp : AppDestination { override val route = "signup" }
object FindPassword : AppDestination { override val route = "find_password" }
object Main : AppDestination { override val route = "main" } // 하단 네비게이션 포함
object DevMenu : AppDestination { override val route = "dev_menu" } // 개발 메뉴 화면

// --- 프로젝트 관련 목적지 ---
object AddProject : AppDestination { override val route = "add_project" }
object SetProjectName : AppDestination { override val route = "set_project_name" }
object JoinProject : AppDestination { override val route = "join_project" }

object ProjectSetting : AppDestination {
    override val route = "project_setting"
    const val projectIdArg = "projectId"
    val routeWithArgs = "$route/{$projectIdArg}"
    val arguments = listOf(navArgument(projectIdArg) { type = NavType.StringType })
    fun createRoute(projectId: String) = "$route/$projectId"
}

// --- 프로젝트 구조 관리 목적지 ---
object CreateCategory : AppDestination {
    override val route = "create_category"
    const val projectIdArg = "projectId"
    val routeWithArgs = "$route/{$projectIdArg}"
    val arguments = listOf(navArgument(projectIdArg) { type = NavType.StringType })
    fun createRoute(projectId: String) = "$route/$projectId"
}

object CreateChannel : AppDestination {
    override val route = "create_channel"
    const val projectIdArg = "projectId"
    const val categoryIdArg = "categoryId"
    val routeWithArgs = "$route/{$projectIdArg}/{$categoryIdArg}"
    val arguments = listOf(
        navArgument(projectIdArg) { type = NavType.StringType },
        navArgument(categoryIdArg) { type = NavType.StringType }
    )
    fun createRoute(projectId: String, categoryId: String) = "$route/$projectId/$categoryId"
}

object EditCategory : AppDestination {
    override val route = "edit_category"
    const val projectIdArg = "projectId"
    const val categoryIdArg = "categoryId"
    val routeWithArgs = "$route/{$projectIdArg}/{$categoryIdArg}"
    val arguments = listOf(
        navArgument(projectIdArg) { type = NavType.StringType },
        navArgument(categoryIdArg) { type = NavType.StringType }
    )
    fun createRoute(projectId: String, categoryId: String) = "$route/$projectId/$categoryId"
}

object EditChannel : AppDestination {
    override val route = "edit_channel"
    const val projectIdArg = "projectId"
    const val categoryIdArg = "categoryId"
    const val channelIdArg = "channelId"
    val routeWithArgs = "$route/{$projectIdArg}/{$categoryIdArg}/{$channelIdArg}"
    val arguments = listOf(
        navArgument(projectIdArg) { type = NavType.StringType },
        navArgument(categoryIdArg) { type = NavType.StringType },
        navArgument(channelIdArg) { type = NavType.StringType }
    )
    fun createRoute(projectId: String, categoryId: String, channelId: String) = "$route/$projectId/$categoryId/$channelId"
}

// --- 멤버 및 역할 관리 목적지 ---
object MemberList : AppDestination {
    override val route = "member_list"
    const val projectIdArg = "projectId"
    val routeWithArgs = "$route/{$projectIdArg}"
    val arguments = listOf(navArgument(projectIdArg) { type = NavType.StringType })
    fun createRoute(projectId: String) = "$route/$projectId"
}

object EditMember : AppDestination {
    override val route = "edit_member"
    const val projectIdArg = "projectId"
    const val userIdArg = "userId"
    val routeWithArgs = "$route/{$projectIdArg}/{$userIdArg}"
    val arguments = listOf(
        navArgument(projectIdArg) { type = NavType.StringType },
        navArgument(userIdArg) { type = NavType.StringType }
    )
    fun createRoute(projectId: String, userId: String) = "$route/$projectId/$userId"
}

object RoleList : AppDestination {
    override val route = "role_list"
    const val projectIdArg = "projectId"
    val routeWithArgs = "$route/{$projectIdArg}"
    val arguments = listOf(navArgument(projectIdArg) { type = NavType.StringType })
    fun createRoute(projectId: String) = "$route/$projectId"
}

object EditRole : AppDestination {
    override val route = "edit_role"
    const val projectIdArg = "projectId"
    const val roleIdArg = "roleId" // 역할 ID (생성 시에는 null 또는 특정 값 사용 가능)
    val routeWithArgs = "$route/{$projectIdArg}?$roleIdArg={$roleIdArg}" // roleId는 옵셔널 인자
    val arguments = listOf(
        navArgument(projectIdArg) { type = NavType.StringType },
        navArgument(roleIdArg) {
            type = NavType.StringType
            nullable = true // 생성 시 null 가능
            // defaultValue = null // 기본값 null
        }
    )
    // 역할 생성 시
    fun createAddRoute(projectId: String) = "$route/$projectId"
    // 역할 수정 시
    fun createEditRoute(projectId: String, roleId: String) = "$route/$projectId?$roleIdArg=$roleId"
}


// --- 친구 관련 목적지 ---
object Friends : AppDestination { override val route = "friends" }
object AcceptFriends : AppDestination { override val route = "accept_friends" }

// --- 설정 관련 목적지 ---
object EditProfile : AppDestination { override val route = "edit_profile" }
object ChangePassword : AppDestination { override val route = "change_password" }
// TODO: PersonalSetting 목적지 추가 필요 시

// --- 채팅 관련 목적지 ---
object Chat : AppDestination {
    override val route = "chat"
    const val channelIdArg = "channelId"
    val routeWithArgs = "$route/{$channelIdArg}"
    val arguments = listOf(navArgument(channelIdArg) { type = NavType.StringType })
    fun createRoute(channelId: String) = "$route/$channelId"
}

// --- 캘린더/스케줄 관련 목적지 ---
object Calendar24Hour : AppDestination {
    override val route = "calendar_24hour"
    const val yearArg = "year"
    const val monthArg = "month"
    const val dayArg = "day"
    val routeWithArgs = "$route/{$yearArg}/{$monthArg}/{$dayArg}"
    val arguments = listOf(
        navArgument(yearArg) { type = NavType.IntType },
        navArgument(monthArg) { type = NavType.IntType },
        navArgument(dayArg) { type = NavType.IntType }
    )
    fun createRoute(year: Int, month: Int, day: Int) = "$route/$year/$month/$day"
}

object AddSchedule : AppDestination {
    override val route = "add_schedule"
    const val yearArg = "year"
    const val monthArg = "month"
    const val dayArg = "day"
    val routeWithArgs = "$route/{$yearArg}/{$monthArg}/{$dayArg}"
    val arguments = listOf(
        navArgument(yearArg) { type = NavType.IntType },
        navArgument(monthArg) { type = NavType.IntType },
        navArgument(dayArg) { type = NavType.IntType }
    )
    fun createRoute(year: Int, month: Int, day: Int) = "$route/$year/$month/$day"
}

object ScheduleDetail : AppDestination {
    override val route = "schedule_detail"
    const val scheduleIdArg = "scheduleId"
    val routeWithArgs = "$route/{$scheduleIdArg}"
    val arguments = listOf(navArgument(scheduleIdArg) { type = NavType.StringType })
    fun createRoute(scheduleId: String) = "$route/$scheduleId"
}

// --- 검색 목적지 ---
object Search : AppDestination { override val route = "search" }

sealed class MainBottomNavDestination(
    override val route: String,
    val title: String,
    val icon: ImageVector
) : AppDestination {
    object Home : MainBottomNavDestination("home", "홈", Icons.Default.Home)
    object Calendar : MainBottomNavDestination("calendar", "캘린더", Icons.Default.CalendarMonth)
    object Profile : MainBottomNavDestination("profile", "프로필", Icons.Default.AccountCircle)
}

// 하단 네비게이션 아이템 리스트
val mainBottomNavItems = listOf(
    MainBottomNavDestination.Home,
    MainBottomNavDestination.Calendar,
    MainBottomNavDestination.Profile
)
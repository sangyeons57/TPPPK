package com.example.core_navigation.core

import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.core_navigation.destination.RouteArgs
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using Kotlinx Serialization
 * 
 * This sealed interface hierarchy provides compile-time safety for navigation arguments
 * while maintaining backward compatibility with the existing AppRoutes system.
 */
@Serializable(with = TypeSafeRouteSerializer::class)
sealed interface TypeSafeRoute

// ===== Authentication Routes =====
@Serializable
data object SplashRoute : TypeSafeRoute

@Serializable
data object LoginRoute : TypeSafeRoute

@Serializable
data object SignUpRoute : TypeSafeRoute

@Serializable
data object FindPasswordRoute : TypeSafeRoute

@Serializable
data object TermsOfServiceRoute : TypeSafeRoute

@Serializable
data object PrivacyPolicyRoute : TypeSafeRoute

// ===== Main App Routes =====
@Serializable
data object MainContainerRoute : TypeSafeRoute

@Serializable
data object HomeRoute : TypeSafeRoute

@Serializable
data object CalendarRoute : TypeSafeRoute

@Serializable
data object ProfileRoute : TypeSafeRoute

// ===== Project Routes =====
@Serializable
data object AddProjectRoute : TypeSafeRoute

@Serializable
data object JoinProjectRoute : TypeSafeRoute

@Serializable
data object SetProjectNameRoute : TypeSafeRoute

@Serializable
data object SelectProjectTypeRoute : TypeSafeRoute

@Serializable
data class ProjectDetailRoute(
    val projectId: String
) : TypeSafeRoute {
    companion object {
        const val ROUTE_PATTERN = "project/{${RouteArgs.PROJECT_ID}}"
        val arguments = listOf(navArgument(RouteArgs.PROJECT_ID) { type = NavType.StringType })
    }
}

@Serializable
data class ProjectSettingsRoute(
    val projectId: String
) : TypeSafeRoute {
    companion object {
        const val ROUTE_PATTERN = "project/{${RouteArgs.PROJECT_ID}}/settings"
        val arguments = listOf(navArgument(RouteArgs.PROJECT_ID) { type = NavType.StringType })
    }
}

// ===== Project Structure Routes =====
@Serializable
data class CreateCategoryRoute(
    val projectId: String
) : TypeSafeRoute {
    companion object {
        const val ROUTE_PATTERN = "project/{${RouteArgs.PROJECT_ID}}/category/create"
        val arguments = listOf(navArgument(RouteArgs.PROJECT_ID) { type = NavType.StringType })
    }
}

@Serializable
data class EditCategoryRoute(
    val projectId: String,
    val categoryId: String
) : TypeSafeRoute {
    companion object {
        const val ROUTE_PATTERN = "project/{${RouteArgs.PROJECT_ID}}/category/edit/{${RouteArgs.CATEGORY_ID}}"
        val arguments = listOf(
            navArgument(RouteArgs.PROJECT_ID) { type = NavType.StringType },
            navArgument(RouteArgs.CATEGORY_ID) { type = NavType.StringType }
        )
    }
}

@Serializable
data class CreateChannelRoute(
    val projectId: String,
    val categoryId: String
) : TypeSafeRoute {
    companion object {
        const val ROUTE_PATTERN = "project/{${RouteArgs.PROJECT_ID}}/category/{${RouteArgs.CATEGORY_ID}}/channel/create"
        val arguments = listOf(
            navArgument(RouteArgs.PROJECT_ID) { type = NavType.StringType },
            navArgument(RouteArgs.CATEGORY_ID) { type = NavType.StringType }
        )
    }
}

@Serializable
data class EditChannelRoute(
    val projectId: String,
    val categoryId: String,
    val channelId: String
) : TypeSafeRoute {
    companion object {
        const val ROUTE_PATTERN = "project/{${RouteArgs.PROJECT_ID}}/category/{${RouteArgs.CATEGORY_ID}}/channel/edit/{${RouteArgs.CHANNEL_ID}}"
        val arguments = listOf(
            navArgument(RouteArgs.PROJECT_ID) { type = NavType.StringType },
            navArgument(RouteArgs.CATEGORY_ID) { type = NavType.StringType },
            navArgument(RouteArgs.CHANNEL_ID) { type = NavType.StringType }
        )
    }
}

// ===== Member Management Routes =====
@Serializable
data class MemberListRoute(
    val projectId: String
) : TypeSafeRoute {
    companion object {
        const val ROUTE_PATTERN = "project/{${RouteArgs.PROJECT_ID}}/members"
        val arguments = listOf(navArgument(RouteArgs.PROJECT_ID) { type = NavType.StringType })
    }
}

@Serializable
data class EditMemberRoute(
    val projectId: String,
    val userId: String
) : TypeSafeRoute {
    companion object {
        const val ROUTE_PATTERN = "project/{${RouteArgs.PROJECT_ID}}/members/edit/{${RouteArgs.USER_ID}}"
        val arguments = listOf(
            navArgument(RouteArgs.PROJECT_ID) { type = NavType.StringType },
            navArgument(RouteArgs.USER_ID) { type = NavType.StringType }
        )
    }
}

// ===== Role Management Routes =====
@Serializable
data class RoleListRoute(
    val projectId: String
) : TypeSafeRoute {
    companion object {
        const val ROUTE_PATTERN = "project/{${RouteArgs.PROJECT_ID}}/roles"
        val arguments = listOf(navArgument(RouteArgs.PROJECT_ID) { type = NavType.StringType })
    }
}

@Serializable
data class EditRoleRoute(
    val projectId: String,
    val roleId: String
) : TypeSafeRoute {
    companion object {
        const val ROUTE_PATTERN = "project/{${RouteArgs.PROJECT_ID}}/roles/edit/{${RouteArgs.ROLE_ID}}"
        val arguments = listOf(
            navArgument(RouteArgs.PROJECT_ID) { type = NavType.StringType },
            navArgument(RouteArgs.ROLE_ID) { type = NavType.StringType }
        )
    }
}

@Serializable
data class AddRoleRoute(
    val projectId: String
) : TypeSafeRoute {
    companion object {
        const val ROUTE_PATTERN = "project/{${RouteArgs.PROJECT_ID}}/roles/add"
        val arguments = listOf(navArgument(RouteArgs.PROJECT_ID) { type = NavType.StringType })
    }
}

// ===== Chat Routes =====
@Serializable
data class ChatRoute(
    val channelId: String,
    val messageId: String? = null
) : TypeSafeRoute {
    companion object {
        const val ROUTE_PATTERN = "chat/{${RouteArgs.CHANNEL_ID}}"
        val arguments = listOf(navArgument(RouteArgs.CHANNEL_ID) { type = NavType.StringType })
    }
}

// ===== Schedule Routes =====
@Serializable
data class Calendar24HourRoute(
    val year: Int,
    val month: Int,
    val day: Int
) : TypeSafeRoute {
    companion object {
        const val ROUTE_PATTERN = "schedule/24hour/{${RouteArgs.YEAR}}/{${RouteArgs.MONTH}}/{${RouteArgs.DAY}}"
        val arguments = listOf(
            navArgument(RouteArgs.YEAR) { type = NavType.IntType },
            navArgument(RouteArgs.MONTH) { type = NavType.IntType },
            navArgument(RouteArgs.DAY) { type = NavType.IntType }
        )
    }
}

@Serializable
data class AddScheduleRoute(
    val year: Int,
    val month: Int,
    val day: Int
) : TypeSafeRoute {
    companion object {
        const val ROUTE_PATTERN = "schedule/add/{${RouteArgs.YEAR}}/{${RouteArgs.MONTH}}/{${RouteArgs.DAY}}"
        val arguments = listOf(
            navArgument(RouteArgs.YEAR) { type = NavType.IntType },
            navArgument(RouteArgs.MONTH) { type = NavType.IntType },
            navArgument(RouteArgs.DAY) { type = NavType.IntType }
        )
    }
}

@Serializable
data class ScheduleDetailRoute(
    val scheduleId: String
) : TypeSafeRoute {
    companion object {
        const val ROUTE_PATTERN = "schedule/detail/{${RouteArgs.SCHEDULE_ID}}"
        val arguments = listOf(navArgument(RouteArgs.SCHEDULE_ID) { type = NavType.StringType })
    }
}

@Serializable
data class EditScheduleRoute(
    val scheduleId: String
) : TypeSafeRoute {
    companion object {
        const val ROUTE_PATTERN = "schedule/edit/{${RouteArgs.SCHEDULE_ID}}"
        val arguments = listOf(navArgument(RouteArgs.SCHEDULE_ID) { type = NavType.StringType })
    }
}

// ===== User Routes =====
@Serializable
data class UserProfileRoute(
    val userId: String
) : TypeSafeRoute {
    companion object {
        const val ROUTE_PATTERN = "user/{${RouteArgs.USER_ID}}/profile"
        val arguments = listOf(navArgument(RouteArgs.USER_ID) { type = NavType.StringType })
    }
}

// ===== Settings Routes =====
@Serializable
data object EditMyProfileRoute : TypeSafeRoute

@Serializable
data object AppSettingsRoute : TypeSafeRoute

@Serializable
data object ChangePasswordRoute : TypeSafeRoute

// ===== Friends Routes =====
@Serializable
data object FriendsListRoute : TypeSafeRoute

@Serializable
data object AcceptFriendsRoute : TypeSafeRoute

// ===== Search Routes =====
@Serializable
data object GlobalSearchRoute : TypeSafeRoute

@Serializable
data class MessageDetailRoute(
    val channelId: String,
    val messageId: String
) : TypeSafeRoute {
    companion object {
        const val ROUTE_PATTERN = "project/channel/{${RouteArgs.CHANNEL_ID}}/message/{${RouteArgs.MESSAGE_ID}}"
        val arguments = listOf(
            navArgument(RouteArgs.CHANNEL_ID) { type = NavType.StringType },
            navArgument(RouteArgs.MESSAGE_ID) { type = NavType.StringType }
        )
    }
}

// ===== Development Routes =====
@Serializable
data object DevMenuRoute : TypeSafeRoute

// ===== FCM Routes =====
@Serializable
data object FCMTestRoute : TypeSafeRoute

/**
 * Backward compatibility layer that maps TypeSafeRoutes to existing AppRoutes paths
 */
object TypeSafeRouteCompat {
    
    /**
     * Converts a TypeSafeRoute to its corresponding AppRoutes path for backward compatibility
     */
    fun TypeSafeRoute.toAppRoutePath(): String = when (this) {
        // Auth routes
        is SplashRoute -> "auth/splash"
        is LoginRoute -> "auth/login"
        is SignUpRoute -> "auth/signup"
        is FindPasswordRoute -> "auth/find_password"
        is TermsOfServiceRoute -> "auth/terms_of_service"
        is PrivacyPolicyRoute -> "auth/privacy_policy"
        
        // Main routes
        is MainContainerRoute -> "main_host"
        is HomeRoute -> "main_home_content"
        is CalendarRoute -> "main_calendar_content"
        is ProfileRoute -> "main_profile_content"
        
        // Project routes
        is AddProjectRoute -> "project/add"
        is JoinProjectRoute -> "project/join"
        is SetProjectNameRoute -> "project/set_name"
        is SelectProjectTypeRoute -> "project/select_type"
        is ProjectDetailRoute -> "project/$projectId"
        is ProjectSettingsRoute -> "project/$projectId/settings"
        
        // Project structure routes
        is CreateCategoryRoute -> "project/$projectId/category/create"
        is EditCategoryRoute -> "project/$projectId/category/edit/$categoryId"
        is CreateChannelRoute -> "project/$projectId/category/$categoryId/channel/create"
        is EditChannelRoute -> "project/$projectId/category/$categoryId/channel/edit/$channelId"
        
        // Member routes
        is MemberListRoute -> "project/$projectId/members"
        is EditMemberRoute -> "project/$projectId/members/edit/$userId"
        
        // Role routes
        is RoleListRoute -> "project/$projectId/roles"
        is EditRoleRoute -> "project/$projectId/roles/edit/$roleId"
        is AddRoleRoute -> "project/$projectId/roles/add"
        
        // Chat routes
        is ChatRoute -> if (messageId != null) "chat/$channelId?messageId=$messageId" else "chat/$channelId"
        
        // Schedule routes
        is Calendar24HourRoute -> "schedule/24hour/$year/$month/$day"
        is AddScheduleRoute -> "schedule/add/$year/$month/$day"
        is ScheduleDetailRoute -> "schedule/detail/$scheduleId"
        is EditScheduleRoute -> "schedule/edit/$scheduleId"
        
        // User routes
        is UserProfileRoute -> "user/$userId/profile"
        
        // Settings routes
        is EditMyProfileRoute -> "settings/profile/edit"
        is AppSettingsRoute -> "settings/application"
        is ChangePasswordRoute -> "settings/password/change"
        
        // Friends routes
        is FriendsListRoute -> "friends/list"
        is AcceptFriendsRoute -> "friends/accept"
        
        // Search routes
        is GlobalSearchRoute -> "search/global"
        is MessageDetailRoute -> "project/channel/$channelId/message/$messageId"
        
        // Development routes
        is DevMenuRoute -> "dev/menu"
        
        // FCM routes
        is FCMTestRoute -> "fcm/test"
    }
}
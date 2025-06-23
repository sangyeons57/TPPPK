package com.example.core_navigation.extension

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.core.NavigationResultManager
import com.example.core_navigation.destination.AppRoutes

/**
 * Extension functions to simplify AppNavigationGraph.kt definitions
 * and provide type-safe argument extraction utilities.
 */

// ===== Type-safe argument extraction =====

/**
 * Safely extracts a String argument from NavBackStackEntry.
 */
fun NavBackStackEntry.getStringArg(key: String): String? {
    return arguments?.getString(key)
}

/**
 * Safely extracts a required String argument from NavBackStackEntry.
 * Throws IllegalArgumentException if the argument is missing.
 */
fun NavBackStackEntry.getRequiredStringArg(key: String): String {
    return getStringArg(key) ?: throw IllegalArgumentException("Required argument '$key' is missing")
}

/**
 * Safely extracts an Int argument from NavBackStackEntry.
 */
fun NavBackStackEntry.getIntArg(key: String): Int? {
    return arguments?.getInt(key)
}

/**
 * Safely extracts a required Int argument from NavBackStackEntry.
 */
fun NavBackStackEntry.getRequiredIntArg(key: String): Int {
    return getIntArg(key) ?: throw IllegalArgumentException("Required argument '$key' is missing")
}

/**
 * Safely extracts a Long argument from NavBackStackEntry.
 */
fun NavBackStackEntry.getLongArg(key: String): Long? {
    return arguments?.getLong(key)
}

/**
 * Safely extracts a Boolean argument from NavBackStackEntry.
 */
fun NavBackStackEntry.getBooleanArg(key: String): Boolean? {
    return arguments?.getBoolean(key)
}

// ===== Enhanced composable builders =====

/**
 * Enhanced composable function that provides automatic argument binding
 * for Project routes with projectId parameter.
 */
fun NavGraphBuilder.projectComposable(
    route: String,
    arguments: List<androidx.navigation.NamedNavArgument> = emptyList(),
    content: @Composable (projectId: String, navigationManger: NavigationManger) -> Unit
) {
    composable(route = route, arguments = arguments) { backStackEntry ->
        val projectId = backStackEntry.getRequiredStringArg(AppRoutes.Project.ARG_PROJECT_ID)
        // Note: AppNavigator should be injected via Hilt in the actual composable
        // This is a simplified example - in practice, you'd get AppNavigator differently
        content(projectId, TODO("Inject AppNavigator"))
    }
}

/**
 * Enhanced composable function for Schedule routes with date parameters.
 */
fun NavGraphBuilder.scheduleComposable(
    route: String,
    arguments: List<androidx.navigation.NamedNavArgument> = emptyList(),
    content: @Composable (year: Int, month: Int, day: Int, navigationManger: NavigationManger) -> Unit
) {
    composable(route = route, arguments = arguments) { backStackEntry ->
        val year = backStackEntry.getRequiredIntArg(AppRoutes.Main.Calendar.ARG_YEAR)
        val month = backStackEntry.getRequiredIntArg(AppRoutes.Main.Calendar.ARG_MONTH)
        val day = backStackEntry.getRequiredIntArg(AppRoutes.Main.Calendar.ARG_DAY)
        content(year, month, day, TODO("Inject AppNavigator"))
    }
}

/**
 * Enhanced composable function for Chat routes with channel and optional message parameters.
 */
fun NavGraphBuilder.chatComposable(
    route: String,
    arguments: List<androidx.navigation.NamedNavArgument> = emptyList(),
    content: @Composable (channelId: String, messageId: String?, navigationManger: NavigationManger) -> Unit
) {
    composable(route = route, arguments = arguments) { backStackEntry ->
        val channelId = backStackEntry.getRequiredStringArg(AppRoutes.Chat.ARG_CHANNEL_ID)
        val messageId = backStackEntry.getStringArg(AppRoutes.Chat.ARG_MESSAGE_ID)
        content(channelId, messageId, TODO("Inject AppNavigator"))
    }
}

// ===== Argument creation helpers =====

/**
 * Creates a required String argument for navigation.
 */
fun stringArg(name: String) = navArgument(name) {
    type = NavType.StringType
}

/**
 * Creates an optional String argument for navigation.
 */
fun optionalStringArg(name: String, defaultValue: String? = null) = navArgument(name) {
    type = NavType.StringType
    nullable = true
    defaultValue?.let { this.defaultValue = it }
}

/**
 * Creates a required Int argument for navigation.
 */
fun intArg(name: String) = navArgument(name) {
    type = NavType.IntType
}

/**
 * Creates an optional Int argument for navigation.
 */
fun optionalIntArg(name: String, defaultValue: Int = 0) = navArgument(name) {
    type = NavType.IntType
    this.defaultValue = defaultValue
}

/**
 * Creates a required Long argument for navigation.
 */
fun longArg(name: String) = navArgument(name) {
    type = NavType.LongType
}

/**
 * Creates a Boolean argument for navigation.
 */
fun booleanArg(name: String, defaultValue: Boolean = false) = navArgument(name) {
    type = NavType.BoolType
    this.defaultValue = defaultValue
}

// ===== Result handling helpers for composables =====

/**
 * Helper to set navigation results within composables.
 */
@Composable
fun SetNavigationResult(
    resultManager: NavigationResultManager,
    navController: androidx.navigation.NavHostController,
    key: String,
    value: Any?
) {
    resultManager.setResult(navController, key, value)
}

/**
 * Helper to observe navigation results within composables.
 */
@Composable
fun <T> ObserveNavigationResult(
    resultManager: NavigationResultManager,
    navController: androidx.navigation.NavHostController,
    key: String,
    onResult: (T) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(key) {
        resultManager.observeResult<T>(navController, key).collect { result ->
            result?.let(onResult)
        }
    }
}

// ===== Migration helpers for existing AppNavigationGraph.kt =====

/**
 * Wrapper for the current argument extraction pattern to ease migration.
 * Replace manual argument extraction with this helper.
 */
fun NavBackStackEntry.extractProjectArguments(): ProjectArguments {
    return ProjectArguments(
        projectId = getRequiredStringArg(AppRoutes.Project.ARG_PROJECT_ID),
        categoryId = getStringArg(AppRoutes.Project.ARG_CATEGORY_ID),
        channelId = getStringArg(AppRoutes.Project.ARG_CHANNEL_ID),
        userId = getStringArg(AppRoutes.Project.ARG_USER_ID),
        roleId = getStringArg(AppRoutes.Project.ARG_ROLE_ID)
    )
}

/**
 * Data class to hold project-related arguments.
 */
data class ProjectArguments(
    val projectId: String,
    val categoryId: String? = null,
    val channelId: String? = null,
    val userId: String? = null,
    val roleId: String? = null
)

/**
 * Wrapper for calendar argument extraction.
 */
fun NavBackStackEntry.extractCalendarArguments(): CalendarArguments {
    return CalendarArguments(
        year = getRequiredIntArg(AppRoutes.Main.Calendar.ARG_YEAR),
        month = getRequiredIntArg(AppRoutes.Main.Calendar.ARG_MONTH),
        day = getRequiredIntArg(AppRoutes.Main.Calendar.ARG_DAY),
        scheduleId = getStringArg(AppRoutes.Main.Calendar.ARG_SCHEDULE_ID)
    )
}

/**
 * Data class to hold calendar-related arguments.
 */
data class CalendarArguments(
    val year: Int,
    val month: Int,
    val day: Int,
    val scheduleId: String? = null
)

/**
 * Wrapper for chat argument extraction.
 */
fun NavBackStackEntry.extractChatArguments(): ChatArguments {
    return ChatArguments(
        channelId = getRequiredStringArg(AppRoutes.Chat.ARG_CHANNEL_ID),
        messageId = getStringArg(AppRoutes.Chat.ARG_MESSAGE_ID)
    )
}

/**
 * Data class to hold chat-related arguments.
 */
data class ChatArguments(
    val channelId: String,
    val messageId: String? = null
)

// ===== Simplified graph building patterns =====

/**
 * Creates a navigation graph with error handling for missing arguments.
 */
inline fun NavGraphBuilder.safeComposable(
    route: String,
    arguments: List<androidx.navigation.NamedNavArgument> = emptyList(),
    crossinline content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(route = route, arguments = arguments) { backStackEntry ->
        try {
            content(backStackEntry)
        } catch (e: IllegalArgumentException) {
            // Handle missing arguments gracefully
            androidx.compose.material3.Text(
                text = "Navigation Error: ${e.message}",
                color = androidx.compose.material3.MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Creates a standard argument list for project routes.
 */
fun projectArguments() = listOf(
    stringArg(AppRoutes.Project.ARG_PROJECT_ID)
)

/**
 * Creates a standard argument list for project with category routes.
 */
fun projectCategoryArguments() = listOf(
    stringArg(AppRoutes.Project.ARG_PROJECT_ID),
    stringArg(AppRoutes.Project.ARG_CATEGORY_ID)
)

/**
 * Creates a standard argument list for project with category and channel routes.
 */
fun projectChannelArguments() = listOf(
    stringArg(AppRoutes.Project.ARG_PROJECT_ID),
    stringArg(AppRoutes.Project.ARG_CATEGORY_ID),
    stringArg(AppRoutes.Project.ARG_CHANNEL_ID)
)

/**
 * Creates a standard argument list for calendar routes.
 */
fun calendarArguments() = listOf(
    intArg(AppRoutes.Main.Calendar.ARG_YEAR),
    intArg(AppRoutes.Main.Calendar.ARG_MONTH),
    intArg(AppRoutes.Main.Calendar.ARG_DAY)
)

/**
 * Creates a standard argument list for schedule routes.
 */
fun scheduleArguments() = listOf(
    stringArg(AppRoutes.Main.Calendar.ARG_SCHEDULE_ID)
)

/**
 * Creates a standard argument list for chat routes.
 */
fun chatArguments() = listOf(
    stringArg(AppRoutes.Chat.ARG_CHANNEL_ID)
)
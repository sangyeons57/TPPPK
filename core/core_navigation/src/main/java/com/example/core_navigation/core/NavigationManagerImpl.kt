package com.example.core_navigation.core

import android.util.Log
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.example.core_navigation.core.TypeSafeRouteCompat.toAppRoutePath
import com.example.domain.model.vo.DocumentId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Direct navigation manager with clean, simple API.
 * 
 * This implementation provides straightforward navigation methods without
 * the complexity of command patterns, making it intuitive for feature developers.
 */
@Singleton
class NavigationManagerImpl @Inject constructor(
    private val resultManager: NavigationResultManager
) : NavigationManger {

    // Primary navigation controller (Activity level NavHost)
    private var parentNavController: NavHostController? = null
    
    // Currently active child navigation controller (nested NavHost)
    private var activeChildNavController: NavHostController? = null

    // ===== NavController Management =====
    
    override fun setNavController(navController: NavHostController) {
        if (parentNavController == null) {
            this.parentNavController = navController
            Log.d("NavigationManager", "Parent NavController set")
        } else {
            Log.w("NavigationManager", "Parent NavController already set")
        }
    }

    override fun getNavController(): NavHostController? = parentNavController

    override fun setChildNavController(navController: NavHostController?) {
        this.activeChildNavController = navController
        val graphRoute = try {
            navController?.graph?.route
        } catch (e: IllegalStateException) {
            null 
        }
        Log.d("NavigationManager", "Child NavController updated. Graph route: $graphRoute")
    }

    override fun getChildNavController(): NavHostController? = activeChildNavController

    override fun getResultManager(): NavigationResultManager = resultManager

    override fun saveScreenState(screenKey: String, state: android.os.Bundle) {
        Log.d("NavigationManager", "Saving screen state for key: $screenKey")
        // TODO: Implement screen state persistence if needed
        // For now, this is a no-op implementation
    }

    // ===== Core Navigation =====
    
    override fun navigateBack(): Boolean {
        // Try child controller first
        if (activeChildNavController?.popBackStack() == true) {
            Log.d("NavigationManager", "Navigated back using child NavController")
            return true
        }
        
        // Try parent controller
        if (parentNavController?.popBackStack() == true) {
            Log.d("NavigationManager", "Navigated back using parent NavController")
            // Clear child controller reference when navigating back on parent
            activeChildNavController = null
            return true
        }
        
        Log.d("NavigationManager", "No back stack entries to pop")
        return false
    }

    override fun navigateTo(route: TypeSafeRoute, navOptions: NavOptions?) {
        val routePath = route.toAppRoutePath()
        executeNavigation(routePath, navOptions)
    }

    override fun navigateToClearingBackStack(route: TypeSafeRoute) {
        val routePath = route.toAppRoutePath()
        executeNavigationClearingBackStack(routePath)
    }

    override fun <T> navigateBackWithResult(key: String, result: T): Boolean {
        return try {
            val currentController = activeChildNavController ?: parentNavController
            currentController?.let { controller ->
                resultManager.setResult(controller, key, result)
                navigateBack()
            } ?: false
        } catch (e: Exception) {
            Log.e("NavigationManager", "Failed to navigate back with result: ${e.message}")
            false
        }
    }

    // ===== Private Navigation Helpers =====

    private fun executeNavigation(route: String, navOptions: NavOptions? = null) {
        val targetController = activeChildNavController ?: parentNavController
        targetController?.let { controller ->
            try {
                controller.navigate(route, navOptions)
                Log.d("NavigationManager", "Navigated to: $route")
            } catch (e: Exception) {
                Log.e("NavigationManager", "Failed to navigate to $route: ${e.message}")
            }
        } ?: Log.e("NavigationManager", "No NavController available for navigation")
    }

    private fun executeNavigationOnParent(route: String, navOptions: NavOptions? = null) {
        parentNavController?.let { controller ->
            try {
                controller.navigate(route, navOptions)
                Log.d("NavigationManager", "Navigated on parent to: $route")
            } catch (e: Exception) {
                Log.e("NavigationManager", "Failed to navigate on parent to $route: ${e.message}")
            }
        } ?: Log.e("NavigationManager", "Parent NavController not available for navigation")
    }

    private fun executeNavigationClearingBackStack(route: String) {
        val targetController = parentNavController // Always use parent for clearing back stack
        targetController?.let { controller ->
            try {
                controller.navigate(route) {
                    popUpTo(controller.graph.findStartDestination().id) { inclusive = true }
                }
                Log.d("NavigationManager", "Navigated to $route clearing back stack")
            } catch (e: Exception) {
                Log.e("NavigationManager", "Failed to navigate clearing back stack: ${e.message}")
            }
        } ?: Log.e("NavigationManager", "No NavController available for navigation")
    }
    
    // ===== Convenience Navigation Methods =====

    override fun navigateToSplash(navOptions: NavOptions?) {
        navigateTo(SplashRoute, navOptions)
    }

    override fun navigateToLogin(navOptions: NavOptions?) {
        navigateTo(LoginRoute, navOptions)
    }

    override fun navigateToSignUp(navOptions: NavOptions?) {
        navigateTo(SignUpRoute, navOptions)
    }

    override fun navigateToMain(navOptions: NavOptions?) {
        navigateTo(MainContainerRoute, navOptions)
    }

    override fun navigateToProjectDetails(projectId: String, navOptions: NavOptions?) {
        executeNavigationOnParent(ProjectDetailRoute(projectId).toAppRoutePath(), navOptions)
    }

    override fun navigateToProjectSettings(projectId: String, navOptions: NavOptions?) {
        executeNavigationOnParent(ProjectSettingsRoute(projectId).toAppRoutePath(), navOptions)
    }

    override fun navigateToChat(channelId: String, messageId: String?, navOptions: NavOptions?) {
        executeNavigationOnParent(ChatRoute(channelId, messageId).toAppRoutePath(), navOptions)
    }

    override fun navigateToAddProject(navOptions: NavOptions?) {
        executeNavigationOnParent(AddProjectRoute.toAppRoutePath(), navOptions)
    }

    override fun navigateToJoinProject(navOptions: NavOptions?) {
        executeNavigationOnParent(JoinProjectRoute.toAppRoutePath(), navOptions)
    }

    override fun navigateToCalendar(year: Int, month: Int, day: Int, navOptions: NavOptions?) {
        executeNavigationOnParent(Calendar24HourRoute(year, month, day).toAppRoutePath(), navOptions)
    }

    override fun navigateToAddSchedule(year: Int, month: Int, day: Int, navOptions: NavOptions?) {
        executeNavigationOnParent(AddScheduleRoute(year, month, day).toAppRoutePath(), navOptions)
    }

    override fun navigateToScheduleDetail(scheduleId: String, navOptions: NavOptions?) {
        executeNavigationOnParent(ScheduleDetailRoute(scheduleId).toAppRoutePath(), navOptions)
    }

    override fun navigateToUserProfile(userId: String, navOptions: NavOptions?) {
        executeNavigationOnParent(UserProfileRoute(userId).toAppRoutePath(), navOptions)
    }

    override fun navigateToEditProfile(navOptions: NavOptions?) {
        executeNavigationOnParent(EditMyProfileRoute.toAppRoutePath(), navOptions)
    }

    override fun navigateToFriends(navOptions: NavOptions?) {
        executeNavigationOnParent(FriendsListRoute.toAppRoutePath(), navOptions)
    }

    override fun navigateToFindPassword(navOptions: NavOptions?) {
        executeNavigationOnParent(FindPasswordRoute.toAppRoutePath(), navOptions)
    }

    override fun navigateToHome(navOptions: NavOptions?) {
        navigateToMain(navOptions)
    }

    override fun navigateToMessageDetail(
        channelId: String,
        messageId: String?,
        navOptions: NavOptions?
    ) {
        val route = if (messageId != null) {
            MessageDetailRoute(channelId, messageId)
        } else {
            ChatRoute(channelId)
        }
        executeNavigationOnParent(route.toAppRoutePath(), navOptions)
    }

    override fun navigateToTermsOfService(navOptions: NavOptions?) {
        executeNavigationOnParent(TermsOfServiceRoute.toAppRoutePath(), navOptions)
    }

    override fun navigateToPrivacyPolicy(navOptions: NavOptions?) {
        executeNavigationOnParent(PrivacyPolicyRoute.toAppRoutePath(), navOptions)
    }

    override fun navigateToEditMember(projectId: String, userId: String, navOptions: NavOptions?) {
        executeNavigationOnParent(EditMemberRoute(projectId, userId).toAppRoutePath(), navOptions)
    }

    override fun navigateToEditSchedule(scheduleId: String, navOptions: NavOptions?) {
        executeNavigationOnParent(EditScheduleRoute(scheduleId).toAppRoutePath(), navOptions)
    }

    override fun navigateToAcceptFriends(navOptions: NavOptions?) {
        executeNavigationOnParent(AcceptFriendsRoute.toAppRoutePath(), navOptions)
    }

    override fun <T> setResult(key: String, result: T) {
        try {
            val currentController = activeChildNavController ?: parentNavController
            currentController?.let { controller ->
                resultManager.setResult(controller, key, result)
                Log.d("NavigationManager", "Set result for key: $key")
            } ?: Log.e("NavigationManager", "No NavController available to set result")
        } catch (e: Exception) {
            Log.e("NavigationManager", "Failed to set result for key $key: ${e.message}")
        }
    }

    override fun <T> getResult(key: String): T? {
        return try {
            val currentController = activeChildNavController ?: parentNavController
            currentController?.let { controller ->
                val result = resultManager.getResult<T>(controller, key)
                Log.d("NavigationManager", "Retrieved result for key: $key, value: $result")
                result
            }
        } catch (e: Exception) {
            Log.e("NavigationManager", "Failed to get result for key $key: ${e.message}")
            null
        }
    }

}
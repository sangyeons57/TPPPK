package com.example.core_navigation.core

import androidx.navigation.NavHostController
import androidx.navigation.NavOptions

/**
 * Clean, direct navigation interface without command pattern complexity.
 * 
 * This interface provides straightforward navigation methods that feature developers
 * can use intuitively without needing to understand complex command patterns.
 */
interface NavigationManger {
    // ===== Core Navigation =====
    
    /**
     * Navigates back in the navigation stack.
     * @return True if navigation was successful
     */
    fun navigateBack(): Boolean
    
    /**
     * Navigates to a type-safe route.
     */
    fun navigateTo(route: TypeSafeRoute, navOptions: NavOptions? = null)
    
    /**
     * Navigates to a route clearing the back stack.
     */
    fun navigateToClearingBackStack(route: TypeSafeRoute)
    
    // ===== NavController Management =====
    
    /**
     * Sets the primary NavController (called from main NavHost).
     */
    fun setNavController(navController: NavHostController)

    /**
     * Gets the primary NavController.
     */
    fun getNavController(): NavHostController?
    
    /**
     * Sets the active child NavController for nested navigation.
     */
    fun setChildNavController(navController: NavHostController?)
    
    /**
     * Gets the currently active child NavController.
     */
    fun getChildNavController(): NavHostController?
    
    // ===== Convenience Navigation Methods =====
    
    /**
     * Navigates to splash screen.
     */
    fun navigateToSplash(navOptions: NavOptions? = null)
    
    /**
     * Navigates to login screen.
     */
    fun navigateToLogin(navOptions: NavOptions? = null)
    
    /**
     * Navigates to signup screen.
     */
    fun navigateToSignUp(navOptions: NavOptions? = null)
    
    /**
     * Navigates to main container (home screen).
     */
    fun navigateToMain(navOptions: NavOptions? = null)
    
    /**
     * Navigates to project details.
     */
    fun navigateToProjectDetails(projectId: String, navOptions: NavOptions? = null)
    
    /**
     * Navigates to project settings.
     */
    fun navigateToProjectSettings(projectId: String, navOptions: NavOptions? = null)
    
    /**
     * Navigates to chat screen.
     */
    fun navigateToChat(channelId: String, messageId: String? = null, navOptions: NavOptions? = null)
    
    /**
     * Navigates to add project screen.
     */
    fun navigateToAddProject(navOptions: NavOptions? = null)
    
    /**
     * Navigates to join project screen.
     */
    fun navigateToJoinProject(navOptions: NavOptions? = null)
    
    /**
     * Navigates to calendar screen with specific date.
     */
    fun navigateToCalendar(year: Int, month: Int, day: Int, navOptions: NavOptions? = null)
    
    /**
     * Navigates to add schedule screen.
     */
    fun navigateToAddSchedule(year: Int, month: Int, day: Int, navOptions: NavOptions? = null)
    
    /**
     * Navigates to schedule detail screen.
     */
    fun navigateToScheduleDetail(scheduleId: String, navOptions: NavOptions? = null)
    
    /**
     * Navigates to user profile screen.
     */
    fun navigateToUserProfile(userId: String, navOptions: NavOptions? = null)
    
    /**
     * Navigates to edit profile screen.
     */
    fun navigateToEditProfile(navOptions: NavOptions? = null)
    
    /**
     * Navigates to friends list screen.
     */
    fun navigateToFriends(navOptions: NavOptions? = null)
    
    // ===== Result Handling Convenience =====
    
    /**
     * Navigates back with a result.
     * Convenience method that sets result and navigates back in one call.
     */
    fun <T> navigateBackWithResult(key: String, result: T): Boolean
    
    /**
     * Sets a result that can be observed by the previous screen.
     * Used for passing data back in navigation flow.
     */
    fun <T> setResult(key: String, result: T)
} 
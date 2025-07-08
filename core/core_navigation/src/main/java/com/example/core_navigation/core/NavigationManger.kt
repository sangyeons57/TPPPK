package com.example.core_navigation.core

import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.example.domain.model.vo.DocumentId
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean, direct navigation interface without command pattern complexity.
 * 
 * This interface provides straightforward navigation methods that feature developers
 * can use intuitively without needing to understand complex command patterns.
 */
interface NavigationManger {
    // ===== Navigation State Management =====
    
    /**
     * 네비게이션이 현재 진행 중인지를 나타내는 StateFlow입니다.
     * UI 컴포넌트에서 구독하여 로딩 상태나 버튼 비활성화 등에 활용할 수 있습니다.
     */
    val isNavigationInProgress: StateFlow<Boolean>
    
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

    /**
     * Gets the NavigationResultManager instance.
     */
    fun getResultManager(): NavigationResultManager

    /**
     * Saves screen state for restoration.
     */
    fun saveScreenState(screenKey: String, state: android.os.Bundle)
    
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
     * Navigates to join project screen with a pre-filled invite code.
     */
    fun navigateToJoinProjectWithInviteCode(inviteCode: String, navOptions: NavOptions? = null)
    
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

    /**
     * Navigates to find password screen.
     */
    fun navigateToFindPassword(navOptions: NavOptions? = null)

    /**
     * Navigates to home screen (main container).
     */
    fun navigateToHome(navOptions: NavOptions? = null)

    /**
     * Navigates to message detail screen.
     */
    fun navigateToMessageDetail(
        channelId: String,
        messageId: String? = null,
        navOptions: NavOptions? = null
    )

    /**
     * Navigates to terms of service screen.
     */
    fun navigateToTermsOfService(navOptions: NavOptions? = null)

    /**
     * Navigates to privacy policy screen.
     */
    fun navigateToPrivacyPolicy(navOptions: NavOptions? = null)

    /**
     * Navigates to edit member screen.
     */
    fun navigateToEditMember(projectId: String, userId: String, navOptions: NavOptions? = null)

    /**
     * Navigates to edit schedule screen.
     */
    fun navigateToEditSchedule(scheduleId: String, navOptions: NavOptions? = null)

    /**
     * Navigates to accept friends screen.
     */
    fun navigateToAcceptFriends(navOptions: NavOptions? = null)
    
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
    
    /**
     * Gets a result that was set by a previous screen.
     * Used for retrieving data passed through navigation flow.
     */
    fun <T> getResult(key: String): T?
} 
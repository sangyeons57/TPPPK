package com.example.core_navigation.core

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages navigation results using SavedStateHandle for lifecycle-aware data transfer between screens.
 * 
 * This class provides a clean, type-safe API for passing results back from destination screens
 * to their source screens, leveraging the NavController's built-in SavedStateHandle mechanism.
 */
@Singleton
class NavigationResultManager @Inject constructor() {
    
    /**
     * Sets a result value that will be delivered to the previous screen in the back stack.
     * 
     * @param navController The current NavHostController
     * @param key The unique key to identify this result
     * @param value The result value to pass back
     * @throws IllegalStateException if there's no previous back stack entry
     */
    fun <T> setResult(navController: NavHostController, key: String, value: T) {
        val previousEntry = navController.previousBackStackEntry
            ?: throw IllegalStateException("No previous back stack entry to set result on")
        
        previousEntry.savedStateHandle[key] = value
    }
    
    /**
     * Gets a result value from the current screen's SavedStateHandle.
     * This is typically called by the screen that expects to receive a result.
     * 
     * @param navController The current NavHostController
     * @param key The unique key to identify the result
     * @return The result value or null if not found
     */
    fun <T> getResult(navController: NavHostController, key: String): T? {
        return navController.currentBackStackEntry?.savedStateHandle?.get<T>(key)
    }
    
    /**
     * Gets a result value from a SavedStateHandle directly.
     * Useful in ViewModels where you have direct access to SavedStateHandle.
     * 
     * @param savedStateHandle The SavedStateHandle from the ViewModel
     * @param key The unique key to identify the result
     * @return The result value or null if not found
     */
    fun <T> getResult(savedStateHandle: SavedStateHandle, key: String): T? {
        return savedStateHandle.get<T>(key)
    }
    
    /**
     * Observes result values as a Flow from the current screen's SavedStateHandle.
     * The Flow will emit whenever the result value changes.
     * 
     * @param navController The current NavHostController
     * @param key The unique key to identify the result
     * @return A Flow that emits result values
     */
    fun <T> observeResult(navController: NavHostController, key: String): Flow<T?> {
        val currentEntry = navController.currentBackStackEntry
            ?: throw IllegalStateException("No current back stack entry to observe results on")
        
        return currentEntry.savedStateHandle.getStateFlow<T?>(key, null)
    }
    
    /**
     * Observes result values as a Flow from a SavedStateHandle directly.
     * 
     * @param savedStateHandle The SavedStateHandle from the ViewModel
     * @param key The unique key to identify the result
     * @return A Flow that emits result values
     */
    fun <T> observeResult(savedStateHandle: SavedStateHandle, key: String): Flow<T?> {
        return savedStateHandle.getStateFlow<T?>(key, null)
    }
    
    /**
     * Observes result values as a non-null Flow.
     * The Flow will only emit when a non-null value is available.
     * 
     * @param navController The current NavHostController
     * @param key The unique key to identify the result
     * @return A Flow that emits non-null result values
     */
    fun <T : Any> observeNonNullResult(navController: NavHostController, key: String): Flow<T> {
        return observeResult<T>(navController, key)
            .map { it ?: throw IllegalStateException("Expected non-null result for key: $key") }
    }
    
    /**
     * Observes result values as a non-null Flow from a SavedStateHandle directly.
     * 
     * @param savedStateHandle The SavedStateHandle from the ViewModel
     * @param key The unique key to identify the result
     * @return A Flow that emits non-null result values
     */
    fun <T : Any> observeNonNullResult(savedStateHandle: SavedStateHandle, key: String): Flow<T> {
        return observeResult<T>(savedStateHandle, key)
            .map { it ?: throw IllegalStateException("Expected non-null result for key: $key") }
    }
    
    /**
     * Removes a result value from the current screen's SavedStateHandle.
     * Useful for one-time consumption of results.
     * 
     * @param navController The current NavHostController
     * @param key The unique key to identify the result
     * @return The result value that was removed, or null if not found
     */
    fun <T> consumeResult(navController: NavHostController, key: String): T? {
        val currentEntry = navController.currentBackStackEntry ?: return null
        val savedStateHandle = currentEntry.savedStateHandle
        val result = savedStateHandle.get<T>(key)
        savedStateHandle.remove<T>(key)
        return result
    }
    
    /**
     * Removes a result value from a SavedStateHandle directly.
     * 
     * @param savedStateHandle The SavedStateHandle from the ViewModel
     * @param key The unique key to identify the result
     * @return The result value that was removed, or null if not found
     */
    fun <T> consumeResult(savedStateHandle: SavedStateHandle, key: String): T? {
        val result = savedStateHandle.get<T>(key)
        savedStateHandle.remove<T>(key)
        return result
    }
    
    /**
     * Checks if a result exists for the given key.
     * 
     * @param navController The current NavHostController
     * @param key The unique key to check
     * @return True if a result exists for the key, false otherwise
     */
    fun hasResult(navController: NavHostController, key: String): Boolean {
        return navController.currentBackStackEntry?.savedStateHandle?.contains(key) == true
    }
    
    /**
     * Checks if a result exists for the given key in a SavedStateHandle.
     * 
     * @param savedStateHandle The SavedStateHandle to check
     * @param key The unique key to check
     * @return True if a result exists for the key, false otherwise
     */
    fun hasResult(savedStateHandle: SavedStateHandle, key: String): Boolean {
        return savedStateHandle.contains(key)
    }
    
    /**
     * Clears all results from the current screen's SavedStateHandle.
     * Use with caution as this removes all saved state data.
     * 
     * @param navController The current NavHostController
     */
    fun clearAllResults(navController: NavHostController) {
        navController.currentBackStackEntry?.savedStateHandle?.let { handle ->
            val keys = handle.keys().toList()
            keys.forEach { key ->
                handle.remove<Any>(key)
            }
        }
    }
    
    /**
     * Sets a result and navigates back in one operation.
     * This is a convenience method for the common pattern of setting a result and going back.
     * 
     * @param navController The current NavHostController
     * @param key The unique key to identify the result
     * @param value The result value to pass back
     * @return True if navigation back was successful, false otherwise
     */
    fun <T> setResultAndNavigateBack(navController: NavHostController, key: String, value: T): Boolean {
        return try {
            setResult(navController, key, value)
            navController.popBackStack()
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Predefined result keys for common navigation result scenarios.
 * Using these constants helps avoid typos and provides better refactoring support.
 */
object NavigationResultKeys {
    // General result keys
    const val RESULT_SUCCESS = "navigation_result_success"
    const val RESULT_ERROR = "navigation_result_error"
    const val RESULT_CANCELLED = "navigation_result_cancelled"
    
    // Project-related result keys
    const val PROJECT_CREATED = "project_created"
    const val PROJECT_UPDATED = "project_updated"
    const val PROJECT_DELETED = "project_deleted"
    const val PROJECT_SELECTED = "project_selected"
    
    // Member-related result keys
    const val MEMBER_ADDED = "member_added"
    const val MEMBER_UPDATED = "member_updated"
    const val MEMBER_REMOVED = "member_removed"
    
    // Category/Channel result keys
    const val CATEGORY_CREATED = "category_created"
    const val CATEGORY_UPDATED = "category_updated"
    const val CATEGORY_DELETED = "category_deleted"
    const val CHANNEL_CREATED = "channel_created"
    const val CHANNEL_UPDATED = "channel_updated"
    const val CHANNEL_DELETED = "channel_deleted"
    
    // Role-related result keys
    const val ROLE_CREATED = "role_created"
    const val ROLE_UPDATED = "role_updated"
    const val ROLE_DELETED = "role_deleted"
    
    // Schedule-related result keys
    const val SCHEDULE_CREATED = "schedule_created"
    const val SCHEDULE_UPDATED = "schedule_updated"
    const val SCHEDULE_DELETED = "schedule_deleted"
    const val REFRESH_SCHEDULE_LIST = "refresh_schedule_list"
    
    // Friend-related result keys
    const val FRIEND_ADDED = "friend_added"
    const val FRIEND_REMOVED = "friend_removed"
    const val FRIEND_REQUEST_SENT = "friend_request_sent"
    const val FRIEND_REQUEST_ACCEPTED = "friend_request_accepted"
    
    // Profile result keys
    const val PROFILE_UPDATED = "profile_updated"
    const val PASSWORD_CHANGED = "password_changed"
    
    // Chat result keys
    const val MESSAGE_SELECTED = "message_selected"
    const val CHANNEL_SELECTED = "channel_selected"
}
package com.example.core_navigation.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.lifecycle.SavedStateHandle
import com.example.core_navigation.core.NavigationResultManager
import com.example.core_navigation.core.TypeSafeRoute
import com.example.core_navigation.core.TypeSafeRouteCompat.toAppRoutePath
import kotlinx.coroutines.flow.Flow

/**
 * Consolidated navigation extension functions providing clean APIs
 * for common navigation operations and result handling.
 */

// ===== Type-safe navigation extensions =====

/**
 * Navigates to a type-safe route.
 */
fun NavHostController.navigateTo(route: TypeSafeRoute, navOptions: NavOptions? = null) {
    val routePath = route.toAppRoutePath()
    navigate(routePath, navOptions)
}

/**
 * Navigates to a route clearing the back stack.
 */
fun NavHostController.navigateToClearingBackStack(route: TypeSafeRoute) {
    val routePath = route.toAppRoutePath()
    navigate(routePath) {
        popUpTo(graph.startDestinationId) { inclusive = true }
    }
}

// ===== Result handling extensions (using NavigationResultManager) =====

/**
 * Sets a navigation result using NavigationResultManager.
 * @deprecated Direct usage - inject NavigationResultManager instead
 */
fun <T> NavController.setNavigationResult(
    resultManager: NavigationResultManager,
    key: String,
    value: T
) {
    if (this is NavHostController) {
        resultManager.setResult(this, key, value)
    }
}

/**
 * Gets a navigation result using NavigationResultManager.
 * @deprecated Direct usage - inject NavigationResultManager instead
 */
fun <T> NavController.getNavigationResult(
    resultManager: NavigationResultManager,
    key: String
): T? {
    return if (this is NavHostController) {
        resultManager.getResult(this, key)
    } else null
}

/**
 * Observes navigation results using NavigationResultManager.
 */
@Composable
fun <T> ObserveNavigationResult(
    navController: NavHostController,
    resultManager: NavigationResultManager,
    key: String,
    onResult: (T) -> Unit
) {
    val currentOnResult = remember(key, onResult) { onResult }
    
    LaunchedEffect(key) {
        resultManager.observeResult<T>(navController, key).collect { value ->
            value?.let(currentOnResult)
        }
    }
}

/**
 * Sets result and navigates back in one operation.
 */
fun <T> NavHostController.setResultAndNavigateBack(
    resultManager: NavigationResultManager,
    key: String,
    result: T
): Boolean {
    return resultManager.setResultAndNavigateBack(this, key, result)
}

// ===== SavedStateHandle extensions for ViewModels =====

/**
 * Observes results from SavedStateHandle.
 */
fun <T> SavedStateHandle.observeResult(key: String): Flow<T?> {
    return getStateFlow<T?>(key, null)
}

/**
 * Sets a result in SavedStateHandle.
 */
fun <T> SavedStateHandle.setResult(key: String, value: T) {
    this[key] = value
}

/**
 * Consumes a result from SavedStateHandle (removes after reading).
 */
fun <T> SavedStateHandle.consumeResult(key: String): T? {
    val result = get<T>(key)
    remove<T>(key)
    return result
}

// ===== Legacy compatibility extensions =====

/**
 * Legacy method for backward compatibility.
 * @deprecated Use NavigationResultManager directly
 */
fun <T> NavController.setResult(key: String, value: T) {
    previousBackStackEntry?.savedStateHandle?.set(key, value)
}

/**
 * Legacy method for backward compatibility.
 * @deprecated Use NavigationResultManager directly
 */
fun <T> NavController.getArgument(key: String): T? {
    return previousBackStackEntry?.savedStateHandle?.get<T>(key)
}

/**
 * Legacy result observation.
 * @deprecated Use ObserveNavigationResult with NavigationResultManager
 */
@Composable
fun <T> NavController.ObserveResult(key: String, onResult: (T) -> Unit) {
    val currentOnResult = remember(key, onResult) { onResult }
    
    LaunchedEffect(key) {
        val flow = currentBackStackEntry?.savedStateHandle?.getStateFlow<T?>(key, null)
            ?: return@LaunchedEffect
        
        flow.collect { value ->
            if (value != null) {
                currentOnResult(value)
                currentBackStackEntry?.savedStateHandle?.set(key, null)
            }
        }
    }
}

// ===== Direct navigation helpers =====

/**
 * Gets the route path from a TypeSafeRoute for direct navigation.
 */
fun TypeSafeRoute.getRoutePath(): String = this.toAppRoutePath()

// ===== Convenience navigation methods for common routes =====

/**
 * Additional convenience methods can be added here if needed.
 * The main navigation methods are now implemented directly in AppNavigator interface.
 */ 
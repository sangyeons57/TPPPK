package com.example.core_navigation.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.core_navigation.core.AppNavigator
import kotlinx.coroutines.flow.collectLatest

/**
 * A composable function that observes a specific result key from the AppNavigator's result flow.
 * When a result is emitted for the given key, the [onResultReceived] callback is invoked.
 *
 * @param T The type of the expected result.
 * @param appNavigator The AppNavigator instance to get the result flow from.
 * @param resultKey The key for the result to observe.
 * @param onResultReceived A callback function that will be invoked with the received result.
 *                         The result can be null if that's what was set or if there's an issue.
 */
@Composable
inline fun <reified T> ObserveNavigationResult(
    appNavigator: AppNavigator,
    resultKey: String,
    crossinline onResultReceived: (T?) -> Unit
) {
    LaunchedEffect(key1 = appNavigator, key2 = resultKey) {
        appNavigator.getResultFlow<T>(resultKey)
            .collectLatest { result ->
                onResultReceived(result)
            }
    }
}

package com.example.domain.model

/**
 * A temporary generic class that holds a value with its loading status.
 * This should be replaced by a common Result class if available in core_common or similar.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception, val message: String? = null) : Result<Nothing>()
    object Loading : Result<Nothing>() // Optional: if you want to represent loading state
}

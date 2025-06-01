package com.example.core_common.result

import android.util.Log

sealed class CustomResult<out S, out E> {
    object Initial : CustomResult<Nothing, Nothing>()
    object Loading : CustomResult<Nothing, Nothing>()
    data class Progress(val progress: Double) : CustomResult<Nothing, Nothing>()

    data class Success<out S>(val data: S) : CustomResult<S, Nothing>()
    data class Failure<out E>(val error: E) : CustomResult<Nothing, E>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    val isLoading: Boolean get() = this is Loading
    val isInitial: Boolean get() = this is Initial
    val isProgress: Boolean get() = this is Progress

    fun onSuccess(block: (S) -> Unit): CustomResult<S, E> {
        if (this is Success) {
            block(data)
        }
        return this
    }

    fun onFailure(block: (E) -> Unit): CustomResult<S, E> {
        if (this is Failure) {
            block(error)
        }
        return this
    }

    fun fold(
        onSuccess: (S) -> Unit,
        onFailure: (E) -> Unit
    ) {
        when (this) {
            is Success -> onSuccess(data)
            is Failure -> onFailure(error)
            else -> Log.e("CustomResult", "Unknown result type")
        }
    }
}


inline fun <T, E> resultTry(
    crossinline block: () -> T,
    crossinline onFailure: (exception: Exception) -> E
): CustomResult<T, E> {
    return try {
        CustomResult.Success(block())
    } catch (e: Exception) {
        CustomResult.Failure(onFailure(e))
    }
}

// 좀 더 간단한 버전 (오류 타입을 Exception으로 고정)
inline fun <T> resultTry(block: () -> T): CustomResult<T, Exception> {
    return try {
        CustomResult.Success(block())
    } catch (e: Exception) {
        CustomResult.Failure(e)
    }
}
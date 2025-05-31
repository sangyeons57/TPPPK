package com.example.core_common.result

sealed class CustomResult<out S, out E> {
    object Initial : CustomResult<Nothing, Nothing>()
    object Loading : CustomResult<Nothing, Nothing>()
    data class Success<out S>(val data: S) : CustomResult<S, Nothing>()
    data class Failure<out E>(val error: E) : CustomResult<Nothing, E>()
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
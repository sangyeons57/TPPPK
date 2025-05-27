package com.example.core_common.result

sealed class CustomResult<out S, out E> {
    object Initial : CustomResult<Nothing, Nothing>()
    object Loading : CustomResult<Nothing, Nothing>()
    data class Success<out S>(val data: S) : CustomResult<S, Nothing>()
    data class Failure<out E>(val error: E) : CustomResult<Nothing, E>()
}
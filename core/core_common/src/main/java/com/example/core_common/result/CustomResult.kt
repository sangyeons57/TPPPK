package com.example.core_common.result

import android.util.Log

sealed class CustomResult<out S, out E> {
    data class Success<out S>(val data: S) : CustomResult<S, Nothing>()
    data class Failure<out E>(val error: E) : CustomResult<Nothing, E>()

    data object Initial : CustomResult<Nothing, Nothing>()
    data object Loading : CustomResult<Nothing, Nothing>()
    data class Progress(val progress: Double) : CustomResult<Nothing, Nothing>()


    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    val isLoading: Boolean get() = this is Loading
    val isInitial: Boolean get() = this is Initial
    val isProgress: Boolean get() = this is Progress

    companion object {
        fun isCustomResult(obj: Any?): Boolean {
            return obj is CustomResult<*, *>
        }
    }

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

    /**
     * Success 상태이면 데이터를 반환하고, 그렇지 않으면 [onOther] 람다를 실행하여 그 결과값을 반환합니다.
     */
    inline fun <S, E> CustomResult<S, E>.getOrElse(onOther: (CustomResult<S,E>) -> S): S {
        return when (this) {
            is CustomResult.Success -> data
            else -> onOther(this)
        }
    }
    /**
     * Success 상태이면 데이터를 반환하고, 그렇지 않으면 [defaultValue]를 반환합니다.
     */
    fun <S, E> CustomResult<S, E>.getOrDefault(defaultValue: S): S {
        return (this as? CustomResult.Success<S>)?.data ?: defaultValue
    }
    /**
     * Success 상태이면 데이터를 반환합니다.
     * 그렇지 않으면 IllegalStateException 예외를 발생시킵니다.
     */
    fun <S, E> CustomResult<S, E>.getOrThrow(): S {
        return when (this) {
            is CustomResult.Success -> data
            is CustomResult.Failure -> throw IllegalStateException("Result is a Failure: ${error}")
            else -> throw IllegalStateException("Result is not a Success state: $this")
        }
    }

    suspend fun <T> suspendSuccessProcess(
        onSuccess: suspend (S) -> T,
    ): CustomResult<T, E> {
        return when (this) {
            is Success -> Success(onSuccess(data))
            is Failure -> Failure(this.error)
            is Initial -> Initial
            is Loading -> Loading
            is Progress -> Progress(progress)
        }
    }
    fun <T> successProcess(
        onSuccess: (S) -> T,
    ): CustomResult<T, E> {
        return when (this) {
            is Success -> Success(onSuccess(data))
            is Failure -> Failure(this.error)
            is Initial -> Initial
            is Loading -> Loading
            is Progress -> Progress(progress)
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


inline fun <T> resultTry(block: () -> T): CustomResult<T, Exception> {
    return try {
        CustomResult.Success(block())
    } catch (e: Exception) {
        CustomResult.Failure(e)
    }
}


// Extension functions
fun <S, E> CustomResult<S, E>.getOrNull(): S? =
    if (this is CustomResult.Success) data else null

fun <S, E> CustomResult<S, E>.exceptionOrNull(): E? =
    if (this is CustomResult.Failure) error else null
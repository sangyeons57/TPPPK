package com.example.core_common.result

import com.example.core_common.error.DomainError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Kotlin Result 유틸리티 확장 함수 모음
 * Repository와 UseCase 계층에서 일관된 에러 처리를 위한 표준 패턴을 제공합니다.
 */

/**
 * 성공 결과를 다른 타입으로 변환합니다.
 * 
 * @param transform 성공 값을 변환하는 함수
 * @return 변환된 값을 포함한 새 Result 객체
 */
inline fun <T, R> Result<T>.mapSuccess(transform: (T) -> R): Result<R> {
    return fold(
        onSuccess = { Result.success(transform(it)) },
        onFailure = { Result.failure(it) }
    )
}

/**
 * 실패 에러를 다른 에러 타입으로 변환합니다.
 * 
 * @param transform 원래 에러를 변환하는 함수
 * @return 변환된 에러를 포함한 새 Result 객체
 */
inline fun <T> Result<T>.mapError(transform: (Throwable) -> Throwable): Result<T> {
    return fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(transform(it)) }
    )
}

/**
 * 실패 시 도메인 에러로 변환합니다.
 * 
 * @param errorMapper 일반 예외를 도메인 에러로 변환하는 함수
 * @return 도메인 에러를 포함한 새 Result 객체
 */
inline fun <T> Result<T>.toDomainError(errorMapper: (Throwable) -> DomainError): Result<T> {
    return mapError { throwable ->
        when (throwable) {
            is DomainError -> throwable 
            is CancellationException -> throwable // 코루틴 취소는 그대로 유지
            else -> errorMapper(throwable)
        }
    }
}

/**
 * Flow에서 받은 데이터를 Result로 래핑합니다.
 * 
 * @return 각 항목을 Result.success로 래핑한 Flow
 */
fun <T> Flow<T>.asResult(): Flow<Result<T>> = map { Result.success(it) }
    .catch { emit(Result.failure(it)) }

/**
 * Flow에서 받은 데이터를 도메인 에러로 래핑합니다.
 * 
 * @param errorMapper 일반 예외를 도메인 에러로 변환하는 함수
 * @return 도메인 에러로 래핑된 Flow
 */
fun <T> Flow<T>.asDomainResult(errorMapper: (Throwable) -> DomainError): Flow<Result<T>> =
    map { Result.success(it) }
        .catch { emit(Result.failure<T>(it).toDomainError(errorMapper)) }

/**
 * Flow<Result<T>>에서 성공 값을 변환합니다.
 * 
 * @param transform 성공 값을 변환하는 함수
 * @return 변환된 값을 포함한 Flow
 */
fun <T, R> Flow<Result<T>>.mapSuccessResult(transform: (T) -> R): Flow<Result<R>> =
    map { it.mapSuccess(transform) }

/**
 * Result.success를 생성하는 유틸리티 함수
 * 
 * @param value 성공 값
 * @return 성공 값을 포함한 Result 객체
 */
fun <T> resultSuccess(value: T): Result<T> = Result.success(value)

/**
 * Result.failure를 생성하는 유틸리티 함수
 * 
 * @param error 실패 에러
 * @return 실패 에러를 포함한 Result 객체
 */
fun <T> resultFailure(error: Throwable): Result<T> = Result.failure(error)

/**
 * 도메인 에러를 포함한 Result.failure를 생성하는 유틸리티 함수
 * 
 * @param error 도메인 에러
 * @return 도메인 에러를 포함한 Result 객체
 */
fun <T> resultError(error: DomainError): Result<T> = Result.failure(error)

/**
 * 인라인 try-catch 블록을 Result로 래핑합니다.
 * 
 * @param block 실행할 코드 블록
 * @return 코드 블록의 결과를 포함한 Result 객체
 */
inline fun <T> resultTry(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }
}

/**
 * 인라인 try-catch 블록을 도메인 에러로 래핑합니다.
 * 
 * @param errorMapper 일반 예외를 도메인 에러로 변환하는 함수
 * @param block 실행할 코드 블록
 * @return 코드 블록의 결과를 포함한 Result 객체 (에러는 도메인 에러로 변환)
 */
inline fun <T> resultTryWithDomainError(
    errorMapper: (Throwable) -> DomainError,
    block: () -> T
): Result<T> {
    return resultTry(block).toDomainError(errorMapper)
} 
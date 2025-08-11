package com.example.core_common.error
import com.example.core_common.network.NetworkException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 표준 예외를 도메인 에러로 변환하는 유틸리티 클래스입니다.
 * Repository 구현에서 발생하는 다양한 예외를 일관된 도메인 에러로 매핑합니다.
 */
@Singleton
class ErrorMapper @Inject constructor() {

    /**
     * 일반 예외를 도메인 에러로 변환합니다.
     *
     * @param throwable 변환할 원본 예외
     * @return 변환된 도메인 에러
     */
    fun mapToDomainError(throwable: Throwable): DomainError {
        return when (throwable) {
            // 이미 도메인 에러인 경우 그대로 반환
            is DomainError -> throwable

            // 네트워크 관련 에러
            is UnknownHostException, is NetworkException.NoConnectivityException ->
                DomainError.NetworkError.NoConnection(cause = throwable)

            is SocketTimeoutException ->
                DomainError.NetworkError.Timeout(cause = throwable)

            is IOException ->
                DomainError.NetworkError.general(cause = throwable)

            // 기타: HttpException 등
            else -> if (isAndroidHttpException(throwable)) {
                mapHttpException(throwable)
            } else {
                DomainError.UnknownError(cause = throwable)
            }
        }
    }
    
    /**
     * 인증 관련 예외를 도메인 에러로 변환합니다.
     *
     * @param throwable 변환할 원본 예외
     * @param defaultMessage 기본 에러 메시지
     * @return 변환된 인증 관련 도메인 에러
     */
    fun mapToAuthError(throwable: Throwable, defaultMessage: String? = null): DomainError.AuthError {
        return when (throwable) {
            // 이미 인증 관련 도메인 에러인 경우 그대로 반환
            is DomainError.AuthError -> throwable

            // HTTP 401, 403 에러
            else -> if (isAndroidHttpException(throwable)) {
                val statusCode = extractStatusCode(throwable)
                when (statusCode) {
                    401 -> DomainError.AuthError.SessionExpired(cause = throwable)
                    403 -> DomainError.AuthError.Unauthorized(cause = throwable)
                    else -> DomainError.AuthError.general(
                        message = defaultMessage ?: "인증 오류가 발생했습니다.",
                        cause = throwable
                    ) as DomainError.AuthError
                }
            } else {
                // 그 외 일반 인증 에러
                DomainError.AuthError.general(
                    message = defaultMessage ?: "인증 오류가 발생했습니다.",
                    cause = throwable
                ) as DomainError.AuthError
            }
        }
    }
    
    /**
     * 데이터 관련 예외를 도메인 에러로 변환합니다.
     *
     * @param throwable 변환할 원본 예외
     * @param defaultMessage 기본 에러 메시지
     * @return 변환된 데이터 관련 도메인 에러
     */
    fun mapToDataError(throwable: Throwable, defaultMessage: String? = null): DomainError.DataError {
        return when (throwable) {
            // 이미 데이터 관련 도메인 에러인 경우 그대로 반환
            is DomainError.DataError -> throwable

            // HTTP 404, 409 에러
            else -> if (isAndroidHttpException(throwable)) {
                val statusCode = extractStatusCode(throwable)
                when (statusCode) {
                    404 -> DomainError.DataError.NotFound(cause = throwable)
                    409 -> DomainError.DataError.AlreadyExists(cause = throwable)
                    422 -> DomainError.DataError.ValidationFailed(cause = throwable)
                    else -> DomainError.DataError.general(
                        message = defaultMessage ?: "데이터 오류가 발생했습니다.",
                        cause = throwable
                    ) as DomainError.DataError
                }
            } else {
                // 그 외 일반 데이터 에러
                DomainError.DataError.general(
                    message = defaultMessage ?: "데이터 오류가 발생했습니다.",
                    cause = throwable
                ) as DomainError.DataError
            }
        }
    }
    
    /**
     * 채팅 관련 예외를 도메인 에러로 변환합니다.
     *
     * @param throwable 변환할 원본 예외
     * @param defaultMessage 기본 에러 메시지
     * @return 변환된 채팅 관련 도메인 에러
     */
    fun mapToChatError(throwable: Throwable, defaultMessage: String? = null): DomainError.ChatError {
        return when (throwable) {
            // 이미 채팅 관련 도메인 에러인 경우 그대로 반환
            is DomainError.ChatError -> throwable

            // HTTP 에러
            else -> if (isAndroidHttpException(throwable)) {
                val statusCode = extractStatusCode(throwable)
                when (statusCode) {
                    403 -> DomainError.ChatError.ChannelAccessDenied(cause = throwable)
                    422 -> DomainError.ChatError.InvalidMessageContent(cause = throwable)
                    else -> DomainError.ChatError.general(
                        message = defaultMessage ?: "채팅 오류가 발생했습니다.",
                        cause = throwable
                    ) as DomainError.ChatError
                }
            } else {
                // 그 외 일반 채팅 에러
                DomainError.ChatError.general(
                    message = defaultMessage ?: "채팅 오류가 발생했습니다.",
                    cause = throwable
                ) as DomainError.ChatError
            }
        }
    }
    
    /**
     * HTTP 예외를 도메인 에러로 변환합니다.
     *
     * @param httpException 변환할 HTTP 예외
     * @return 변환된 도메인 에러
     */
    private fun mapHttpException(httpThrowable: Throwable): DomainError {
        val statusCode = extractStatusCode(httpThrowable)
        return when (statusCode) {
            in 400..499 -> {
                when (statusCode) {
                    401 -> DomainError.AuthError.SessionExpired(cause = httpThrowable)
                    403 -> DomainError.AuthError.Unauthorized(cause = httpThrowable)
                    404 -> DomainError.DataError.NotFound(cause = httpThrowable)
                    409 -> DomainError.DataError.AlreadyExists(cause = httpThrowable)
                    422 -> DomainError.DataError.ValidationFailed(cause = httpThrowable)
                    else -> DomainError.DataError.general(cause = httpThrowable)
                }
            }

            in 500..599 -> DomainError.NetworkError.ServerError(cause = httpThrowable)
            else -> DomainError.UnknownError(cause = httpThrowable)
        }
    }

    // android.net.http.HttpException 존재 시에만 처리하기 위해 런타임 클래스명 기반 체크
    private fun isAndroidHttpException(throwable: Throwable): Boolean {
        return throwable.javaClass.name == "android.net.http.HttpException"
    }

    private fun extractStatusCode(throwable: Throwable): Int {
        // 현재 구현은 message에 숫자 상태코드가 포함된다는 가정.
        return throwable.message?.toIntOrNull() ?: 0
    }
} 
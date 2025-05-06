package com.example.core_common.error

import android.net.http.HttpException
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
                
            // HTTP 에러
            is HttpException -> mapHttpException(throwable)
            
            // 그 외 알 수 없는 에러
            else -> DomainError.UnknownError(cause = throwable)
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
            is HttpException -> {
                val statusCode = throwable.message?.toIntOrNull() ?: 0
                when (statusCode) {
                    401 -> DomainError.AuthError.SessionExpired(cause = throwable)
                    403 -> DomainError.AuthError.Unauthorized(cause = throwable)
                    else -> DomainError.AuthError.general(
                        message = defaultMessage ?: "인증 오류가 발생했습니다.",
                        cause = throwable
                    ) as DomainError.AuthError
                }
            }
            
            // 그 외 일반 인증 에러
            else -> DomainError.AuthError.general(
                message = defaultMessage ?: "인증 오류가 발생했습니다.",
                cause = throwable
            ) as DomainError.AuthError
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
            is HttpException -> {
                val statusCode = throwable.message?.toIntOrNull() ?: 0
                when (statusCode) {
                    404 -> DomainError.DataError.NotFound(cause = throwable)
                    409 -> DomainError.DataError.AlreadyExists(cause = throwable)
                    422 -> DomainError.DataError.ValidationFailed(cause = throwable)
                    else -> DomainError.DataError.general(
                        message = defaultMessage ?: "데이터 오류가 발생했습니다.",
                        cause = throwable
                    ) as DomainError.DataError
                }
            }
            
            // 그 외 일반 데이터 에러
            else -> DomainError.DataError.general(
                message = defaultMessage ?: "데이터 오류가 발생했습니다.",
                cause = throwable
            ) as DomainError.DataError
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
            is HttpException -> {
                val statusCode = throwable.message?.toIntOrNull() ?: 0
                when (statusCode) {
                    403 -> DomainError.ChatError.ChannelAccessDenied(cause = throwable)
                    422 -> DomainError.ChatError.InvalidMessageContent(cause = throwable)
                    else -> DomainError.ChatError.general(
                        message = defaultMessage ?: "채팅 오류가 발생했습니다.",
                        cause = throwable
                    ) as DomainError.ChatError
                }
            }
            
            // 그 외 일반 채팅 에러
            else -> DomainError.ChatError.general(
                message = defaultMessage ?: "채팅 오류가 발생했습니다.",
                cause = throwable
            ) as DomainError.ChatError
        }
    }
    
    /**
     * HTTP 예외를 도메인 에러로 변환합니다.
     *
     * @param httpException 변환할 HTTP 예외
     * @return 변환된 도메인 에러
     */
    private fun mapHttpException(httpException: HttpException): DomainError {
        val statusCode = httpException.message?.toIntOrNull() ?: 0
        return when (statusCode) {
            in 400..499 -> {
                when (statusCode) {
                    401 -> DomainError.AuthError.SessionExpired(cause = httpException)
                    403 -> DomainError.AuthError.Unauthorized(cause = httpException)
                    404 -> DomainError.DataError.NotFound(cause = httpException)
                    409 -> DomainError.DataError.AlreadyExists(cause = httpException)
                    422 -> DomainError.DataError.ValidationFailed(cause = httpException)
                    else -> DomainError.DataError.general(cause = httpException)
                }
            }
            in 500..599 -> DomainError.NetworkError.ServerError(cause = httpException)
            else -> DomainError.UnknownError(cause = httpException)
        }
    }
} 
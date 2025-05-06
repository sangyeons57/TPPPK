package com.example.core_common.network

import java.io.IOException

/**
 * 네트워크 작업과 관련된 예외를 정의하는 클래스입니다.
 */
sealed class NetworkException(
    override val message: String,
    override val cause: Throwable? = null
) : IOException(message, cause) {

    /**
     * 인터넷 연결이 없을 때 발생하는 예외
     */
    class NoConnectivityException(
        override val message: String = "인터넷 연결이 없습니다. 네트워크 상태를 확인해주세요.",
        override val cause: Throwable? = null
    ) : NetworkException(message, cause)

    /**
     * API 호출 시간이 초과되었을 때 발생하는 예외
     */
    class ApiTimeoutException(
        override val message: String = "서버 응답 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.",
        override val cause: Throwable? = null
    ) : NetworkException(message, cause)

    /**
     * 서버 내부 오류 (5xx) 응답을 받았을 때 발생하는 예외
     */
    class ServerErrorException(
        override val message: String = "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
        override val cause: Throwable? = null
    ) : NetworkException(message, cause)

    /**
     * 네트워크 요청이 취소되었을 때 발생하는 예외
     */
    class RequestCancelledException(
        override val message: String = "네트워크 요청이 취소되었습니다.",
        override val cause: Throwable? = null
    ) : NetworkException(message, cause)
} 
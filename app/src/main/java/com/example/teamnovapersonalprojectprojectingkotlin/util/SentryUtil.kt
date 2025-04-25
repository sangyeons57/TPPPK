package com.example.teamnovapersonalprojectprojectingkotlin.util

import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.Message
import io.sentry.protocol.User

/**
 * Sentry 관련 유틸리티 함수 모음
 */
object SentryUtil {

    // --- 레벨별 메시지 로깅 ---

    /**
     * DEBUG 레벨 메시지를 Sentry로 전송합니다. (개발/디버깅용 상세 정보)
     * @param message 기록할 메시지
     * @param tags 추가 태그 (선택 사항)
     */
    fun logDebug(message: String, tags: Map<String, String>? = null) {
        captureMessageWithScope(message, SentryLevel.DEBUG, tags)
    }

    /**
     * INFO 레벨 메시지를 Sentry로 전송합니다. (일반 정보)
     * @param message 기록할 메시지
     * @param tags 추가 태그 (선택 사항)
     */
    fun logInfo(message: String, tags: Map<String, String>? = null) {
        captureMessageWithScope(message, SentryLevel.INFO, tags)
    }

    /**
     * WARNING 레벨 메시지를 Sentry로 전송합니다. (잠재적 문제 경고)
     * @param message 기록할 메시지
     * @param tags 추가 태그 (선택 사항)
     */
    fun logWarning(message: String, tags: Map<String, String>? = null) {
        captureMessageWithScope(message, SentryLevel.WARNING, tags)
    }

    /**
     * ERROR 레벨 메시지를 Sentry로 전송합니다. (처리된 오류 또는 비정상 상황 기록)
     * 예외가 발생하지 않았지만 오류로 기록하고 싶을 때 사용합니다.
     * @param message 기록할 메시지
     * @param tags 추가 태그 (선택 사항)
     */
    fun logError(message: String, tags: Map<String, String>? = null) {
        captureMessageWithScope(message, SentryLevel.ERROR, tags)
    }

    /**
     * FATAL 레벨 메시지를 Sentry로 전송합니다. (앱 기능이 중단되는 심각한 오류 기록)
     * 예외가 발생하지 않았지만 치명적인 오류로 기록하고 싶을 때 사용합니다.
     * @param message 기록할 메시지
     * @param tags 추가 태그 (선택 사항)
     */
    fun logFatal(message: String, tags: Map<String, String>? = null) {
        captureMessageWithScope(message, SentryLevel.FATAL, tags)
    }

    /**
     * 메시지와 함께 Scope 설정을 적용하여 Sentry 이벤트를 전송하는 내부 함수
     */
    private fun captureMessageWithScope(
        message: String,
        level: SentryLevel,
        tags: Map<String, String>? = null
    ) {
        Sentry.captureEvent(SentryEvent().apply {
            this.message = Message().apply { this.formatted = message }
            this.level = level
        }) { scope ->
            tags?.forEach { (key, value) -> scope.setTag(key, value) }
        }
        println("SentryUtil: Message captured [$level] - $message") // 개발 중 로그
    }


    // --- 레벨별 예외 로깅 ---

    /**
     * ERROR 레벨로 예외를 Sentry로 전송합니다. (기본 예외 처리)
     *
     * @param throwable 전송할 예외 객체
     * @param message 추가적으로 기록할 메시지 (선택 사항)
     * @param tags 추가 태그 (선택 사항)
     */
    fun captureError(throwable: Throwable, message: String? = null, tags: Map<String, String>? = null) {
        captureExceptionWithScope(throwable, SentryLevel.ERROR, message, tags)
    }

    /**
     * FATAL 레벨로 예외를 Sentry로 전송합니다. (앱의 정상 작동이 불가능한 심각한 예외)
     *
     * @param throwable 전송할 예외 객체
     * @param message 추가적으로 기록할 메시지 (선택 사항)
     * @param tags 추가 태그 (선택 사항)
     */
    fun captureFatal(throwable: Throwable, message: String? = null, tags: Map<String, String>? = null) {
        captureExceptionWithScope(throwable, SentryLevel.FATAL, message, tags)
    }

    /**
     * 예외와 함께 Scope 설정을 적용하여 Sentry 이벤트를 전송하는 내부 함수
     */
    private fun captureExceptionWithScope(
        throwable: Throwable,
        level: SentryLevel,
        message: String? = null,
        tags: Map<String, String>? = null
    ) {
        Sentry.captureException(throwable) { scope ->
            scope.level = level // 레벨 설정
            message?.let { scope.setContexts("Additional Info", mapOf("message" to it)) }
            tags?.forEach { (key, value) -> scope.setTag(key, value) }
        }
        println("SentryUtil: Exception captured [$level] - ${throwable.message}") // 개발 중 로그
    }


    // --- 기존 유틸리티 함수들 (UserInfo, Breadcrumb, Context, Tag) ---

    /**
     * 사용자 정보를 Sentry에 설정합니다. 로그인/로그아웃 시 또는 사용자 정보 변경 시 호출합니다.
     *
     * @param userId 사용자 고유 ID (null 전달 시 사용자 정보 제거)
     * @param email 사용자 이메일 (선택 사항)
     * @param username 사용자 이름 (선택 사항)
     * @param data 추가 사용자 정보 (선택 사항)
     */
    fun setUserInfo(userId: String?, email: String? = null, username: String? = null, data: Map<String, String>? = null) {
        if (userId == null) {
            Sentry.setUser(null) // 로그아웃 시 사용자 정보 제거
            println("SentryUtil: User info cleared")
        } else {
            val user = User().apply {
                this.id = userId
                this.email = email
                this.username = username
                this.data = data
            }
            Sentry.setUser(user)
            println("SentryUtil: User info set - ID: $userId")
        }
    }

    /**
     * Breadcrumb를 추가하여 사용자의 행동 흐름을 추적합니다.
     *
     * @param category Breadcrumb 분류 (예: "ui.click", "navigation")
     * @param message Breadcrumb 내용
     * @param level 심각도 (선택 사항, 기본값: INFO)
     */
    fun addBreadcrumb(category: String, message: String, level: SentryLevel = SentryLevel.INFO) {
        val breadcrumb = Breadcrumb().apply {
            this.category = category
            this.message = message
            this.level = level
        }
        Sentry.addBreadcrumb(breadcrumb)
        println("SentryUtil: Breadcrumb added [$category] - $message")
    }

    /**
     * 검색이나 필터링에 사용할 커스텀 태그를 설정합니다.
     *
     * @param key 태그 키
     * @param value 태그 값
     */
    fun setCustomTag(key: String, value: String) {
        Sentry.setTag(key, value)
        println("SentryUtil: Tag set - $key: $value")
    }

    /**
     * 이벤트에 추가적인 Extra 정보를 첨부합니다.
     *
     * @param key Extra 키
     * @param value 첨부할 데이터
     */
    fun setCustomExtra(key: String, value: String) {
        Sentry.setExtra(key, value)
        println("SentryUtil: Extra set - $key: $value")
    }
}
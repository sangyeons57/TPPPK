package com.example.core_logging

import android.content.Context
import io.sentry.Breadcrumb
import io.sentry.ITransaction
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SpanStatus
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.Message
import io.sentry.protocol.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Sentry 관련 유틸리티 함수 모음
 */
object SentryUtil {

    // Sentry 초기화 여부
    private var isInitialized = false
    
    // 현재 활성 트랜잭션 (메모리 릭 방지를 위해 약한 참조 권장)
    private var currentTransaction: ITransaction? = null

    /**
     * Sentry 초기화 함수
     * 중복 초기화 방지를 위한 체크 로직 추가
     */
    fun SentryInit(context: Context) {
        if (isInitialized) {
            println("SentryUtil: 이미 초기화되어 있습니다.")
            return
        }
        
        try {
            SentryAndroid.init(context) { options ->
                // 기본 DSN 설정
                options.dsn = "https://0a3d0d1fe57deb2e7baebd1f244a04de@o4509194335223808.ingest.us.sentry.io/4509194511974400"
                
                // 디버그 모드 설정 (개발 중일 때만)
                val isDebug = try {
                    BuildConfig.DEBUG
                } catch (e: Exception) {
                    // BuildConfig에 접근할 수 없는 경우 기본값
                    true
                }
                options.isDebug = isDebug
                
                // 앱 환경 설정 (개발/테스트/프로덕션)
                options.environment = if (isDebug) "development" else "production"
                
                // 성능 모니터링 설정
                options.tracesSampleRate = 1.0
                options.profilesSampleRate = 1.0
                
                // 앱 버전 설정 (사용 가능한 경우)
                try {
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val versionName = packageInfo.versionName ?: "unknown"
                    val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toString()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toString()
                    }
                    options.release = "${context.packageName}@$versionName+$versionCode"
                } catch (e: Exception) {
                    // 패키지 정보를 가져올 수 없는 경우 기본값 사용
                    options.release = "app@unknown"
                }
                
                // ANR 감지 설정
                options.anrTimeoutIntervalMillis = 5000 // 5초
            }
            
            // 시스템 정보 태그 추가
            Sentry.setTag("device.manufacturer", android.os.Build.MANUFACTURER)
            Sentry.setTag("device.model", android.os.Build.MODEL)
            Sentry.setTag("os.version", android.os.Build.VERSION.RELEASE)
            Sentry.setTag("app.locale", java.util.Locale.getDefault().toString())
            
            println("SentryUtil: 초기화 완료")
            isInitialized = true
            
            // 앱 시작 이벤트 기록
            addBreadcrumb("app.lifecycle", "애플리케이션 시작됨")
            
        } catch (e: Exception) {
            // 초기화 실패 시 예외 처리
            println("SentryUtil: 초기화 실패 - ${e.message}")
            e.printStackTrace()
        }
    }
    
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
     * @return 생성된 Breadcrumb 객체
     */
    fun addBreadcrumb(category: String, message: String, level: SentryLevel = SentryLevel.INFO): Breadcrumb {
        val breadcrumb = Breadcrumb().apply {
            this.category = category
            this.message = message
            this.level = level
        }
        Sentry.addBreadcrumb(breadcrumb)
        println("SentryUtil: Breadcrumb added [$category] - $message")
        return breadcrumb
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
    
    // --- 성능 모니터링 유틸리티 함수 ---
    
    /**
     * 화면 또는 기능의 성능 트랜잭션을 시작합니다.
     * 
     * @param name 트랜잭션 이름
     * @param operation 작업 유형 (예: "navigation", "ui.load", "db.query")
     * @param tags 추가 태그 (선택 사항)
     * @return 생성된 트랜잭션 객체
     */
    fun startTransaction(name: String, operation: String, tags: Map<String, String>? = null): ITransaction {
        val transaction = Sentry.startTransaction(name, operation)
        tags?.forEach { (key, value) -> transaction.setTag(key, value) }
        
        // 현재 트랜잭션 저장 (기존 트랜잭션이 있다면 종료)
        currentTransaction?.finish()
        currentTransaction = transaction
        
        println("SentryUtil: Transaction started - $name [$operation]")
        return transaction
    }
    
    /**
     * 성능 트랜잭션의 스팬을 생성하여 특정 작업을 측정합니다.
     * 
     * @param transaction 상위 트랜잭션
     * @param operation 작업 유형 (예: "http.client", "db.query", "ui.render")
     * @param description 작업 설명
     * @param block 측정할 코드 블록
     * @return 코드 블록의 실행 결과
     */
    inline fun <T> withSpan(
        transaction: ITransaction, 
        operation: String, 
        description: String, 
        block: () -> T
    ): T {
        val span = transaction.startChild(operation, description)
        try {
            val result = block()
            span.finish(SpanStatus.OK)
            return result
        } catch (e: Exception) {
            span.finish(SpanStatus.INTERNAL_ERROR)
            throw e
        }
    }
    
    /**
     * 비동기 작업을 위한 코루틴 스코프에서 스팬을 생성하여 특정 작업을 측정합니다.
     * 
     * @param transaction 상위 트랜잭션
     * @param operation 작업 유형 (예: "http.client", "db.query", "ui.render")
     * @param description 작업 설명
     * @param block 측정할 비동기 코드 블록
     * @return 코드 블록의 실행 결과
     */
    suspend inline fun <T> withAsyncSpan(
        transaction: ITransaction, 
        operation: String, 
        description: String, 
        crossinline block: suspend () -> T
    ): T {
        val span = transaction.startChild(operation, description)
        try {
            val result = withContext(Dispatchers.IO) {
                block()
            }
            span.finish(SpanStatus.OK)
            return result
        } catch (e: Exception) {
            span.finish(SpanStatus.INTERNAL_ERROR)
            throw e
        }
    }
    
    /**
     * 화면 전환을 추적합니다. 새 화면으로 이동할 때 호출합니다.
     * 
     * @param screenName 화면 이름
     * @param previousScreen 이전 화면 이름 (선택 사항)
     */
    fun trackScreenNavigation(screenName: String, previousScreen: String? = null) {
        val breadcrumb = addBreadcrumb("navigation", "Navigated to $screenName" + 
            (previousScreen?.let { " from $it" } ?: ""))
        
        // 화면 이동 시 현재 화면 정보를 태그로 설정
        setCustomTag("current_screen", screenName)
    }
    
    /**
     * 사용자 액션을 추적합니다. 버튼 클릭, 스와이프 등 사용자 상호작용 시 호출합니다.
     * 
     * @param actionType 액션 유형 (예: "button_click", "swipe", "long_press")
     * @param actionDescription 액션 설명
     * @param screenName 발생한 화면 이름
     */
    fun trackUserAction(actionType: String, actionDescription: String, screenName: String) {
        addBreadcrumb("user.action", "$actionType: $actionDescription on $screenName")
    }
    
    /**
     * 네트워크 호출 시작 시 호출하여 네트워크 요청을 추적합니다.
     * 
     * @param url 요청 URL
     * @param method HTTP 메서드 (GET, POST 등)
     * @param transactionName 연관된 트랜잭션 이름 (선택 사항)
     */
    fun trackNetworkRequestStart(url: String, method: String, transactionName: String? = null) {
        val breadcrumb = addBreadcrumb("network", "Request started: $method $url", SentryLevel.INFO)
        
        // Breadcrumb에 추가 정보를 첨부
        breadcrumb.setData("url", url)
        breadcrumb.setData("method", method)
        transactionName?.let { breadcrumb.setData("transaction", it) }
    }
    
    /**
     * 네트워크 호출 완료 시 호출하여 네트워크 응답을 추적합니다.
     * 
     * @param url 요청 URL
     * @param method HTTP 메서드 (GET, POST 등)
     * @param statusCode 응답 상태 코드
     * @param durationMs 요청 처리 시간 (밀리초)
     */
    fun trackNetworkRequestEnd(url: String, method: String, statusCode: Int, durationMs: Long) {
        val level = if (statusCode in 200..399) SentryLevel.INFO else SentryLevel.ERROR
        val breadcrumb = addBreadcrumb("network", "Request completed: $method $url - $statusCode ($durationMs ms)", level)
        
        // Breadcrumb에 추가 정보를 첨부
        breadcrumb.setData("url", url)
        breadcrumb.setData("method", method)
        breadcrumb.setData("status_code", statusCode.toString())
        breadcrumb.setData("duration_ms", durationMs.toString())
    }
    
    /**
     * Session Replay 시작을 시뮬레이션합니다.
     * 실제 Session Replay가 지원되지 않으므로 태그와 브레드크럼으로 표시합니다.
     * UI 테스트나 중요한 사용자 시나리오 시작 시 호출합니다.
     */
    fun startSessionReplay() {
        setCustomTag("session_replay_active", "true")
        addBreadcrumb("session", "Session replay started")
        println("SentryUtil: Session Replay simulation started")
    }
    
    /**
     * Session Replay 중지를 시뮬레이션합니다.
     * 실제 Session Replay가 지원되지 않으므로 태그와 브레드크럼으로 표시합니다.
     * UI 테스트나 중요한 사용자 시나리오 종료 시 호출합니다.
     */
    fun stopSessionReplay() {
        setCustomTag("session_replay_active", "false")
        addBreadcrumb("session", "Session replay stopped")
        println("SentryUtil: Session Replay simulation stopped")
    }
    
    /**
     * 현재 세션에 대한 피드백 보고서를 Sentry로 전송합니다.
     * 
     * @param name 사용자 이름 (선택 사항)
     * @param email 사용자 이메일 (선택 사항)
     * @param comments 사용자 의견
     * @param feedbackType 피드백 유형 (기본값: "feedback") 
     */
    fun sendUserFeedback(
        name: String? = null,
        email: String? = null,
        comments: String,
        feedbackType: String = "feedback"
    ) {
        try {
            Sentry.captureMessage(comments) { scope ->
                scope.setTag("feedback_type", feedbackType)
                scope.setTag("feedback_source", "in_app")
                
                // 사용자 정보가 있으면 첨부
                if (name != null || email != null) {
                    scope.user = User().apply {
                        this.username = name
                        this.email = email
                    }
                }
                
                // 브레드크럼 추가
                scope.addBreadcrumb(Breadcrumb().apply {
                    this.category = "feedback"
                    this.message = "User provided feedback"
                    this.level = SentryLevel.INFO
                    this.setData("comments", comments)
                    this.setData("feedback_type", feedbackType)
                })
            }
            
            println("SentryUtil: User feedback sent - $comments")
        } catch (e: Exception) {
            println("SentryUtil: Failed to send user feedback - ${e.message}")
        }
    }
}
package com.example.domain.util

import com.example.core_common.result.CustomResult
import com.example.domain.error.FileUploadError
import kotlinx.coroutines.delay
// import timber.log.Timber - Domain 모듈에서 제거됨
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * 재시도 정책을 정의하는 인터페이스
 */
interface RetryPolicy {
    val maxRetries: Int
    val baseDelayMs: Long
    val maxDelayMs: Long
    val backoffMultiplier: Double
    
    fun shouldRetry(attempt: Int, error: Throwable): Boolean
    fun calculateDelay(attempt: Int): Long
}

/**
 * 기본 지수 백오프 재시도 정책
 */
data class ExponentialBackoffPolicy(
    override val maxRetries: Int = 3,
    override val baseDelayMs: Long = 1000L,
    override val maxDelayMs: Long = 30000L,
    override val backoffMultiplier: Double = 2.0,
    private val jitterMs: Long = 100L
) : RetryPolicy {
    
    override fun shouldRetry(attempt: Int, error: Throwable): Boolean {
        if (attempt >= maxRetries) return false
        
        return when (error) {
            is FileUploadError -> error.isRetryable
            else -> true // 알 수 없는 에러는 재시도
        }
    }
    
    override fun calculateDelay(attempt: Int): Long {
        val exponentialDelay = baseDelayMs * backoffMultiplier.pow(attempt).toLong()
        val delayWithJitter = exponentialDelay + (Math.random() * jitterMs).toLong()
        return min(delayWithJitter, maxDelayMs)
    }
}

/**
 * 네트워크 전용 재시도 정책 (더 적극적)
 */
data class NetworkRetryPolicy(
    override val maxRetries: Int = 5,
    override val baseDelayMs: Long = 500L,
    override val maxDelayMs: Long = 10000L,
    override val backoffMultiplier: Double = 1.5
) : RetryPolicy {
    
    override fun shouldRetry(attempt: Int, error: Throwable): Boolean {
        if (attempt >= maxRetries) return false
        
        return when (error) {
            is FileUploadError.NetworkError -> error.isRetryable
            is FileUploadError.FirebaseError.FirebaseUnavailable -> true
            else -> false
        }
    }
    
    override fun calculateDelay(attempt: Int): Long {
        val delay = baseDelayMs * backoffMultiplier.pow(attempt).toLong()
        return min(delay, maxDelayMs)
    }
}

/**
 * 재시도 컨텍스트 정보
 */
data class RetryContext(
    val operationName: String,
    val attempt: Int,
    val maxAttempts: Int,
    val lastError: Throwable?,
    val totalElapsedTime: Long,
    val nextRetryDelay: Long?
)

/**
 * 재시도 결과
 */
sealed class RetryResult<T> {
    data class Success<T>(val value: T, val attemptCount: Int) : RetryResult<T>()
    data class Failure<T>(val error: Throwable, val attemptCount: Int, val finalError: FileUploadError) : RetryResult<T>()
}

/**
 * 재시도 이벤트 리스너
 */
interface RetryEventListener {
    suspend fun onRetryAttempt(context: RetryContext)
    suspend fun onRetrySuccess(context: RetryContext)
    suspend fun onRetryFailed(context: RetryContext)
    suspend fun onRetryAbandoned(context: RetryContext)
}

/**
 * 파일 업로드와 관련된 재시도 로직을 관리하는 유틸리티
 */
@Singleton
class RetryManager @Inject constructor() {
    
    private val defaultPolicy = ExponentialBackoffPolicy()
    private val networkPolicy = NetworkRetryPolicy()
    private val eventListeners = mutableListOf<RetryEventListener>()
    
    /**
     * 재시도 이벤트 리스너를 추가합니다.
     */
    fun addEventListener(listener: RetryEventListener) {
        eventListeners.add(listener)
    }
    
    /**
     * 재시도 이벤트 리스너를 제거합니다.
     */
    fun removeEventListener(listener: RetryEventListener) {
        eventListeners.remove(listener)
    }
    
    /**
     * 지정된 정책에 따라 작업을 재시도합니다.
     * @param operationName 작업 이름 (로깅용)
     * @param policy 재시도 정책
     * @param operation 실행할 작업
     * @return 재시도 결과
     */
    suspend fun <T> executeWithRetry(
        operationName: String,
        policy: RetryPolicy = defaultPolicy,
        operation: suspend (attempt: Int) -> CustomResult<T, Exception>
    ): RetryResult<T> {
        
        val startTime = System.currentTimeMillis()
        var lastError: Throwable? = null
        
        for (attempt in 0 until policy.maxRetries) {
            try {
                // 재시도 시작 알림
                val context = RetryContext(
                    operationName = operationName,
                    attempt = attempt,
                    maxAttempts = policy.maxRetries,
                    lastError = lastError,
                    totalElapsedTime = System.currentTimeMillis() - startTime,
                    nextRetryDelay = if (attempt > 0) policy.calculateDelay(attempt - 1) else null
                )
                
                notifyListeners { it.onRetryAttempt(context) }
                
                // 작업 실행
                val result = operation(attempt)
                
                when (result) {
                    is CustomResult.Success -> {
                        // 성공
                        val successContext = context.copy(
                            totalElapsedTime = System.currentTimeMillis() - startTime
                        )
                        notifyListeners { it.onRetrySuccess(successContext) }
                        
                        println("DEBUG: Operation '$operationName' succeeded on attempt ${attempt + 1}")
                        return RetryResult.Success(result.data, attempt + 1)
                    }
                    
                    is CustomResult.Failure -> {
                        lastError = result.error
                        
                        // 재시도 가능한지 확인
                        if (!policy.shouldRetry(attempt, result.error)) {
                            val failureContext = context.copy(
                                lastError = lastError,
                                totalElapsedTime = System.currentTimeMillis() - startTime
                            )
                            notifyListeners { it.onRetryAbandoned(failureContext) }
                            
                            println("WARNING: Operation '$operationName' failed and won't be retried: ${result.error.message}")
                            
                            val finalError = convertToFileUploadError(result.error)
                            return RetryResult.Failure(result.error, attempt + 1, finalError)
                        }
                        
                        // 마지막 시도가 아니면 대기 후 재시도
                        if (attempt < policy.maxRetries - 1) {
                            val delay = policy.calculateDelay(attempt)
                            
                            println("WARNING: Operation '$operationName' failed on attempt ${attempt + 1}, retrying in ${delay}ms: ${result.error.message}")
                            
                            val retryContext = context.copy(
                                lastError = lastError,
                                totalElapsedTime = System.currentTimeMillis() - startTime,
                                nextRetryDelay = delay
                            )
                            notifyListeners { it.onRetryFailed(retryContext) }
                            
                            delay(delay)
                        }
                    }
                    
                    is CustomResult.Initial -> {
                        // 초기 상태 - 아직 처리되지 않음
                        lastError = Exception("Operation not yet started")
                    }
                    
                    is CustomResult.Loading -> {
                        // 로딩 상태 - 진행 중
                        // 로딩 상태는 계속 진행하도록 함
                    }
                    
                    is CustomResult.Progress -> {
                        // 진행 상태 - 아직 완료되지 않음
                        // 진행 상태는 계속 진행하도록 함
                    }
                }
                
            } catch (e: Exception) {
                lastError = e
                
                if (!policy.shouldRetry(attempt, e)) {
                    val finalError = convertToFileUploadError(e)
                    println("ERROR: Operation '$operationName' failed with non-retryable error: ${e.message}")
                    return RetryResult.Failure(e, attempt + 1, finalError)
                }
                
                if (attempt < policy.maxRetries - 1) {
                    val delay = policy.calculateDelay(attempt)
                    println("WARNING: Operation '$operationName' threw exception on attempt ${attempt + 1}, retrying in ${delay}ms: ${e.message}")
                    delay(delay)
                }
            }
        }
        
        // 모든 재시도 실패
        val finalError = convertToFileUploadError(lastError ?: Exception("Unknown error"))
        val abandonedContext = RetryContext(
            operationName = operationName,
            attempt = policy.maxRetries,
            maxAttempts = policy.maxRetries,
            lastError = lastError,
            totalElapsedTime = System.currentTimeMillis() - startTime,
            nextRetryDelay = null
        )
        notifyListeners { it.onRetryAbandoned(abandonedContext) }
        
        println("ERROR: Operation '$operationName' failed after ${policy.maxRetries} attempts")
        return RetryResult.Failure(
            error = lastError ?: Exception("All retry attempts failed"),
            attemptCount = policy.maxRetries,
            finalError = finalError
        )
    }
    
    /**
     * 네트워크 관련 작업을 위한 재시도 (더 적극적인 정책 사용)
     */
    suspend fun <T> executeNetworkOperationWithRetry(
        operationName: String,
        operation: suspend (attempt: Int) -> CustomResult<T, Exception>
    ): RetryResult<T> {
        return executeWithRetry(operationName, networkPolicy, operation)
    }
    
    /**
     * 간단한 재시도 (정책 커스터마이징)
     */
    suspend fun <T> executeWithCustomRetry(
        operationName: String,
        maxRetries: Int,
        baseDelayMs: Long = 1000L,
        operation: suspend (attempt: Int) -> CustomResult<T, Exception>
    ): RetryResult<T> {
        val policy = ExponentialBackoffPolicy(
            maxRetries = maxRetries,
            baseDelayMs = baseDelayMs
        )
        return executeWithRetry(operationName, policy, operation)
    }
    
    /**
     * Exception을 FileUploadError로 변환합니다.
     */
    private fun convertToFileUploadError(error: Throwable): FileUploadError {
        return when (error) {
            is FileUploadError -> error
            is java.net.UnknownHostException -> FileUploadError.NetworkError.NoConnection
            is java.net.SocketTimeoutException -> FileUploadError.NetworkError.Timeout
            is java.io.FileNotFoundException -> FileUploadError.FileSystemError.FileNotFound(error.message ?: "unknown")
            is SecurityException -> FileUploadError.FileSystemError.FileAccessDenied(error.message ?: "unknown")
            is OutOfMemoryError -> FileUploadError.CompressionError.OutOfMemory(0L)
            else -> FileUploadError.GenericError.UnexpectedError(error)
        }
    }
    
    /**
     * 모든 이벤트 리스너에게 알림을 보냅니다.
     */
    private suspend fun notifyListeners(action: suspend (RetryEventListener) -> Unit) {
        eventListeners.forEach { listener ->
            try {
                action(listener)
            } catch (e: Exception) {
                println("WARNING: Error notifying retry event listener: ${e.message}")
            }
        }
    }
    
    /**
     * 에러 타입에 따른 권장 재시도 정책을 반환합니다.
     */
    fun getRecommendedPolicy(error: Throwable): RetryPolicy {
        return when (error) {
            is FileUploadError.NetworkError -> networkPolicy
            is FileUploadError.FirebaseError.FirebaseUnavailable -> networkPolicy
            is FileUploadError.CompressionError.CompressionFailed -> defaultPolicy
            is FileUploadError.GenericError.ServiceUnavailable -> networkPolicy
            else -> defaultPolicy
        }
    }
}
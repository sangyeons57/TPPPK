package com.example.orchestrator.util

import android.util.Log
import com.example.domain.model.sync.OutBoxRecord
import com.example.domain.model.sync.RemoteBatch

/**
 * 동기화 시스템 전용 통합 로깅 유틸리티
 *
 * 모든 동기화 관련 컴포넌트에서 일관된 로깅을 제공하며,
 * 프로덕션/디버그 모드에 따른 로그 레벨 제어를 지원합니다.
 *
 * 사용법:
 * ```kotlin
 * private val syncLogger = SyncLogger("MessageSyncPort")
 * syncLogger.logSyncStart()
 * syncLogger.logPullSuccess(batch)
 * ```
 */
class SyncLogger(private val tag: String) {

    companion object {
        private const val BASE_TAG = "Sync"

        /**
         * 전역 로그 레벨 제어
         * 프로덕션에서는 WARNING 이상만 출력
         */
        private val logLevel = if (isDebugMode()) LogLevel.DEBUG else LogLevel.WARNING

        private fun isDebugMode(): Boolean {
            // BuildConfig를 직접 참조하면 순환 의존성이 발생할 수 있으므로
            // 시스템 속성으로 확인
            return try {
                Class.forName("android.os.Debug").getMethod("isDebuggerConnected")
                    .invoke(null) as? Boolean ?: false
            } catch (e: Exception) {
                // 기본값은 디버그 모드로 설정
                true
            }
        }
    }

    enum class LogLevel(val priority: Int) {
        VERBOSE(Log.VERBOSE),
        DEBUG(Log.DEBUG),
        INFO(Log.INFO),
        WARNING(Log.WARN),
        ERROR(Log.ERROR)
    }

    private val fullTag = "$BASE_TAG-$tag"

    // ================================
    // 기본 로깅 메서드
    // ================================

    private fun log(level: LogLevel, message: String, throwable: Throwable? = null) {
        if (level.priority >= logLevel.priority) {
            when (level) {
                LogLevel.VERBOSE -> Log.v(fullTag, message, throwable)
                LogLevel.DEBUG -> Log.d(fullTag, message, throwable)
                LogLevel.INFO -> Log.i(fullTag, message, throwable)
                LogLevel.WARNING -> Log.w(fullTag, message, throwable)
                LogLevel.ERROR -> Log.e(fullTag, message, throwable)
            }
        }
    }

    fun debug(message: String) = log(LogLevel.DEBUG, message)
    fun info(message: String) = log(LogLevel.INFO, message)
    fun warn(message: String, throwable: Throwable? = null) =
        log(LogLevel.WARNING, message, throwable)

    fun error(message: String, throwable: Throwable? = null) =
        log(LogLevel.ERROR, message, throwable)

    // ================================
    // 동기화 전용 로깅 메서드
    // ================================

    /**
     * 동기화 시작 로그
     */
    fun logSyncStart(portName: String) {
        debug("🔄 [SYNC START] Starting sync for port: $portName")
    }

    /**
     * 동기화 완료 로그
     */
    fun logSyncEnd(portName: String, duration: Long? = null) {
        val durationText = duration?.let { " (${it}ms)" } ?: ""
        debug("✅ [SYNC END] Sync finished for port: $portName$durationText")
    }

    /**
     * Push 동기화 로그
     */
    fun logPushStart(portName: String, itemCount: Int, items: List<OutBoxRecord>) {
        if (itemCount > 0) {
            val itemIds = items.take(3).joinToString { it.aggregateId }
            val moreText = if (items.size > 3) " and ${items.size - 3} more" else ""
            debug("📤 [SYNC PUSH] Pushing $itemCount items for port: $portName. IDs: $itemIds$moreText")
        } else {
            debug("📤 [SYNC PUSH] No items to push for port: $portName")
        }
    }

    /**
     * Push 결과 로그
     */
    fun logPushResult(portName: String, successCount: Int, failCount: Int) {
        if (successCount > 0 || failCount > 0) {
            debug("📊 [SYNC PUSH] Results for port: $portName - Success: $successCount, Failed: $failCount")
        }
    }

    /**
     * Pull 동기화 로그
     */
    fun logPullStart(portName: String, cursor: String?) {
        debug("📥 [SYNC PULL] Pulling items for port: $portName since cursor: $cursor")
    }

    /**
     * Pull 성공 로그
     */
    fun <T> logPullSuccess(portName: String, batch: RemoteBatch<T>) {
        if (batch.items.isNotEmpty()) {
            debug("📥 [SYNC PULL] Pulled ${batch.items.size} items for port: $portName. HasMore: ${batch.hasMore}")
        } else {
            debug("📥 [SYNC PULL] No new items to pull for port: $portName")
        }
    }

    /**
     * Pull 실패 로그
     */
    fun logPullFailure(portName: String, cursor: String?, error: Throwable) {
        error("❌ [SYNC PULL] Failed to pull items for port: $portName, cursor: $cursor", error)
    }

    /**
     * Apply 동기화 로그
     */
    fun logApplyStart(portName: String, itemCount: Int) {
        debug("📲 [SYNC APPLY] Applying $itemCount items for port: $portName")
    }

    /**
     * Apply 결과 로그
     */
    fun logApplyResult(portName: String, successCount: Int, failCount: Int) {
        debug("📊 [SYNC APPLY] Results for port: $portName - Success: $successCount, Failed: $failCount")
    }

    /**
     * Apply 실패 로그
     */
    fun logApplyFailure(portName: String, error: Throwable) {
        error("❌ [SYNC APPLY] Failed to apply remote batch for port: $portName", error)
    }

    /**
     * 커서 업데이트 로그
     */
    fun logCursorUpdate(portName: String, oldCursor: String?, newCursor: String?) {
        debug("🔄 [SYNC CURSOR] Updated cursor for port: $portName from '$oldCursor' to '$newCursor'")
    }

    /**
     * OutBox 관련 로그
     */
    fun logOutboxOperation(operation: String, portName: String, itemCount: Int) {
        debug("📦 [SYNC OUTBOX] $operation for port: $portName - Items: $itemCount")
    }

    /**
     * 예외 상황 로그 (항상 출력)
     */
    fun logException(operation: String, portName: String, error: Throwable) {
        error("💥 [SYNC EXCEPTION] $operation failed for port: $portName", error)
    }

    // ================================
    // 성능 측정 유틸리티
    // ================================

    /**
     * 동기화 성능 측정을 위한 타이머
     */
    class SyncTimer(private val logger: SyncLogger, private val operation: String) {
        private val startTime = System.currentTimeMillis()

        fun finish(portName: String) {
            val duration = System.currentTimeMillis() - startTime
            logger.debug("⏱️ [SYNC TIMING] $operation for port: $portName took ${duration}ms")
        }
    }

    /**
     * 성능 측정 시작
     */
    fun startTimer(operation: String) = SyncTimer(this, operation)
}
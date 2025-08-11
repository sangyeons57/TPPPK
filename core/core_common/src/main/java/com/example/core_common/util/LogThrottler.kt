package com.example.core_common.util

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 로그 출력을 최적화하는 유틸리티 클래스
 * 과도한 로그 출력을 방지하고 성능 영향을 최소화합니다.
 */
object LogThrottler {
    private const val TAG = "LogThrottler"

    // 로그 스로틀링을 위한 데이터 저장소
    private val lastLogTimes = ConcurrentHashMap<String, Long>()
    private val logCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val totalLogCount = AtomicInteger(0)
    private val startTime = System.currentTimeMillis()

    // 디버그 모드에서만 상세 로그 출력 (Log 레벨로 판단)
    val isDebugMode: Boolean = Log.isLoggable("LogThrottler", Log.DEBUG)

    /**
     * 스로틀링된 디버그 로그 출력
     * @param tag 로그 태그
     * @param message 로그 메시지
     * @param throttleKey 스로틀링 키 (동일한 키는 함께 스로틀링됨)
     * @param intervalMs 최소 출력 간격 (밀리초)
     */
    fun d(tag: String, message: String, throttleKey: String, intervalMs: Long = 1000L) {
        if (!shouldLog(throttleKey, intervalMs)) return

        val count = logCounts.getOrPut(throttleKey) { AtomicInteger(0) }.incrementAndGet()
        totalLogCount.incrementAndGet()

        if (isDebugMode) {
            Log.d(tag, "[$count] $message")
        }
    }

    /**
     * 스로틀링된 경고 로그 출력 (항상 출력)
     */
    fun w(tag: String, message: String, throttleKey: String, intervalMs: Long = 5000L) {
        if (!shouldLog(throttleKey, intervalMs)) return

        val count = logCounts.getOrPut(throttleKey) { AtomicInteger(0) }.incrementAndGet()
        totalLogCount.incrementAndGet()

        Log.w(tag, "[$count] $message")
    }

    /**
     * 스로틀링된 오류 로그 출력 (항상 출력)
     */
    fun e(
        tag: String,
        message: String,
        throttleKey: String,
        intervalMs: Long = 10000L,
        throwable: Throwable? = null
    ) {
        if (!shouldLog(throttleKey, intervalMs)) return

        val count = logCounts.getOrPut(throttleKey) { AtomicInteger(0) }.incrementAndGet()
        totalLogCount.incrementAndGet()

        if (throwable != null) {
            Log.e(tag, "[$count] $message", throwable)
        } else {
            Log.e(tag, "[$count] $message")
        }
    }

    /**
     * 샘플링된 로그 출력 (N번마다 1번 출력)
     */
    fun sampleLog(tag: String, message: String, sampleKey: String, sampleRate: Int = 10) {
        val count = logCounts.getOrPut(sampleKey) { AtomicInteger(0) }.incrementAndGet()

        if (count % sampleRate == 0) {
            totalLogCount.incrementAndGet()

            if (isDebugMode) {
                Log.d(tag, "[#$count] $message")
            }
        }
    }

    /**
     * 로그 출력 여부 결정
     */
    private fun shouldLog(key: String, intervalMs: Long): Boolean {
        val now = System.currentTimeMillis()
        val lastTime = lastLogTimes[key] ?: 0L

        if (now - lastTime >= intervalMs) {
            lastLogTimes[key] = now
            return true
        }

        return false
    }

    /**
     * 로그 통계 정보 조회
     */
    fun getLogStats(): String {
        val currentTime = System.currentTimeMillis()
        val uptimeMinutes = (currentTime - startTime) / (1000 * 60)
        val logsPerMinute = if (uptimeMinutes > 0) totalLogCount.get() / uptimeMinutes else 0

        return buildString {
            appendLine("=== LogThrottler Stats ===")
            appendLine("Debug Mode: $isDebugMode")
            appendLine("Total Logs: ${totalLogCount.get()}")
            appendLine("Uptime: ${uptimeMinutes}min")
            appendLine("Logs/min: $logsPerMinute")
            appendLine("Unique Keys: ${logCounts.size}")

            if (isDebugMode && logCounts.isNotEmpty()) {
                appendLine("\nTop Log Sources:")
                logCounts.entries
                    .sortedByDescending { it.value.get() }
                    .take(5)
                    .forEach { (key, count) ->
                        appendLine("  $key: ${count.get()}")
                    }
            }
        }
    }

    /**
     * 통계 초기화
     */
    fun resetStats() {
        lastLogTimes.clear()
        logCounts.clear()
        totalLogCount.set(0)
        Log.d(TAG, "로그 통계가 초기화되었습니다")
    }

    /**
     * 메모리 정리 (주기적으로 호출 권장)
     */
    fun cleanup() {
        val now = System.currentTimeMillis()
        val cutoff = now - (60 * 60 * 1000L) // 1시간 전

        val iterator = lastLogTimes.entries.iterator()
        var removedCount = 0

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value < cutoff) {
                iterator.remove()
                logCounts.remove(entry.key)
                removedCount++
            }
        }

        if (removedCount > 0 && isDebugMode) {
            Log.d(TAG, "로그 스로틀러 정리 완료: ${removedCount}개 항목 제거")
        }
    }
}
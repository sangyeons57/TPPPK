package com.example.core_common.util

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 채널별 동기화 요청을 쓰로틀링(throttling)하여 과도한 동기화 방지
 *
 * 동일 채널에 대해 설정된 쿨다운 시간 내에는 중복 동기화를 차단하여
 * 네트워크 부하와 불필요한 서버 요청을 최소화합니다.
 *
 * 사용 예시:
 * ```kotlin
 * if (syncThrottler.canSync(channelId)) {
 *     syncUseCase.syncChannel(channelId)
 *     syncThrottler.markSynced(channelId)
 * }
 * ```
 */
@Singleton
class SyncThrottler @Inject constructor() {

    companion object {
        private const val TAG = "SyncThrottler"

        /** 동기화 쿨다운 시간 (밀리초) */
        private const val SYNC_COOLDOWN_MS = 60_000L // 1분

        /** 로그 출력용 쿨다운 시간 (초) */
        private const val COOLDOWN_SECONDS = SYNC_COOLDOWN_MS / 1000
    }

    /**
     * 채널별 마지막 동기화 시간을 저장하는 맵
     * Thread-safe한 ConcurrentHashMap 사용
     */
    private val lastSyncTimes = ConcurrentHashMap<String, Long>()

    /**
     * 해당 채널의 특정 로드 타입에 대해 동기화 실행이 가능한지 확인
     *
     * @param channelId 확인할 채널 ID
     * @param loadType 로드 타입 (Refresh, Append, Prepend, Initial 등)
     * @return 동기화 가능 여부 (true: 가능, false: 쿨다운 중)
     */
    fun canSync(channelId: String, loadType: String): Boolean {
        val key = "$channelId:$loadType"
        val now = System.currentTimeMillis()
        val lastSync = lastSyncTimes[key] ?: 0L
        val timeSinceLastSync = now - lastSync
        val canSyncNow = timeSinceLastSync >= SYNC_COOLDOWN_MS

        if (canSyncNow) {
            Log.d(TAG, "✅ Sync allowed for '$key' (${timeSinceLastSync}ms since last sync)")
        } else {
            val remainingCooldown = SYNC_COOLDOWN_MS - timeSinceLastSync
            Log.d(
                TAG,
                "🔥 Sync blocked for '$key' (cooldown: ${remainingCooldown}ms remaining)"
            )
        }

        return canSyncNow
    }

    /**
     * 하위 호환성을 위한 기존 메서드 (loadType 없이 호출)
     *
     * @param channelId 확인할 채널 ID
     * @return 동기화 가능 여부 (true: 가능, false: 쿨다운 중)
     * @deprecated loadType과 함께 사용하는 canSync(channelId, loadType) 사용 권장
     */
    @Deprecated("Use canSync(channelId, loadType) for page-specific throttling")
    fun canSync(channelId: String): Boolean {
        return canSync(channelId, "Default")
    }

    /**
     * 해당 채널의 특정 로드 타입의 동기화 완료를 기록
     *
     * 동기화가 실제로 수행된 후에만 호출해야 합니다.
     * 이 메서드 호출 후 SYNC_COOLDOWN_MS 시간 동안 해당 채널의 해당 로드 타입 동기화가 차단됩니다.
     *
     * @param channelId 동기화가 완료된 채널 ID
     * @param loadType 동기화가 완료된 로드 타입
     */
    fun markSynced(channelId: String, loadType: String) {
        val key = "$channelId:$loadType"
        val now = System.currentTimeMillis()
        lastSyncTimes[key] = now
        Log.d(
            TAG,
            "📝 '$key' marked as synced. Next sync available after ${COOLDOWN_SECONDS}s"
        )
    }

    /**
     * 하위 호환성을 위한 기존 메서드 (loadType 없이 호출)
     *
     * @param channelId 동기화가 완료된 채널 ID
     * @deprecated loadType과 함께 사용하는 markSynced(channelId, loadType) 사용 권장
     */
    @Deprecated("Use markSynced(channelId, loadType) for page-specific throttling")
    fun markSynced(channelId: String) {
        markSynced(channelId, "Default")
    }

    /**
     * 특정 채널의 특정 로드 타입 동기화 쿨다운을 강제로 리셋
     *
     * 테스트나 특별한 상황에서 즉시 동기화를 허용하고 싶을 때 사용
     *
     * @param channelId 리셋할 채널 ID
     * @param loadType 리셋할 로드 타입
     */
    fun resetCooldown(channelId: String, loadType: String) {
        val key = "$channelId:$loadType"
        val removed = lastSyncTimes.remove(key)
        if (removed != null) {
            Log.d(TAG, "🔄 Cooldown reset for '$key'")
        } else {
            Log.d(TAG, "🔄 No cooldown to reset for '$key'")
        }
    }

    /**
     * 특정 채널의 모든 로드 타입 쿨다운을 리셋
     *
     * @param channelId 리셋할 채널 ID
     */
    fun resetCooldown(channelId: String) {
        val keysToRemove = lastSyncTimes.keys.filter { it.startsWith("$channelId:") }
        var removedCount = 0
        keysToRemove.forEach { key ->
            if (lastSyncTimes.remove(key) != null) {
                removedCount++
            }
        }
        Log.d(TAG, "🔄 Cooldown reset for channel '$channelId' ($removedCount load types cleared)")
    }

    /**
     * 모든 채널의 동기화 쿨다운을 리셋
     *
     * 앱 재시작이나 전체적인 동기화 상태 초기화가 필요할 때 사용
     */
    fun resetAllCooldowns() {
        val channelCount = lastSyncTimes.size
        lastSyncTimes.clear()
        Log.d(TAG, "🔄 All cooldowns reset ($channelCount channels cleared)")
    }

    /**
     * 특정 채널의 특정 로드 타입 마지막 동기화 시간 조회
     *
     * @param channelId 조회할 채널 ID
     * @param loadType 조회할 로드 타입
     * @return 마지막 동기화 시간 (Unix timestamp), 없으면 null
     */
    fun getLastSyncTime(channelId: String, loadType: String): Long? {
        val key = "$channelId:$loadType"
        return lastSyncTimes[key]
    }

    /**
     * 특정 채널의 특정 로드 타입 남은 쿨다운 시간 조회
     *
     * @param channelId 조회할 채널 ID
     * @param loadType 조회할 로드 타입
     * @return 남은 쿨다운 시간 (밀리초), 쿨다운이 끝났으면 0
     */
    fun getRemainingCooldown(channelId: String, loadType: String): Long {
        val key = "$channelId:$loadType"
        val lastSync = lastSyncTimes[key] ?: return 0L
        val now = System.currentTimeMillis()
        val elapsed = now - lastSync
        return maxOf(0L, SYNC_COOLDOWN_MS - elapsed)
    }

    /**
     * 하위 호환성을 위한 기존 메서드들
     */
    @Deprecated("Use getLastSyncTime(channelId, loadType) for page-specific info")
    fun getLastSyncTime(channelId: String): Long? {
        return getLastSyncTime(channelId, "Default")
    }

    @Deprecated("Use getRemainingCooldown(channelId, loadType) for page-specific info")
    fun getRemainingCooldown(channelId: String): Long {
        return getRemainingCooldown(channelId, "Default")
    }

    /**
     * 현재 쿨다운 중인 채널 수 조회
     *
     * @return 쿨다운 중인 채널 수
     */
    fun getActiveCooldownCount(): Int {
        val now = System.currentTimeMillis()
        return lastSyncTimes.values.count { (now - it) < SYNC_COOLDOWN_MS }
    }

    /**
     * 디버깅용 현재 상태 출력
     */
    fun logCurrentState() {
        val now = System.currentTimeMillis()
        Log.d(TAG, "=".repeat(50))
        Log.d(TAG, "📊 SyncThrottler Current State")
        Log.d(TAG, "   Total channels tracked: ${lastSyncTimes.size}")
        Log.d(TAG, "   Active cooldowns: ${getActiveCooldownCount()}")
        Log.d(TAG, "   Cooldown duration: ${COOLDOWN_SECONDS}s")

        lastSyncTimes.forEach { (channelId, lastSync) ->
            val elapsed = now - lastSync
            val remaining = maxOf(0L, SYNC_COOLDOWN_MS - elapsed)
            val status = if (remaining > 0) "🔥 ${remaining}ms remaining" else "✅ Available"
            Log.d(TAG, "   Channel '$channelId': $status")
        }
        Log.d(TAG, "=".repeat(50))
    }
}
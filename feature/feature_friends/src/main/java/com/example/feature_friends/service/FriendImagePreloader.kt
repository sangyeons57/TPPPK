package com.example.feature_friends.service

import android.util.Log
import com.example.core_common.cache.GlobalImageUrlCache
import com.example.feature_friends.viewmodel.FriendItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 친구 리스트의 프로필 이미지를 배치로 프리로딩하는 서비스
 */
@Singleton
class FriendImagePreloader @Inject constructor(
    private val globalImageUrlCache: GlobalImageUrlCache
) {

    private var preloadingJob: Job? = null

    /**
     * 친구 리스트의 프로필 이미지들을 백그라운드에서 배치 프리로딩
     * @param friends 친구 목록
     * @param scope 코루틴 스코프
     */
    fun preloadFriendImages(friends: List<FriendItem>, scope: CoroutineScope) {
        // 이전 프리로딩 작업 취소
        preloadingJob?.cancel()

        if (friends.isEmpty()) {
            Log.d("FriendImagePreloader", "preloadFriendImages: empty friend list")
            return
        }

        val userIds = friends.map { it.friendId.value }.toSet()
        Log.d(
            "FriendImagePreloader",
            "preloadFriendImages: starting preload for ${userIds.size} friends"
        )

        preloadingJob = scope.launch {
            try {
                // 백그라운드에서 배치 로딩
                val results = globalImageUrlCache.getUserProfileImageUrls(userIds)
                val successCount = results.values.count { it != null }

                Log.d(
                    "FriendImagePreloader",
                    "preloadFriendImages: completed $successCount/${results.size} profile URLs"
                )
            } catch (e: Exception) {
                Log.e("FriendImagePreloader", "preloadFriendImages: error during batch preload", e)
            }
        }
    }

    /**
     * 프리로딩 작업을 취소합니다.
     */
    fun cancelPreloading() {
        preloadingJob?.cancel()
        preloadingJob = null
        Log.d("FriendImagePreloader", "cancelPreloading: preloading job cancelled")
    }
}
package com.example.feature_member_list.service

import android.util.Log
import com.example.core_common.cache.GlobalImageUrlCache
import com.example.domain.model.ui.data.MemberUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 멤버 리스트의 프로필 이미지를 배치로 프리로딩하는 서비스
 */
@Singleton
class MemberImagePreloader @Inject constructor(
    private val globalImageUrlCache: GlobalImageUrlCache
) {

    private var preloadingJob: Job? = null

    /**
     * 멤버 리스트의 프로필 이미지들을 백그라운드에서 배치 프리로딩
     * @param members 멤버 목록
     * @param scope 코루틴 스코프
     */
    fun preloadMemberImages(members: List<MemberUiModel>, scope: CoroutineScope) {
        // 이전 프리로딩 작업 취소
        preloadingJob?.cancel()

        if (members.isEmpty()) {
            Log.d("MemberImagePreloader", "preloadMemberImages: empty member list")
            return
        }

        val userIds = members.map { it.userId.value }.toSet()
        Log.d(
            "MemberImagePreloader",
            "preloadMemberImages: starting preload for ${userIds.size} members"
        )

        preloadingJob = scope.launch {
            try {
                // 백그라운드에서 배치 로딩
                val results = globalImageUrlCache.getUserProfileImageUrls(userIds)
                val successCount = results.values.count { it != null }

                Log.d(
                    "MemberImagePreloader",
                    "preloadMemberImages: completed $successCount/${results.size} profile URLs"
                )
            } catch (e: Exception) {
                Log.e("MemberImagePreloader", "preloadMemberImages: error during batch preload", e)
            }
        }
    }

    /**
     * 프리로딩 작업을 취소합니다.
     */
    fun cancelPreloading() {
        preloadingJob?.cancel()
        preloadingJob = null
        Log.d("MemberImagePreloader", "cancelPreloading: preloading job cancelled")
    }
}
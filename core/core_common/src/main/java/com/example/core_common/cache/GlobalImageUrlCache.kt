package com.example.core_common.cache

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Storage URL을 위한 글로벌 캐시 매니저
 * 모든 컴포넌트 간에 URL 캐시를 공유하여 중복 Firebase 호출을 방지합니다.
 */
@Singleton
class GlobalImageUrlCache @Inject constructor() {

    private val urlCache = mutableMapOf<String, String?>()
    private val loadingSet = mutableSetOf<String>()
    private val mutex = Mutex()

    /**
     * 단일 사용자의 프로필 이미지 URL을 가져옵니다.
     * @param userId 사용자 ID
     * @return Firebase Storage URL 또는 null (이미지가 없을 경우)
     */
    suspend fun getUserProfileImageUrl(userId: String): String? = mutex.withLock {
        // 이미 캐시된 경우 반환
        if (urlCache.containsKey(userId)) {
            val cachedUrl = urlCache[userId]
            Log.d("GlobalImageUrlCache", "getUserProfileImageUrl($userId): cached = $cachedUrl")
            return@withLock cachedUrl
        }

        // 이미 로딩 중인 경우 대기
        if (loadingSet.contains(userId)) {
            Log.d(
                "GlobalImageUrlCache",
                "getUserProfileImageUrl($userId): already loading, waiting..."
            )
            // 로딩이 완료될 때까지 잠시 대기
            while (loadingSet.contains(userId)) {
                mutex.unlock()
                kotlinx.coroutines.delay(50)
                mutex.lock()
            }
            return@withLock urlCache[userId]
        }

        // 새로 로딩 시작
        loadingSet.add(userId)
        Log.d("GlobalImageUrlCache", "getUserProfileImageUrl($userId): starting new load")

        try {
            val url = fetchFirebaseStorageUrl(userId)
            urlCache[userId] = url
            Log.d("GlobalImageUrlCache", "getUserProfileImageUrl($userId): loaded = $url")
            return@withLock url
        } catch (e: Exception) {
            Log.e("GlobalImageUrlCache", "getUserProfileImageUrl($userId): error", e)
            urlCache[userId] = null
            return@withLock null
        } finally {
            loadingSet.remove(userId)
        }
    }

    /**
     * 여러 사용자의 프로필 이미지 URL을 병렬로 가져옵니다.
     * @param userIds 사용자 ID 목록
     * @return userId to URL 매핑
     */
    suspend fun getUserProfileImageUrls(userIds: Set<String>): Map<String, String?> =
        coroutineScope {
            if (userIds.isEmpty()) return@coroutineScope emptyMap()

            Log.d(
                "GlobalImageUrlCache",
                "getUserProfileImageUrls: processing ${userIds.size} users"
            )

            mutex.withLock {
                // 캐시되지 않은 사용자들만 필터링
                val uncachedUserIds =
                    userIds.filter { !urlCache.containsKey(it) && !loadingSet.contains(it) }

                if (uncachedUserIds.isEmpty()) {
                    Log.d(
                        "GlobalImageUrlCache",
                        "getUserProfileImageUrls: all users cached/loading"
                    )
                    return@withLock userIds.associateWith { urlCache[it] }
                }

                // 로딩 상태로 마킹
                uncachedUserIds.forEach { loadingSet.add(it) }
                Log.d(
                    "GlobalImageUrlCache",
                    "getUserProfileImageUrls: loading ${uncachedUserIds.size} uncached users"
                )
            }

            try {
                // 병렬로 Firebase Storage URL 가져오기
                val loadResults = userIds.map { userId ->
                    async {
                        if (mutex.withLock { urlCache.containsKey(userId) }) {
                            // 이미 캐시된 경우
                            userId to mutex.withLock { urlCache[userId] }
                        } else {
                            // 새로 로딩
                            try {
                                val url = fetchFirebaseStorageUrl(userId)
                                mutex.withLock {
                                    urlCache[userId] = url
                                    loadingSet.remove(userId)
                                }
                                userId to url
                            } catch (e: Exception) {
                                Log.e(
                                    "GlobalImageUrlCache",
                                    "getUserProfileImageUrls: error loading $userId",
                                    e
                                )
                                mutex.withLock {
                                    urlCache[userId] = null
                                    loadingSet.remove(userId)
                                }
                                userId to null
                            }
                        }
                    }
                }.awaitAll().toMap()

                val successCount = loadResults.values.count { it != null }
                Log.d(
                    "GlobalImageUrlCache",
                    "getUserProfileImageUrls: loaded $successCount/${loadResults.size} URLs"
                )

                return@coroutineScope loadResults
            } catch (e: Exception) {
                // 에러 발생시 로딩 상태 클리어
                mutex.withLock {
                    userIds.forEach { loadingSet.remove(it) }
                }
                Log.e("GlobalImageUrlCache", "getUserProfileImageUrls: batch error", e)
                return@coroutineScope userIds.associateWith { null }
            }
        }

    /**
     * Firebase Storage에서 실제 URL을 가져오는 내부 메서드
     */
    private suspend fun fetchFirebaseStorageUrl(userId: String): String? {
        return try {
            val storage = Firebase.storage
            val pathString = "user_profiles/$userId/profile.webp"
            val imageRef = storage.reference.child(pathString)

            // 파일 존재 여부를 먼저 확인 (404 ERROR 로그 방지)
            imageRef.metadata.await()

            // 파일이 존재하면 URL 요청
            val uri = imageRef.downloadUrl.await()
            val urlString = uri.toString()

            Log.d("GlobalImageUrlCache", "fetchFirebaseStorageUrl($userId): success = $urlString")
            urlString
        } catch (e: Exception) {
            when {
                e.message?.contains("Object does not exist") == true -> {
                    Log.d("GlobalImageUrlCache", "fetchFirebaseStorageUrl($userId): no image found")
                    null
                }

                e.message?.contains("Permission denied") == true -> {
                    Log.w(
                        "GlobalImageUrlCache",
                        "fetchFirebaseStorageUrl($userId): permission denied"
                    )
                    null
                }

                e.message?.contains("StorageException") == true && e.message?.contains("404") == true -> {
                    Log.d("GlobalImageUrlCache", "fetchFirebaseStorageUrl($userId): 404 not found")
                    null
                }

                else -> {
                    Log.e(
                        "GlobalImageUrlCache",
                        "fetchFirebaseStorageUrl($userId): unexpected error",
                        e
                    )
                    null
                }
            }
        }
    }

    /**
     * 특정 사용자의 캐시를 클리어합니다.
     */
    suspend fun clearUserCache(userId: String) = mutex.withLock {
        urlCache.remove(userId)
        loadingSet.remove(userId)
        Log.d("GlobalImageUrlCache", "clearUserCache($userId): cleared")
    }

    /**
     * 여러 사용자의 캐시를 클리어합니다.
     */
    suspend fun clearUserCaches(userIds: Set<String>) = mutex.withLock {
        userIds.forEach { userId ->
            urlCache.remove(userId)
            loadingSet.remove(userId)
        }
        Log.d("GlobalImageUrlCache", "clearUserCaches: cleared ${userIds.size} users")
    }

    /**
     * 모든 캐시를 클리어합니다.
     */
    suspend fun clearAllCache() = mutex.withLock {
        urlCache.clear()
        loadingSet.clear()
        Log.d("GlobalImageUrlCache", "clearAllCache: all caches cleared")
    }

    /**
     * 캐시된 URL을 동기적으로 가져옵니다 (캐시에 없으면 null)
     */
    fun getCachedUrl(userId: String): String? = urlCache[userId]

    /**
     * 캐시 상태를 반환합니다.
     */
    fun getCacheStats(): String {
        return buildString {
            appendLine("=== Global Image URL Cache Stats ===")
            appendLine("Cached URLs: ${urlCache.size}")
            appendLine("Currently Loading: ${loadingSet.size}")
            appendLine("Success Rate: ${urlCache.values.count { it != null }}/${urlCache.size}")
        }
    }
}
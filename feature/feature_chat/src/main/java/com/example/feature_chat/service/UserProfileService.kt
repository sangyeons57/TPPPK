package com.example.feature_chat.service

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.vo.DocumentId
import com.example.domain_usecase.provider.file.FileManagementUseCases
import com.example.domain_usecase.provider.user.UserUseCases
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/**
 * 사용자 프로필 및 이미지 관리를 담당하는 Service
 * 프로필 데이터 로딩, 캐시 관리, 프로필 이미지 URL 관리 등의 기능을 제공합니다.
 */
class UserProfileService(
    private val userUseCases: UserUseCases,
    private val fileUseCases: FileManagementUseCases
) {
    
    // User profile cache
    private val userProfileCache = mutableMapOf<String, User>()
    // Profile URL cache to avoid repeated file existence checks
    private val profileUrlCache = mutableMapOf<String, String?>()
    // Loading state tracking
    private val loadingProfileUserIds = mutableSetOf<String>()
    
    /**
     * 사용자 프로필을 로딩하고 캐시에 저장 (비동기)
     * 이미 로딩된 경우나 로딩 중인 경우는 스킵
     */
    suspend fun loadUserProfile(userId: String, retryCount: Int = 0) {
        Log.d("UserProfileService", "loadUserProfile($userId): starting profile load (retry: $retryCount)")
        
        if (userProfileCache.containsKey(userId)) {
            Log.d("UserProfileService", "loadUserProfile($userId): already cached, skipping")
            return
        }
        
        if (loadingProfileUserIds.contains(userId)) {
            Log.d("UserProfileService", "loadUserProfile($userId): already loading, skipping")
            return
        }
        
        Log.d("UserProfileService", "loadUserProfile($userId): marking as loading")
        loadingProfileUserIds.add(userId)
        
        // Actually perform the loading
        try {
            loadUserProfileAsync(userId, retryCount)
        } catch (e: Exception) {
            Log.e("UserProfileService", "loadUserProfile($userId): error in loading", e)
            loadingProfileUserIds.remove(userId)
        }
    }
    
    /**
     * 사용자 프로필을 비동기로 로딩 (코루틴 스코프 내에서 호출)
     */
    suspend fun loadUserProfileAsync(userId: String, retryCount: Int = 0): User? = coroutineScope {
        Log.d("UserProfileService", "loadUserProfileAsync($userId): starting async profile load (retry: $retryCount)")
        
        if (userProfileCache.containsKey(userId)) {
            Log.d("UserProfileService", "loadUserProfileAsync($userId): already cached")
            return@coroutineScope userProfileCache[userId]
        }
        
        Log.d("UserProfileService", "loadUserProfileAsync($userId): starting async profile and URL jobs")
        
        // Load both profile data and profile URL
        val profileJob = async {
            Log.d("UserProfileService", "loadUserProfileAsync($userId): calling getUserByIdUseCase")
            try {
                when (val result = userUseCases.getUserByIdUseCase(DocumentId(userId))) {
                    is CustomResult.Success -> {
                        userProfileCache[userId] = result.data
                        Log.d("UserProfileService", "loadUserProfileAsync($userId): SUCCESS - loaded profile: ${result.data.name.value}")
                        result.data
                    }
                    is CustomResult.Failure -> {
                        Log.e("UserProfileService", "loadUserProfileAsync($userId): FAILURE - ${result.error.message}", result.error)
                        
                        // Don't create fallback - return null to indicate user not found
                        null
                    }
                    is CustomResult.Loading -> {
                        Log.d("UserProfileService", "loadUserProfileAsync($userId): still loading")
                        
                        // If we get Loading status, retry after a delay
                        if (retryCount < 2) {
                            delay(1000)
                            Log.d("UserProfileService", "loadUserProfileAsync($userId): retrying after Loading status")
                            return@async loadUserProfileAsync(userId, retryCount + 1)
                        }
                        null
                    }
                    is CustomResult.Initial -> {
                        Log.d("UserProfileService", "loadUserProfileAsync($userId): initial state")
                        null
                    }
                    else -> {
                        Log.w("UserProfileService", "loadUserProfileAsync($userId): unexpected result type: ${result::class.simpleName}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("UserProfileService", "loadUserProfileAsync($userId): exception during profile fetch", e)
                // Don't create fallback - return null to indicate user not found
                null
            }
        }
        
        val urlJob = async { 
            try {
                if (!profileUrlCache.containsKey(userId)) {
                    Log.d("UserProfileService", "loadUserProfileAsync($userId): fetching profile URL")
                    getUserProfileUrl(userId) // This will cache the result
                } else {
                    Log.d("UserProfileService", "loadUserProfileAsync($userId): profile URL already cached")
                }
            } catch (e: Exception) {
                Log.e("UserProfileService", "loadUserProfileAsync($userId): error fetching profile URL", e)
                // Cache null to prevent repeated attempts
                profileUrlCache[userId] = null
            }
        }
        
        // Wait for both to complete
        val profileResult = profileJob.await()
        urlJob.await()
        
        Log.d("UserProfileService", "loadUserProfileAsync($userId): both jobs completed, profileResult: ${profileResult?.name?.value}")
        
        // Mark as no longer loading
        loadingProfileUserIds.remove(userId)
        Log.d("UserProfileService", "loadUserProfileAsync($userId): removed from loading set")
        
        profileResult
    }
    
    /**
     * 사용자 표시 이름 반환
     */
    fun getUserDisplayName(userId: String): String {
        val cachedUser = userProfileCache[userId]
        val isLoading = loadingProfileUserIds.contains(userId)
        val result = when {
            cachedUser != null -> cachedUser.name.value
            isLoading -> "로딩 중..."
            else -> "알 수 없는 사용자" // More descriptive name for unknown users
        }
        
        Log.d("UserProfileService", "getUserDisplayName($userId): cached=${cachedUser?.name?.value}, loading=$isLoading, result='$result'")
        return result
    }
    
    /**
     * 사용자 프로필 이미지 URL 반환 (비동기)
     */
    suspend fun getUserProfileUrl(userId: String): String? {
        Log.d("UserProfileService", "getUserProfileUrl($userId): starting profile URL lookup")
        
        // Check cache first
        if (profileUrlCache.containsKey(userId)) {
            val cachedUrl = profileUrlCache[userId]
            Log.d("UserProfileService", "getUserProfileUrl($userId): found in cache: $cachedUrl")
            return cachedUrl
        }
        
        // Use the fixed path that matches Firebase Functions implementation
        // Based on functions/src/triggers/user/userImage.trigger.ts line 57
        val fixedPath = "user_profiles/$userId/profile.webp"
        
        Log.d("UserProfileService", "getUserProfileUrl($userId): checking fixed path: $fixedPath")
        
        var foundUrl: String? = null
        try {
            // Use getFileUrlUseCase to check if file exists and get URL directly
            when (val result = fileUseCases.getFileUrlUseCase(fixedPath)) {
                is CustomResult.Success -> {
                    foundUrl = result.data
                    Log.d("UserProfileService", "getUserProfileUrl($userId): found file at path $fixedPath with URL: $foundUrl")
                }
                is CustomResult.Failure -> {
                    // Check if this is specifically a 404 error (file not found)
                    val errorMessage = result.error.message ?: ""
                    if (errorMessage.contains("404") || errorMessage.contains("Object does not exist") || 
                        errorMessage.contains("Not Found")) {
                        Log.d("UserProfileService", "getUserProfileUrl($userId): profile image not found (404), will use default")
                        foundUrl = null
                    } else {
                        Log.w("UserProfileService", "getUserProfileUrl($userId): other error getting file URL: $errorMessage")
                        foundUrl = null
                    }
                }
                else -> {
                    Log.d("UserProfileService", "getUserProfileUrl($userId): unexpected result checking path $fixedPath")
                    foundUrl = null
                }
            }
        } catch (e: Exception) {
            Log.e("UserProfileService", "getUserProfileUrl($userId): exception checking path $fixedPath", e)
            // Check if the exception is related to storage/network issues
            if (e.message?.contains("404") == true || e.message?.contains("Object does not exist") == true) {
                Log.d("UserProfileService", "getUserProfileUrl($userId): profile image not found (exception), will use default")
            }
            foundUrl = null
        }
        
        val url = if (foundUrl != null) {
            Log.d("UserProfileService", "getUserProfileUrl($userId): using Firebase-generated URL: $foundUrl")
            
            // Test URL accessibility before returning
            val finalUrl = validateAndGetAccessibleUrl(foundUrl, userId)
            Log.d("UserProfileService", "getUserProfileUrl($userId): final validated URL: $finalUrl")
            finalUrl
        } else {
            Log.d("UserProfileService", "getUserProfileUrl($userId): no profile image found, will use default")
            // Return null to use default profile image from SimpleUserProfileImage component
            null
        }
        
        // Cache the result (even if null)
        profileUrlCache[userId] = url
        Log.d("UserProfileService", "getUserProfileUrl($userId): cached result: $url")
        return url
    }
    
    /**
     * 캐시된 프로필 URL 반환 (동기)
     */
    fun getCachedProfileUrl(userId: String): String? {
        return profileUrlCache[userId]
    }
    
    /**
     * 특정 사용자의 메시지들을 위해 프로필 정보 업데이트
     */
    suspend fun updateMessagesForUser(userId: String): Pair<String, String?> {
        Log.d("UserProfileService", "updateMessagesForUser($userId): updating profile info")
        
        val newDisplayName = getUserDisplayName(userId)
        val newProfileUrl = getCachedProfileUrl(userId)
        Log.d("UserProfileService", "updateMessagesForUser($userId): newDisplayName='$newDisplayName', newProfileUrl='$newProfileUrl'")
        
        return Pair(newDisplayName, newProfileUrl)
    }
    
    /**
     * 모든 캐시 클리어
     */
    fun clearAllCaches() {
        Log.d("UserProfileService", "Clearing all caches")
        userProfileCache.clear()
        profileUrlCache.clear()
        loadingProfileUserIds.clear()
    }
    
    /**
     * 특정 사용자의 캐시 클리어
     */
    fun clearUserCache(userId: String) {
        Log.d("UserProfileService", "Clearing cache for user: $userId")
        userProfileCache.remove(userId)
        profileUrlCache.remove(userId)
        loadingProfileUserIds.remove(userId)
    }
    
    /**
     * 현재 로딩 중인 사용자 ID 목록 반환
     */
    fun getLoadingUserIds(): Set<String> {
        return loadingProfileUserIds.toSet()
    }
    
    private suspend fun validateAndGetAccessibleUrl(url: String, userId: String): String? {
        return try {
            // For now, we'll do a basic validation and let the image loading component handle 404s
            // In the future, we could add actual HTTP HEAD request validation here
            Log.d("UserProfileService", "validateAndGetAccessibleUrl($userId): validating URL: $url")
            
            // Basic URL format validation
            if (url.startsWith("https://firebasestorage.googleapis.com/")) {
                Log.d("UserProfileService", "validateAndGetAccessibleUrl($userId): URL format is valid")
                url
            } else {
                Log.w("UserProfileService", "validateAndGetAccessibleUrl($userId): invalid URL format")
                null
            }
        } catch (e: Exception) {
            Log.e("UserProfileService", "validateAndGetAccessibleUrl($userId): error validating URL", e)
            null
        }
    }
    
    
    /**
     * 사용자 프로필이 캐시되어 있는지 확인
     */
    fun isUserCached(userId: String): Boolean {
        return userProfileCache.containsKey(userId)
    }
    
    /**
     * 여러 사용자의 프로필을 배치로 로딩 (현재 로딩된 메시지에 있는 사용자들만)
     * 이미 캐시된 사용자들은 스킵하여 성능 최적화
     */
    suspend fun loadUserProfiles(userIds: Set<String>) = coroutineScope {
        Log.d("UserProfileService", "loadUserProfiles: checking ${userIds.size} users")
        
        // 캐시되지 않은 사용자들만 필터링
        val uncachedUserIds = userIds.filter { userId -> 
            !isUserCached(userId) && !loadingProfileUserIds.contains(userId)
        }
        
        if (uncachedUserIds.isEmpty()) {
            Log.d("UserProfileService", "loadUserProfiles: all users already cached or loading")
            return@coroutineScope
        }
        
        Log.d("UserProfileService", "loadUserProfiles: loading ${uncachedUserIds.size} uncached users")
        
        // 로딩 상태로 마킹
        uncachedUserIds.forEach { loadingProfileUserIds.add(it) }
        
        try {
            // 병렬로 프로필 로딩
            uncachedUserIds.map { userId ->
                async {
                    try {
                        loadUserProfileAsync(userId)
                    } catch (e: Exception) {
                        Log.e("UserProfileService", "loadUserProfiles: error loading profile for $userId", e)
                        loadingProfileUserIds.remove(userId)
                        null
                    }
                }
            }.awaitAll()
            
            Log.d("UserProfileService", "loadUserProfiles: completed loading ${uncachedUserIds.size} profiles")
        } catch (e: Exception) {
            Log.e("UserProfileService", "loadUserProfiles: batch loading error", e)
            // 에러 발생시 로딩 상태 클리어
            uncachedUserIds.forEach { loadingProfileUserIds.remove(it) }
        }
    }

    // 멘션 기능을 위한 username -> userId 매핑 캐시
    private val usernameToUserIdCache = mutableMapOf<String, String?>()

    /**
     * username으로 userId를 찾는 메서드
     * 멘션 파싱에서 사용되며, 결과를 캐시하여 성능 최적화
     * @param username 찾을 사용자명
     * @return userId (찾지 못하면 null)
     */
    suspend fun getUserIdByUsername(username: String): String? {
        // 캐시에서 먼저 확인
        if (usernameToUserIdCache.containsKey(username)) {
            val cachedUserId = usernameToUserIdCache[username]
            Log.d(
                "UserProfileService",
                "getUserIdByUsername($username): found in cache: $cachedUserId"
            )
            return cachedUserId
        }

        try {
            Log.d(
                "UserProfileService",
                "getUserIdByUsername($username): searching in user database"
            )

            // 현재 캐시된 사용자들에서 username 매칭 시도
            val matchingUser = userProfileCache.values.find { user ->
                user.name.value.equals(username, ignoreCase = true)
            }

            if (matchingUser != null) {
                val userId = matchingUser.id.value
                Log.d(
                    "UserProfileService",
                    "getUserIdByUsername($username): found in profile cache: $userId"
                )

                // 결과 캐시
                usernameToUserIdCache[username] = userId
                return userId
            }

            // 캐시에서 찾지 못한 경우, 향후 실제 데이터베이스 검색 기능 구현 필요
            Log.w(
                "UserProfileService",
                "getUserIdByUsername($username): user not found in current cache. Database search not implemented yet."
            )

            // 찾지 못한 결과도 캐시 (중복 검색 방지)
            usernameToUserIdCache[username] = null
            return null
        } catch (e: Exception) {
            Log.e(
                "UserProfileService",
                "getUserIdByUsername($username): exception during search",
                e
            )

            // 예외 발생 시에도 null을 캐시하여 재시도 방지
            usernameToUserIdCache[username] = null
            return null
        }
    }

    /**
     * username → userId 캐시 클리어
     */
    fun clearUsernameCache() {
        Log.d("UserProfileService", "Clearing username to userId cache")
        usernameToUserIdCache.clear()
    }

    /**
     * 모든 캐시 클리어 (기존 메서드 확장)
     */
    fun clearAllCachesIncludingUsername() {
        clearAllCaches()
        clearUsernameCache()
    }
}

package com.example.core_ui.components.user

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.core_ui.R
import com.example.domain_usecase.provider.user.UserUseCaseProvider
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로필 이미지 업로드 완료 이벤트
 */
data class ProfileImageUpdateEvent(
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 전역 프로필 이미지 업데이트 이벤트 매니저
 * 프로필 이미지 업로드 완료 시 모든 화면에 알림을 전달합니다.
 */
@Singleton
class ProfileImageUpdateEventManager @Inject constructor() {
    
    private val _profileImageUpdateEvents = MutableSharedFlow<ProfileImageUpdateEvent>()
    val profileImageUpdateEvents: SharedFlow<ProfileImageUpdateEvent> = _profileImageUpdateEvents.asSharedFlow()
    
    /**
     * 프로필 이미지 업로드 완료 이벤트 발생
     */
    suspend fun notifyProfileImageUpdated(userId: String) {
        _profileImageUpdateEvents.emit(ProfileImageUpdateEvent(userId))
    }
}

/**
 * UserProfileImage 컴포넌트를 위한 ViewModel
 * 사용자의 updatedAt 필드 변경을 감지하여 프로필 이미지를 자동으로 새로고침합니다.
 */
@HiltViewModel
class UserProfileImageViewModel @Inject constructor(
    private val userUseCaseProvider: UserUseCaseProvider,
    private val imageLoader: ImageLoader
) : ViewModel() {
    
    private val userUseCases = userUseCaseProvider.createForUser()
    
    // ImageLoader를 외부에서 접근할 수 있도록 제공
    fun getImageLoader(): ImageLoader = imageLoader
    
    private val _imageUrl = MutableStateFlow<String?>(null)
    val imageUrl: StateFlow<String?> = _imageUrl.asStateFlow()
    
    // 현재 관찰 중인 사용자 ID와 해당 updatedAt
    private val _currentUserId = MutableStateFlow<String?>(null)
    private val _userUpdatedAt = MutableStateFlow(0L)
    val userUpdatedAt: StateFlow<Long> = _userUpdatedAt.asStateFlow()
    
    /**
     * 특정 사용자의 updatedAt 변경 감지를 시작합니다.
     */
    fun observeUserUpdates(userId: String) {
        if (_currentUserId.value == userId) return // 이미 같은 사용자를 관찰 중이면 리턴
        
        _currentUserId.value = userId
        
        viewModelScope.launch {
            userUseCases.observeUserUpdatedAtUseCase(userId).collect { result ->
                when (result) {
                    is com.example.core_common.result.CustomResult.Success -> {
                        val newTimestamp = result.data
                        val previousTimestamp = _userUpdatedAt.value
                        
                        if (newTimestamp != previousTimestamp) {
                            _userUpdatedAt.value = newTimestamp
                            Log.d("UserProfileImage", "User $userId updatedAt changed: $newTimestamp")
                        }
                    }
                    is com.example.core_common.result.CustomResult.Failure -> {
                        Log.e("UserProfileImage", "Failed to observe user updates for $userId", result.error)
                    }
                    else -> { /* Loading, Initial, Progress states */ }
                }
            }
        }
    }
    
    /**
     * 특정 사용자의 프로필 이미지 캐시를 지웁니다.
     */
    fun clearImageCache(userId: String) {
        viewModelScope.launch {
            try {
                // 현재 로드된 Firebase Storage URL이 있으면 캐시에서 제거
                val currentUrl = _imageUrl.value
                if (currentUrl != null) {
                    val cacheKey = MemoryCache.Key(currentUrl)
                    imageLoader.memoryCache?.remove(cacheKey)
                    imageLoader.diskCache?.remove(currentUrl)
                    Log.d("UserProfileImage", "Cache cleared for Firebase Storage URL: $currentUrl")
                }
                
                Log.d("UserProfileImage", "Cache cleared for user: $userId")
            } catch (e: Exception) {
                Log.e("UserProfileImage", "Failed to clear cache for user: $userId", e)
            }
        }
    }
    
    /**
     * 사용자 업데이트 여부를 확인합니다.
     */
    fun hasUserBeenUpdated(userId: String, currentTimestamp: Long): Boolean {
        return currentTimestamp > 0L && userId == _currentUserId.value
    }
    
    /**
     * Firebase Storage에서 사용자 프로필 이미지 URL을 가져옵니다.
     * 파일 존재 여부를 먼저 확인하여 404 ERROR 로그를 방지합니다.
     */
    fun loadUserProfileImageUrl(userId: String) {
        viewModelScope.launch {
            try {
                val storage = Firebase.storage
                val pathString = "user_profiles/$userId/profile.webp"
                val imageRef = storage.reference.child(pathString)
                
                // 파일 존재 여부를 먼저 확인 (404 ERROR 로그 방지)
                imageRef.metadata.await()
                
                // 파일이 존재하면 URL 요청
                val uri = imageRef.downloadUrl.await()
                _imageUrl.value = uri.toString()
                
                Log.d("UserProfileImage", "Profile image URL loaded for user: $userId")
            } catch (e: Exception) {
                when {
                    e.message?.contains("Object does not exist") == true -> {
                        // 프로필 이미지가 없는 것은 정상적인 상황이므로 DEBUG 레벨로 로깅
                        Log.d("UserProfileImage", "Profile image does not exist for user: $userId, showing default placeholder")
                        _imageUrl.value = null
                    }
                    e.message?.contains("Permission denied") == true -> {
                        Log.w("UserProfileImage", "Permission denied for profile image of user: $userId, showing default placeholder")
                        _imageUrl.value = null
                    }
                    e.message?.contains("StorageException") == true && e.message?.contains("404") == true -> {
                        // Firebase Storage 404 오류도 정상적인 상황 (이미지 없음)
                        Log.d("UserProfileImage", "Profile image not found (404) for user: $userId, showing default placeholder")
                        _imageUrl.value = null
                    }
                    else -> {
                        // 실제 오류인 경우에만 ERROR 레벨로 로깅
                        Log.e("UserProfileImage", "Unexpected error loading profile image for user: $userId", e)
                        _imageUrl.value = null
                    }
                }
            }
        }
    }
}

@Composable
fun UserProfileImage(
    userId: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    forceRefresh: Boolean = false,
    viewModel: UserProfileImageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    val userUpdatedAt by viewModel.userUpdatedAt.collectAsState()
    val firebaseImageUrl by viewModel.imageUrl.collectAsState()
    
    // 이전 updatedAt 값을 기억하여 업데이트 감지
    val previousUpdatedAt = remember { mutableStateOf(0L) }
    val isUpdated = userUpdatedAt > 0L && userUpdatedAt != previousUpdatedAt.value
    
    // 캐시 사용 여부 결정
    val shouldDisableCache = forceRefresh || isUpdated
    
    // userId가 변경되거나 사용자가 업데이트될 때 처리
    LaunchedEffect(userId) {
        if (!userId.isNullOrEmpty()) {
            // 사용자 업데이트 감지 시작
            viewModel.observeUserUpdates(userId)
            // 프로필 이미지 URL 로드
            viewModel.loadUserProfileImageUrl(userId)
        }
    }
    
    // 사용자 updatedAt이 변경될 때 캐시 클리어 및 이미지 재로드
    LaunchedEffect(userUpdatedAt) {
        if (!userId.isNullOrEmpty() && isUpdated) {
            viewModel.clearImageCache(userId)
            viewModel.loadUserProfileImageUrl(userId)
            previousUpdatedAt.value = userUpdatedAt
            Log.d("UserProfileImage", "Profile image refreshed for user: $userId due to updatedAt change")
        }
    }
    
    // Firebase Storage에서 가져온 URL 사용
    val imageUrl = if (userId.isNullOrEmpty()) {
        null
    } else {
        firebaseImageUrl?.let { url ->
            if (shouldDisableCache) {
                // 업데이트된 경우 타임스탬프 쿼리 파라미터 추가
                if (url.contains("?")) {
                    "$url&v=$userUpdatedAt"
                } else {
                    "$url?v=$userUpdatedAt"
                }
            } else {
                url
            }
        }
    }
    
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .placeholder(R.drawable.ic_default_profile_placeholder)
            .error(R.drawable.ic_default_profile_placeholder)
            .crossfade(true)
            // 강제 새로고침이 아닌 경우 캐시 사용
            .memoryCachePolicy(if (shouldDisableCache) CachePolicy.DISABLED else CachePolicy.ENABLED)
            .diskCachePolicy(if (shouldDisableCache) CachePolicy.DISABLED else CachePolicy.ENABLED)
            // 네트워크 캐시 정책 - HTTP Cache-Control 헤더 활용
            .networkCachePolicy(if (shouldDisableCache) CachePolicy.WRITE_ONLY else CachePolicy.ENABLED)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        imageLoader = viewModel.getImageLoader()
    )
}

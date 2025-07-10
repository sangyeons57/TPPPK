package com.example.core_ui.components.user

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import coil.ImageLoader
import coil.memory.MemoryCache
import com.example.core_ui.R
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

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
 * 프로필 이미지 업데이트 이벤트를 자동으로 감지합니다.
 */
@HiltViewModel
class UserProfileImageViewModel @Inject constructor(
    private val profileImageUpdateEventManager: ProfileImageUpdateEventManager
) : ViewModel() {
    
    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger: StateFlow<Long> = _refreshTrigger.asStateFlow()
    
    private val _imageUrl = MutableStateFlow<String?>(null)
    val imageUrl: StateFlow<String?> = _imageUrl.asStateFlow()
    
    init {
        observeProfileImageUpdates()
    }
    
    private fun observeProfileImageUpdates() {
        viewModelScope.launch {
            profileImageUpdateEventManager.profileImageUpdateEvents.collect { event ->
                // 프로필 이미지 업데이트 시 새로고침 트리거
                _refreshTrigger.value = event.timestamp
                Log.d("UserProfileImage", "Profile image updated for user: ${event.userId}")
            }
        }
    }
    
    /**
     * 특정 사용자의 프로필 이미지 캐시를 지웁니다.
     */
    fun clearImageCache(userId: String, imageLoader: ImageLoader) {
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
                
                // URL 재로드 강제 (refreshTrigger를 갱신하여 자동으로 재로드됨)
                _refreshTrigger.value = System.currentTimeMillis()
                
                Log.d("UserProfileImage", "Cache cleared for user: $userId")
            } catch (e: Exception) {
                Log.e("UserProfileImage", "Failed to clear cache for user: $userId", e)
            }
        }
    }
    
    fun getRefreshKey(userId: String?, forceRefresh: Boolean): Long {
        return when {
            forceRefresh -> System.currentTimeMillis()
            userId != null -> _refreshTrigger.value
            else -> 0L
        }
    }
    
    /**
     * Firebase Storage에서 사용자 프로필 이미지 URL을 가져옵니다.
     */
    fun loadUserProfileImageUrl(userId: String) {
        viewModelScope.launch {
            try {
                val storage = Firebase.storage
                val pathString = "user_profiles/$userId/profile.webp"
                val imageRef = storage.reference.child(pathString)
                
                val uri = imageRef.downloadUrl.await()
                _imageUrl.value = uri.toString()
                
                Log.d("UserProfileImage", "Profile image URL loaded for user: $userId")
            } catch (e: Exception) {
                when {
                    e.message?.contains("Object does not exist") == true -> {
                        Log.d("UserProfileImage", "Profile image does not exist for user: $userId, will show default")
                        _imageUrl.value = null
                    }
                    e.message?.contains("Permission denied") == true -> {
                        Log.w("UserProfileImage", "Permission denied for profile image of user: $userId")
                        _imageUrl.value = null
                    }
                    else -> {
                        Log.e("UserProfileImage", "Failed to load profile image URL for user: $userId", e)
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
    val imageLoader = remember { ImageLoader(context) }
    
    val globalRefreshTrigger by viewModel.refreshTrigger.collectAsState()
    val firebaseImageUrl by viewModel.imageUrl.collectAsState()
    
    // 이전 refreshTrigger 값을 기억하여 업데이트 감지
    val previousRefreshTrigger = remember { mutableStateOf(0L) }
    val isUpdated = globalRefreshTrigger > 0L && globalRefreshTrigger != previousRefreshTrigger.value
    
    // forceRefresh가 true이거나 전역 이벤트가 발생했을 때 새로고침
    val refreshKey = remember(userId, forceRefresh, globalRefreshTrigger) { 
        viewModel.getRefreshKey(userId, forceRefresh)
    }
    
    // 캐시 사용 여부 결정
    val shouldDisableCache = forceRefresh || isUpdated
    
    // userId가 변경되거나 refreshKey가 변경될 때 Firebase Storage URL 로드
    LaunchedEffect(userId, refreshKey) {
        if (!userId.isNullOrEmpty()) {
            // 업데이트가 감지된 경우 캐시 클리어
            if (isUpdated) {
                viewModel.clearImageCache(userId, imageLoader)
                previousRefreshTrigger.value = globalRefreshTrigger
            }
            viewModel.loadUserProfileImageUrl(userId)
        }
    }
    
    // Firebase Storage에서 가져온 URL 사용, 캐시 무효화를 위해 refreshKey 추가
    val imageUrl = if (userId.isNullOrEmpty()) {
        null
    } else {
        firebaseImageUrl?.let { url ->
            if (shouldDisableCache) {
                // 업데이트된 경우 타임스탬프 쿼리 파라미터 추가
                if (url.contains("?")) {
                    "$url&v=$globalRefreshTrigger"
                } else {
                    "$url?v=$globalRefreshTrigger"
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
            .memoryCachePolicy(if (shouldDisableCache) CachePolicy.DISABLED else CachePolicy.ENABLED)
            .diskCachePolicy(if (shouldDisableCache) CachePolicy.DISABLED else CachePolicy.ENABLED)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}

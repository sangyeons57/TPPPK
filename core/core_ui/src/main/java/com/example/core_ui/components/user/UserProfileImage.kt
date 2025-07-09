package com.example.core_ui.components.user

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.example.core_ui.R
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
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
    
    init {
        observeProfileImageUpdates()
    }
    
    private fun observeProfileImageUpdates() {
        viewModelScope.launch {
            profileImageUpdateEventManager.profileImageUpdateEvents.collect { event ->
                _refreshTrigger.value = event.timestamp
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
}

@Composable
fun UserProfileImage(
    userId: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    forceRefresh: Boolean = false
) {
    val viewModel: UserProfileImageViewModel = hiltViewModel()
    val globalRefreshTrigger by viewModel.refreshTrigger.collectAsState()
    
    // forceRefresh가 true이거나 전역 이벤트가 발생했을 때 새로고침
    val refreshKey = remember(userId, forceRefresh, globalRefreshTrigger) { 
        viewModel.getRefreshKey(userId, forceRefresh)
    }
    
    // 고정 경로 사용 - 간단하고 직접적
    val imageUrl = if (userId.isNullOrEmpty()) {
        null
    } else {
        val storagePath = "user_profiles/$userId/profile.webp"
        val baseUrl = "https://firebasestorage.googleapis.com/v0/b/${Firebase.storage.reference.bucket}/o/${storagePath.replace("/", "%2F")}"
        if (forceRefresh) {
            "$baseUrl?alt=media&token=${System.currentTimeMillis()}"
        } else {
            "$baseUrl?alt=media"
        }
    }
    
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .placeholder(R.drawable.ic_default_profile_placeholder)
            .error(R.drawable.ic_default_profile_placeholder)
            .crossfade(true)
            .memoryCachePolicy(if (forceRefresh) CachePolicy.DISABLED else CachePolicy.ENABLED)
            .diskCachePolicy(if (forceRefresh) CachePolicy.DISABLED else CachePolicy.ENABLED)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}

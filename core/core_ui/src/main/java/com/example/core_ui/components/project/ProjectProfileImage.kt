package com.example.core_ui.components.project

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
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 프로젝트 이미지 업로드 완료 이벤트
 */
data class ProjectImageUpdateEvent(
    val projectId: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 전역 프로젝트 이미지 업데이트 이벤트 매니저
 * 프로젝트 이미지 업로드 완료 시 모든 화면에 알림을 전달합니다.
 */
@Singleton
class ProjectImageUpdateEventManager @Inject constructor() {
    
    private val _projectImageUpdateEvents = MutableSharedFlow<ProjectImageUpdateEvent>()
    val projectImageUpdateEvents: SharedFlow<ProjectImageUpdateEvent> = _projectImageUpdateEvents.asSharedFlow()
    
    /**
     * 프로젝트 이미지 업로드 완료 이벤트 발생
     */
    suspend fun notifyProjectImageUpdated(projectId: String) {
        _projectImageUpdateEvents.emit(ProjectImageUpdateEvent(projectId))
    }
}

/**
 * ProjectProfileImage 컴포넌트를 위한 ViewModel
 * 프로젝트 이미지 업데이트 이벤트를 자동으로 감지합니다.
 */
@HiltViewModel
class ProjectProfileImageViewModel @Inject constructor(
    private val projectImageUpdateEventManager: ProjectImageUpdateEventManager
) : ViewModel() {
    
    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger: StateFlow<Long> = _refreshTrigger.asStateFlow()
    
    private val _imageUrl = MutableStateFlow<String?>(null)
    val imageUrl: StateFlow<String?> = _imageUrl.asStateFlow()
    
    init {
        observeProjectImageUpdates()
    }
    
    private fun observeProjectImageUpdates() {
        viewModelScope.launch {
            projectImageUpdateEventManager.projectImageUpdateEvents.collect { event ->
                // 모든 프로젝트 이미지 업데이트 시 새로고침 (현재 로직 유지)
                _refreshTrigger.value = event.timestamp
            }
        }
    }
    
    /**
     * 특정 프로젝트의 이미지 캐시를 지웁니다.
     */
    fun clearImageCache(projectId: String, imageLoader: ImageLoader) {
        viewModelScope.launch {
            try {
                // 현재 로드된 Firebase Storage URL이 있으면 캐시에서 제거
                val currentUrl = _imageUrl.value
                if (currentUrl != null) {
                    val cacheKey = MemoryCache.Key(currentUrl)
                    imageLoader.memoryCache?.remove(cacheKey)
                    imageLoader.diskCache?.remove(currentUrl)
                    Log.d("ProjectProfileImage", "Cache cleared for Firebase Storage URL: $currentUrl")
                }
                
                // URL 재로드 강제 (refreshTrigger를 갱신하여 자동으로 재로드됨)
                _refreshTrigger.value = System.currentTimeMillis()
                
                Log.d("ProjectProfileImage", "Cache cleared for project: $projectId")
            } catch (e: Exception) {
                Log.e("ProjectProfileImage", "Failed to clear cache for project: $projectId", e)
            }
        }
    }
    
    fun getRefreshKey(projectId: String?, forceRefresh: Boolean): Long {
        return when {
            forceRefresh -> System.currentTimeMillis()
            projectId != null -> _refreshTrigger.value
            else -> 0L
        }
    }
    
    /**
     * Firebase Storage에서 프로젝트 프로필 이미지 URL을 가져옵니다.
     * 파일 존재 여부를 먼저 확인하여 404 ERROR 로그를 방지합니다.
     */
    fun loadProjectProfileImageUrl(projectId: String) {
        viewModelScope.launch {
            try {
                val storage = Firebase.storage
                val pathString = "project_profiles/$projectId/profile.webp"
                val imageRef = storage.reference.child(pathString)
                
                // 파일 존재 여부를 먼저 확인 (404 ERROR 로그 방지)
                val metadata = imageRef.metadata.await()
                
                // 파일이 존재하면 URL 요청
                val uri = imageRef.downloadUrl.await()
                _imageUrl.value = uri.toString()
                
                Log.d("ProjectProfileImage", "Project image URL loaded for project: $projectId")
            } catch (e: Exception) {
                when {
                    e.message?.contains("Object does not exist") == true -> {
                        // 프로젝트 이미지가 없는 것은 정상적인 상황이므로 DEBUG 레벨로 로깅
                        Log.d("ProjectProfileImage", "Project image does not exist for project: $projectId, showing default placeholder")
                        _imageUrl.value = null
                    }
                    e.message?.contains("Permission denied") == true -> {
                        Log.w("ProjectProfileImage", "Permission denied for project image of project: $projectId, showing default placeholder")
                        _imageUrl.value = null
                    }
                    e.message?.contains("StorageException") == true && e.message?.contains("404") == true -> {
                        // Firebase Storage 404 오류도 정상적인 상황 (이미지 없음)
                        Log.d("ProjectProfileImage", "Project image not found (404) for project: $projectId, showing default placeholder")
                        _imageUrl.value = null
                    }
                    else -> {
                        // 실제 오류인 경우에만 ERROR 레벨로 로깅
                        Log.e("ProjectProfileImage", "Unexpected error loading project image for project: $projectId", e)
                        _imageUrl.value = null
                    }
                }
            }
        }
    }
}

/**
 * 프로젝트 이미지를 표시하는 컴포넌트
 * 고정 경로 방식을 사용: project_profiles/{projectId}/profile.webp
 * 
 * @param projectId 프로젝트 ID (null이면 기본 이미지 표시)
 * @param contentDescription 접근성을 위한 설명
 * @param modifier Compose Modifier
 * @param contentScale 이미지 스케일링 방식
 * @param forceRefresh 강제 새로고침 여부 (캐시 무시)
 */
@Composable
fun ProjectProfileImage(
    projectId: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    forceRefresh: Boolean = false
) {
    val viewModel: ProjectProfileImageViewModel = hiltViewModel()
    val globalRefreshTrigger by viewModel.refreshTrigger.collectAsState()
    val firebaseImageUrl by viewModel.imageUrl.collectAsState()
    
    // forceRefresh가 true이거나 전역 이벤트가 발생했을 때 새로고침
    val refreshKey = remember(projectId, forceRefresh, globalRefreshTrigger) { 
        viewModel.getRefreshKey(projectId, forceRefresh)
    }
    
    // projectId가 변경되거나 refreshKey가 변경될 때 Firebase Storage URL 로드
    LaunchedEffect(projectId, refreshKey) {
        if (!projectId.isNullOrEmpty()) {
            viewModel.loadProjectProfileImageUrl(projectId)
        }
    }
    
    // Firebase Storage에서 가져온 URL 사용, 캐시 무효화를 위해 refreshKey 추가
    val imageUrl = if (projectId.isNullOrEmpty()) {
        null
    } else {
        firebaseImageUrl?.let { url ->
            if (refreshKey > 0) {
                // 이미 있는 쿼리 파라미터에 추가
                if (url.contains("?")) {
                    "$url&v=$refreshKey"
                } else {
                    "$url?v=$refreshKey"
                }
            } else {
                url
            }
        }
    }
    
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .placeholder(R.drawable.ic_default_profile_placeholder)
            .error(R.drawable.ic_default_profile_placeholder)
            .crossfade(true)
            .memoryCachePolicy(if (refreshKey > 0) CachePolicy.DISABLED else CachePolicy.ENABLED)
            .diskCachePolicy(if (refreshKey > 0) CachePolicy.DISABLED else CachePolicy.ENABLED)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
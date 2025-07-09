package com.example.core_ui.components.project

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
    
    init {
        observeProjectImageUpdates()
    }
    
    private fun observeProjectImageUpdates() {
        viewModelScope.launch {
            projectImageUpdateEventManager.projectImageUpdateEvents.collect { event ->
                _refreshTrigger.value = event.timestamp
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
    
    // forceRefresh가 true이거나 전역 이벤트가 발생했을 때 새로고침
    val refreshKey = remember(projectId, forceRefresh, globalRefreshTrigger) { 
        viewModel.getRefreshKey(projectId, forceRefresh)
    }
    
    var imageUrl by remember(projectId, refreshKey) { mutableStateOf<String?>(null) }
    var isLoading by remember(projectId, refreshKey) { mutableStateOf(true) }
    var hasError by remember(projectId, refreshKey) { mutableStateOf(false) }

    // Firebase Storage에서 다운로드 URL 가져오기
    LaunchedEffect(projectId, refreshKey) {
        if (projectId.isNullOrEmpty()) {
            Log.d("ProjectProfileImage", "🖼️ ProjectProfileImage: No projectId provided, using default image")
            isLoading = false
            hasError = false
            imageUrl = null
            return@LaunchedEffect
        }

        try {
            Log.d("ProjectProfileImage", "🖼️ ProjectProfileImage: Loading image for projectId = $projectId")
            
            // 1. Firebase Storage 인스턴스 가져오기
            val storage = Firebase.storage

            // 2. 파일 경로를 가리키는 참조 만들기 (Firebase Functions에서 생성하는 고정 경로)
            val pathString = "project_profiles/$projectId/profile.webp"
            Log.d("ProjectProfileImage", "🖼️ ProjectProfileImage: Storage path = $pathString")
            
            val imageRef = storage.reference.child(pathString)

            // 3. 다운로드 URL 가져오기 (비동기 작업)
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                // 성공 시, uri 변수에 다운로드 URL이 담겨 있습니다.
                val downloadUrl = uri.toString()
                Log.d("ProjectProfileImage", "✅ ProjectProfileImage: Successfully got download URL")
                Log.d("ProjectProfileImage", "✅ ProjectProfileImage: URL = $downloadUrl")
                
                // Cache busting을 위해 timestamp 추가
                val cacheBustingUrl = if (forceRefresh) {
                    "$downloadUrl&v=${System.currentTimeMillis()}"
                } else {
                    downloadUrl
                }
                
                imageUrl = cacheBustingUrl
                isLoading = false
                hasError = false
                
            }.addOnFailureListener { exception ->
                // 실패 시, 에러 처리 (예: 권한 문제, 파일 없음 등)
                Log.e("ProjectProfileImage", "❌ ProjectProfileImage: Failed to get download URL", exception)
                Log.e("ProjectProfileImage", "❌ ProjectProfileImage: Error: ${exception.message}")
                
                imageUrl = null
                isLoading = false
                hasError = true
            }
        } catch (e: Exception) {
            Log.e("ProjectProfileImage", "❌ ProjectProfileImage: Exception during Firebase Storage access", e)
            imageUrl = null
            isLoading = false
            hasError = true
        }
    }

    // 이미지 로딩 로직
    val imageRequest = when {
        isLoading -> {
            Log.d("ProjectProfileImage", "🔄 ProjectProfileImage: Loading...")
            ImageRequest.Builder(LocalContext.current)
                .data(R.drawable.ic_default_profile_placeholder)
                .crossfade(true)
                .build()
        }
        hasError || imageUrl == null -> {
            Log.d("ProjectProfileImage", "🖼️ ProjectProfileImage: Using default image (error or no URL)")
            ImageRequest.Builder(LocalContext.current)
                .data(R.drawable.ic_default_profile_placeholder)
                .crossfade(true)
                .build()
        }
        else -> {
            Log.d("ProjectProfileImage", "🖼️ ProjectProfileImage: Building request for downloaded URL")
            ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .placeholder(R.drawable.ic_default_profile_placeholder)
                .error(R.drawable.ic_default_profile_placeholder)
                .crossfade(true)
                .memoryCachePolicy(if (forceRefresh) CachePolicy.DISABLED else CachePolicy.ENABLED)
                .diskCachePolicy(if (forceRefresh) CachePolicy.DISABLED else CachePolicy.ENABLED)
                .listener(
                    onStart = { 
                        Log.d("ProjectProfileImage", "🖼️ ProjectProfileImage: Started loading image from Firebase URL")
                    },
                    onSuccess = { _, result ->
                        Log.d("ProjectProfileImage", "✅ ProjectProfileImage: Successfully loaded image from Firebase URL")
                        Log.d("ProjectProfileImage", "✅ ProjectProfileImage: Image source: ${result.dataSource}")
                    },
                    onError = { _, error ->
                        Log.e("ProjectProfileImage", "❌ ProjectProfileImage: Failed to load image from Firebase URL")
                        Log.e("ProjectProfileImage", "❌ ProjectProfileImage: Error: ${error.throwable?.message}")
                        error.throwable?.printStackTrace()
                    }
                )
                .build()
        }
    }
    
    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
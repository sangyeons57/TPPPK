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
    
    // 고정 경로 사용 - 간단하고 직접적
    val imageUrl = if (projectId.isNullOrEmpty()) {
        null
    } else {
        val storagePath = "project_profiles/$projectId/profile.webp"
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
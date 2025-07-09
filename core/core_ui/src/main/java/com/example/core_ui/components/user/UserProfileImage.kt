package com.example.core_ui.components.user

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.example.core_ui.R // Ensure this R is correct

@Composable
fun UserProfileImage(
    profileImageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop // Default to Crop, common for profile pics
) {
    // URL 로깅 추가
    println("🖼️ UserProfileImage: Loading image with URL = $profileImageUrl")
    
    val imageRequest = if (profileImageUrl == "DEFAULT_PROFILE_IMAGE_MARKER" || profileImageUrl.isNullOrEmpty()) {
        ImageRequest.Builder(LocalContext.current)
            .data(R.drawable.ic_default_profile_placeholder)
            .crossfade(true)
            .build()
    } else {
        ImageRequest.Builder(LocalContext.current)
            .data(profileImageUrl) // 서버에서 이미 ?v=timestamp가 포함되어 옴
            .placeholder(R.drawable.ic_default_profile_placeholder)
            .error(R.drawable.ic_default_profile_placeholder)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.DISABLED) // 메모리 캐시 비활성화
            .diskCachePolicy(CachePolicy.DISABLED) // 디스크 캐시 비활성화
            .build()
    }
    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}

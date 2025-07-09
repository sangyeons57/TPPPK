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
    val imageRequest = if (profileImageUrl == "DEFAULT_PROFILE_IMAGE_MARKER" || profileImageUrl.isNullOrEmpty()) {
        ImageRequest.Builder(LocalContext.current)
            .data(R.drawable.ic_default_profile_placeholder)
            .crossfade(true)
            .build()
    } else {
        // 캐시 무효화를 위해 timestamp 추가
        val urlWithTimestamp = if (profileImageUrl.contains("?")) {
            "$profileImageUrl&t=${System.currentTimeMillis()}"
        } else {
            "$profileImageUrl?t=${System.currentTimeMillis()}"
        }
        
        ImageRequest.Builder(LocalContext.current)
            .data(urlWithTimestamp)
            .placeholder(R.drawable.ic_default_profile_placeholder)
            .error(R.drawable.ic_default_profile_placeholder)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.DISABLED) // 프로필 이미지 메모리 캐시 비활성화
            .diskCachePolicy(CachePolicy.DISABLED) // 디스크 캐시도 비활성화
            .build()
    }
    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}

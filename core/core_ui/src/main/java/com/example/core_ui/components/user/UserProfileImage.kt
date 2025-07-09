package com.example.core_ui.components.user

import android.util.Log
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
    Log.d("UserProfileImage", "🖼️ UserProfileImage: Loading image with URL = $profileImageUrl")
    
    val imageRequest = if (profileImageUrl == "DEFAULT_PROFILE_IMAGE_MARKER" || profileImageUrl.isNullOrEmpty()) {
        Log.d("UserProfileImage", "🖼️ UserProfileImage: Using default image")
        ImageRequest.Builder(LocalContext.current)
            .data(R.drawable.ic_default_profile_placeholder)
            .crossfade(true)
            .build()
    } else {
        Log.d("UserProfileImage", "🖼️ UserProfileImage: Building request for URL: $profileImageUrl")
        ImageRequest.Builder(LocalContext.current)
            .data(profileImageUrl) // 서버에서 이미 ?v=timestamp가 포함되어 옴
            .placeholder(R.drawable.ic_default_profile_placeholder)
            .error(R.drawable.ic_default_profile_placeholder)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.DISABLED) // 메모리 캐시 비활성화
            .diskCachePolicy(CachePolicy.DISABLED) // 디스크 캐시 비활성화
            .listener(
                onStart = { 
                    Log.d("UserProfileImage", "🖼️ UserProfileImage: Started loading image from $profileImageUrl")
                },
                onSuccess = { _, result ->
                    Log.d("UserProfileImage", "✅ UserProfileImage: Successfully loaded image from $profileImageUrl")
                    Log.d("UserProfileImage", "✅ UserProfileImage: Image source: ${result.dataSource}")
                    Log.d("UserProfileImage", "✅ UserProfileImage: Image drawable: ${result.drawable}")
                },
                onError = { _, error ->
                    Log.e("UserProfileImage", "❌ UserProfileImage: Failed to load image from $profileImageUrl")
                    Log.e("UserProfileImage", "❌ UserProfileImage: Error: ${error.throwable?.message}")
                    Log.e("UserProfileImage", "❌ UserProfileImage: Error type: ${error.throwable?.javaClass?.simpleName}")
                    error.throwable?.printStackTrace()
                }
            )
            .build()
    }
    
    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}

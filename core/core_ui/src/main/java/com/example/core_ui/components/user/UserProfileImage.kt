package com.example.core_ui.components.user

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.example.core_common.constants.FirebaseStorageConstants
import com.example.core_ui.R
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

@Composable
fun UserProfileImage(
    userId: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    forceRefresh: Boolean = false
) {
    val refreshKey = remember(userId, forceRefresh) { 
        if (forceRefresh) System.currentTimeMillis() else 0L 
    }
    
    var imageUrl by remember(userId, refreshKey) { mutableStateOf<String?>(null) }
    var isLoading by remember(userId, refreshKey) { mutableStateOf(true) }
    var hasError by remember(userId, refreshKey) { mutableStateOf(false) }

    // Firebase Storage에서 다운로드 URL 가져오기
    LaunchedEffect(userId, refreshKey) {
        if (userId.isNullOrEmpty()) {
            Log.d("UserProfileImage", "🖼️ UserProfileImage: No userId provided, using default image")
            isLoading = false
            hasError = false
            imageUrl = null
            return@LaunchedEffect
        }

        try {
            Log.d("UserProfileImage", "🖼️ UserProfileImage: Loading image for userId = $userId")
            
            // 1. Firebase Storage 인스턴스 가져오기
            val storage = Firebase.storage

            // 2. 파일 경로를 가리키는 참조 만들기 (Firebase Functions에서 생성하는 고정 경로)
            val pathString = "user_profiles/$userId/profile.webp"
            Log.d("UserProfileImage", "🖼️ UserProfileImage: Storage path = $pathString")
            
            val imageRef = storage.reference.child(pathString)

            // 3. 다운로드 URL 가져오기 (비동기 작업)
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                // 성공 시, uri 변수에 다운로드 URL이 담겨 있습니다.
                val downloadUrl = uri.toString()
                Log.d("UserProfileImage", "✅ UserProfileImage: Successfully got download URL")
                Log.d("UserProfileImage", "✅ UserProfileImage: URL = $downloadUrl")
                
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
                Log.e("UserProfileImage", "❌ UserProfileImage: Failed to get download URL", exception)
                Log.e("UserProfileImage", "❌ UserProfileImage: Error: ${exception.message}")
                
                imageUrl = null
                isLoading = false
                hasError = true
            }
        } catch (e: Exception) {
            Log.e("UserProfileImage", "❌ UserProfileImage: Exception during Firebase Storage access", e)
            imageUrl = null
            isLoading = false
            hasError = true
        }
    }

    // 이미지 로딩 로직
    val imageRequest = when {
        isLoading -> {
            Log.d("UserProfileImage", "🔄 UserProfileImage: Loading...")
            ImageRequest.Builder(LocalContext.current)
                .data(R.drawable.ic_default_profile_placeholder)
                .crossfade(true)
                .build()
        }
        hasError || imageUrl == null -> {
            Log.d("UserProfileImage", "🖼️ UserProfileImage: Using default image (error or no URL)")
            ImageRequest.Builder(LocalContext.current)
                .data(R.drawable.ic_default_profile_placeholder)
                .crossfade(true)
                .build()
        }
        else -> {
            Log.d("UserProfileImage", "🖼️ UserProfileImage: Building request for downloaded URL")
            ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .placeholder(R.drawable.ic_default_profile_placeholder)
                .error(R.drawable.ic_default_profile_placeholder)
                .crossfade(true)
                .memoryCachePolicy(if (forceRefresh) CachePolicy.DISABLED else CachePolicy.ENABLED)
                .diskCachePolicy(if (forceRefresh) CachePolicy.DISABLED else CachePolicy.ENABLED)
                .listener(
                    onStart = { 
                        Log.d("UserProfileImage", "🖼️ UserProfileImage: Started loading image from Firebase URL")
                    },
                    onSuccess = { _, result ->
                        Log.d("UserProfileImage", "✅ UserProfileImage: Successfully loaded image from Firebase URL")
                        Log.d("UserProfileImage", "✅ UserProfileImage: Image source: ${result.dataSource}")
                    },
                    onError = { _, error ->
                        Log.e("UserProfileImage", "❌ UserProfileImage: Failed to load image from Firebase URL")
                        Log.e("UserProfileImage", "❌ UserProfileImage: Error: ${error.throwable?.message}")
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

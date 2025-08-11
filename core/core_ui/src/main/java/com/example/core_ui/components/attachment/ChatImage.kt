package com.example.core_ui.components.attachment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale

@Composable
fun ChatImage(
    model: Any?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    android.util.Log.d("ChatImage", "🖼️ [UI표시] Coil 이미지 로딩 요청: $model")

    val request = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
        .data(model)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .crossfade(true)
        .scale(Scale.FILL)
        .listener(
            onSuccess = { _, _ ->
                android.util.Log.d("ChatImage", "✅ [UI표시] Coil 이미지 로딩 성공: $model")
            },
            onError = { _, result ->
                android.util.Log.e("ChatImage", "❌ [UI표시] Coil 이미지 로딩 실패: $model", result.throwable)
            }
        )
        .build()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}



package com.example.core_ui.components.project

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core_ui.R

@Composable
fun ProjectProfileImage(
    projectImageUrl: String?,
    selectedImageUri: Uri?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    // 선택된 이미지가 있으면 우선 표시, 그 다음 기존 프로젝트 이미지, 마지막으로 기본 이미지
    val imageRequest = when {
        selectedImageUri != null -> {
            // 새로 선택된 이미지 (로컬 Uri)
            ImageRequest.Builder(LocalContext.current)
                .data(selectedImageUri)
                .placeholder(R.drawable.ic_default_profile_placeholder)
                .error(R.drawable.ic_default_profile_placeholder)
                .crossfade(true)
                .build()
        }
        !projectImageUrl.isNullOrEmpty() -> {
            // 기존 프로젝트 이미지 (서버 URL)
            ImageRequest.Builder(LocalContext.current)
                .data(projectImageUrl)
                .placeholder(R.drawable.ic_default_profile_placeholder)
                .error(R.drawable.ic_default_profile_placeholder)
                .crossfade(true)
                .build()
        }
        else -> {
            // 기본 이미지
            ImageRequest.Builder(LocalContext.current)
                .data(R.drawable.ic_default_profile_placeholder)
                .crossfade(true)
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
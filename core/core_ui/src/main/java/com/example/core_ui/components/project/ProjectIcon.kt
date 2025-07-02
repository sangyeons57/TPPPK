package com.example.core_ui.components.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core_ui.R

/**
 * 프로젝트 아이콘을 원형으로 표시하는 컴포넌트입니다.
 * 이미지가 있으면 이미지를 표시하고, 없으면 프로젝트 이름의 첫 글자를 표시합니다.
 *
 * @param projectImageUrl 프로젝트 이미지 URL (null이거나 빈 문자열이면 첫 글자 표시)
 * @param projectName 프로젝트 이름 (첫 글자 표시용)
 * @param size 아이콘 크기
 * @param modifier 추가 Modifier
 * @param contentDescription 접근성을 위한 설명
 */
@Composable
fun ProjectIcon(
    projectImageUrl: String?,
    projectName: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = "프로젝트 아이콘"
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant), // 기본 배경색
        contentAlignment = Alignment.Center
    ) {
        // 이미지가 있으면 이미지 표시, 없으면 첫 글자 표시
        if (!projectImageUrl.isNullOrEmpty()) {
            val imageRequest = ImageRequest.Builder(LocalContext.current)
                .data(projectImageUrl)
                .placeholder(R.drawable.ic_default_profile_placeholder)
                .error(R.drawable.ic_default_profile_placeholder)
                .crossfade(true)
                .build()

            AsyncImage(
                model = imageRequest,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // 이미지가 없는 경우 프로젝트 이름 첫 글자 표시
            Text(
                text = projectName.firstOrNull()?.uppercase() ?: "P",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
} 
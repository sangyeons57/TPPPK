package com.example.core_ui.components.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 프로젝트 아이콘을 원형으로 표시하는 컴포넌트입니다.
 * 고정 경로 기반의 ProjectProfileImage를 사용하여 이미지를 표시하고,
 * 이미지가 없으면 프로젝트 이름의 첫 글자를 표시합니다.
 *
 * @param projectId 프로젝트 ID (고정 경로에서 이미지를 로드하는데 사용)
 * @param projectImageUrl 프로젝트 이미지 URL (Deprecated: 고정 경로 방식으로 대체됨)
 * @param projectName 프로젝트 이름 (첫 글자 표시용)
 * @param size 아이콘 크기
 * @param modifier 추가 Modifier
 * @param contentDescription 접근성을 위한 설명
 */
@Composable
fun ProjectIcon(
    projectId: String? = null,
    @Deprecated("Use fixed path system with projectId instead")
    projectImageUrl: String? = null,
    projectName: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = "프로젝트 아이콘"
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        // 프로젝트 ID가 있으면 고정 경로 방식으로 이미지 표시
        if (!projectId.isNullOrEmpty()) {
            ProjectProfileImage(
                projectId = projectId,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // 프로젝트 ID가 없는 경우 프로젝트 이름 첫 글자 표시
            Text(
                text = projectName.firstOrNull()?.uppercase() ?: "P",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
} 
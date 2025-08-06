package com.example.core_ui.components.attachment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * 첨부파일을 렌더링하는 통합 컴포넌트
 * 
 * Map<String, Any?>의 kind에 따라 다른 UI를 보여줍니다:
 * - image: 이미지 표시 (클릭 시 원본 보기)
 * - video: 비디오 썸네일과 재생 버튼 (클릭 시 재생)  
 * - file: 파일 아이콘과 정보 (클릭 시 다운로드/열기)
 */
@Composable
fun AttachmentRenderer(
    attachment: Map<String, Any?>,
    modifier: Modifier = Modifier,
    onAttachmentClick: ((Map<String, Any?>) -> Unit)? = null
) {
    val kind = attachment["kind"] as? String ?: "file"
    
    when (kind) {
        "image" -> {
            ImageAttachment(
                attachment = attachment,
                modifier = modifier,
                onClick = onAttachmentClick
            )
        }
        
        "video" -> {
            VideoAttachment(
                attachment = attachment,
                modifier = modifier,
                onClick = onAttachmentClick
            )
        }
        
        else -> {
            // 일반 파일이거나 알 수 없는 타입은 파일로 처리
            FileAttachment(
                attachment = attachment,
                modifier = modifier,
                onClick = onAttachmentClick
            )
        }
    }
}

/**
 * 이미지 첨부파일 렌더링
 */
@Composable
fun ImageAttachment(
    attachment: Map<String, Any?>,
    modifier: Modifier = Modifier,
    onClick: ((Map<String, Any?>) -> Unit)? = null
) {
    val context = LocalContext.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick(attachment) }
                } else Modifier
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 이미지 표시
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(attachment["url"] as? String ?: "")
                    .crossfade(true)
                    .build(),
                contentDescription = getDisplayName(attachment),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                contentScale = ContentScale.Crop
            )
            
            // 이미지 정보 (선택적)
            val filename = attachment["filename"] as? String
            val size = (attachment["size"] as? Number)?.toLong()
            if (filename != null || size != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getDisplayName(attachment),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    
                    getReadableSize(size)?.let { sizeText ->
                        Text(
                            text = sizeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 비디오 첨부파일 렌더링
 */
@Composable
fun VideoAttachment(
    attachment: Map<String, Any?>,
    modifier: Modifier = Modifier,
    onClick: ((Map<String, Any?>) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick(attachment) }
                } else Modifier
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // 비디오 썸네일 (실제 구현에서는 비디오 첫 프레임 추출 필요)
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = "비디오",
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            
            // 재생 버튼
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "재생",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // 비디오 정보
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getDisplayName(attachment),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                
                (attachment["duration_ms"] as? Number)?.toLong()?.let { duration ->
                    val minutes = duration / 60000
                    val seconds = (duration % 60000) / 1000
                    Text(
                        text = String.format("%d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

/**
 * 일반 파일 첨부파일 렌더링
 */
@Composable
fun FileAttachment(
    attachment: Map<String, Any?>,
    modifier: Modifier = Modifier,
    onClick: ((Map<String, Any?>) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick(attachment) }
                } else Modifier
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 파일 아이콘
            Card(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getFileIcon(attachment["mime"] as? String ?: "application/octet-stream"),
                        contentDescription = "파일",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // 파일 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = getDisplayName(attachment),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // MIME 타입
                    Text(
                        text = getFileTypeDisplay(attachment["mime"] as? String ?: "application/octet-stream"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    
                    // 파일 크기
                    val size = (attachment["size"] as? Number)?.toLong()
                    getReadableSize(size)?.let { sizeText ->
                        Text(
                            text = "• $sizeText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            
            // 다운로드 아이콘
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "다운로드",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * MIME 타입에 따른 파일 아이콘 반환
 */
private fun getFileIcon(mimeType: String): ImageVector {
    return when {
        mimeType.startsWith("text/") -> Icons.Default.TextSnippet
        mimeType.startsWith("application/pdf") -> Icons.Default.PictureAsPdf
        mimeType.startsWith("application/zip") || mimeType.contains("archive") -> Icons.Default.Archive
        mimeType.startsWith("application/") && mimeType.contains("document") -> Icons.Default.Description
        mimeType.startsWith("application/") && mimeType.contains("sheet") -> Icons.Default.TableChart
        mimeType.startsWith("application/") && mimeType.contains("presentation") -> Icons.Default.Slideshow
        else -> Icons.Default.InsertDriveFile
    }
}

/**
 * MIME 타입의 사용자 친화적 표시명 반환
 */
private fun getFileTypeDisplay(mimeType: String): String {
    return when {
        mimeType.startsWith("application/pdf") -> "PDF"
        mimeType.startsWith("text/") -> "텍스트"
        mimeType.startsWith("application/zip") -> "압축파일"
        mimeType.contains("document") -> "문서"
        mimeType.contains("sheet") -> "스프레드시트"
        mimeType.contains("presentation") -> "프레젠테이션"
        mimeType.startsWith("audio/") -> "오디오"
        else -> mimeType.substringAfterLast("/").uppercase()
    }
}

/**
 * 표시용 파일명 반환 (filename이 없으면 URL에서 추출)
 */
private fun getDisplayName(attachment: Map<String, Any?>): String {
    val filename = attachment["filename"] as? String
    val url = attachment["url"] as? String ?: ""
    return filename ?: url.substringAfterLast('/').ifBlank { "첨부파일" }
}

/**
 * 파일 크기를 사람이 읽을 수 있는 형태로 변환
 */
private fun getReadableSize(bytes: Long?): String? {
    return bytes?.let { 
        when {
            it < 1024 -> "${it}B"
            it < 1024 * 1024 -> "${it / 1024}KB"
            it < 1024 * 1024 * 1024 -> "${it / (1024 * 1024)}MB"
            else -> "${it / (1024 * 1024 * 1024)}GB"
        }
    }
}
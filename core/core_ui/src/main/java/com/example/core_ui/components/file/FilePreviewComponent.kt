package com.example.core_ui.components.file

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.model.base.MessageAttachment
import com.example.domain.model.enum.MessageAttachmentType
import com.example.domain.model.enum.MessageAttachmentUploadStatus

/**
 * 파일 첨부파일 미리보기 컴포넌트
 */
@Composable
fun MessageAttachmentPreview(
    attachment: MessageAttachment,
    onAttachmentClick: (MessageAttachment) -> Unit = {},
    onDownloadClick: ((MessageAttachment) -> Unit)? = null,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 280.dp,
    showProgress: Boolean = true
) {
    when (attachment.attachmentType) {
        MessageAttachmentType.IMAGE -> {
            ImageAttachmentPreview(
                attachment = attachment,
                onImageClick = onAttachmentClick,
                modifier = modifier,
                maxWidth = maxWidth,
                showProgress = showProgress
            )
        }
        MessageAttachmentType.VIDEO -> {
            VideoAttachmentPreview(
                attachment = attachment,
                onVideoClick = onAttachmentClick,
                onDownloadClick = onDownloadClick,
                modifier = modifier,
                maxWidth = maxWidth,
                showProgress = showProgress
            )
        }
        MessageAttachmentType.AUDIO -> {
            AudioAttachmentPreview(
                attachment = attachment,
                onAudioClick = onAttachmentClick,
                onDownloadClick = onDownloadClick,
                modifier = modifier,
                maxWidth = maxWidth,
                showProgress = showProgress
            )
        }
        MessageAttachmentType.FILE -> {
            FileAttachmentPreview(
                attachment = attachment,
                onFileClick = onAttachmentClick,
                onDownloadClick = onDownloadClick,
                modifier = modifier,
                maxWidth = maxWidth,
                showProgress = showProgress
            )
        }
        MessageAttachmentType.LINK -> {
            LinkAttachmentPreview(
                attachment = attachment,
                onLinkClick = onAttachmentClick,
                modifier = modifier,
                maxWidth = maxWidth
            )
        }
        MessageAttachmentType.UNKNOWN -> {
            UnknownAttachmentPreview(
                attachment = attachment,
                onFileClick = onAttachmentClick,
                modifier = modifier,
                maxWidth = maxWidth
            )
        }
    }
}

/**
 * 이미지 첨부파일 미리보기
 */
@Composable
fun ImageAttachmentPreview(
    attachment: MessageAttachment,
    onImageClick: (MessageAttachment) -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 280.dp,
    showProgress: Boolean = true
) {
    Card(
        modifier = modifier
            .widthIn(max = maxWidth)
            .clickable { onImageClick(attachment) }
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(attachment.thumbnailUrl?.value ?: attachment.attachmentUrl.value)
                    .crossfade(true)
                    .build(),
                contentDescription = attachment.fileName?.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            // 업로드 진행률 오버레이
            if (showProgress && attachment.uploadStatus != MessageAttachmentUploadStatus.COMPLETED) {
                UploadOverlay(
                    status = attachment.uploadStatus,
                    progress = attachment.uploadProgress.value,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * 비디오 첨부파일 미리보기
 */
@Composable
fun VideoAttachmentPreview(
    attachment: MessageAttachment,
    onVideoClick: (MessageAttachment) -> Unit,
    onDownloadClick: ((MessageAttachment) -> Unit)?,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 280.dp,
    showProgress: Boolean = true
) {
    Card(
        modifier = modifier.widthIn(max = maxWidth)
    ) {
        Box {
            // 비디오 썸네일 또는 기본 배경
            val thumbnailUrl = attachment.thumbnailUrl
            if (thumbnailUrl != null && thumbnailUrl.value.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnailUrl.value)
                        .crossfade(true)
                        .build(),
                    contentDescription = attachment.fileName?.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 재생 버튼 오버레이
            if (attachment.uploadStatus == MessageAttachmentUploadStatus.COMPLETED) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onVideoClick(attachment) },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "재생",
                            modifier = Modifier.padding(12.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            // 업로드 진행률 오버레이
            if (showProgress && attachment.uploadStatus != MessageAttachmentUploadStatus.COMPLETED) {
                UploadOverlay(
                    status = attachment.uploadStatus,
                    progress = attachment.uploadProgress.value,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 파일 정보
        FileInfoRow(
            fileName = attachment.fileName?.value ?: "비디오 파일",
            fileSize = attachment.fileSize?.value,
            onDownloadClick = onDownloadClick?.let { { it(attachment) } }
        )
    }
}

/**
 * 오디오 첨부파일 미리보기
 */
@Composable
fun AudioAttachmentPreview(
    attachment: MessageAttachment,
    onAudioClick: (MessageAttachment) -> Unit,
    onDownloadClick: ((MessageAttachment) -> Unit)?,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 280.dp,
    showProgress: Boolean = true
) {
    Card(
        modifier = modifier
            .widthIn(max = maxWidth)
            .clickable { onAudioClick(attachment) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 오디오 아이콘
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.AudioFile,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 파일 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = attachment.fileName?.value ?: "오디오 파일",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                attachment.fileSize?.value?.let { size ->
                    Text(
                        text = formatFileSize(size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 업로드 진행률
                if (showProgress && attachment.uploadStatus != MessageAttachmentUploadStatus.COMPLETED) {
                    Spacer(modifier = Modifier.height(4.dp))
                    UploadProgressBar(
                        status = attachment.uploadStatus,
                        progress = attachment.uploadProgress.value
                    )
                }
            }

            // 다운로드/재생 버튼
            if (attachment.uploadStatus == MessageAttachmentUploadStatus.COMPLETED) {
                onDownloadClick?.let { downloadClick ->
                    IconButton(onClick = { downloadClick(attachment) }) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "다운로드"
                        )
                    }
                }
            }
        }
    }
}

/**
 * 일반 파일 첨부파일 미리보기
 */
@Composable
fun FileAttachmentPreview(
    attachment: MessageAttachment,
    onFileClick: (MessageAttachment) -> Unit,
    onDownloadClick: ((MessageAttachment) -> Unit)?,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 280.dp,
    showProgress: Boolean = true
) {
    Card(
        modifier = modifier
            .widthIn(max = maxWidth)
            .clickable { onFileClick(attachment) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 파일 아이콘
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    imageVector = getFileTypeIcon(attachment.fileName?.value),
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 파일 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = attachment.fileName?.value ?: "파일",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                attachment.fileSize?.value?.let { size ->
                    Text(
                        text = formatFileSize(size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 업로드 진행률
                if (showProgress && attachment.uploadStatus != MessageAttachmentUploadStatus.COMPLETED) {
                    Spacer(modifier = Modifier.height(4.dp))
                    UploadProgressBar(
                        status = attachment.uploadStatus,
                        progress = attachment.uploadProgress.value
                    )
                }
            }

            // 다운로드 버튼
            if (attachment.uploadStatus == MessageAttachmentUploadStatus.COMPLETED) {
                onDownloadClick?.let { downloadClick ->
                    IconButton(onClick = { downloadClick(attachment) }) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "다운로드"
                        )
                    }
                }
            }
        }
    }
}

/**
 * 링크 첨부파일 미리보기
 */
@Composable
fun LinkAttachmentPreview(
    attachment: MessageAttachment,
    onLinkClick: (MessageAttachment) -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 280.dp
) {
    Card(
        modifier = modifier
            .widthIn(max = maxWidth)
            .clickable { onLinkClick(attachment) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = attachment.attachmentUrl.value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 알 수 없는 파일 타입 미리보기
 */
@Composable
fun UnknownAttachmentPreview(
    attachment: MessageAttachment,
    onFileClick: (MessageAttachment) -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 280.dp
) {
    Card(
        modifier = modifier
            .widthIn(max = maxWidth)
            .clickable { onFileClick(attachment) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.HelpOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = attachment.fileName?.value ?: "알 수 없는 파일",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 업로드 상태 오버레이
 */
@Composable
private fun UploadOverlay(
    status: MessageAttachmentUploadStatus,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            MessageAttachmentUploadStatus.UPLOADING -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(40.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            MessageAttachmentUploadStatus.PENDING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = Color.White
                )
            }
            MessageAttachmentUploadStatus.FAILED -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "업로드 실패",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "업로드 실패",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            MessageAttachmentUploadStatus.COMPLETED -> {
                // 완료 상태에서는 오버레이 표시 안함
            }
        }
    }
}

/**
 * 업로드 진행률 바
 */
@Composable
private fun UploadProgressBar(
    status: MessageAttachmentUploadStatus,
    progress: Float,
    modifier: Modifier = Modifier
) {
    when (status) {
        MessageAttachmentUploadStatus.UPLOADING -> {
            LinearProgressIndicator(
                progress = { progress },
                modifier = modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }
        MessageAttachmentUploadStatus.PENDING -> {
            LinearProgressIndicator(
                modifier = modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }
        else -> {
            // 완료 또는 실패 상태에서는 진행률 바 표시 안함
        }
    }
}

/**
 * 파일 정보 행
 */
@Composable
private fun FileInfoRow(
    fileName: String,
    fileSize: Long?,
    onDownloadClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            fileSize?.let { size ->
                Text(
                    text = formatFileSize(size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        onDownloadClick?.let { downloadClick ->
            IconButton(onClick = downloadClick) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "다운로드"
                )
            }
        }
    }
}

/**
 * 파일 타입에 따른 아이콘 반환
 */
private fun getFileTypeIcon(fileName: String?): ImageVector {
    if (fileName == null) return Icons.Default.InsertDriveFile
    
    val extension = fileName.substringAfterLast('.', "").lowercase()
    
    return when (extension) {
        "pdf" -> Icons.Default.PictureAsPdf
        "doc", "docx" -> Icons.Default.Description
        "xls", "xlsx" -> Icons.Default.TableChart
        "ppt", "pptx" -> Icons.Default.Slideshow
        "txt" -> Icons.Default.TextSnippet
        "zip", "rar", "7z" -> Icons.Default.Archive
        else -> Icons.Default.InsertDriveFile
    }
}

/**
 * 파일 크기를 사람이 읽기 쉬운 형태로 포맷
 */
private fun formatFileSize(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    
    return "%.1f %s".format(size, units[unitIndex])
}
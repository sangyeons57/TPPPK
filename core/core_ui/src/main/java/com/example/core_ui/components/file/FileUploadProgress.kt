package com.example.core_ui.components.file

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.enum.MessageAttachmentUploadStatus

/**
 * 파일 업로드 상태 데이터
 */
data class FileUploadState(
    val fileName: String,
    val status: MessageAttachmentUploadStatus,
    val progress: Float = 0f,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val errorMessage: String? = null
)

/**
 * 개별 파일 업로드 진행률 표시 컴포넌트
 */
@Composable
fun FileUploadProgressCard(
    uploadState: FileUploadState,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (uploadState.status) {
                MessageAttachmentUploadStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                MessageAttachmentUploadStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 상단 행: 파일명 + 상태 아이콘 + 취소 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 파일명
                Text(
                    text = uploadState.fileName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 상태 아이콘
                    when (uploadState.status) {
                        MessageAttachmentUploadStatus.PENDING -> {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = "대기 중",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        MessageAttachmentUploadStatus.UPLOADING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        MessageAttachmentUploadStatus.COMPLETED -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "완료",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        MessageAttachmentUploadStatus.FAILED -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "실패",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 취소 버튼 (업로드 중이거나 대기 중일 때만 표시)
                    if (onCancel != null && (uploadState.status == MessageAttachmentUploadStatus.UPLOADING || uploadState.status == MessageAttachmentUploadStatus.PENDING)) {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "취소",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 진행률 바 및 상태 텍스트
            when (uploadState.status) {
                MessageAttachmentUploadStatus.UPLOADING -> {
                    LinearProgressIndicator(
                        progress = { uploadState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "업로드 중...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(uploadState.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                MessageAttachmentUploadStatus.COMPLETED -> {
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "업로드 완료",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                MessageAttachmentUploadStatus.FAILED -> {
                    uploadState.errorMessage?.let { error ->
                        Text(
                            text = "실패: $error",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                MessageAttachmentUploadStatus.PENDING -> {
                    Text(
                        text = "업로드 대기 중...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 다중 파일 업로드 진행률 표시 컴포넌트
 */
@Composable
fun MultipleFileUploadProgress(
    uploadStates: List<FileUploadState>,
    onCancelUpload: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (uploadStates.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "파일 업로드 (${uploadStates.count { it.status == MessageAttachmentUploadStatus.COMPLETED }}/${uploadStates.size})",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        uploadStates.forEachIndexed { index, uploadState ->
            FileUploadProgressCard(
                uploadState = uploadState,
                onCancel = onCancelUpload?.let { { it(index) } }
            )
        }
    }
}

/**
 * 간단한 전체 진행률 표시 컴포넌트 (여러 파일의 전체 진행률)
 */
@Composable
fun OverallUploadProgress(
    uploadStates: List<FileUploadState>,
    modifier: Modifier = Modifier
) {
    if (uploadStates.isEmpty()) return

    val completedCount = uploadStates.count { it.status == MessageAttachmentUploadStatus.COMPLETED }
    val totalCount = uploadStates.size
    val overallProgress = completedCount.toFloat() / totalCount.toFloat()

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "파일 업로드",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "$completedCount / $totalCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { overallProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}
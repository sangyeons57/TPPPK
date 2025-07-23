package com.example.core_ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core_ui.R
import com.example.core_ui.theme.TeamnovaTheme
import com.example.domain.model.enum.MessageAttachmentUploadStatus
import com.example.domain.model.vo.messageattachment.AttachmentUploadState
import com.example.domain.model.vo.messageattachment.MessageAttachmentUploadProgress
import kotlin.math.cos
import kotlin.math.sin

/**
 * 접근성을 고려한 파일 업로드 진행률 표시 컴포넌트
 */
@Composable
fun AccessibleFileUploadProgress(
    uploadState: AttachmentUploadState,
    fileName: String,
    modifier: Modifier = Modifier,
    onCancelClick: (() -> Unit)? = null,
    onRetryClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val progressPercentage = uploadState.progressPercentage
    
    // 접근성을 위한 상태 설명
    val statusDescription = when (uploadState.status) {
        MessageAttachmentUploadStatus.PENDING -> context.getString(R.string.upload_status_pending)
        MessageAttachmentUploadStatus.UPLOADING -> context.getString(R.string.upload_status_uploading, progressPercentage)
        MessageAttachmentUploadStatus.COMPLETED -> context.getString(R.string.upload_status_completed)
        MessageAttachmentUploadStatus.FAILED -> context.getString(R.string.upload_status_failed, uploadState.errorMessage ?: "")
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$fileName $statusDescription"
                if (uploadState.status == MessageAttachmentUploadStatus.UPLOADING) {
                    stateDescription = context.getString(R.string.upload_progress_description, progressPercentage)
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (uploadState.status) {
                MessageAttachmentUploadStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                MessageAttachmentUploadStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 파일명과 상태 아이콘
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 파일명
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = when (uploadState.status) {
                        MessageAttachmentUploadStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
                        MessageAttachmentUploadStatus.COMPLETED -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f)
                )
                
                // 상태 아이콘
                when (uploadState.status) {
                    MessageAttachmentUploadStatus.COMPLETED -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.upload_completed),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    MessageAttachmentUploadStatus.FAILED -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = stringResource(R.string.upload_failed),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> {
                        // 진행 중이거나 대기 중일 때는 아이콘 없음
                    }
                }
            }
            
            // 진행률 표시
            when (uploadState.status) {
                MessageAttachmentUploadStatus.UPLOADING -> {
                    AccessibleProgressIndicator(
                        progress = uploadState.progress.value,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        text = stringResource(R.string.upload_progress_text, progressPercentage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                MessageAttachmentUploadStatus.PENDING -> {
                    AccessibleIndeterminateProgress(
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        text = stringResource(R.string.upload_preparing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                MessageAttachmentUploadStatus.FAILED -> {
                    uploadState.errorMessage?.let { errorMessage ->
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                MessageAttachmentUploadStatus.COMPLETED -> {
                    Text(
                        text = stringResource(R.string.upload_completed_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // 액션 버튼들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                when (uploadState.status) {
                    MessageAttachmentUploadStatus.UPLOADING, 
                    MessageAttachmentUploadStatus.PENDING -> {
                        onCancelClick?.let { cancelClick ->
                            OutlinedButton(
                                onClick = cancelClick,
                                modifier = Modifier.semantics {
                                    contentDescription = context.getString(R.string.cancel_upload_description)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    }
                    
                    MessageAttachmentUploadStatus.FAILED -> {
                        onRetryClick?.let { retryClick ->
                            Button(
                                onClick = retryClick,
                                modifier = Modifier.semantics {
                                    contentDescription = context.getString(R.string.retry_upload_description)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                    
                    MessageAttachmentUploadStatus.COMPLETED -> {
                        // 완료 상태에서는 액션 버튼 없음
                    }
                }
            }
        }
    }
}

/**
 * 접근성을 고려한 진행률 표시 바
 */
@Composable
fun AccessibleProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "progress_animation"
    )
    
    LinearProgressIndicator(
        progress = animatedProgress,
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(animatedProgress, 0f..1f)
            },
        color = color,
        trackColor = backgroundColor
    )
}

/**
 * 접근성을 고려한 무한 진행률 표시
 */
@Composable
fun AccessibleIndeterminateProgress(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    LinearProgressIndicator(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .semantics {
                stateDescription = stringResource(R.string.loading_in_progress)
            },
        color = color
    )
}

/**
 * 원형 진행률 표시 (작은 공간용)
 */
@Composable
fun AccessibleCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Float = 48f,
    strokeWidth: Float = 4f,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    showPercentage: Boolean = true
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "circular_progress_animation"
    )
    
    Box(
        modifier = modifier
            .size(size.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(animatedProgress, 0f..1f)
                contentDescription = "원형 진행률 표시"
            },
        contentAlignment = Alignment.Center
    ) {
        // 배경 원
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.width - strokeWidth) / 2
            
            // 배경 원
            drawCircle(
                color = backgroundColor,
                radius = radius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth.dp.toPx())
            )
            
            // 진행률 호
            if (animatedProgress > 0) {
                val sweepAngle = 360f * animatedProgress
                drawArc(
                    color = color,
                    startAngle = -90f, // 12시 방향부터 시작
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidth.dp.toPx(),
                        cap = StrokeCap.Round
                    ),
                    topLeft = Offset(
                        center.x - radius,
                        center.y - radius
                    ),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
            }
        }
        
        // 퍼센트 표시
        if (showPercentage) {
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (size * 0.2f).sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 파일 업로드 상태 칩 (간단한 상태 표시용)
 */
@Composable
fun FileUploadStatusChip(
    uploadState: AttachmentUploadState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    val (chipColor, textColor, iconVector, statusText) = when (uploadState.status) {
        MessageAttachmentUploadStatus.PENDING -> {
            Tuple4(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant,
                Icons.Default.Schedule,
                context.getString(R.string.status_pending)
            )
        }
        MessageAttachmentUploadStatus.UPLOADING -> {
            Tuple4(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer,
                Icons.Default.CloudUpload,
                context.getString(R.string.status_uploading, uploadState.progressPercentage)
            )
        }
        MessageAttachmentUploadStatus.COMPLETED -> {
            Tuple4(
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer,
                Icons.Default.CloudDone,
                context.getString(R.string.status_completed)
            )
        }
        MessageAttachmentUploadStatus.FAILED -> {
            Tuple4(
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer,
                Icons.Default.CloudOff,
                context.getString(R.string.status_failed)
            )
        }
    }
    
    Surface(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        onClickLabel = context.getString(R.string.view_upload_details)
                    ) { onClick() }
                } else Modifier
            )
            .semantics {
                contentDescription = statusText
                if (uploadState.status == MessageAttachmentUploadStatus.UPLOADING) {
                    stateDescription = context.getString(R.string.upload_progress_description, uploadState.progressPercentage)
                }
            },
        shape = CircleShape,
        color = chipColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = textColor
            )
        }
    }
}

// 헬퍼 데이터 클래스
private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

// 프리뷰들
@Preview(showBackground = true)
@Composable
private fun AccessibleFileUploadProgressPreview() {
    TeamnovaTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 업로드 중
            AccessibleFileUploadProgress(
                uploadState = AttachmentUploadState(
                    status = MessageAttachmentUploadStatus.UPLOADING,
                    progress = MessageAttachmentUploadProgress(0.65f)
                ),
                fileName = "example_photo.jpg",
                onCancelClick = { }
            )
            
            // 완료됨
            AccessibleFileUploadProgress(
                uploadState = AttachmentUploadState(
                    status = MessageAttachmentUploadStatus.COMPLETED,
                    progress = MessageAttachmentUploadProgress.complete()
                ),
                fileName = "document.pdf"
            )
            
            // 실패
            AccessibleFileUploadProgress(
                uploadState = AttachmentUploadState.failed("네트워크 오류가 발생했습니다"),
                fileName = "video.mp4",
                onRetryClick = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AccessibleCircularProgressPreview() {
    TeamnovaTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AccessibleCircularProgress(progress = 0.0f)
            AccessibleCircularProgress(progress = 0.25f)
            AccessibleCircularProgress(progress = 0.65f)
            AccessibleCircularProgress(progress = 1.0f)
        }
    }
}
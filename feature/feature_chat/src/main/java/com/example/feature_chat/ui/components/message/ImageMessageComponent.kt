package com.example.feature_chat.ui.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.core_ui.components.attachment.ChatImage
import com.example.feature_chat.model.ChatMessageUiModel

/**
 * 이미지가 포함된 메시지를 표시하는 컴포넌트
 */
@Composable
fun ImageMessageComponent(
    message: ChatMessageUiModel,
    onImageClick: (String, List<String>, Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    LocalContext.current
    val imageUrls = message.imageUrls

    android.util.Log.d(
        "ImageMessageComponent",
        "🖼️ [UI진입] imageUrls.size=${imageUrls.size}, hasImages=${message.hasImages}, payload=${message.payload}"
    )

    // imageUrls가 비어있을 때 fallback으로 payload에서 직접 추출 시도
    val finalImageUrls = if (imageUrls.isEmpty() && message.hasImages) {
        android.util.Log.d(
            "ImageMessageComponent",
            "🖼️ [Fallback] imageUrls가 비어있어서 payload에서 직접 추출 시도"
        )

        try {
            val messagePayload = com.example.domain.vo.message.MessagePayload(message.payload)
            val attachments = messagePayload.getAttachments()
            val fallbackUrls = attachments
                .filter { attachment ->
                    val kind =
                        attachment[com.example.domain.vo.message.MessagePayload.KEY_KIND] as? String
                    kind == "image"
                }
                .mapNotNull { attachment ->
                    attachment[com.example.domain.vo.message.MessagePayload.KEY_URL] as? String
                }
            android.util.Log.d(
                "ImageMessageComponent",
                "🖼️ [Fallback] payload에서 ${fallbackUrls.size}개 URL 추출: $fallbackUrls"
            )
            fallbackUrls
        } catch (e: Exception) {
            android.util.Log.e(
                "ImageMessageComponent",
                "🖼️ [Fallback] payload 파싱 실패: ${e.message}",
                e
            )
            emptyList()
        }
    } else {
        imageUrls
    }

    if (finalImageUrls.isEmpty()) {
        android.util.Log.d("ImageMessageComponent", "🖼️ [UI중단] 최종적으로 표시할 이미지 URL이 없음")
        return
    }

    android.util.Log.d(
        "ImageMessageComponent",
        "🖼️ [UI표시] 이미지 메시지 컴포넌트 렌더링: ${finalImageUrls.size}개 이미지"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 텍스트 내용이 있으면 표시
        if (message.message.isNotBlank()) {
            Text(
                text = message.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // 이미지 표시
        when (finalImageUrls.size) {
            1 -> {
                // 단일 이미지 - 큰 크기로 표시
                SingleImageView(
                    imageUrl = finalImageUrls.first(),
                    onClick = { onImageClick(finalImageUrls.first(), finalImageUrls, 0) },
                    isLoading = message.isSending
                )
            }

            else -> {
                // 다중 이미지 - 그리드 또는 가로 스크롤로 표시
                MultipleImagesView(
                    imageUrls = finalImageUrls,
                    onImageClick = onImageClick,
                    isLoading = message.isSending
                )
            }
        }

        // 전송 상태 표시
        if (message.isSending) {
            // 업로드 진행률 표시 로직 추가
            val uploadProgress = message.getUploadProgress()
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uploadProgress < 1f) {
                    // 진행률 표시
                    CircularProgressIndicator(
                        progress = uploadProgress,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "업로드 중... ${(uploadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // 업로드 완료, 서버 전송 중
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "전송 중...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (message.sendFailed) {
            Text(
                text = "이미지 전송 실패",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SingleImageView(
    imageUrl: String,
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        android.util.Log.d("SingleImageView", "🖼️ [UI표시] 단일 이미지 뷰 렌더링: $imageUrl")
        ChatImage(
            model = imageUrl,
            contentDescription = "이미지",
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun MultipleImagesView(
    imageUrls: List<String>,
    onImageClick: (String, List<String>, Int) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    // 모든 이미지를 세로로 순서대로 배치
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        imageUrls.forEachIndexed { index, imageUrl ->
            android.util.Log.d(
                "MultipleImagesView",
                "🖼️ [UI표시] 이미지 ${index + 1}/${imageUrls.size}: $imageUrl"
            )

            SingleImageView(
                imageUrl = imageUrl,
                onClick = { onImageClick(imageUrl, imageUrls, index) },
                isLoading = isLoading,
                modifier = Modifier.heightIn(max = 200.dp) // 각 이미지 최대 높이 200dp로 제한
            )
        }
    }
}


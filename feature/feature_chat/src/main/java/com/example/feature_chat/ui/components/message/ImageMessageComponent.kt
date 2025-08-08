package com.example.feature_chat.ui.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    val context = LocalContext.current
    val imageUrls = message.imageUrls

    if (imageUrls.isEmpty()) return

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
        when (imageUrls.size) {
            1 -> {
                // 단일 이미지 - 큰 크기로 표시
                SingleImageView(
                    imageUrl = imageUrls.first(),
                    onClick = { onImageClick(imageUrls.first(), imageUrls, 0) },
                    isLoading = message.isSending
                )
            }

            else -> {
                // 다중 이미지 - 그리드 또는 가로 스크롤로 표시
                MultipleImagesView(
                    imageUrls = imageUrls,
                    onImageClick = onImageClick,
                    isLoading = message.isSending
                )
            }
        }

        // 전송 상태 표시
        if (message.isSending) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "이미지 업로드 중...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "이미지",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
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
    when (imageUrls.size) {
        2 -> {
            // 2개 이미지 - 나란히 배치
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                imageUrls.forEachIndexed { index, imageUrl ->
                    MultipleImageItem(
                        imageUrl = imageUrl,
                        onClick = { onImageClick(imageUrl, imageUrls, index) },
                        isLoading = isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        3 -> {
            // 3개 이미지 - 첫 번째는 크게, 나머지는 작게
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MultipleImageItem(
                    imageUrl = imageUrls[0],
                    onClick = { onImageClick(imageUrls[0], imageUrls, 0) },
                    isLoading = isLoading,
                    modifier = Modifier.weight(2f)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MultipleImageItem(
                        imageUrl = imageUrls[1],
                        onClick = { onImageClick(imageUrls[1], imageUrls, 1) },
                        isLoading = isLoading,
                        modifier = Modifier.weight(1f)
                    )
                    MultipleImageItem(
                        imageUrl = imageUrls[2],
                        onClick = { onImageClick(imageUrls[2], imageUrls, 2) },
                        isLoading = isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        4 -> {
            // 4개 이미지 - 2x2 그리드
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MultipleImageItem(
                        imageUrl = imageUrls[0],
                        onClick = { onImageClick(imageUrls[0], imageUrls, 0) },
                        isLoading = isLoading,
                        modifier = Modifier.weight(1f)
                    )
                    MultipleImageItem(
                        imageUrl = imageUrls[1],
                        onClick = { onImageClick(imageUrls[1], imageUrls, 1) },
                        isLoading = isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MultipleImageItem(
                        imageUrl = imageUrls[2],
                        onClick = { onImageClick(imageUrls[2], imageUrls, 2) },
                        isLoading = isLoading,
                        modifier = Modifier.weight(1f)
                    )
                    MultipleImageItem(
                        imageUrl = imageUrls[3],
                        onClick = { onImageClick(imageUrls[3], imageUrls, 3) },
                        isLoading = isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        else -> {
            // 5개 이상 이미지 - 가로 스크롤
            LazyRow(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(imageUrls) { imageUrl ->
                    val index = imageUrls.indexOf(imageUrl)
                    MultipleImageItem(
                        imageUrl = imageUrl,
                        onClick = { onImageClick(imageUrl, imageUrls, index) },
                        isLoading = isLoading,
                        modifier = Modifier.width(120.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MultipleImageItem(
    imageUrl: String,
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "이미지",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
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
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
package com.example.feature_chat.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature_chat.model.ChatMessageUiModel

/**
 * 메시지 상태를 표시하는 컴포넌트
 * 수정 표시, 전송 상태, 시간을 포함합니다.
 */
@Composable
fun MessageStatusRow(
    message: ChatMessageUiModel,
    onRetryMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // 수정 표시
        if (message.isModified) {
            Text(
                text = "(수정됨)",
                fontSize = 10.sp,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        // 내 메시지인 경우 전송 상태 표시
        if (message.isMyMessage) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when {
                    message.isSending -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(12.dp)
                                .testTag("delivery_indicator"),
                            strokeWidth = 1.dp
                        )
                    }

                    message.sendFailed -> {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = "전송 실패",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(12.dp)
                                .testTag("delivery_indicator")
                        )
                        if (message.canRetry) {
                            IconButton(
                                onClick = { onRetryMessage(message.messageId) },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "재전송",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    message.deliveryState is com.example.feature_chat.model.MessageDeliveryState.Retry -> {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "재전송 대기",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .size(12.dp)
                                .testTag("delivery_indicator")
                        )
                    }

                    else -> {
                        // 성공적으로 전송된 경우 시간만 표시 (기존 로직 유지)
                    }
                }
            }
        }
    }
} 
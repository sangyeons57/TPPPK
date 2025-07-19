package com.example.feature_chat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core_common.websocket.WebSocketConnectionState

@Composable
fun ConnectionStatusBar(
    connectionState: WebSocketConnectionState,
    queuedMessagesCount: Int,
    onRetryConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (isVisible, color, text, icon) = when (connectionState) {
        is WebSocketConnectionState.Disconnected -> {
            val queueText = if (queuedMessagesCount > 0) " ($queuedMessagesCount 대기 중)" else ""
            Quadruple(
                true,
                MaterialTheme.colorScheme.error,
                "연결 끊김$queueText - 탭하여 재연결",
                Icons.Default.WifiOff
            )
        }
        is WebSocketConnectionState.Connecting -> {
            Quadruple(
                true,
                MaterialTheme.colorScheme.primary,
                "연결 중...",
                Icons.Default.Refresh
            )
        }
        is WebSocketConnectionState.Connected -> {
            Quadruple(
                false,
                MaterialTheme.colorScheme.primary,
                "연결됨",
                Icons.Default.Wifi
            )
        }
        is WebSocketConnectionState.Error -> {
            Quadruple(
                true,
                MaterialTheme.colorScheme.error,
                "연결 오류: ${connectionState.message} - 탭하여 재시도",
                Icons.Default.CloudOff
            )
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = connectionState !is WebSocketConnectionState.Connecting) {
                    onRetryConnection()
                }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("connection_status_bar"),
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.1f)
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = CardDefaults.outlinedCardBorder().brush,
                width = 1.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = text,
                    color = color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("connection_status_text")
                )
                
                if (connectionState is WebSocketConnectionState.Connecting) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = color
                    )
                }
            }
        }
    }
}

// Helper data class for multiple return values
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
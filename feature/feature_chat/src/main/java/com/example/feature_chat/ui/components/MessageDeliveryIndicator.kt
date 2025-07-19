package com.example.feature_chat.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature_chat.model.MessageDeliveryState

@Composable
fun MessageDeliveryIndicator(
    deliveryState: MessageDeliveryState,
    modifier: Modifier = Modifier
) {
    val (icon, color, isVisible) = when (deliveryState) {
        is MessageDeliveryState.Sending -> Triple(
            Icons.Default.Schedule,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            true
        )
        is MessageDeliveryState.Sent -> Triple(
            Icons.Default.Done,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            false // Hide for sent messages to reduce clutter
        )
        is MessageDeliveryState.Delivered -> Triple(
            Icons.Default.DoneAll,
            MaterialTheme.colorScheme.primary,
            false // Hide for now, could be shown if needed
        )
        is MessageDeliveryState.Failed -> Triple(
            Icons.Default.Error,
            MaterialTheme.colorScheme.error,
            true
        )
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = when (deliveryState) {
                    is MessageDeliveryState.Sending -> "전송 중"
                    is MessageDeliveryState.Sent -> "전송됨"
                    is MessageDeliveryState.Delivered -> "전달됨"
                    is MessageDeliveryState.Failed -> "전송 실패"
                },
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            
            if (deliveryState is MessageDeliveryState.Failed) {
                Text(
                    text = "전송 실패",
                    color = color,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun MessageStatusRow(
    deliveryState: MessageDeliveryState,
    timestamp: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)
    ) {
        Text(
            text = timestamp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
        
        MessageDeliveryIndicator(deliveryState = deliveryState)
    }
}

@Composable
fun OptimisticMessageOverlay(
    isOptimistic: Boolean,
    deliveryState: MessageDeliveryState,
    modifier: Modifier = Modifier
) {
    if (isOptimistic) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            when (deliveryState) {
                is MessageDeliveryState.Sending -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
                is MessageDeliveryState.Failed -> {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "재시도",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
                else -> { /* No overlay for sent/delivered */ }
            }
        }
    }
}
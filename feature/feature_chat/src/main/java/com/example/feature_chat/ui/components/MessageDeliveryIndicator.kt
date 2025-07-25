package com.example.feature_chat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        is MessageDeliveryState.Retry -> Triple(
            Icons.Default.Refresh,
            MaterialTheme.colorScheme.tertiary,
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
                    is MessageDeliveryState.Retry -> "재전송 대기"
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
                        imageVector = Icons.Default.Error,
                        contentDescription = "전송 실패",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }

                is MessageDeliveryState.Retry -> {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "재전송 대기",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                else -> { /* No overlay for sent/delivered */ }
            }
        }
    }
}
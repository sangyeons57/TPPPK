package com.example.feature_chat.model

sealed class MessageDeliveryState {
    data object Sending : MessageDeliveryState()
    data object Sent : MessageDeliveryState()
    data object Delivered : MessageDeliveryState()
    data class Failed(val error: String) : MessageDeliveryState()
    data object Retry : MessageDeliveryState() // 재전송 대기 상태
}

data class OptimisticMessage(
    val localId: String,
    val content: String,
    val senderId: String,
    val timestamp: java.time.Instant,
    val deliveryState: MessageDeliveryState = MessageDeliveryState.Sending,
    val attachmentUris: List<android.net.Uri> = emptyList()
)
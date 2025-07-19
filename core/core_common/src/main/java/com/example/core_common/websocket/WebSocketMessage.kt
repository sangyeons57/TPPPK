package com.example.core_common.websocket

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class WebSocketMessage(
    @SerialName("type") val type: String,
    @SerialName("roomId") val roomId: String? = null,
    @SerialName("senderId") val senderId: String? = null,
    @SerialName("content") val content: String? = null,
    @SerialName("timestamp") val timestamp: String? = null,
    @SerialName("messageId") val messageId: String? = null,
    @SerialName("replyToMessageId") val replyToMessageId: String? = null,
    @SerialName("payload") val payload: Map<String, String>? = null
) {
    companion object {
        // Message Types
        const val TYPE_AUTH = "AUTH"
        const val TYPE_JOIN_ROOM = "JOIN_ROOM"
        const val TYPE_LEAVE_ROOM = "LEAVE_ROOM"
        const val TYPE_MESSAGE = "MESSAGE"
        const val TYPE_EDIT_MESSAGE = "EDIT_MESSAGE"
        const val TYPE_DELETE_MESSAGE = "DELETE_MESSAGE"
        const val TYPE_SYSTEM = "SYSTEM"
        const val TYPE_ACK = "ACK"
        const val TYPE_ERROR = "ERROR"
        const val TYPE_HEARTBEAT = "HEARTBEAT"
    }
}
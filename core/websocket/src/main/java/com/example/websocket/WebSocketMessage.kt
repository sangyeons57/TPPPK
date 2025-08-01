package com.example.websocket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebSocketMessage(
    @SerialName("type") val type: String,
    @SerialName("roomId") val roomId: String? = null,
    @SerialName("senderId") val senderId: String? = null,
    @SerialName("content") val content: String? = null,
    @SerialName("timestamp") val timestamp: Double? = null,
    @SerialName("messageId") val messageId: String? = null,
    @SerialName("replyToMessageId") val replyToMessageId: String? = null,
    @SerialName("payload") val payload: Map<String, String>? = null,
    @SerialName("projectId") val projectId: String? = null,
    @SerialName("channelType") val channelType: String? = null
) {
    companion object {
        // Message Types - WebSocketEventTypes를 참조하여 중복 제거
        const val TYPE_AUTH = WebSocketEventTypes.AUTH
        const val TYPE_AUTH_SUCCESS = WebSocketEventTypes.AUTH_SUCCESS
        const val TYPE_JOIN_ROOM = WebSocketEventTypes.JOIN_ROOM
        const val TYPE_LEAVE_ROOM = WebSocketEventTypes.LEAVE_ROOM
        const val TYPE_MESSAGE = WebSocketEventTypes.MESSAGE
        const val TYPE_EDIT_MESSAGE = WebSocketEventTypes.EDIT_MESSAGE
        const val TYPE_DELETE_MESSAGE = WebSocketEventTypes.DELETE_MESSAGE
        const val TYPE_SYSTEM = WebSocketEventTypes.SYSTEM
        const val TYPE_ACK = WebSocketEventTypes.ACK
        const val TYPE_MESSAGE_ACK = WebSocketEventTypes.MESSAGE_ACK
        const val TYPE_EDIT_MESSAGE_ACK = WebSocketEventTypes.EDIT_MESSAGE_ACK
        const val TYPE_DELETE_MESSAGE_ACK = WebSocketEventTypes.DELETE_MESSAGE_ACK

        const val TYPE_ERROR = WebSocketEventTypes.ERROR
        const val TYPE_HEARTBEAT = WebSocketEventTypes.HEARTBEAT

        // Channel Types
        const val CHANNEL_TYPE_DM = WebSocketEventTypes.CHANNEL_TYPE_DM
        const val CHANNEL_TYPE_PROJECT = WebSocketEventTypes.CHANNEL_TYPE_PROJECT
    }
}
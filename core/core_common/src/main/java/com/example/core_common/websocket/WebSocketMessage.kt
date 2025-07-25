package com.example.core_common.websocket

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
        // Message Types
        const val TYPE_AUTH = "AUTH"
        const val TYPE_AUTH_SUCCESS = "AUTH_SUCCESS"
        const val TYPE_JOIN_ROOM = "JOIN_ROOM"
        const val TYPE_LEAVE_ROOM = "LEAVE_ROOM"
        const val TYPE_MESSAGE = "MESSAGE"
        const val TYPE_EDIT_MESSAGE = "EDIT_MESSAGE"
        const val TYPE_DELETE_MESSAGE = "DELETE_MESSAGE"
        const val TYPE_SYSTEM = "SYSTEM"
        const val TYPE_ACK = "ACK"
        const val TYPE_ERROR = "ERROR"
        const val TYPE_HEARTBEAT = "HEARTBEAT"

        // Channel Types
        const val CHANNEL_TYPE_DM = "DM"
        const val CHANNEL_TYPE_PROJECT = "PROJECT"
    }
}
package com.example.feature_chat.websocket

sealed class ChatWebSocketEvent {
    data class MessageReceived(
        val messageId: String,
        val senderId: String,
        val content: String,
        val timestamp: String,
        val replyToMessageId: String? = null
    ) : ChatWebSocketEvent()
    
    data class MessageEdited(
        val messageId: String,
        val newContent: String,
        val timestamp: String
    ) : ChatWebSocketEvent()
    
    data class MessageDeleted(
        val messageId: String,
        val timestamp: String
    ) : ChatWebSocketEvent()
    
    data class SystemMessage(
        val content: String,
        val timestamp: String
    ) : ChatWebSocketEvent()
    
    data class Error(
        val message: String
    ) : ChatWebSocketEvent()
    
    data class MessageAck(
        val messageId: String,
        val ackType: String // MESSAGE_ACK, EDIT_MESSAGE_ACK, DELETE_MESSAGE_ACK
    ) : ChatWebSocketEvent()
    
    data class MessageFailed(
        val messageId: String,
        val failureType: String // MESSAGE_FAILED, EDIT_MESSAGE_FAILED, DELETE_MESSAGE_FAILED
    ) : ChatWebSocketEvent()
    
    data class Unknown(
        val type: String
    ) : ChatWebSocketEvent()
}
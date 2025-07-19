package com.example.feature_chat.queue

import com.example.core_common.websocket.WebSocketConnectionState
import com.example.domain.model.base.Message
import com.example.feature_chat.websocket.ChatWebSocketClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

sealed class QueuedMessageAction {
    data class Send(val message: Message, val roomId: String) : QueuedMessageAction()
    data class Edit(val messageId: String, val newContent: String, val roomId: String) : QueuedMessageAction()
    data class Delete(val messageId: String, val roomId: String) : QueuedMessageAction()
}

@Singleton
class OfflineMessageQueue @Inject constructor(
    private val webSocketClient: ChatWebSocketClient
) {
    private val queue = ConcurrentLinkedQueue<QueuedMessageAction>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        // Watch for connection state changes
        scope.launch {
            webSocketClient.connectionState.collectLatest { state ->
                if (state is WebSocketConnectionState.Connected) {
                    processQueue()
                }
            }
        }
    }
    
    fun queueMessage(action: QueuedMessageAction) {
        queue.offer(action)
    }
    
    private suspend fun processQueue() {
        while (queue.isNotEmpty()) {
            val action = queue.poll() ?: break
            
            try {
                when (action) {
                    is QueuedMessageAction.Send -> {
                        webSocketClient.sendMessage(
                            roomId = action.roomId,
                            senderId = action.message.senderId,
                            content = action.message.content.value,
                            messageId = action.message.id,
                            replyToMessageId = action.message.replyToMessageId
                        )
                    }
                    is QueuedMessageAction.Edit -> {
                        webSocketClient.editMessage(
                            roomId = action.roomId,
                            messageId = com.example.domain.model.vo.DocumentId(action.messageId),
                            newContent = action.newContent
                        )
                    }
                    is QueuedMessageAction.Delete -> {
                        webSocketClient.deleteMessage(
                            roomId = action.roomId,
                            messageId = com.example.domain.model.vo.DocumentId(action.messageId)
                        )
                    }
                }
                
                // Small delay between sends to avoid overwhelming server
                delay(100)
                
            } catch (e: Exception) {
                // If sending fails, put the action back in queue
                queue.offer(action)
                break // Stop processing until next connection
            }
        }
    }
    
    fun clearQueue() {
        queue.clear()
    }
    
    fun getQueueSize(): Int = queue.size
}
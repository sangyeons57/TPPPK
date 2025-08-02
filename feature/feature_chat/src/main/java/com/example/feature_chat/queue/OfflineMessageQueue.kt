package com.example.feature_chat.queue

import com.example.domain.model.base.Message
import com.example.websocket.core.WebSocketConnectionState
import com.example.websocket.usecase.WebSocketUseCaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider
) {
    private val queue = ConcurrentLinkedQueue<QueuedMessageAction>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val webSocketUseCases by lazy { webSocketUseCaseProvider.create() }
    
    init {
        // Watch for connection state changes
        scope.launch {
            webSocketUseCases.getConnectionStateUseCase().collectLatest { state ->
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
                        val roomUseCases = webSocketUseCaseProvider.createForRoom(action.roomId)
                        roomUseCases.sendMessageUseCase(
                            senderId = action.message.senderId,
                            content = action.message.content.value,
                            messageId = action.message.id,
                            replyToMessageId = action.message.replyToMessageId
                        )
                    }
                    is QueuedMessageAction.Edit -> {
                        val roomUseCases = webSocketUseCaseProvider.createForRoom(action.roomId)
                        roomUseCases.editMessageUseCase(
                            messageId = com.example.domain.model.vo.DocumentId(action.messageId),
                            newContent = action.newContent
                        )
                    }
                    is QueuedMessageAction.Delete -> {
                        val roomUseCases = webSocketUseCaseProvider.createForRoom(action.roomId)
                        roomUseCases.deleteMessageUseCase(
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
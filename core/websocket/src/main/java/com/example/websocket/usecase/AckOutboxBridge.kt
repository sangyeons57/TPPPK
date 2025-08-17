package com.example.websocket.usecase

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain_repository.base.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges WebSocket ACK/FAIL events to OutBox updates.
 * Call start() after WS connection is established.
 */
@Singleton
class AckOutboxBridge @Inject constructor(
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider,
    private val messageRepository: MessageRepository,
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    fun start() {
        if (job != null) return
        val useCases = webSocketUseCaseProvider.create()
        job = Job()

        // ACK stream
        useCases.subscribeToAckEventsUseCase()
            .onEach { event ->
                when (event) {
                    is com.example.websocket.event.WebSocketDomainEvent.MessageAck -> {
                        scope.launch {
                            val r = messageRepository.handleMessageAck(event.messageId)
                            if (r is CustomResult.Failure) {
                                Log.e(TAG, "Failed to mark ACK for ${event.messageId}", r.error)
                            }
                        }
                    }

                    is com.example.websocket.event.WebSocketDomainEvent.MessageFailed -> {
                        scope.launch {
                            val r = messageRepository.handleMessageFailure(event.messageId)
                            if (r is CustomResult.Failure) {
                                Log.e(TAG, "Failed to mark FAIL for ${event.messageId}", r.error)
                            }
                        }
                    }

                    else -> {
                        // Ignore other event types
                    }
                }
            }
            .launchIn(CoroutineScope(Dispatchers.IO + job!!))
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val TAG = "AckOutboxBridge"
    }
}


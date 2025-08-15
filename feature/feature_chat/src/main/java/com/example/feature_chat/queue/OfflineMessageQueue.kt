package com.example.feature_chat.queue

import com.example.domain.model.base.Message
import com.example.domain.vo.DocumentId
import com.example.domain.vo.message.MessagePayload
import com.example.websocket.core.WebSocketConnectionState
import com.example.websocket.usecase.WebSocketUseCaseProvider
import com.example.websocket.usecase.SendMessageUseCase
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
    data class Send(val message: Message, val roomId: String, val retryCount: Int = 0) :
        QueuedMessageAction()

    data class SendImage(
        val senderId: String,
        val imageUri: android.net.Uri,
        val content: String,
        val roomId: String,
        val retryCount: Int = 0
    ) : QueuedMessageAction()

    data class SendImages(
        val senderId: String,
        val imageUris: List<android.net.Uri>,
        val content: String,
        val roomId: String,
        val retryCount: Int = 0
    ) : QueuedMessageAction()

    data class Edit(
        val messageId: String,
        val newContent: String,
        val roomId: String,
        val retryCount: Int = 0
    ) : QueuedMessageAction()

    data class Delete(val messageId: String, val roomId: String, val retryCount: Int = 0) :
        QueuedMessageAction()
}

@Singleton
class OfflineMessageQueue @Inject constructor(
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider,
    private val sendMessageUseCase: SendMessageUseCase,
) {
    private val queue = ConcurrentLinkedQueue<QueuedMessageAction>()
    private val failedQueue = ConcurrentLinkedQueue<QueuedMessageAction>() // 실패한 작업들
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val webSocketUseCases by lazy { webSocketUseCaseProvider.create() }

    private val maxRetries = 3
    private val baseDelayMs = 1000L
    
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
        android.util.Log.d(
            "OfflineMessageQueue",
            "📤 큐 처리 시작 - 대기중: ${queue.size}개, 실패: ${failedQueue.size}개"
        )

        // 메인 큐 처리
        while (queue.isNotEmpty()) {
            val action = queue.poll() ?: break
            
            try {
                val success = processAction(action)

                if (success) {
                    android.util.Log.d(
                        "OfflineMessageQueue",
                        "✅ 큐 액션 처리 성공: ${action::class.simpleName}"
                    )
                    delay(100) // 서버 부하 방지
                } else {
                    android.util.Log.w(
                        "OfflineMessageQueue",
                        "❌ 큐 액션 처리 실패, 재시도 예약: ${action::class.simpleName}"
                    )
                    handleFailedAction(action)
                    break // 실패 시 큐 처리 중단
                }
            } catch (e: Exception) {
                android.util.Log.e(
                    "OfflineMessageQueue",
                    "❌ 큐 액션 처리 중 예외: ${action::class.simpleName}",
                    e
                )
                handleFailedAction(action)
                break
            }
        }

        // 실패한 작업들 재시도
        processFailedQueue()
    }

    private suspend fun processAction(action: QueuedMessageAction): Boolean {
        return when (action) {
            is QueuedMessageAction.Send -> {
                // Use unified use case; projectId unknown here → null
                val result = sendMessageUseCase(action.message, projectId = null)
                result is com.example.core_common.result.CustomResult.Success
            }

            is QueuedMessageAction.SendImage -> {
                // 이미지 메시지는 MessageService를 통해 처리되어야 함
                // 현재는 기본 메시지와 같이 처리하되, 향후 개선 필요
                android.util.Log.w("OfflineMessageQueue", "⚠️ 이미지 메시지 큐 처리는 향후 구현 예정")
                false // 일단 실패로 처리
            }

            is QueuedMessageAction.SendImages -> {
                // 다중 이미지 메시지 처리
                android.util.Log.w("OfflineMessageQueue", "⚠️ 다중 이미지 메시지 큐 처리는 향후 구현 예정")
                false // 일단 실패로 처리
            }

            is QueuedMessageAction.Edit -> {
                val roomUseCases = webSocketUseCaseProvider.createForRoom(action.roomId)
                val newPayload = MessagePayload.forText(action.newContent)
                val result = roomUseCases.editMessageUseCase(
                    messageId = DocumentId(action.messageId),
                    newPayload = newPayload
                )
                result.isSuccess
            }

            is QueuedMessageAction.Delete -> {
                val roomUseCases = webSocketUseCaseProvider.createForRoom(action.roomId)
                val result = roomUseCases.deleteMessageUseCase(
                    messageId = DocumentId(action.messageId)
                )
                result.isSuccess
            }
        }
    }

    private fun handleFailedAction(action: QueuedMessageAction) {
        val updatedAction = when (action) {
            is QueuedMessageAction.Send -> action.copy(retryCount = action.retryCount + 1)
            is QueuedMessageAction.SendImage -> action.copy(retryCount = action.retryCount + 1)
            is QueuedMessageAction.SendImages -> action.copy(retryCount = action.retryCount + 1)
            is QueuedMessageAction.Edit -> action.copy(retryCount = action.retryCount + 1)
            is QueuedMessageAction.Delete -> action.copy(retryCount = action.retryCount + 1)
        }

        if (updatedAction.getRetryCount() < maxRetries) {
            failedQueue.offer(updatedAction)
            android.util.Log.d(
                "OfflineMessageQueue",
                "📋 실패한 액션 재시도 큐에 추가: 재시도 ${updatedAction.getRetryCount()}/$maxRetries"
            )
        } else {
            android.util.Log.e(
                "OfflineMessageQueue",
                "💀 액션 최대 재시도 횟수 초과, 포기: ${action::class.simpleName}"
            )
        }
    }

    private suspend fun processFailedQueue() {
        if (failedQueue.isEmpty()) return

        android.util.Log.d("OfflineMessageQueue", "🔄 실패한 액션 재시도 시작: ${failedQueue.size}개")

        val actionsToRetry = mutableListOf<QueuedMessageAction>()
        while (failedQueue.isNotEmpty()) {
            failedQueue.poll()?.let { actionsToRetry.add(it) }
        }

        for (action in actionsToRetry) {
            try {
                val retryDelay = baseDelayMs * (action.getRetryCount() + 1)
                android.util.Log.d(
                    "OfflineMessageQueue",
                    "⏳ ${retryDelay}ms 대기 후 재시도: ${action::class.simpleName}"
                )
                delay(retryDelay)

                val success = processAction(action)

                if (success) {
                    android.util.Log.d(
                        "OfflineMessageQueue",
                        "✅ 재시도 성공: ${action::class.simpleName}"
                    )
                } else {
                    handleFailedAction(action)
                }
            } catch (e: Exception) {
                android.util.Log.e(
                    "OfflineMessageQueue",
                    "❌ 재시도 중 예외: ${action::class.simpleName}",
                    e
                )
                handleFailedAction(action)
            }
        }
    }

    private fun QueuedMessageAction.getRetryCount(): Int {
        return when (this) {
            is QueuedMessageAction.Send -> retryCount
            is QueuedMessageAction.SendImage -> retryCount
            is QueuedMessageAction.SendImages -> retryCount
            is QueuedMessageAction.Edit -> retryCount
            is QueuedMessageAction.Delete -> retryCount
        }
    }
    
    fun clearQueue() {
        queue.clear()
        failedQueue.clear()
        android.util.Log.d("OfflineMessageQueue", "🗑️ 모든 큐 초기화 완료")
    }

    fun getQueueSize(): Int = queue.size + failedQueue.size

    fun getQueueInfo(): Pair<Int, Int> {
        return Pair(queue.size, failedQueue.size)
    }

    /**
     * 이미지 메시지 큐잉 (향후 구현을 위한 준비)
     */
    fun queueImageMessage(
        senderId: String,
        imageUri: android.net.Uri,
        content: String,
        roomId: String
    ) {
        val action = QueuedMessageAction.SendImage(
            senderId = senderId,
            imageUri = imageUri,
            content = content,
            roomId = roomId
        )
        queue.offer(action)
        android.util.Log.d("OfflineMessageQueue", "📋 이미지 메시지 큐에 추가: $roomId")
    }

    /**
     * 다중 이미지 메시지 큐잉 (향후 구현을 위한 준비)
     */
    fun queueImagesMessage(
        senderId: String,
        imageUris: List<android.net.Uri>,
        content: String,
        roomId: String
    ) {
        val action = QueuedMessageAction.SendImages(
            senderId = senderId,
            imageUris = imageUris,
            content = content,
            roomId = roomId
        )
        queue.offer(action)
        android.util.Log.d(
            "OfflineMessageQueue",
            "📋 다중 이미지 메시지 큐에 추가: $roomId (${imageUris.size}개)"
        )
    }
}

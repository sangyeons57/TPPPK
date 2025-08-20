package com.example.websocket.usecase

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.vo.CollectionPath
import com.example.domain.vo.DocumentId
import com.example.domain.vo.ProjectId
import com.example.domain.vo.ChannelId
import com.example.domain_repository.base.MessageRepository
import javax.inject.Inject

/**
 * Unified message send UseCase (SRP):
 * - Persist locally (Room) and enqueue OutBox
 * - Send via WebSocket for the message's channel (room)
 *
 * Minimal params: Message + optional ProjectId
 */
class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider,
) {

    suspend operator fun invoke(
        message: Message,
        projectId: ProjectId? = null,
    ): CustomResult<DocumentId, Exception> {
        return try {
            // Compose composite channelId when projectId is provided (ProjectId:ChannelId)
            val compositeChannelId = if (projectId != null && !message.channelId.hasDelimiter()) {
                ChannelId.compose(projectId.value, message.channelId.value)
            } else message.channelId

            val channelIdValue = compositeChannelId.value

            // Ensure message has a valid ID - no ID regeneration allowed for consistency
            if (message.id.isNotAssigned()) {
                Log.e(TAG, "Message ID must be assigned before calling SendMessageUseCase")
                return CustomResult.Failure(IllegalArgumentException("Message ID is required"))
            }
            val messageToSave = if (compositeChannelId.value != message.channelId.value) {
                // Reconstitute message with the composite ChannelId while preserving fields
                Message.fromDataSource(
                    id = message.id,
                    senderId = message.senderId,
                    messageType = message.messageType,
                    payload = message.payload,
                    replyToMessageId = message.replyToMessageId,
                    createdAt = message.createdAt,
                    updatedAt = message.updatedAt,
                    isDeleted = message.isDeleted,
                    mentions = message.mentions,
                    channelId = compositeChannelId
                )
            } else message

            // Ensure repository is scoped to the correct collection
            try {
                messageRepository.setCollection(
                    CollectionPath.messages(compositeChannelId)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set repository collection", e)
            }

            // 1) Local save (also enqueues OutBox inside repository transaction)
            when (val saveResult = messageRepository.sendMessage(messageToSave)) {
                is CustomResult.Failure -> {
                    return CustomResult.Failure(saveResult.error)
                }
                is CustomResult.Success -> {
                    val messageId = saveResult.data
                    // 2) WebSocket send (best-effort; return success on local save)
                    try {
                        // WebSocket room uses leaf channel id
                        val roomId = compositeChannelId.last()
                        val roomUseCases = webSocketUseCaseProvider.createForRoom(roomId)
                        // 도메인 메시지 기반 전송 API 사용
                        val wsResult = roomUseCases.sendMessageUseCase(
                            message = messageToSave
                        )
                        if (!wsResult.isSuccess) {
                            Log.e(TAG, "WebSocket send failed for message: ${messageId.value}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception during WebSocket send: ${messageId.value}", e)
                    }

                    CustomResult.Success(messageId)
                }
                else -> {
                    // Should not reach here, keep safe fallback
                    CustomResult.Failure(IllegalStateException("Unknown save result"))
                }
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    companion object {
        private const val TAG = "UnifiedSendMessageUseCase"
    }
}

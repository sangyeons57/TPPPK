package com.example.websocket.usecase

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.vo.CollectionPath
import com.example.domain.vo.DocumentId
import com.example.domain.vo.ProjectId
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
            val channelId = message.channelId.value

            // Ensure message has a non-empty id before any persistence or sending
            val messageToSave = if (message.id.isNotAssigned()) {
                try {
                    Message.create(
                        id = DocumentId.generate(),
                        senderId = message.senderId,
                        messageType = message.messageType,
                        payload = message.payload,
                        replyToMessageId = message.replyToMessageId,
                        mentions = message.mentions,
                        channelId = message.channelId
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to generate Message ID; aborting send", e)
                    return CustomResult.Failure(e)
                }
            } else message

            // Ensure repository is scoped to the correct collection
            try {
                if (projectId != null) {
                    messageRepository.setCollection(
                        CollectionPath.projectChannelMessages(projectId.value, channelId)
                    )
                } else {
                    messageRepository.setCollection(CollectionPath.dmChannelMessages(channelId))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set repository collection", e)
            }

            // 1) Local save (also enqueues OutBox inside repository transaction)
            when (val saveResult = messageRepository.save(messageToSave)) {
                is CustomResult.Failure -> {
                    return CustomResult.Failure(saveResult.error)
                }
                is CustomResult.Success -> {
                    val messageId = saveResult.data
                    // 2) WebSocket send (best-effort; return success on local save)
                    try {
                        val roomUseCases = webSocketUseCaseProvider.createForRoom(channelId)
                        val wsResult = roomUseCases.sendMessageUseCase(
                            senderId = messageToSave.senderId,
                            payload = messageToSave.payload,
                            messageId = messageId,
                            replyToMessageId = messageToSave.replyToMessageId,
                            projectId = projectId?.value
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

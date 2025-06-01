package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.MessageRemoteDataSource
import com.example.data.datasource.remote.MessageAttachmentRemoteDataSource
import com.example.data.model.remote.MessageAttachmentDTO
import com.example.data.model.remote.MessageDTO
import com.example.domain.model.base.Message
import com.example.domain.repository.MessageAttachmentToSend
import com.example.domain.repository.MessageRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject


class MessageRepositoryImpl @Inject constructor(
    private val messageRemoteDataSource: MessageRemoteDataSource,
    private val messageAttachmentRemoteDataSource: MessageAttachmentRemoteDataSource // 첨부파일 처리용
    // private val messageMapper: MessageMapper // 개별 매퍼 사용시
) : MessageRepository {

    /**
     * 특정 채널의 메시지 목록을 스트림으로 가져옵니다.
     * 
     * @param channelId 채널 ID
     * @param limit 가져올 최대 메시지 수
     * @return 메시지 목록 스트림
     */
    override fun getMessagesStream(channelId: String, limit: Int): Flow<CustomResult<List<Message>, Exception>> {
        // Determine the proper path for the channel
        val channelPath = resolveChannelPath(channelId)
        
        // Convert limit to Long as expected by the datasource
        val longLimit = limit.toLong()
        
        // Observe messages and transform the Flow
        return messageRemoteDataSource.observeMessages(channelPath, longLimit)
            .map { messageDTOs ->
                try {
                    // Transform DTOs to domain models
                    val domainMessages = messageDTOs.map { it.toDomain() }
                    CustomResult.Success(domainMessages)
                } catch (e: Exception) {
                    CustomResult.Failure(e)
                }
            }
    }

    /**
     * 이전 메시지를 가져옵니다 (페이지네이션).
     * 
     * @param channelId 채널 ID
     * @param startAfterMessageId 이 메시지 다음부터 가져옴 (널이면 처음부터)
     * @param limit 가져올 최대 메시지 수
     * @return 메시지 목록
     */
    override suspend fun getPastMessages(channelId: String, startAfterMessageId: String?, limit: Int): CustomResult<List<Message>, Exception> {
        // This implementation would depend on how your backend handles pagination
        // For now, we'll use the same observeMessages method but add filtering logic
        val channelPath = resolveChannelPath(channelId)
        
        try {
            // Get a snapshot of messages from the Flow
            val messages = messageRemoteDataSource.observeMessages(channelPath, limit.toLong() * 2) // Fetch more to allow for filtering
                .map { messageDTOs -> messageDTOs.map { it.toDomain() } }
                .first() // Get the first emission as a snapshot
            
            // Apply pagination logic
            val filteredMessages = if (startAfterMessageId != null) {
                // Find the index of the message to start after
                val startIndex = messages.indexOfFirst { it.id == startAfterMessageId }
                // If found, return messages after that index, limited by the limit parameter
                if (startIndex >= 0 && startIndex + 1 < messages.size) {
                    messages.subList(startIndex + 1, minOf(startIndex + 1 + limit, messages.size))
                } else {
                    // If not found, return the most recent messages up to the limit
                    messages.take(limit)
                }
            } else {
                // If no startAfterMessageId, return the most recent messages up to the limit
                messages.take(limit)
            }
            
            return CustomResult.Success(filteredMessages)
        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }

    /**
     * 새 메시지를 채널에 발송합니다.
     * 
     * @param channelId 채널 ID
     * @param content 메시지 내용
     * @param attachments 첨부파일 목록
     * @param senderId 발송자 ID
     * @return 생성된 메시지 ID
     */
    override suspend fun sendMessage(
        channelId: String,
        content: String?,
        attachments: List<MessageAttachmentToSend>,
        senderId: String
    ): CustomResult<String, Exception> {
        TODO("나중에 메시지 기능 만들떄 작업처리")
        /**
        return try {
            coroutineScope {
                // 채널 경로 해석
                val channelPath = resolveChannelPath(channelId)
                
                // 현재 시간 가져오기
                val now = Timestamp.now()
                
                // 메시지 DTO 생성
                val messageDTO = MessageDTO(
                    id = "", // ID는 Firebase에서 자동 생성
                    channelId = channelId,
                    senderId = senderId,
                    content = content ?: "",
                    timestamp = now,
                    edited = false,
                    attachments = emptyList() // 첨부파일은 별도로 처리
                )
                
                // 메시지 추가
                val messageResult = messageRemoteDataSource.addMessage(channelPath, messageDTO)
                
                when (messageResult) {
                    is CustomResult.Success -> {
                        val messageId = messageResult.data
                        
                        // 첨부파일 처리
                        if (attachments.isNotEmpty()) {
                            // 모든 첨부파일 비동기 업로드
                            val attachmentResults = attachments.map { attachment ->
                                async {
                                    // 첨부파일 DTO 생성
                                    val attachmentDTO = MessageAttachmentDTO(
                                        id = "", // Firebase에서 자동 생성
                                        messageId = messageId,
                                        type = mapMimeTypeToAttachmentType(attachment.mimeType),
                                        url = "", // 업로드 후 상세
                                        name = attachment.fileName,
                                        size = attachment.fileSize,
                                        mimeType = attachment.mimeType,
                                        metadata = attachment.metadata
                                    )
                                    
                                    // 첨부파일 업로드
                                    messageAttachmentRemoteDataSource.uploadAttachment(
                                        channelPath,
                                        messageId,
                                        attachmentDTO,
                                        attachment.fileUri
                                    )
                                }
                            }.awaitAll()
                            
                            // 업로드 실패 처리
                            attachmentResults.forEach { result ->
                                if (result is CustomResult.Failure) {
                                    // 업로드 실패 로깅
                                    // 실패해도 메시지 자체는 이미 생성되었으므로 성공 처리
                                }
                            }
                        }
                        
                        CustomResult.Success(messageId)
                    }
                    is CustomResult.Failure -> CustomResult.Failure(Exception("Failed to send message"))
                    else -> CustomResult.Failure(Exception("Unknown error sending message"))
                }
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
        /**
        return try {
            coroutineScope {
                // 1. 첨부파일 업로드 (병렬 처리)
                val uploadedAttachmentDtos = resultTry {
                    attachments.map { attachmentToSend ->
                        async {
                            // Since MessageAttachmentRemoteDataSource doesn't have a direct uploadAttachment method with these parameters,
                            // we need to handle file upload and then create the attachment
                            val channelPath = resolveChannelPath(channelId)

                            // Create a DTO for the attachment based on the attachment information
                            val attachmentDto = MessageAttachmentDTO(
                                id = "", // Will be assigned by Firestore
                                attachmentType = mapMimeTypeToAttachmentType(attachmentToSend.mimeType),
                                attachmentUrl = attachmentToSend.sourceUri, // In a real implementation, we would upload and get URL
                                fileName = attachmentToSend.fileName,
                                fileSize = null // Not available from the sourceUri directly
                            )

                            // Add the attachment to the specified channel path
                            val uploadResult = messageAttachmentRemoteDataSource.addAttachment(channelPath, attachmentDto)

                            if (uploadResult is CustomResult.Failure) {
                                throw Exception("Failed to upload attachment")
                            }

                            // Return the uploaded attachment DTO
                            (uploadResult as CustomResult.Success).data
                        }
                    }.awaitAll()
                } catch (e: Exception) {
                    return@coroutineScope CustomResult.Failure(Unit)
                }

                // 2. 메시지 DTO 생성 (첨부파일 정보 포함)
                // Only include the content if it's not null or empty
                val messageContent = content?.takeIf { it.isNotBlank() } ?: ""

                // Resolve the channel path
                val channelPath = resolveChannelPath(channelId)

                // 3. 메시지 저장
                // Note: The sendMessage method only accepts content, not a full DTO
                // In a real implementation, we would modify the data source to accept attachments
                val sendResult = messageRemoteDataSource.sendMessage(channelPath, messageContent)
                if (sendResult is CustomResult.Failure) {
                    return@coroutineScope CustomResult.Failure(Unit)
                }

                return@coroutineScope CustomResult.Success((sendResult as CustomResult.Success).data)
            }
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
        */
         * **/
    }

    // MIME 타입을 AttachmentType 문자열로 변환하는 헬퍼 함수 (예시)
    /**
     * MIME 타입을 AttachmentType 문자열로 변환하는 헬퍼 함수
     * @param mimeType MIME 타입 문자열 (예: "image/jpeg", "video/mp4")
     * @return AttachmentType에 해당하는 문자열 값
     */
    private fun mapMimeTypeToAttachmentType(mimeType: String): String {
        return when {
            mimeType.startsWith("image/") -> "IMAGE"
            mimeType.startsWith("video/") -> "VIDEO"
            mimeType.startsWith("audio/") -> "AUDIO"
            else -> "FILE"
        }
    }
    
    /**
     * 채널 ID를 Firestore 경로로 변환하는 헬퍼 함수
     * @param channelId 채널 ID
     * @return 채널의 전체 Firestore 경로
     */
    private fun resolveChannelPath(channelId: String): String {
        // This is a simplified implementation
        // In a real app, we would determine if this is a DM or project channel
        // and construct the path accordingly using FireStorePaths utility
        return if (channelId.startsWith("dm_")) {
            "dm_channels/$channelId"
        } else {
            "projects/$channelId/messages"
        }
    }

    /**
     * 기존 메시지의 내용을 수정합니다.
     * 
     * @param channelId 채널 ID
     * @param messageId 수정할 메시지 ID
     * @param newContent 새 메시지 내용
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun editMessage(
        channelId: String,
        messageId: String,
        newContent: String
    ): CustomResult<Unit, Exception> {
        TODO("메시지 작업 할때 처리")
        /**
        return try {
            // Resolve the channel path
            val channelPath = resolveChannelPath(channelId)
            
            // Call the remote data source to update the message
            // Note that the data source method is updateMessage, not editMessage
            val result = messageRemoteDataSource.updateMessage(channelPath, messageId, newContent)
            
            when (result) {
                is CustomResult.Success -> CustomResult.Success(Unit)
                is CustomResult.Failure -> CustomResult.Failure(Unit)
                else -> CustomResult.Failure(Unit)
            }
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
        */
    }

    /**
     * 메시지를 삭제합니다.
     * 
     * @param channelId 채널 ID
     * @param messageId 삭제할 메시지 ID
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun deleteMessage(channelId: String, messageId: String): CustomResult<Unit, Exception> {
        TODO("메시지 작업 할떄 처리")
        /**
        return try {
            // Resolve the channel path
            val channelPath = resolveChannelPath(channelId)
            
            // Call the remote data source to delete the message
            val result = messageRemoteDataSource.deleteMessage(channelPath, messageId)
            
            when (result) {
                is CustomResult.Success -> CustomResult.Success(Unit)
                is CustomResult.Failure -> CustomResult.Failure(Unit)
                else -> CustomResult.Failure(Unit)
            }
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
        */
    }

    /**
     * 메시지에 반응(이모지)을 추가합니다.
     * 
     * @param channelId 채널 ID
     * @param messageId 메시지 ID
     * @param reactionEmoji 추가할 이모지
     * @param userId 사용자 ID
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun addReaction(channelId: String, messageId: String, reactionEmoji: String, userId: String): CustomResult<Unit, Exception> {
        TODO("메시지 작업 할떄 처리")
        /**
        return try {
            // Resolve the channel path
            val channelPath = resolveChannelPath(channelId)
            
            // The data source doesn't accept userId, so we need a custom implementation
            // In a real app, we would need to modify the data source or store the userId in a different way
            // For now, we'll simply call the data source method with the emoji
            val result = messageRemoteDataSource.addReaction(channelPath, messageId, reactionEmoji)
            
            when (result) {
                is CustomResult.Success -> CustomResult.Success(Unit)
                is CustomResult.Failure -> CustomResult.Failure(Unit)
                else -> CustomResult.Failure(Unit)
            }
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
        */
    }

    /**
     * 메시지에서 반응(이모지)을 제거합니다.
     * 
     * @param channelId 채널 ID
     * @param messageId 메시지 ID
     * @param reactionEmoji 제거할 이모지
     * @param userId 사용자 ID
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun removeReaction(channelId: String, messageId: String, reactionEmoji: String, userId: String): CustomResult<Unit, Exception> {
        TODO("메시지 작업 할떄 처리")
        /**
        return try {
            // Resolve the channel path
            val channelPath = resolveChannelPath(channelId)
            
            // The data source doesn't accept userId, so we need a custom implementation
            // Similar to addReaction, we'll simply call the data source method with the emoji
            val result = messageRemoteDataSource.removeReaction(channelPath, messageId, reactionEmoji)
            
            when (result) {
                is CustomResult.Success -> CustomResult.Success(Unit)
                is CustomResult.Failure -> CustomResult.Failure(Unit)
                else -> CustomResult.Failure(Unit)
            }
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
        */
    }

    /**
     * 특정 메시지의 상세 정보를 가져옵니다.
     * 
     * @param channelId 채널 ID
     * @param messageId 메시지 ID
     * @return 메시지 상세 정보
     */
    override suspend fun getMessage(channelId: String, messageId: String): CustomResult<Message, Exception> {
        TODO("메시지 작업 할떄 처리")
        /**
        return try {
            // Resolve the channel path
            val channelPath = resolveChannelPath(channelId)
            
            // Get the message from the remote data source
            val result = messageRemoteDataSource.getMessage(channelPath, messageId)
            
            when (result) {
                is CustomResult.Success -> {
                    try {
                        // Handle potential null result from data source
                        val messageDto = result.data ?: return CustomResult.Failure(Exception("Message not found"))
                        val domainMessage = messageDto.toDomain()
                        CustomResult.Success(domainMessage)
                    } catch (e: Exception) {
                        CustomResult.Failure(e)
                    }
                }
                is CustomResult.Failure -> CustomResult.Failure(Unit)
                else -> CustomResult.Failure(Unit)
            }
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
        */
    }
}

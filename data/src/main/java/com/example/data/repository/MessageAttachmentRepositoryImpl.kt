package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.MessageAttachmentRemoteDataSource
import com.example.data.model.remote.MessageAttachmentDTO
import com.example.data.model.remote.toDto
import com.example.domain.model._new.enum.MessageAttachmentType
import com.example.domain.model.base.MessageAttachment
import com.example.domain.repository.MessageAttachmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
// import java.io.File // 안드로이드 Uri 대신 File 객체를 사용한다면
import javax.inject.Inject

class MessageAttachmentRepositoryImpl @Inject constructor(
    private val messageAttachmentRemoteDataSource: MessageAttachmentRemoteDataSource
    // private val localMediaDataSource: LocalMediaDataSource, // 파일 업로드 전처리 등에 사용 가능
    // TODO: 필요한 Mapper 주입
) : MessageAttachmentRepository {

    override suspend fun getAttachmentsForMessage(messageId: String): CustomResult<List<MessageAttachment>, Unit> {
        // Create a path for the message using Firebase path conventions
        // Note: In a real implementation, we'd need to know if this is a DM or project message
        // For simplicity, we'll assume we have a method to resolve the full path
        val messagePath = resolveMessagePath(messageId)
        
        try {
            // Get attachments as a Flow, collect the latest value and wrap in CustomResult
            val attachmentsList = messageAttachmentRemoteDataSource.getAttachments(messagePath).map { dtoList ->
                dtoList.map { it.toDomain() }
            }
            // Since we're returning a non-Flow result, collect the first emission
            // In a real implementation, this might need more sophisticated handling
            return CustomResult.Success(attachmentsList.first())
        } catch (e: Exception) {
            return CustomResult.Failure(Unit)
        }
    }
    
    /**
     * Helper method to resolve a message ID to its full Firestore path
     * In a real implementation, this would need to determine if it's a DM or project message
     */
    private fun resolveMessagePath(messageId: String): String {
        // This is a simplified implementation
        // In a real app, we'd need to know the channel type and ID
        return "messages/$messageId"
    }

    override suspend fun uploadAttachment(channelId: String?, fileUri: String, type: MessageAttachmentType): CustomResult<MessageAttachment, Unit> {
        // For this implementation, we need to:
        // 1. Create a MessageAttachmentDTO with the provided details
        // 2. Add it to the appropriate location using the data source
        
        try {
            // Create a path for the channel - in a real implementation this would be more complex
            val channelPath = channelId?.let { "channels/$it/messages" } ?: return CustomResult.Failure(Unit)
            
            // Create a DTO for the attachment
            val attachmentDto = MessageAttachmentDTO(
                attachmentType = type.value,
                attachmentUrl = fileUri, // In a real implementation, this would be a proper URL after upload
                fileName = fileUri.substringAfterLast('/'),
                fileSize = null // We don't have this information from just the URI
            )
            
            // Add the attachment to the message
            // In a real implementation, we might need to first upload the file to storage
            val result = messageAttachmentRemoteDataSource.addAttachment(channelPath, attachmentDto)
            
            return if (result.isSuccess) {
                // Get the ID from the result and update our DTO
                val id = result.getOrNull() ?: ""
                val updatedDto = attachmentDto.copy(id = id)
                CustomResult.Success(updatedDto.toDomain())
            } else {
                CustomResult.Failure(Unit)
            }
        } catch (e: Exception) {
            return CustomResult.Failure(Unit)
        }
    }

    override suspend fun deleteAttachment(attachmentId: String): CustomResult<Unit, Unit> {
        // In a real implementation, we'd need to know which message this attachment belongs to
        // For simplicity, we're assuming we can derive this from the attachmentId or have it stored
        
        try {
            // This is a simplified implementation
            // In a real app, we'd need to know the message path
            val messagePath = "messages/${attachmentId.split('_').firstOrNull() ?: return CustomResult.Failure(Unit)}"
            
            val result = messageAttachmentRemoteDataSource.removeAttachment(messagePath, attachmentId)
            
            return if (result.isSuccess) {
                CustomResult.Success(Unit)
            } else {
                CustomResult.Failure(Unit)
            }
        } catch (e: Exception) {
            return CustomResult.Failure(Unit)
        }
    }
}

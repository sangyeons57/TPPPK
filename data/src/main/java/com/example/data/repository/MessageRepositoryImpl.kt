package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.MessageRemoteDataSource
import com.example.data.datasource.remote.MessageAttachmentRemoteDataSource // 첨부파일 업로드/다운로드용
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
import kotlinx.coroutines.flow.map
import javax.inject.Inject


class MessageRepositoryImpl @Inject constructor(
    private val messageRemoteDataSource: MessageRemoteDataSource,
    private val messageAttachmentRemoteDataSource: MessageAttachmentRemoteDataSource // 첨부파일 처리용
    // private val messageMapper: MessageMapper // 개별 매퍼 사용시
) : MessageRepository {

    override fun getMessagesStream(channelId: String, limit: Int): Flow<CustomResult<List<Message>, Unit>> {
        return messageRemoteDataSource.getMessagesStream(channelId, limit).map { result ->
            when (result) {
                is CustomResult.Success -> {
                    try {
                        val domainMessages = result.data.map { it.toDomain() }
                        CustomResult.Success(domainMessages)
                    } catch (e: Exception) {
                        CustomResult.Failure(Unit)
                    }
                }
                is CustomResult.Failure -> CustomResult.Failure(Unit)
                else -> CustomResult.Failure(Unit)
            }
        }
    }

    override suspend fun getPastMessages(channelId: String, startAfterMessageId: String?, limit: Int): CustomResult<List<Message>, Unit> {
        return try {
            val messagesResult = messageRemoteDataSource.getPastMessages(channelId, startAfterMessageId, limit)
            when (messagesResult) {
                is CustomResult.Success -> {
                    val domainMessages = messagesResult.data.map { it.toDomain() }
                    CustomResult.Success(domainMessages)
                }
                is CustomResult.Failure -> CustomResult.Failure(Unit)
                else -> CustomResult.Failure(Unit)
            }
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
    }

    override suspend fun sendMessage(
        channelId: String,
        content: String?,
        attachments: List<MessageAttachmentToSend>,
        senderId: String
    ): CustomResult<String, Unit> {
        return try {
            coroutineScope {
                // 1. 첨부파일 업로드 (병렬 처리)
                val uploadedAttachmentDtos = try {
                    attachments.map { attachmentToSend ->
                        async {
                            // MessageAttachmentRemoteDataSource를 사용하여 파일 업로드 후 URL 등 정보 받아오기
                            val uploadResult = messageAttachmentRemoteDataSource.uploadAttachment(
                                channelId = channelId,
                                fileName = attachmentToSend.fileName,
                                mimeType = attachmentToSend.mimeType,
                                uri = attachmentToSend.sourceUri // 또는 ByteArray
                            )
                            
                            if (uploadResult is CustomResult.Failure) {
                                throw Exception("Failed to upload attachment")
                            }
                            
                            val downloadUrl = (uploadResult as CustomResult.Success).data

                            MessageAttachmentDTO(
                                id = "", // DataSource에서 생성 또는 URL 자체가 ID 역할
                                messageId = "", // 메시지 생성 후 채워짐
                                type = mapMimeTypeToAttachmentType(attachmentToSend.mimeType), // "IMAGE", "FILE" 등
                                url = downloadUrl,
                                name = attachmentToSend.fileName,
                                size = 0L // 실제 파일 크기 (DataSource에서 설정)
                            )
                        }
                    }.awaitAll()
                } catch (e: Exception) {
                    return@coroutineScope CustomResult.Failure(Unit)
                }

                // 2. MessageDTO 생성
                val messageDto = MessageDTO(
                    // id는 Firestore에서 자동 생성
                    channelId = channelId,
                    senderId = senderId,
                    content = content,
                    attachments = uploadedAttachmentDtos,
                    createdAt = Timestamp.now(),
                    isEdited = false,
                    reactions = emptyMap() // 초기 리액션 없음
                    // senderName, senderProfileImageUrl은 DataSource에서 User 정보 조회 후 채울 수 있음 (비정규화)
                )

                // 3. 메시지 전송
                val sendResult = messageRemoteDataSource.sendMessage(messageDto)
                if (sendResult is CustomResult.Failure) {
                    return@coroutineScope CustomResult.Failure(Unit)
                }
                
                return@coroutineScope CustomResult.Success((sendResult as CustomResult.Success).data)
            }
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
    }

    // MIME 타입을 AttachmentType 문자열로 변환하는 헬퍼 함수 (예시)
    private fun mapMimeTypeToAttachmentType(mimeType: String): String {
        return when {
            mimeType.startsWith("image/") -> "IMAGE"
            mimeType.startsWith("video/") -> "VIDEO"
            mimeType.startsWith("audio/") -> "AUDIO"
            else -> "FILE"
        }
    }

    override suspend fun editMessage(
        channelId: String,
        messageId: String,
        newContent: String
    ): CustomResult<Unit, Unit> {
        return try {
            // 첨부파일 수정 로직은 복잡하므로 여기서는 텍스트 내용만 수정하는 것으로 가정
            // 실제로는 기존 DTO를 가져와서 content와 updatedAt만 변경하여 DataSource에 전달
            val result = messageRemoteDataSource.editMessage(channelId, messageId, newContent)
            when (result) {
                is CustomResult.Success -> CustomResult.Success(Unit)
                is CustomResult.Failure -> CustomResult.Failure(Unit)
                else -> CustomResult.Failure(Unit)
            }
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
    }

    override suspend fun deleteMessage(channelId: String, messageId: String): CustomResult<Unit, Unit> {
        return try {
            val result = messageRemoteDataSource.deleteMessage(channelId, messageId)
            when (result) {
                is CustomResult.Success -> CustomResult.Success(Unit)
                is CustomResult.Failure -> CustomResult.Failure(Unit)
                else -> CustomResult.Failure(Unit)
            }
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
    }

    override suspend fun addReaction(channelId: String, messageId: String, reactionEmoji: String, userId: String): CustomResult<Unit, Unit> {
        return try {
            val result = messageRemoteDataSource.addReaction(channelId, messageId, reactionEmoji, userId)
            when (result) {
                is CustomResult.Success -> CustomResult.Success(Unit)
                is CustomResult.Failure -> CustomResult.Failure(Unit)
                else -> CustomResult.Failure(Unit)
            }
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
    }

    override suspend fun removeReaction(channelId: String, messageId: String, reactionEmoji: String, userId: String): CustomResult<Unit, Unit> {
        return try {
            val result = messageRemoteDataSource.removeReaction(channelId, messageId, reactionEmoji, userId)
            when (result) {
                is CustomResult.Success -> CustomResult.Success(Unit)
                is CustomResult.Failure -> CustomResult.Failure(Unit)
                else -> CustomResult.Failure(Unit)
            }
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
    }

    override suspend fun getMessage(channelId: String, messageId: String): CustomResult<Message, Unit> {
        return try {
            val result = messageRemoteDataSource.getMessage(channelId, messageId)
            when (result) {
                is CustomResult.Success -> {
                    try {
                        val domainMessage = result.data.toDomain()
                        CustomResult.Success(domainMessage)
                    } catch (e: Exception) {
                        CustomResult.Failure(Unit)
                    }
                }
                is CustomResult.Failure -> CustomResult.Failure(Unit)
                else -> CustomResult.Failure(Unit)
            }
        } catch (e: Exception) {
            CustomResult.Failure(Unit)
        }
    }
}

package com.example.data._repository

import com.example.core_common.result.resultTry
import com.example.data.datasource._remote.MessageRemoteDataSource
import com.example.data.datasource._remote.MessageAttachmentRemoteDataSource // 첨부파일 업로드/다운로드용
import com.example.data.model._remote.MessageDTO
import com.example.data.model._remote.MessageAttachmentDTO
import com.example.data.model._remote.ReactionDTO
import com.example.data.model.mapper.toDomain // ChatMessageDTO -> ChatMessage
import com.example.data.model.mapper.toDto // ChatMessage -> ChatMessageDTO
import com.example.data.model.mapper.toDomain // MessageAttachmentDTO -> MessageAttachment
import com.example.data.model.mapper.toDto // MessageAttachment -> MessageAttachmentDTO
import com.example.domain.model.ChatMessage
import com.example.domain.model.MessageAttachment
import com.example.domain._repository.MessageAttachmentToSend
import com.example.domain._repository.MessageRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.Result

class MessageRepositoryImpl @Inject constructor(
    private val messageRemoteDataSource: MessageRemoteDataSource,
    private val messageAttachmentRemoteDataSource: MessageAttachmentRemoteDataSource // 첨부파일 처리용
    // private val messageMapper: MessageMapper // 개별 매퍼 사용시
) : MessageRepository {

    override fun getMessagesStream(channelId: String, limit: Int): Flow<Result<List<ChatMessage>>> {
        return messageRemoteDataSource.getMessagesStream(channelId, limit).map { result ->
            result.mapCatching { dtoList ->
                dtoList.map { it.toDomain() }
            }
        }
    }

    override suspend fun getPastMessages(channelId: String, startAfterMessageId: String?, limit: Int): Result<List<ChatMessage>> = resultTry {
        messageRemoteDataSource.getPastMessages(channelId, startAfterMessageId, limit).getOrThrow().map { it.toDomain() }
    }

    override suspend fun sendMessage(
        channelId: String,
        content: String?,
        attachments: List<MessageAttachmentToSend>,
        senderId: String
    ): Result<String> = resultTry {
        coroutineScope {
            // 1. 첨부파일 업로드 (병렬 처리)
            val uploadedAttachmentDtos = attachments.map { attachmentToSend ->
                async {
                    // MessageAttachmentRemoteDataSource를 사용하여 파일 업로드 후 URL 등 정보 받아오기
                    // 이 부분은 MessageAttachmentRemoteDataSource의 실제 함수 시그니처에 맞춰야 함
                    val downloadUrl = messageAttachmentRemoteDataSource.uploadAttachment(
                        channelId = channelId,
                        fileName = attachmentToSend.fileName,
                        mimeType = attachmentToSend.mimeType,
                        uri = attachmentToSend.sourceUri // 또는 ByteArray
                    ).getOrThrow()

                    MessageAttachmentDTO(
                        id = \
\, // DataSource에서 생성 또는 URL 자체가 ID 역할
                        messageId = \\, // 메시지 생성 후 채워짐
                        type = mapMimeTypeToAttachmentType(attachmentToSend.mimeType), // \IMAGE\, \FILE\ 등
                        url = downloadUrl,
                        name = attachmentToSend.fileName,
                        size = 0L // 실제 파일 크기 (DataSource에서 설정)
                    )
                }
            }.awaitAll()

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
            messageRemoteDataSource.sendMessage(messageDto).getOrThrow() // ID 반환 가정
        }
    }

    // MIME 타입을 AttachmentType 문자열로 변환하는 헬퍼 함수 (예시)
    private fun mapMimeTypeToAttachmentType(mimeType: String): String {
        return when {
            mimeType.startsWith(\image/\) -> \IMAGE\
            mimeType.startsWith(\video/\) -> \VIDEO\
            mimeType.startsWith(\audio/\) -> \AUDIO\
            else -> \FILE\
        }
    }

    override suspend fun editMessage(
        channelId: String,
        messageId: String,
        newContent: String
    ): Result<Unit> = resultTry {
        // 첨부파일 수정 로직은 복잡하므로 여기서는 텍스트 내용만 수정하는 것으로 가정
        // 실제로는 기존 DTO를 가져와서 content와 updatedAt만 변경하여 DataSource에 전달
        messageRemoteDataSource.editMessage(channelId, messageId, newContent).getOrThrow()
    }

    override suspend fun deleteMessage(channelId: String, messageId: String): Result<Unit> = resultTry {
        messageRemoteDataSource.deleteMessage(channelId, messageId).getOrThrow()
    }

    override suspend fun addReaction(channelId: String, messageId: String, reactionEmoji: String, userId: String): Result<Unit> = resultTry {
        messageRemoteDataSource.addReaction(channelId, messageId, reactionEmoji, userId).getOrThrow()
    }

    override suspend fun removeReaction(channelId: String, messageId: String, reactionEmoji: String, userId: String): Result<Unit> = resultTry {
        messageRemoteDataSource.removeReaction(channelId, messageId, reactionEmoji, userId).getOrThrow()
    }

    override suspend fun getMessage(channelId: String, messageId: String): Result<ChatMessage> = resultTry {
        messageRemoteDataSource.getMessage(channelId, messageId).getOrThrow().toDomain()
    }
}

package com.example.data.model.mapper

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.util.DateTimeUtil
import com.example.data.model.local.chat.ChatMessageEntity
import com.example.data.model.remote.chat.ChatMessageDto
import com.example.data.model.remote.media.MediaImageDto
import com.example.domain.model.AttachmentType
import com.example.domain.model.ChatMessage
import com.example.domain.model.MessageAttachment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant
import javax.inject.Inject

/**
 * ChatMessageDto를 완전한 ChatMessage 도메인 모델로 변환합니다.
 * 기본적인 필드 매핑 후, 시간(timestamp, updatedAt) 및 첨부파일(attachments) 필드를 변환하여 채웁니다.
 * @param dateTimeUtil 시간 변환을 위한 유틸리티 클래스 인스턴스
 * @return 완전하게 매핑된 ChatMessage 도메인 모델
 */
fun ChatMessageDto.toDomainModelWithTime(): ChatMessage {
    val basicDomainModel = this.toBasicDomainModel()
    val convertedTimestamp = DateTimeUtil.firebaseTimestampToInstant(this.timestamp) // Already Instant in DTO if directly set, but Timestamp from Firestore
    val convertedUpdatedAt = this.updatedAt?.let { DateTimeUtil.firebaseTimestampToInstant(it) }

    // Attachments: List<MediaImageDto>? -> List<MessageAttachment>
    val convertedAttachments = this.attachments?.mapNotNull { mediaImageDto ->
        MessageAttachment(
            id = mediaImageDto.id,
            // Attempt to convert String type from DTO to AttachmentType enum.
            // Fallback to UNKNOWN if conversion fails.
            type = AttachmentType.fromString(mediaImageDto.type),
            url = mediaImageDto.url,
            fileName = mediaImageDto.fileName,
            size = mediaImageDto.size,
            mimeType = mediaImageDto.mimeType,
            thumbnailUrl = mediaImageDto.thumbnailUrl
        )
    } ?: emptyList()

    return basicDomainModel.copy(
        timestamp = convertedTimestamp ?: Instant.EPOCH, // Ensure timestamp is not null
        updatedAt = convertedUpdatedAt,
        attachments = convertedAttachments
    )
}

/**
 * ChatMessage 도메인 모델을 완전한 ChatMessageDto로 변환합니다.
 * 기본적인 필드 매핑 후, 시간(timestamp, updatedAt) 및 첨부파일(attachments) 필드를 변환하여 채웁니다.
 * @param dateTimeUtil 시간 변환을 위한 유틸리티 클래스 인스턴스
 * @return 완전하게 매핑된 ChatMessageDto
 */
fun ChatMessage.toDtoWithTime(): ChatMessageDto {
    val basicDto = ChatMessageDto.fromBasicDomainModel(this)
    val convertedTimestamp = DateTimeUtil.instantToFirebaseTimestamp(this.timestamp)
    val convertedUpdatedAt = this.updatedAt?.let { DateTimeUtil.instantToFirebaseTimestamp(it) }

    // Attachments: List<MessageAttachment> -> List<MediaImageDto>?
    val convertedAttachmentsDto = if (this.attachments.isNotEmpty()) {
        this.attachments.map { messageAttachment ->
            MediaImageDto(
                id = messageAttachment.id,
                url = messageAttachment.url,
                fileName = messageAttachment.fileName,
                type = messageAttachment.type.typeString, // Enum to typeString
                path = "", // Not available in MessageAttachment
                mimeType = messageAttachment.mimeType ?: "",
                size = messageAttachment.size ?: 0L,
                thumbnailUrl = messageAttachment.thumbnailUrl,
                dateAdded = 0L // Not available in MessageAttachment, default to epoch
            )
        }
    } else {
        null
    }

    return basicDto.copy(
        timestamp = convertedTimestamp ?: Timestamp.now(), // Ensure timestamp is not null
        updatedAt = convertedUpdatedAt,
        attachments = convertedAttachmentsDto
    )
}

/**
 * Entity <-> Domain 확장 함수 정의 시작
 */
fun ChatMessageEntity.toDomainModel(): ChatMessage {
    val attachmentUrls = this.attachmentImageUrls?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    return ChatMessage(
        id = this.chatId,
        channelId = this.channelId,
        senderId = this.userId,
        senderName = this.userName,
        senderProfileUrl = this.userProfileUrl,
        text = this.message,
        timestamp = Instant.ofEpochMilli(this.sentAt), // Long to Instant
        isEdited = this.isModified,
        // isDeleted, updatedAt, reactions, metadata 등은 Entity에 없으므로 도메인 모델 기본값 사용
        attachments = attachmentUrls.map { url ->
            MessageAttachment(
                id = "${this.chatId}_${url.hashCode()}", // Entity에는 개별 attachment ID가 없으므로 생성
                type = AttachmentType.IMAGE, // Entity는 IMAGE 타입만 저장한다고 가정
                url = url,
                fileName = url.substringAfterLast("/"),
                thumbnailUrl = url // Entity는 썸네일 URL을 별도로 저장하지 않는다고 가정
            )
        }
    )
}

fun ChatMessage.toEntity(channelType: String): ChatMessageEntity {
    val attachmentImageUrls = this.attachments
        .filter { it.type == AttachmentType.IMAGE } // IMAGE 타입의 URL만 추출
        .map { it.url }
        .joinToString(",")
    
    return ChatMessageEntity(
        chatId = this.id,
        channelId = this.channelId,
        channelType = channelType,
        userId = this.senderId,
        userName = this.senderName,
        userProfileUrl = this.senderProfileUrl,
        message = this.text,
        sentAt = this.timestamp.toEpochMilli(), // Instant to Long
        isModified = this.isEdited,
        attachmentImageUrls = if (attachmentImageUrls.isNotEmpty()) attachmentImageUrls else null
        // ChatMessageEntity는 isDeleted, updatedAt 등을 저장하지 않음
    )
}
/**
 * Entity <-> Domain 확장 함수 정의 끝
 */

/**
 * 채팅 메시지 모델 매핑을 위한 유틸리티 클래스
 * Domain, DTO, Entity 간의 변환을 담당합니다.
 */
class ChatMessageMapper @Inject constructor(private val dateTimeUtil: DateTimeUtil) {

    /**
     * Firestore DocumentSnapshot을 Domain 모델 ChatMessage로 변환합니다.
     * @param doc Firestore DocumentSnapshot
     * @return ChatMessage 도메인 모델
     */
    fun fromSnapshotToDomain(doc: DocumentSnapshot): ChatMessage? { // Return nullable if doc.data is null
        val data = doc.data ?: return null
        // Use ChatMessageDto.fromMap to parse the document data
        val dto = ChatMessageDto.fromMap(data, doc.id)
        // Use the extension function to convert DTO to Domain model
        return dto.toDomainModelWithTime()
    }

    /**
     * Domain 모델 ChatMessage를 Firestore에 저장하기 위한 Map으로 변환합니다.
     * ChatMessageDto.toMap()을 활용합니다.
     * @param chatMessage 변환할 ChatMessage 도메인 모델
     * @return Firestore에 저장 가능한 Map<String, Any?>
     */
    fun toFirestoreMap(chatMessage: ChatMessage): Map<String, Any?> {
        // Convert domain model to DTO first using the extension function
        val dto = chatMessage.toDtoWithTime()
        // Then use DTO's toMap() method
        return dto.toMap()
    }

    /**
     * @Deprecated Use ChatMessageDto.toDomainModelWithTime(dateTimeUtil) extension function instead.
     */
    @Deprecated("Use ChatMessageDto.toDomainModelWithTime(dateTimeUtil) extension function instead.")
    fun mapDtoToDomain(dto: ChatMessageDto): ChatMessage {
        // This is the old direct DTO -> Domain mapping, now superseded by the extension function.
        // For completeness, updating its attachment logic similarly, though it's deprecated.
        val convertedAttachments = dto.attachments?.mapNotNull { mediaImageDto ->
            MessageAttachment(
                id = mediaImageDto.id,
                type = AttachmentType.fromString(mediaImageDto.type),
                url = mediaImageDto.url,
                fileName = mediaImageDto.fileName,
                size = mediaImageDto.size,
                mimeType = mediaImageDto.mimeType,
                thumbnailUrl = mediaImageDto.thumbnailUrl
            )
        } ?: emptyList()

        return ChatMessage(
            id = dto.id,
            channelId = dto.channelId,
            senderId = dto.senderId,
            senderName = dto.senderName,
            senderProfileUrl = dto.senderProfileUrl,
            text = dto.text,
            timestamp = dateTimeUtil.firebaseTimestampToInstant(dto.timestamp) ?: Instant.EPOCH,
            isEdited = dto.isEdited,
            isDeleted = dto.isDeleted,
            updatedAt = dto.updatedAt?.let { dateTimeUtil.firebaseTimestampToInstant(it) },
            attachments = convertedAttachments,
            replyToMessageId = dto.replyToMessageId,
            reactions = dto.reactions ?: emptyMap(),
            metadata = dto.metadata
        )
    }
}

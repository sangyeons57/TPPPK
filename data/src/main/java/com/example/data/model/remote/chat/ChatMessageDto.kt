package com.example.data.model.remote.chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.example.core_common.constants.FirestoreConstants // FirestoreConstants 임포트 추가
import com.example.data.model.remote.media.MediaImageDto // Import MediaImageDto
import com.example.domain.model.ChatMessage
import java.time.Instant // Instant 임포트

/**
 * 채팅 메시지 데이터 전송 객체
 * Firebase Firestore와 데이터를 주고받기 위한 모델
 * 
 * @property id 메시지의 고유 ID (Firestore 문서 ID와 동일하게 사용)
 * @property channelId 메시지가 속한 채널 ID
 * @property senderId 메시지 작성자의 사용자 ID // Firestore 필드명은 userId 였으나 senderId로 통일
 * @property senderName 메시지 작성자 이름 // Firestore 필드명은 userName이었으나 senderName으로 통일
 * @property senderProfileUrl 메시지 작성자 프로필 이미지 URL (nullable) // Firestore 필드명은 userProfileUrl이었으나 senderProfileUrl로 통일
 * @property text 메시지 내용 // Firestore 필드명은 message 였으나 text로 통일
 * @property timestamp 메시지 전송 시간 (Firestore Timestamp) // Firestore 필드명은 sentAt (Long) 이었으나 timestamp (Timestamp)로 변경
 * @property attachments 첨부 파일 목록 (MediaImageDto 객체 리스트, nullable)
 * @property replyToMessageId 답장할 메시지의 ID (nullable)
 * @property reactions 메시지 반응 (nullable)
 * @property metadata 추가 메타데이터 (nullable)
 * @property isEdited 메시지 수정 여부 // Firestore 필드명은 isModified 였으나 isEdited로 통일
 * @property isDeleted 메시지 삭제 여부 (논리적 삭제 플래그)
 * @property updatedAt 메시지 마지막 수정 시간 (nullable)
 */
data class ChatMessageDto(
    @DocumentId // Firestore 문서 ID임을 명시
    val id: String = "", 

    @PropertyName(FirestoreConstants.MessageFields.CHANNEL_ID)
    val channelId: String = "",

    @PropertyName(FirestoreConstants.MessageFields.SENDER_ID)
    val senderId: String = "", 

    @PropertyName(FirestoreConstants.MessageFields.SENDER_NAME)
    val senderName: String = "", 

    @PropertyName(FirestoreConstants.MessageFields.SENDER_PROFILE_URL)
    val senderProfileUrl: String? = null,

    @PropertyName(FirestoreConstants.MessageFields.MESSAGE)
    val text: String = "", 

    @PropertyName(FirestoreConstants.MessageFields.SENT_AT)
    val timestamp: Timestamp = Timestamp.now(), 

    @PropertyName(FirestoreConstants.MessageFields.ATTACHMENTS)
    val attachments: List<MediaImageDto>? = null, 

    @PropertyName(FirestoreConstants.MessageFields.REPLY_TO_MESSAGE_ID)
    val replyToMessageId: String? = null, 

    @PropertyName(FirestoreConstants.MessageFields.REACTIONS)
    val reactions: Map<String, List<String>>? = null, 

    @PropertyName(FirestoreConstants.MessageFields.METADATA)
    val metadata: Map<String, Any>? = null,

    @PropertyName(FirestoreConstants.MessageFields.IS_EDITED)
    val isEdited: Boolean = false,

    @PropertyName(FirestoreConstants.MessageFields.IS_DELETED)
    val isDeleted: Boolean = false,

    @PropertyName(FirestoreConstants.MessageFields.UPDATED_AT)
    val updatedAt: Timestamp? = null
) {
    /**
     * 이 DTO를 기본적인 ChatMessage 도메인 모델로 변환합니다.
     * 시간(timestamp, updatedAt) 및 첨부파일(attachments) 필드는 변환 로직의 복잡성으로 인해
     * 여기서는 기본값 또는 임시값으로 설정되며, 추후 확장 함수에서 완전한 변환을 처리합니다.
     */
    fun toBasicDomainModel(): ChatMessage {
        return ChatMessage(
            id = this.id,
            channelId = this.channelId,
            senderId = this.senderId,
            senderName = this.senderName,
            senderProfileUrl = this.senderProfileUrl,
            text = this.text,
            timestamp = Instant.EPOCH, // 임시값, 추후 확장 함수에서 DateTimeUtil로 변환 필요
            updatedAt = null, // 임시값, 추후 확장 함수에서 DateTimeUtil로 변환 필요
            reactions = this.reactions ?: emptyMap(),
            attachments = emptyList(), // 임시값, 추후 확장 함수에서 List<MediaImageDto> -> List<MessageAttachment> 변환 필요
            replyToMessageId = this.replyToMessageId,
            isEdited = this.isEdited,
            isDeleted = this.isDeleted,
            metadata = this.metadata
        )
    }

    /**
     * Firestore 문서에서 사용할 수 있는 Map으로 변환합니다.
     * @DocumentId 필드는 toMap() 결과에 포함되지 않으므로 직접 추가할 필요 없습니다.
     * @PropertyName 어노테이션으로 인해 Firestore 직렬화/역직렬화 시 자동으로 필드 이름이 매핑됩니다.
     * 
     * @return Firestore 문서로 저장 가능한 Map (id는 제외됨)
     */
    fun toMap(): Map<String, Any?> {
        // @PropertyName 어노테이션 덕분에 Firestore 라이브러리가 직렬화 시 자동으로 올바른 필드 이름을 사용합니다.
        // 따라서 여기서는 각 프로퍼티 이름(id 제외)과 값을 그대로 사용하면 됩니다.
        // Firestore는 data class를 Map으로 변환할 때 @DocumentId 필드를 제외합니다.
        return mapOf(
            FirestoreConstants.MessageFields.CHANNEL_ID to channelId,
            FirestoreConstants.MessageFields.SENDER_ID to senderId,
            FirestoreConstants.MessageFields.SENDER_NAME to senderName,
            FirestoreConstants.MessageFields.SENDER_PROFILE_URL to senderProfileUrl,
            FirestoreConstants.MessageFields.MESSAGE to text,
            FirestoreConstants.MessageFields.SENT_AT to timestamp,
            FirestoreConstants.MessageFields.ATTACHMENTS to attachments?.map { it.toAttachmentMap() },
            FirestoreConstants.MessageFields.REPLY_TO_MESSAGE_ID to replyToMessageId,
            FirestoreConstants.MessageFields.REACTIONS to reactions,
            FirestoreConstants.MessageFields.METADATA to metadata,
            FirestoreConstants.MessageFields.IS_EDITED to isEdited,
            FirestoreConstants.MessageFields.IS_DELETED to isDeleted,
            FirestoreConstants.MessageFields.UPDATED_AT to updatedAt
        ).filterValues { it != null } // Null 값은 Firestore에 저장하지 않도록 필터링 (선택적)
    }
    
    companion object {
        /**
         * ChatMessage 도메인 모델로부터 기본적인 ChatMessageDto 객체를 생성합니다.
         * 시간(timestamp, updatedAt) 및 첨부파일(attachments) 필드는 변환 로직의 복잡성으로 인해
         * 여기서는 DTO의 기본값 또는 임시값으로 설정되며, 추후 확장 함수에서 완전한 변환을 처리합니다.
         */
        fun fromBasicDomainModel(domain: ChatMessage): ChatMessageDto {
            return ChatMessageDto(
                id = domain.id,
                channelId = domain.channelId,
                senderId = domain.senderId,
                senderName = domain.senderName,
                senderProfileUrl = domain.senderProfileUrl,
                text = domain.text,
                // timestamp, updatedAt, attachments는 DTO의 기본값(Timestamp.now(), null, null)을 사용하거나
                // 명시적으로 null/기본값으로 설정합니다. 추후 확장 함수에서 정확히 변환합니다.
                timestamp = Timestamp.now(), // DTO 기본값 활용 또는 명시적 기본값
                updatedAt = null, // DTO 기본값 활용 또는 명시적 null
                attachments = null, // DTO 기본값 활용. List<MediaImageDto> -> List<MediaImageDto> 변환은 확장 함수에서.
                reactions = domain.reactions,
                replyToMessageId = domain.replyToMessageId,
                isEdited = domain.isEdited,
                isDeleted = domain.isDeleted,
                metadata = domain.metadata
            )
        }

        /**
         * Firestore 문서 데이터에서 ChatMessageDto 객체를 생성합니다.
         * @DocumentId 필드는 Firestore가 자동으로 채워주므로 data Map에는 포함되지 않습니다.
         * @PropertyName 어노테이션으로 인해 Firestore가 역직렬화 시 자동으로 필드를 매핑합니다.
         * 
         * @param data Firestore 문서 데이터 (Map<String, Any?>)
         * @param documentId Firestore 문서 ID (Firestore가 DTO 생성 시 @DocumentId 필드에 자동으로 채워줌)
         * @return 생성된 ChatMessageDto 객체 (또는 null). Firestore의 toObject() 메서드 사용을 권장.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(data: Map<String, Any?>, documentId: String): ChatMessageDto {
            // Firestore의 toObject(ChatMessageDto::class.java) 사용을 권장합니다.
            // 이 fromMap은 Firestore가 자동으로 @DocumentId 필드를 채워주기 때문에
            // 사실상 Firestore 라이브러리 내부 동작을 수동으로 모방하는 것에 가깝습니다.
            // 직접 이 함수를 호출하기보다는 Firestore의 .toObject()를 사용하세요.
            val rawAttachments = data[FirestoreConstants.MessageFields.ATTACHMENTS] as? List<Map<String, Any?>>
            return ChatMessageDto(
                id = documentId, // @DocumentId 필드는 Firestore가 채워주지만, 수동 매핑 시에는 명시적 할당.
                channelId = data[FirestoreConstants.MessageFields.CHANNEL_ID] as? String ?: "",
                senderId = data[FirestoreConstants.MessageFields.SENDER_ID] as? String ?: "",
                senderName = data[FirestoreConstants.MessageFields.SENDER_NAME] as? String ?: "",
                senderProfileUrl = data[FirestoreConstants.MessageFields.SENDER_PROFILE_URL] as? String,
                text = data[FirestoreConstants.MessageFields.MESSAGE] as? String ?: "",
                timestamp = data[FirestoreConstants.MessageFields.SENT_AT] as? Timestamp ?: Timestamp.now(),
                attachments = rawAttachments?.map { MediaImageDto.fromAttachmentMap(it, it[FirestoreConstants.MessageFields.AttachmentMapKeys.ID] as? String ?: "") },
                replyToMessageId = data[FirestoreConstants.MessageFields.REPLY_TO_MESSAGE_ID] as? String,
                reactions = data[FirestoreConstants.MessageFields.REACTIONS] as? Map<String, List<String>>,
                metadata = data[FirestoreConstants.MessageFields.METADATA] as? Map<String, Any>,
                isEdited = data[FirestoreConstants.MessageFields.IS_EDITED] as? Boolean ?: false,
                isDeleted = data[FirestoreConstants.MessageFields.IS_DELETED] as? Boolean ?: false,
                updatedAt = data[FirestoreConstants.MessageFields.UPDATED_AT] as? Timestamp
            )
        }
    }
} 
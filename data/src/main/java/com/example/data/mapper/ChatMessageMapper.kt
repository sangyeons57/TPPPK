package com.example.data.mapper

import com.example.data.model.local.ChatMessageEntity
import com.example.domain.model.base.Message
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MessageContent
import com.example.domain.model.vo.message.MessageIsDeleted
import com.example.domain.model.vo.message.MentionInfo
import com.example.domain.model.vo.MentionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant

/**
 * ChatMessageEntity와 Message 도메인 모델 간의 변환을 담당하는 매퍼
 * JSON 직렬화/역직렬화를 통해 복잡한 객체들을 로컬 저장소에 효율적으로 저장
 */
object ChatMessageMapper {

    private val gson = Gson()

    /**
     * Message 도메인 모델을 ChatMessageEntity로 변환
     * @param message 변환할 Message 도메인 모델
     * @param channelId 채널 ID
     * @return ChatMessageEntity
     */
    fun toEntity(message: Message, channelId: String): ChatMessageEntity {
        return ChatMessageEntity(
            messageId = message.id.value,
            channelId = channelId,
            userId = message.senderId.value,
            content = message.content.value,
            createdAt = message.createdAt,
            updatedAt = message.updatedAt,
            replyToMessageId = message.replyToMessageId?.value,
            mentions = serializeMentions(message.mentions),
            attachments = null, // TODO: 첨부파일 직렬화 추가 예정
            isDeleted = message.isDeleted.value,
            localSavedAt = Instant.now()
        )
    }

    /**
     * ChatMessageEntity를 Message 도메인 모델로 변환
     * @param entity 변환할 ChatMessageEntity
     * @return Message 도메인 모델
     */
    fun toDomain(entity: ChatMessageEntity): Message {
        return Message.fromDataSource(
            id = DocumentId(entity.messageId),
            senderId = UserId(entity.userId),
            content = MessageContent(entity.content),
            replyToMessageId = entity.replyToMessageId?.let { DocumentId(it) },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isDeleted = MessageIsDeleted(entity.isDeleted),
            mentions = deserializeMentions(entity.mentions)
        )
    }

    /**
     * Message 도메인 모델 리스트를 ChatMessageEntity 리스트로 변환
     * @param messages 변환할 Message 도메인 모델 리스트
     * @param channelId 채널 ID
     * @return ChatMessageEntity 리스트
     */
    fun toEntityList(messages: List<Message>, channelId: String): List<ChatMessageEntity> {
        return messages.map { toEntity(it, channelId) }
    }

    /**
     * ChatMessageEntity 리스트를 Message 도메인 모델 리스트로 변환
     * @param entities 변환할 ChatMessageEntity 리스트
     * @return Message 도메인 모델 리스트
     */
    fun toDomainList(entities: List<ChatMessageEntity>): List<Message> {
        return entities.map { toDomain(it) }
    }

    /**
     * MentionInfo 리스트를 JSON 문자열로 직렬화
     * @param mentions 직렬화할 MentionInfo 리스트
     * @return JSON 문자열 (빈 리스트면 null)
     */
    private fun serializeMentions(mentions: List<MentionInfo>): String? {
        if (mentions.isEmpty()) return null

        val mentionMaps = mentions.map { mention ->
            mapOf(
                "type" to mention.type.name,
                "id" to mention.id,
                "displayName" to mention.displayName
            )
        }

        return gson.toJson(mentionMaps)
    }

    /**
     * JSON 문자열을 MentionInfo 리스트로 역직렬화
     * @param mentionsJson JSON 문자열
     * @return MentionInfo 리스트 (null이면 빈 리스트)
     */
    private fun deserializeMentions(mentionsJson: String?): List<MentionInfo> {
        if (mentionsJson.isNullOrBlank()) return emptyList()

        return try {
            val type = object : TypeToken<List<Map<String, String>>>() {}.type
            val mentionMaps: List<Map<String, String>> = gson.fromJson(mentionsJson, type)

            mentionMaps.mapNotNull { map ->
                try {
                    val mentionType = MentionType.valueOf(map["type"] ?: "USER")
                    val id = map["id"] ?: return@mapNotNull null
                    val displayName = map["displayName"] ?: return@mapNotNull null

                    MentionInfo(mentionType, id, displayName)
                } catch (e: Exception) {
                    // 잘못된 형식의 멘션 데이터는 무시
                    null
                }
            }
        } catch (e: Exception) {
            // JSON 파싱 실패 시 빈 리스트 반환
            emptyList()
        }
    }
}
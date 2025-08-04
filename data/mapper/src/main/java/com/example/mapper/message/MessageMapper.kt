package com.example.mapper.message

import android.util.Log
import com.example.core_common.constants.Constants
import com.example.data_model.local.MessageEntity
import com.example.data_model.remote.MessageDTO
import com.example.domain.model.base.Message
import com.example.domain.model.vo.ChannelId
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.MentionType
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MentionInfo
import com.example.domain.model.vo.message.MessageContent
import com.example.domain.model.vo.message.MessageIsDeleted
import com.example.mapper.DtoMapper
import com.example.mapper.Mapper
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Message 관련 Entity, Domain, DTO 간의 매핑을 담당하는 Mapper
 * JSON 의존성 없이 순수한 객체 변환만 담당
 */
@Singleton
class MessageMapper @Inject constructor() : Mapper<MessageEntity, Message, MessageDTO>,
    DtoMapper<Message, MessageDTO> {

    override fun entityToDomain(entity: MessageEntity): Message {
        return Message.fromDataSource(
            id = DocumentId(entity.id),
            senderId = UserId(entity.senderId),
            content = MessageContent(entity.content),
            replyToMessageId = entity.replyToMessageId?.let { DocumentId(it) },
            createdAt = Instant.ofEpochMilli(entity.createdAt),
            updatedAt = Instant.ofEpochMilli(entity.updatedAt),
            isDeleted = MessageIsDeleted.fromBoolean(entity.isDeleted),
            mentions = emptyList(), // JSON 파싱은 별도 처리
            channelId = ChannelId(entity.channelId)
        )
    }

    override fun domainToEntity(domain: Message): MessageEntity {
        return MessageEntity(
            id = domain.id.value,
            channelId = domain.channelId.value,
            senderId = domain.senderId.value,
            content = domain.content.value,
            replyToMessageId = domain.replyToMessageId?.value,
            isDeleted = domain.isDeleted.value,
            mentions = "[]", // JSON 직렬화는 별도 처리
            createdAt = domain.createdAt.toEpochMilli(),
            updatedAt = domain.updatedAt.toEpochMilli(),
            syncStatus = ""
        )
    }

    override fun dtoToDomain(dto: MessageDTO): Message {
        Log.d("MessageMapper", "🔄 DTO → 도메인 변환 시작: id=${dto.id}")

        val domainMentions = dto.mentions.mapNotNull { map ->
            try {
                val type = MentionType.valueOf(map[MentionInfo.KEY_TYPE] as String)
                val id = map[MentionInfo.KEY_ID] as String
                val displayName = map[MentionInfo.KEY_DISPLAY_NAME] as String
                MentionInfo(type, id, displayName)
            } catch (e: Exception) {
                Log.w("MessageMapper", "⚠️ 멘션 변환 실패: ${map}", e)
                null
            }
        }
        Log.d("MessageMapper", "📝 멘션 변환 완료: ${domainMentions.size}개")

        // channelId가 비어있으면 기본값 사용
        val channelId = if (dto.channelId.isBlank()) {
            Log.w(
                "MessageMapper",
                "⚠️ channelId가 비어있음. 기본값 사용: ${Constants.Chat.UNKNOWN_CHANNEL_ID}"
            )
            Constants.Chat.UNKNOWN_CHANNEL_ID
        } else {
            Log.d("MessageMapper", "✅ channelId 정상: ${dto.channelId}")
            dto.channelId
        }

        return try {
            val message = Message.fromDataSource(
                id = DocumentId(dto.id),
                senderId = UserId(dto.senderId),
                content = MessageContent(dto.content),
                createdAt = dto.createdAt?.toInstant() ?: Instant.now(),
                updatedAt = dto.updatedAt?.toInstant() ?: Instant.now(),
                replyToMessageId = dto.replyToMessageId?.let { DocumentId(it) },
                isDeleted = MessageIsDeleted.fromBoolean(dto.isDeleted),
                mentions = domainMentions,
                channelId = ChannelId(channelId)
            )
            Log.d(
                "MessageMapper",
                "✅ 도메인 변환 성공: id=${message.id.value}, channelId=${message.channelId.value}"
            )
            message
        } catch (e: Exception) {
            Log.e("MessageMapper", "❌ 도메인 변환 실패: id=${dto.id}, channelId=$channelId", e)
            throw e
        }
    }

    override fun domainToDto(domain: Message): MessageDTO {
        val dtoMentions = domain.mentions.map { mention ->
            mapOf(
                MentionInfo.KEY_TYPE to mention.type.name,
                MentionInfo.KEY_ID to mention.id,
                MentionInfo.KEY_DISPLAY_NAME to mention.displayName
            )
        }

        return MessageDTO(
            id = domain.id.value,
            channelId = domain.channelId.value,
            senderId = domain.senderId.value,
            content = domain.content.value,
            createdAt = null, // ServerTimestamp가 처리
            updatedAt = null, // ServerTimestamp가 처리
            replyToMessageId = domain.replyToMessageId?.value,
            isDeleted = domain.isDeleted.value,
            mentions = dtoMentions
        )
    }
}
/** SSOT  작업을 하면서 해당 내용을 일시적으로 주석처리 추후 제거하거나 개편될수잇음
@Singleton
class MessageMapper @Inject constructor() : Mapper<MessageEntity, Message, MessageDTO>,
DtoMapper<Message, MessageDTO> {

    override fun entityToDomain(entity: MessageEntity): Message {
        return Message.fromDataSource(
            id = DocumentId(entity.id),
            senderId = UserId(entity.senderId),
            content = MessageContent(entity.content),
            replyToMessageId = entity.replyToMessageId?.let { DocumentId(it) },
            createdAt = Instant.ofEpochMilli(entity.createdAt),
            updatedAt = Instant.ofEpochMilli(entity.updatedAt),
            isDeleted = if (entity.isDeleted) MessageIsDeleted.TRUE else MessageIsDeleted.FALSE,
mentions = emptyList(), // TODO: JSON 파싱 로직은 JsonConverter에서 처리
channelId = ChannelId(entity.channelId)
        )
    }

    override fun domainToEntity(domain: Message): MessageEntity {
        return MessageEntity(
            id = domain.id.value,
channelId = domain.channelId.value,
            senderId = domain.senderId.value,
            content = domain.content.value,
            replyToMessageId = domain.replyToMessageId?.value,
            isDeleted = domain.isDeleted.value,
            mentions = "[]", // TODO: JSON 직렬화는 JsonConverter에서 처리
            createdAt = domain.createdAt.toEpochMilli(),
            updatedAt = domain.updatedAt.toEpochMilli(),
            serverVersion = null,
            serverUpdatedAt = null,
            syncStatus = SyncStatus.DEFAULT.name,

        )
    }

    override fun dtoToDomain(dto: MessageDTO): Message {
        val domainMentions = dto.mentions.mapNotNull { map ->
            try {
                val type = MentionType.valueOf(map[MentionInfo.KEY_TYPE] as String)
                val id = map[MentionInfo.KEY_ID] as String
                val displayName = map[MentionInfo.KEY_DISPLAY_NAME] as String
                MentionInfo(type, id, displayName)
            } catch (e: Exception) {
                null
            }
        }

        return Message.fromDataSource(
            id = DocumentId(dto.id),
            senderId = UserId(dto.senderId),
            content = MessageContent(dto.content),
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant(),
            replyToMessageId = dto.replyToMessageId?.let { DocumentId(it) },
            isDeleted = MessageIsDeleted(dto.isDeleted),
mentions = domainMentions,
channelId = ChannelId(dto.channelId)
        )
    }

    override fun domainToDto(domain: Message): MessageDTO {
        val dtoMentions = domain.mentions.map { mention ->
            mapOf(
                MentionInfo.KEY_TYPE to mention.type.name,
                MentionInfo.KEY_ID to mention.id,
                MentionInfo.KEY_DISPLAY_NAME to mention.displayName
            )
        }

        return MessageDTO(
            id = domain.id.value,
channelId = domain.channelId.value,
            senderId = domain.senderId.value,
            content = domain.content.value,
            createdAt = null, // ServerTimestamp가 처리
            updatedAt = null, // ServerTimestamp가 처리
            replyToMessageId = domain.replyToMessageId?.value,
            isDeleted = domain.isDeleted.value,
            mentions = dtoMentions
        )
    }

    /**
     * Entity를 Domain으로 변환할 때 추가 sync 정보 포함
     */
    fun entityToDomainWithSync(
        entity: MessageEntity,
        serverVersion: Long? = null,
        serverUpdatedAt: Long? = null,
        syncStatus: SyncStatus = SyncStatus.DEFAULT,
        deleted: Boolean = false
    ): Message {
        return entityToDomain(entity)
    }

    /**
     * Domain을 Entity로 변환할 때 sync 정보 포함
     */
    fun domainToEntityWithSync(
        domain: Message,
        serverVersion: Long? = null,
        serverUpdatedAt: Long? = null,
syncStatus: SyncStatus = SyncStatus.DEFAULT
    ): MessageEntity {
        return MessageEntity(
            id = domain.id.value,
channelId = domain.channelId.value,
            senderId = domain.senderId.value,
            content = domain.content.value,
            replyToMessageId = domain.replyToMessageId?.value,
            isDeleted = domain.isDeleted.value,
            mentions = "[]", // TODO: JSON 직렬화는 JsonConverter에서 처리
            createdAt = domain.createdAt.toEpochMilli(),
            updatedAt = domain.updatedAt.toEpochMilli(),
            serverVersion = serverVersion,
            serverUpdatedAt = serverUpdatedAt,
syncStatus = syncStatus.name
        )
    }
}        */

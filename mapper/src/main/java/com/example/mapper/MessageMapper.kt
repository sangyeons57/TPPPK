package com.example.mapper

import com.example.data_core.model.remote.MessageDTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Message
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.MentionType
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MentionInfo
import com.example.domain.model.vo.message.MessageContent
import com.example.domain.model.vo.message.MessageIsDeleted
import com.example.mapper.base.BaseMapper
import java.util.Date

interface MessageMapper : BaseMapper<Message, MessageDTO>

class MessageMapperImpl : MessageMapper {
    override fun toDomain(dto: MessageDTO): Message {
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
            mentions = domainMentions
        )
    }

    override fun toDto(domain: Message): MessageDTO {
        val dtoMentions = domain.mentions.map { mention ->
            mapOf(
                MentionInfo.KEY_TYPE to mention.type.name,
                MentionInfo.KEY_ID to mention.id,
                MentionInfo.KEY_DISPLAY_NAME to mention.displayName
            )
        }

        return MessageDTO(
            id = domain.id.value,
            senderId = domain.senderId.value,
            content = domain.content.value,
            createdAt = null,
            updatedAt = null,
            replyToMessageId = domain.replyToMessageId?.value,
            isDeleted = domain.isDeleted.value,
            mentions = dtoMentions
        )
    }

    override fun domainToMap(domain: Message): Map<String, Any?> {
        return mapOf(
            Message.KEY_SENDER_ID to domain.senderId.value,
            Message.KEY_SEND_MESSAGE to domain.content.value,
            Message.KEY_REPLY_TO_MESSAGE_ID to domain.replyToMessageId?.value,
            Message.KEY_IS_DELETED to domain.isDeleted.value,
            Message.KEY_MENTIONS to domain.mentions.map {
                mapOf(
                    "type" to it.type.name,
                    "id" to it.id,
                    "displayName" to it.displayName
                )
            }
        )
    }

    override fun dataToMap(data: MessageDTO): Map<String, Any?> {
        return mapOf(
            Message.KEY_SENDER_ID to data.senderId,
            Message.KEY_SEND_MESSAGE to data.content,
            Message.KEY_REPLY_TO_MESSAGE_ID to data.replyToMessageId,
            Message.KEY_IS_DELETED to data.isDeleted,
            Message.KEY_MENTIONS to data.mentions
        )
    }

    override fun mapToDto(map: Map<String, Any?>): MessageDTO {
        return MessageDTO(
            id = map["id"] as? String ?: "",
            senderId = map[Message.KEY_SENDER_ID] as? String ?: "",
            content = map[Message.KEY_SEND_MESSAGE] as? String ?: "",
            replyToMessageId = map[Message.KEY_REPLY_TO_MESSAGE_ID] as? String,
            isDeleted = map[Message.KEY_IS_DELETED] as? Boolean ?: false,
            mentions = map[Message.KEY_MENTIONS] as? List<Map<String, String>> ?: emptyList(),
            createdAt = (map[AggregateRoot.KEY_CREATED_AT] as? Date),
            updatedAt = (map[AggregateRoot.KEY_UPDATED_AT] as? Date)
        )
    }
}

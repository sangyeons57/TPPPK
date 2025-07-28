package com.example.mapper

import com.example.data_core.model.local.MessagesEntity
import com.example.domain.model.base.Message
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MessageContent
import com.example.domain.model.vo.message.MessageType
import com.example.mapper.base.BaseEntityMapper
import javax.inject.Inject

interface MessageEntityMapper : BaseEntityMapper<Message, MessagesEntity>

class MessageEntityMapperImpl @Inject constructor() : MessageEntityMapper {
    override fun toDomain(entity: MessagesEntity): Message {
        return Message.fromDataSource(
            id = DocumentId(entity.id),
            channelId = DocumentId(entity.channelId),
            senderId = UserId(entity.senderId),
            content = MessageContent(entity.content),
            messageType = MessageType.valueOf(entity.messageType),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(domain: Message): MessagesEntity {
        return MessagesEntity(
            id = domain.id.value,
            channelId = domain.channelId.value,
            senderId = domain.senderId.value,
            content = domain.content.value,
            messageType = domain.messageType.name,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}

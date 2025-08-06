package com.example.mapper.message

import android.util.Log
import com.example.core_common.constants.Constants
import com.example.data_model.local.MessageEntity
import com.example.data_model.remote.MessageDTO
import com.example.domain.model.base.Message
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.MentionType
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MentionInfo
import com.example.domain.vo.message.MessageIsDeleted
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
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
        // MessageType 파싱
        val messageType = try {
            MessageType.valueOf(entity.messageType)
        } catch (e: Exception) {
            Log.w("MessageMapper", "⚠️ 알 수 없는 messageType: ${entity.messageType}, TEXT로 기본 설정")
            MessageType.TEXT
        }

        // Payload 검증 및 기본값 설정
        val payload = if (entity.payload.isNotEmpty() && entity.payload != "{}") {
            MessagePayload(entity.payload)
        } else {
            // 하위 호환성: entity에 payload가 없으면 기본 TEXT 페이로드 생성
            Log.d("MessageMapper", "📄 payload가 비어있음, 기본 TEXT 페이로드 생성")
            MessagePayload.forText("")
        }
        
        return Message.fromDataSource(
            id = DocumentId(entity.id),
            senderId = UserId(entity.senderId),
            messageType = messageType,
            payload = payload,
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
            messageType = domain.messageType.name,
            payload = domain.payload.value,
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

        // 하위 호환성: messageType 처리
        val messageType = try {
            MessageType.valueOf(dto.messageType)
        } catch (e: Exception) {
            Log.w("MessageMapper", "⚠️ 알 수 없는 messageType: ${dto.messageType}, TEXT로 기본 설정")
            MessageType.TEXT
        }

        // 하위 호환성: payload vs content 처리
        val payload = when {
            dto.payload.isNotEmpty() && dto.payload != "{}" -> {
                Log.d("MessageMapper", "✅ 새로운 payload 필드 사용: ${dto.payload}")
                MessagePayload(dto.payload)
            }

            dto.content.isNotEmpty() -> {
                Log.w("MessageMapper", "⚠️ 레거시 content 필드 감지, payload로 변환: ${dto.content}")
                MessagePayload.forText(dto.content)
            }

            else -> {
                Log.w("MessageMapper", "⚠️ payload와 content 모두 비어있음, 기본 텍스트 페이로드 생성")
                MessagePayload.forText("")
            }
        }

        return try {
            val message = Message.fromDataSource(
                id = DocumentId(dto.id),
                senderId = UserId(dto.senderId),
                messageType = messageType,
                payload = payload,
                createdAt = dto.createdAt?.toInstant() ?: Instant.now(),
                updatedAt = dto.updatedAt?.toInstant() ?: Instant.now(),
                replyToMessageId = dto.replyToMessageId?.let { DocumentId(it) },
                isDeleted = MessageIsDeleted.fromBoolean(dto.isDeleted),
                mentions = domainMentions,
                channelId = ChannelId(channelId)
            )
            Log.d(
                "MessageMapper",
                "✅ 도메인 변환 성공: id=${message.id.value}, type=${message.messageType}, channelId=${message.channelId.value}"
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
            messageType = domain.messageType.name,
            payload = domain.payload.value,
            createdAt = null, // ServerTimestamp가 처리
            updatedAt = null, // ServerTimestamp가 처리
            replyToMessageId = domain.replyToMessageId?.value,
            isDeleted = domain.isDeleted.value,
            mentions = dtoMentions,
            content = "" // 하위 호환성을 위해 빈 값으로 설정 (읽기 전용)
        )
    }
}

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
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Message 관련 Entity, Domain, DTO 간의 매핑을 담당하는 Mapper
 * JSON 의존성 없이 순수한 객체 변환만 담당 (단, org.json으로 Map 변환만 사용)
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
            updatedAt = domain.updatedAt.toEpochMilli()
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

        // 강화된 payload 호환성 처리: String, Map, null 모두 지원 (전체 보존)
        val payload = when (dto.payload) {
            is Map<*, *> -> {
                try {
                    val json = mapToJson(dto.payload as Map<String, Any?>).toString()
                    MessagePayload(json)
                } catch (e: Exception) {
                    Log.w("MessageMapper", "⚠️ Map -> JSON 변환 실패, 기본값 사용", e)
                    MessagePayload.forText("")
                }
            }

            is String -> {
                val payloadString = dto.payload as String
                if (payloadString.isNotEmpty() && payloadString != "{}") {
                    try {
                        MessagePayload(payloadString)
                    } catch (_: Exception) {
                        MessagePayload.forText(payloadString)
                    }
                } else {
                    MessagePayload.forText("")
                }
            }

            null -> MessagePayload.forText("")
            else -> MessagePayload.forText("")
        }

        return Message.fromDataSource(
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
    }

    override fun domainToDto(domain: Message): MessageDTO {
        domain.mentions.map { mention ->
            mapOf(
                MentionInfo.KEY_TYPE to mention.type.name,
                MentionInfo.KEY_ID to mention.id,
                MentionInfo.KEY_DISPLAY_NAME to mention.displayName
            )
        }

        // Firestore에는 payload를 전체 객체(Map)로 저장한다 (모든 키 보존)
        val payloadMap: Map<String, Any?> = try {
            jsonToMap(JSONObject(domain.payload.value))
        } catch (e: Exception) {
            Log.w("MessageMapper", "⚠️ JSON -> Map 변환 실패, 최소 content만 저장", e)
            val content = domain.payload.getTextContent() ?: ""
            mapOf(MessagePayload.KEY_CONTENT to content)
        }

        return MessageDTO(
            id = domain.id.value,
            channelId = domain.channelId.value,
            senderId = domain.senderId.value,
            messageType = domain.messageType.name,
            payload = payloadMap,
            createdAt = null,
            updatedAt = null
        )
    }

    // ================================
    // JSON <-> Map 변환 헬퍼 (org.json 기반)
    // ================================
    private fun jsonToMap(jsonObject: JSONObject): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.opt(key)
            result[key] = when (value) {
                is JSONObject -> jsonToMap(value)
                is JSONArray -> jsonArrayToList(value)
                JSONObject.NULL -> null
                else -> value
            }
        }
        return result
    }

    private fun jsonArrayToList(array: JSONArray): List<Any?> {
        val list = mutableListOf<Any?>()
        for (i in 0 until array.length()) {
            val value = array.opt(i)
            list.add(
                when (value) {
                    is JSONObject -> jsonToMap(value)
                    is JSONArray -> jsonArrayToList(value)
                    JSONObject.NULL -> null
                    else -> value
                }
            )
        }
        return list
    }

    private fun mapToJson(map: Map<String, Any?>): JSONObject {
        val obj = JSONObject()
        map.forEach { (k, v) ->
            obj.put(
                k, when (v) {
                    null -> JSONObject.NULL
                    is Map<*, *> -> mapToJson(v as Map<String, Any?>)
                    is List<*> -> listToJsonArray(v as List<Any?>)
                    else -> v
                }
            )
        }
        return obj
    }

    private fun listToJsonArray(list: List<Any?>): JSONArray {
        val arr = JSONArray()
        list.forEach { v ->
            arr.put(
                when (v) {
                    null -> JSONObject.NULL
                    is Map<*, *> -> mapToJson(v as Map<String, Any?>)
                    is List<*> -> listToJsonArray(v as List<Any?>)
                    else -> v
                }
            )
        }
        return arr
    }
}

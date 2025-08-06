package com.example.data_converter

import com.example.domain.AggregateRoot
import com.example.domain.model.base.Message
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.MentionType
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MentionInfo
import com.example.domain.vo.message.MessageIsDeleted
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Message Domain Model과 JSON 간의 변환을 담당하는 Converter
 */
@Singleton
class MessageJsonConverter @Inject constructor(
    private val gson: Gson
) : JsonConverter<Message> {

    override fun toJson(data: Message): String {
        return try {
            val messageData = mapOf(
                AggregateRoot.KEY_ID to data.id.value,
                Message.KEY_CHANNEL_ID to data.channelId.value,
                Message.KEY_SENDER_ID to data.senderId.value,
                Message.KEY_MESSAGE_TYPE to data.messageType.name,
                Message.KEY_PAYLOAD to data.payload.value,
                Message.KEY_REPLY_TO_MESSAGE_ID to data.replyToMessageId?.value,
                Message.KEY_IS_DELETED to data.isDeleted.value,
                Message.KEY_MENTIONS to data.mentions.map { mention ->
                    mapOf(
                        "type" to mention.type.name,
                        "id" to mention.id,
                        "displayName" to mention.displayName
                    )
                },
                AggregateRoot.KEY_CREATED_AT to data.createdAt.toEpochMilli(),
                AggregateRoot.KEY_UPDATED_AT to data.updatedAt.toEpochMilli()
            )
            gson.toJson(messageData)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert Message to JSON: ${e.message}", e)
        }
    }

    override fun fromJson(json: String): Message {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val messageData: Map<String, Any?> = gson.fromJson(json, type)

            val mentions =
                (messageData[Message.KEY_MENTIONS] as? List<Map<String, Any?>>)?.mapNotNull { mentionMap ->
                    try {
                        val type = MentionType.valueOf(mentionMap["type"] as String)
                        val id = mentionMap["id"] as String
                        val displayName = mentionMap["displayName"] as String
                        MentionInfo(type, id, displayName)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

            // 하위 호환성: messageType 처리
            val messageType = try {
                val typeString = messageData[Message.KEY_MESSAGE_TYPE] as? String ?: "TEXT"
                MessageType.valueOf(typeString)
            } catch (e: Exception) {
                MessageType.TEXT
            }

            // 하위 호환성: payload vs content 처리
            val payload = when {
                messageData[Message.KEY_PAYLOAD] != null -> {
                    val payloadString = messageData[Message.KEY_PAYLOAD] as String
                    MessagePayload(payloadString)
                }

                messageData[Message.KEY_SEND_MESSAGE] != null -> {
                    // 레거시 content 필드를 payload로 변환
                    val contentString = messageData[Message.KEY_SEND_MESSAGE] as String
                    MessagePayload.forText(contentString)
                }

                else -> {
                    MessagePayload.forText("")
                }
            }

            Message.fromDataSource(
                id = DocumentId(messageData[AggregateRoot.KEY_ID] as String),
                senderId = UserId(messageData[Message.KEY_SENDER_ID] as String),
                messageType = messageType,
                payload = payload,
                replyToMessageId = (messageData[Message.KEY_REPLY_TO_MESSAGE_ID] as? String)?.let {
                    DocumentId(
                        it
                    )
                },
                createdAt = Instant.ofEpochMilli((messageData[AggregateRoot.KEY_CREATED_AT] as Double).toLong()),
                updatedAt = Instant.ofEpochMilli((messageData[AggregateRoot.KEY_UPDATED_AT] as Double).toLong()),
                isDeleted = if (messageData[Message.KEY_IS_DELETED] as Boolean) MessageIsDeleted.TRUE else MessageIsDeleted.FALSE,
                mentions = mentions,
                channelId = ChannelId(messageData[Message.KEY_CHANNEL_ID] as? String ?: "")
            )
        } catch (e: JsonSyntaxException) {
            throw JsonConversionException("Invalid JSON format for Message: ${e.message}", e)
        } catch (e: ClassCastException) {
            throw JsonConversionException("JSON structure mismatch for Message: ${e.message}", e)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert JSON to Message: ${e.message}", e)
        }
    }
}
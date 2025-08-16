package com.example.data_repository.util

import com.example.data_model.local.MessageEntity
import com.example.domain.model.base.Message
import com.example.domain.vo.message.MentionInfo
import com.example.domain.vo.message.MessagePayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

object OutboxPayloadUtil {

    /**
     * MessagePayload JSON에 channelId를 병합한 OutBox 전송용 JSON 문자열을 생성합니다.
     */
    fun buildForMessage(channelId: String, payload: MessagePayload): String {
        return try {
            val jsonObj = payload.asJsonObject()
            val merged = buildJsonObject {
                jsonObj.forEach { (k, v) -> put(k, v) }
                put("channelId", JsonPrimitive(channelId))
            }
            merged.toString()
        } catch (_: Exception) {
            // 실패 시 최소 필드만 포함
            buildJsonObject {
                put("content", JsonPrimitive(payload.getTextContent() ?: ""))
                put("channelId", JsonPrimitive(channelId))
            }.toString()
        }
    }

    /**
     * Room MessageEntity 전체를 OutBox payload로 직렬화합니다.
     * stream에서 지칭하는 row 전체를 보존한다는 의도를 충족합니다.
     */
    fun toPayload(entity: MessageEntity): String {
        val payloadJson = parseOrString(entity.payload)
        val mentionsJson = parseOrArray(entity.mentions)

        val obj = buildJsonObject {
            put("id", JsonPrimitive(entity.id))
            put("channelId", JsonPrimitive(entity.channelId))
            put("senderId", JsonPrimitive(entity.senderId))
            put("messageType", JsonPrimitive(entity.messageType))
            put("payload", payloadJson)
            if (entity.replyToMessageId != null) put(
                "replyToMessageId",
                JsonPrimitive(entity.replyToMessageId)
            ) else put("replyToMessageId", JsonNull)
            put("isDeleted", JsonPrimitive(entity.isDeleted))
            put("mentions", mentionsJson)
            put("createdAt", JsonPrimitive(entity.createdAt))
            put("updatedAt", JsonPrimitive(entity.updatedAt))
        }
        return obj.toString()
    }

    /**
     * 도메인 Message 전체를 OutBox payload로 직렬화합니다.
     */
    fun toPayload(message: Message): String {
        val payload = try {
            message.payload.asJsonObject()
        } catch (_: Exception) {
            JsonObject(emptyMap())
        }
        val mentions = buildJsonArray {
            message.mentions.forEach { m ->
                add(
                    buildJsonObject {
                        put(MentionInfo.KEY_TYPE, JsonPrimitive(m.type.name))
                        put(MentionInfo.KEY_ID, JsonPrimitive(m.id))
                        put(MentionInfo.KEY_DISPLAY_NAME, JsonPrimitive(m.displayName))
                    }
                )
            }
        }
        val obj = buildJsonObject {
            put("id", JsonPrimitive(message.id.value))
            put("channelId", JsonPrimitive(message.channelId.value))
            put("senderId", JsonPrimitive(message.senderId.value))
            put("messageType", JsonPrimitive(message.messageType.name))
            put("payload", payload)
            message.replyToMessageId?.let { put("replyToMessageId", JsonPrimitive(it.value)) }
                ?: put("replyToMessageId", JsonNull)
            put("isDeleted", JsonPrimitive(message.isDeleted.value))
            put("mentions", mentions)
            put("createdAt", JsonPrimitive(message.createdAt.toEpochMilli()))
            put("updatedAt", JsonPrimitive(message.updatedAt.toEpochMilli()))
        }
        return obj.toString()
    }

    private fun parseOrString(json: String): JsonElement =
        runCatching { Json.parseToJsonElement(json) }.getOrElse { JsonPrimitive(json) }

    private fun parseOrArray(json: String): JsonElement =
        runCatching { Json.parseToJsonElement(json) }.getOrElse { buildJsonArray { } }

    /**
     * 주어진 Json 문자열의 모든 값(JsonPrimitive 포함)에 대해 변환 함수를 적용합니다.
     * path는 예: "root.items[2].name" 형태로 전달됩니다.
     */
    fun replaceAllValues(
        json: String,
        transform: (path: String, value: JsonElement) -> JsonElement
    ): String {
        return try {
            val root = Json.parseToJsonElement(json)
            transformRecursive("root", root, transform).toString()
        } catch (_: Exception) {
            json
        }
    }

    private fun transformRecursive(
        path: String,
        element: JsonElement,
        transform: (path: String, value: JsonElement) -> JsonElement
    ): JsonElement {
        return when (element) {
            is JsonObject -> {
                buildJsonObject {
                    element.forEach { (k, v) ->
                        val childPath = "$path.$k"
                        put(k, transformRecursive(childPath, v, transform))
                    }
                }
            }

            is kotlinx.serialization.json.JsonArray -> {
                val newArray = element.mapIndexed { idx, v ->
                    val childPath = "$path[$idx]"
                    transformRecursive(childPath, v, transform)
                }
                kotlinx.serialization.json.JsonArray(newArray)
            }

            is JsonPrimitive, JsonNull -> transform(path, element)
            else -> transform(path, element)
        }
    }
}

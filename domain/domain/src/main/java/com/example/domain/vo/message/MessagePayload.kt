package com.example.domain.vo.message

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * 메시지 페이로드를 나타내는 값 객체
 *
 * 실제 JSON 객체 기반으로 처리하여 타입 안전성과 유지보수성을 확보
 * AttachmentData 클래스 없이 JSON 객체를 직접 다룸
 *
 * 예시:
 * - TEXT: {"content": "실제 메시지 내용"}
 * - TEXT with attachments: {"content": "메시지", "attachments": [{"kind": "image", "url": "...", ...}]}
 * - SYSTEM_PROJECT_JOIN: {"projectId": "...", "projectName": "...", "actionText": "참여하기"}
 * - SYSTEM_DATE: {"date": "2024-01-01", "displayText": "2024년 1월 1일"}
 */
@JvmInline
value class MessagePayload(val value: String) {
    
    init {
        require(value.isNotEmpty()) { "MessagePayload cannot be empty." }
        // JSON 유효성 검사 - kotlinx.serialization으로 검증
        try {
            Json.parseToJsonElement(value) as JsonObject
        } catch (e: Exception) {
            throw IllegalArgumentException("MessagePayload must be valid JSON: ${e.message}", e)
        }
    }

    /**
     * JSON 객체로 파싱하여 반환
     */
    fun asJsonObject(): JsonObject {
        return Json.parseToJsonElement(value) as JsonObject
    }

    /**
     * TEXT 타입 메시지용 팩토리 메서드
     */
    companion object {
        /**
         * 일반 텍스트 메시지용 페이로드 생성
         */
        fun forText(content: String): MessagePayload {
            val jsonObject = buildJsonObject {
                put("content", content)
            }
            return MessagePayload(jsonObject.toString())
        }

        /**
         * 첨부파일 포함 텍스트 메시지용 페이로드 생성
         * @param content 텍스트 내용
         * @param attachments 첨부파일 데이터 맵들의 리스트
         */
        fun forTextWithAttachments(
            content: String,
            attachments: List<Map<String, Any?>>
        ): MessagePayload {
            val attachmentJsonObjects = attachments.map { attachmentMap ->
                buildJsonObject {
                    attachmentMap.forEach { (key, value) ->
                        when (value) {
                            is String -> put(key, value)
                            is Int -> put(key, value)
                            is Long -> put(key, value)
                            is Boolean -> put(key, value)
                            null -> put(key, JsonNull)
                            else -> put(key, value.toString())
                        }
                    }
                }
            }
            val jsonObject = buildJsonObject {
                put("content", content)
                put("attachments", JsonArray(attachmentJsonObjects))
            }
            return MessagePayload(jsonObject.toString())
        }

        /**
         * 단일 첨부파일 데이터 맵 생성 헬퍼
         */
        fun createAttachment(
            kind: String,
            url: String,
            mime: String,
            filename: String? = null,
            width: Int? = null,
            height: Int? = null,
            duration_ms: Long? = null,
            size: Long? = null
        ): Map<String, Any?> {
            return buildMap {
                put("kind", kind)
                put("url", url)
                put("mime", mime)
                filename?.let { put("filename", it) }
                width?.let { put("width", it) }
                height?.let { put("height", it) }
                duration_ms?.let { put("duration_ms", it) }
                size?.let { put("size", it) }
            }
        }

        /**
         * 프로젝트 참여 시스템 메시지용 페이로드 생성
         */
        fun forProjectJoin(
            projectId: String,
            projectName: String,
            actionText: String = "참여하기"
        ): MessagePayload {
            val jsonObject = buildJsonObject {
                put("projectId", projectId)
                put("projectName", projectName)
                put("actionText", actionText)
            }
            return MessagePayload(jsonObject.toString())
        }

        /**
         * 날짜 시스템 메시지용 페이로드 생성
         */
        fun forDateSystem(date: String, displayText: String): MessagePayload {
            val jsonObject = buildJsonObject {
                put("date", date)
                put("displayText", displayText)
            }
            return MessagePayload(jsonObject.toString())
        }

        /**
         * 채팅 시작 시스템 메시지용 페이로드 생성
         */
        fun forChatStart(channelName: String, welcomeText: String = "채팅이 시작되었습니다"): MessagePayload {
            val jsonObject = buildJsonObject {
                put("channelName", channelName)
                put("welcomeText", welcomeText)
            }
            return MessagePayload(jsonObject.toString())
        }

        /**
         * 프로젝트 멤버 초대 시스템 메시지용 페이로드 생성
         */
        fun forMemberInvitation(
            projectId: String,
            projectName: String,
            inviterName: String,
            targetUserId: String,
            actionText: String = "멤버로 추가"
        ): MessagePayload {
            val jsonObject = buildJsonObject {
                put("projectId", projectId)
                put("projectName", projectName)
                put("inviterName", inviterName)
                put("targetUserId", targetUserId)
                put("actionText", actionText)
            }
            return MessagePayload(jsonObject.toString())
        }

        /**
         * 이미지 메시지용 페이로드 생성 (단일 이미지)
         */
        fun forImage(
            content: String = "",
            imageUrl: String,
            imageFilename: String? = null,
            width: Int? = null,
            height: Int? = null,
            size: Long? = null
        ): MessagePayload {
            val attachment = createAttachment(
                kind = "image",
                url = imageUrl,
                mime = "image/jpeg", // 기본값, 추후 개선 가능
                filename = imageFilename,
                width = width,
                height = height,
                size = size
            )
            return forTextWithAttachments(content, listOf(attachment))
        }

        /**
         * 이미지 메시지용 페이로드 생성 (다중 이미지)
         */
        fun forImages(
            content: String = "",
            images: List<Map<String, Any?>>
        ): MessagePayload {
            val imageAttachments = images.map { imageData ->
                createAttachment(
                    kind = "image",
                    url = imageData["url"] as? String ?: "",
                    mime = imageData["mime"] as? String ?: "image/jpeg",
                    filename = imageData["filename"] as? String,
                    width = imageData["width"] as? Int,
                    height = imageData["height"] as? Int,
                    size = imageData["size"] as? Long
                )
            }
            return forTextWithAttachments(content, imageAttachments)
        }
    }

    /**
     * TEXT 타입 메시지의 content 추출
     */
    fun getTextContent(): String? {
        return try {
            asJsonObject()["content"]?.jsonPrimitive?.contentOrNull
        } catch (e: Exception) {
            null
        }
    }

    /**
     * JSON 값 추출 (문자열 타입)
     */
    fun getValue(key: String): String? {
        return try {
            asJsonObject()[key]?.jsonPrimitive?.contentOrNull
        } catch (e: Exception) {
            null
        }
    }

    /**
     * JSON 값 추출 (정수 타입)
     */
    fun getIntValue(key: String): Int? {
        return try {
            asJsonObject()[key]?.jsonPrimitive?.intOrNull
        } catch (e: Exception) {
            null
        }
    }

    /**
     * JSON 값 추출 (Long 타입)
     */
    fun getLongValue(key: String): Long? {
        return try {
            asJsonObject()[key]?.jsonPrimitive?.longOrNull
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 첨부파일 목록 추출 (Map 리스트 반환)
     * JSON 처리는 여기서만 하고, 다른 모듈들은 단순한 Map을 사용
     */
    fun getAttachments(): List<Map<String, Any?>> {
        return try {
            val jsonObject = asJsonObject()
            val attachmentsArray = jsonObject["attachments"]?.jsonArray ?: return emptyList()

            attachmentsArray.mapNotNull { element ->
                try {
                    val jsonObj = element.jsonObject
                    // JsonObject를 Map으로 변환
                    buildMap<String, Any?> {
                        jsonObj.forEach { (key, value) ->
                            when {
                                value is JsonPrimitive && value.isString -> put(key, value.content)
                                value is JsonPrimitive && !value.isString -> {
                                    // 숫자나 불린값 처리
                                    val longValue = value.longOrNull
                                    val intValue = value.intOrNull
                                    val boolValue = value.booleanOrNull
                                    when {
                                        boolValue != null -> put(key, boolValue)
                                        intValue != null && intValue.toLong() == longValue -> put(
                                            key,
                                            intValue
                                        )

                                        longValue != null -> put(key, longValue)
                                        else -> put(key, value.content)
                                    }
                                }

                                value is JsonNull -> put(key, null)
                                else -> put(key, value.toString())
                            }
                        }
                    }
                } catch (e: Exception) {
                    null // 유효하지 않은 객체는 무시
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 첨부파일이 있는지 확인
     */
    fun hasAttachments(): Boolean {
        return try {
            asJsonObject().containsKey("attachments") && getAttachments().isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * 하위 호환성을 위한 MessageContent 타입 별칭
 * @deprecated MessagePayload를 사용하세요
 */
@Deprecated("Use MessagePayload instead", ReplaceWith("MessagePayload"))
typealias MessageContent = MessagePayload
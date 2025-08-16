package com.example.websocket.core

import com.example.websocket.constant.WebSocketFieldConstants
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * WebSocket을 통해 송수신되는 메시지의 데이터 클래스
 *
 * 목적과 매핑(중요):
 * - 도메인 Message 필드 매핑 규칙을 명확히 문서화하여 송수신 스키마를 일관되게 유지한다.
 * - 현재 구현은 "중첩 메시지(message: NestedMessage) 우선, 봉투(envelope) 폴백" 전략을 사용한다.
 *   (향후 평탄(Flat) 스키마로 전환 시, 동일한 필드명을 봉투에 배치하면 된다.)
 *
 * 도메인 Message ←→ WebSocketMessage 매핑 규칙
 * - Message.id            ← message.id (우선), 없으면 mapper에서 별도 처리
 * - Message.senderId      ← message.senderId (우선), 없으면 senderId(봉투)
 * - Message.messageType   ← message.messageType (우선), 없으면 TEXT 등 기본값/휴리스틱
 * - Message.payload       ← message.payload (우선), 없으면 payload(봉투)
 * - Message.replyToMessageId ← message.replyToMessageId (우선), 없으면 replyToMessageId(봉투)
 * - Message.createdAt     ← (message.timestamp 또는 timestamp) epoch seconds → Instant
 * - Message.channelId     ← roomId(봉투)
 * - Message.projectId     ← projectId(봉투, 선택)
 *
 * 송신 시 권장 값 배치(현재 호환 로직 유지):
 * - 가능하면 NestedMessage 안에 도메인 소유 필드(id, senderId, payload, messageType, replyToMessageId, timestamp)를 채운다.
 * - 봉투(envelope)는 라우팅/메타(type, roomId, projectId, authToken 등)를 담는다.
 * - 평탄(Flat) 전환 시에도 동일 이름을 봉투에 배치하면 서버는 폴백 로직으로 수신 호환됨.
 */
@Serializable
data class WebSocketMessage(
    /**
     * 메시지 타입 (필수)
     * - MESSAGE / EDIT_MESSAGE / DELETE_MESSAGE / MESSAGE_ACK / EDIT_MESSAGE_ACK / DELETE_MESSAGE_ACK
     * - JOIN_ROOM / LEAVE_ROOM / JOINED_ROOM / LEFT_ROOM / AUTH / AUTH_SUCCESS / ERROR / SYSTEM
     */
    val type: String,

    /**
     * 방 ID (옵션)
     * - 도메인 Message.channelId 로 매핑
     */
    val roomId: String? = null,

    /**
     * 발신자 ID (옵션)
     * - 도메인 Message.senderId 로 매핑 (NestedMessage.senderId 가 우선)
     */
    val senderId: String? = null,


    /**
     * 메시지 페이로드 (옵션)
     * - 도메인 Message.payload 로 매핑 (NestedMessage.payload 가 우선)
     * - content 키를 포함하는 JSON 객체(텍스트 메시지의 경우)
     */
    val payload: JsonObject? = null,

    /**
     * 답장 대상 메시지 ID (옵션)
     * - 도메인 Message.replyToMessageId 로 매핑 (NestedMessage.replyToMessageId 가 우선)
     */
    val replyToMessageId: String? = null,

    /**
     * 타임스탬프 (옵션, Unix epoch seconds)
     * - 도메인 Message.createdAt 로 매핑 (NestedMessage.timestamp 가 우선)
     */
    val timestamp: Double? = null,

    /**
     * 프로젝트 ID (옵션)
     * - 라우팅/컨텍스트 용도. 도메인 Message 자체의 필드는 아님(외부 컨텍스트)
     */
    val projectId: String? = null,

    // channelType 제거: projectId 존재 여부로 라우팅 구분 (DM/PROJECT)

    /**
     * 인증 토큰 (옵션)
     * - AUTH 메시지에서 사용
     */
    val authToken: String? = null,

    /**
     * 오류 코드 (옵션)
     * - ERROR 메시지에서 사용
     */
    val errorCode: String? = null,

    /**
     * 추가 메타데이터 (옵션)
     * - 라우팅/표시용 메타데이터 (도메인 Message 비핵심)
     */
    val metadata: Map<String, String>? = null,

    // 도메인 Message를 래핑하는 중첩 객체 (수신/파싱 시 우선 참조, 송신 시는 선택)
    // 서버/클라 하위호환을 위해 유지. 평탄 스키마 전환 시 제거 가능.
    val message: NestedMessage? = null
) {

    companion object {

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

        // ================================
        // JSON/Map 변환 메서드
        // ================================

        /**
         * JSON 문자열을 WebSocketMessage로 변환
         */
        fun fromJson(jsonString: String): WebSocketMessage {
            return json.decodeFromString(jsonString)
        }

        /**
         * WebSocketMessage를 JSON 문자열로 변환
         */
        fun toJson(message: WebSocketMessage): String {
            return json.encodeToString(message)
        }

        /**
         * Map을 WebSocketMessage로 변환
         */
        fun fromMap(map: Map<String, Any?>): WebSocketMessage {
            return WebSocketMessage(
                type = map[WebSocketFieldConstants.FIELD_TYPE] as String,
                roomId = map[WebSocketFieldConstants.FIELD_ROOM_ID] as? String,
                senderId = map[WebSocketFieldConstants.FIELD_SENDER_ID] as? String,
                payload = when (val p = map[WebSocketFieldConstants.FIELD_PAYLOAD]) {
                    is String -> try {
                        Json.parseToJsonElement(p).jsonObject
                    } catch (_: Exception) {
                        null
                    }

                    is Map<*, *> -> buildJsonObject {
                        p.forEach { (k, v) ->
                            val key = k?.toString() ?: return@forEach
                            when (v) {
                                null -> put(key, JsonPrimitive(""))
                                is String -> put(key, JsonPrimitive(v))
                                is Number, is Boolean -> put(key, JsonPrimitive(v.toString()))
                                else -> put(key, JsonPrimitive(v.toString()))
                            }
                        }
                    }

                    else -> null
                },
                replyToMessageId = map[WebSocketFieldConstants.FIELD_REPLY_TO_MESSAGE_ID] as? String,
                timestamp = (map[WebSocketFieldConstants.FIELD_TIMESTAMP] as? Number)?.toDouble(),
                projectId = map[WebSocketFieldConstants.FIELD_PROJECT_ID] as? String,
                authToken = map[WebSocketFieldConstants.FIELD_AUTH_TOKEN] as? String,
                errorCode = map[WebSocketFieldConstants.FIELD_ERROR_CODE] as? String,
                metadata = convertToStringMap(map[WebSocketFieldConstants.FIELD_METADATA]),
                message = when (val m = map[WebSocketFieldConstants.FIELD_MESSAGE]) {
                    is String -> try {
                        val obj = Json.parseToJsonElement(m).jsonObject
                        NestedMessage(
                            id = obj[WebSocketFieldConstants.FIELD_MESSAGE_ID]?.jsonPrimitive?.contentOrNull,
                            messageType = obj[WebSocketFieldConstants.FIELD_MESSAGE_TYPE]?.jsonPrimitive?.contentOrNull,
                            payload = obj[WebSocketFieldConstants.FIELD_PAYLOAD]?.jsonObject,
                            senderId = obj[WebSocketFieldConstants.FIELD_SENDER_ID]?.jsonPrimitive?.contentOrNull,
                            replyToMessageId = obj[WebSocketFieldConstants.FIELD_REPLY_TO_MESSAGE_ID]?.jsonPrimitive?.contentOrNull,
                            timestamp = obj[WebSocketFieldConstants.FIELD_TIMESTAMP]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                        )
                    } catch (_: Exception) {
                        null
                    }

                    is Map<*, *> -> {
                        val id = m[WebSocketFieldConstants.FIELD_MESSAGE_ID]?.toString()
                        val type = m[WebSocketFieldConstants.FIELD_MESSAGE_TYPE]?.toString()
                        val payloadMap = m[WebSocketFieldConstants.FIELD_PAYLOAD]
                        val sender = m[WebSocketFieldConstants.FIELD_SENDER_ID]?.toString()
                        val reply = m[WebSocketFieldConstants.FIELD_REPLY_TO_MESSAGE_ID]?.toString()
                        val ts = (m[WebSocketFieldConstants.FIELD_TIMESTAMP] as? Number)?.toDouble()
                        val payloadJson = when (payloadMap) {
                            is String -> try {
                                Json.parseToJsonElement(payloadMap).jsonObject
                            } catch (_: Exception) {
                                null
                            }

                            is Map<*, *> -> buildJsonObject {
                                payloadMap.forEach { (k, v) ->
                                    val key = k?.toString() ?: return@forEach
                                    when (v) {
                                        null -> put(key, JsonPrimitive(""))
                                        is String -> put(key, JsonPrimitive(v))
                                        is Number, is Boolean -> put(
                                            key,
                                            JsonPrimitive(v.toString())
                                        )

                                        else -> put(key, JsonPrimitive(v.toString()))
                                    }
                                }
                            }

                            else -> null
                        }
                        NestedMessage(
                            id = id,
                            messageType = type,
                            payload = payloadJson,
                            senderId = sender,
                            replyToMessageId = reply,
                            timestamp = ts
                        )
                    }

                    else -> null
                }
            )
        }

        /**
         * WebSocketMessage를 Map으로 변환
         */
        fun toMap(message: WebSocketMessage): Map<String, Any?> {
            return buildMap {
                put(WebSocketFieldConstants.FIELD_TYPE, message.type)
                message.roomId?.let { put(WebSocketFieldConstants.FIELD_ROOM_ID, it) }
                message.senderId?.let { put(WebSocketFieldConstants.FIELD_SENDER_ID, it) }
                message.payload?.let { put(WebSocketFieldConstants.FIELD_PAYLOAD, it.toString()) }
                message.replyToMessageId?.let {
                    put(
                        WebSocketFieldConstants.FIELD_REPLY_TO_MESSAGE_ID,
                        it
                    )
                }
                message.timestamp?.let { put(WebSocketFieldConstants.FIELD_TIMESTAMP, it) }
                message.projectId?.let { put(WebSocketFieldConstants.FIELD_PROJECT_ID, it) }
                message.authToken?.let { put(WebSocketFieldConstants.FIELD_AUTH_TOKEN, it) }
                message.errorCode?.let { put(WebSocketFieldConstants.FIELD_ERROR_CODE, it) }
                message.metadata?.let { put(WebSocketFieldConstants.FIELD_METADATA, it) }
                message.message?.let { nested ->
                    val nestedMap = buildMap<String, Any?> {
                        nested.id?.let { put(WebSocketFieldConstants.FIELD_MESSAGE_ID, it) }
                        nested.messageType?.let {
                            put(
                                WebSocketFieldConstants.FIELD_MESSAGE_TYPE,
                                it
                            )
                        }
                        nested.payload?.let {
                            put(
                                WebSocketFieldConstants.FIELD_PAYLOAD,
                                it.toString()
                            )
                        }
                        nested.senderId?.let { put(WebSocketFieldConstants.FIELD_SENDER_ID, it) }
                        nested.replyToMessageId?.let {
                            put(
                                WebSocketFieldConstants.FIELD_REPLY_TO_MESSAGE_ID,
                                it
                            )
                        }
                        nested.timestamp?.let { put(WebSocketFieldConstants.FIELD_TIMESTAMP, it) }
                    }
                    put(WebSocketFieldConstants.FIELD_MESSAGE, nestedMap)
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun convertToStringMap(obj: Any?): Map<String, String>? {
            return when (obj) {
                null -> null
                is Map<*, *> -> {
                    obj.entries.associate { (k, v) ->
                        k.toString() to (v?.toString() ?: "")
                    }
                }

                else -> null
            }
        }
        
        // ================================
        // 메시지 타입 상수
        // ================================

        // 기본 메시지 타입
        const val TYPE_MESSAGE = "MESSAGE"
        const val TYPE_EDIT_MESSAGE = "EDIT_MESSAGE"
        const val TYPE_DELETE_MESSAGE = "DELETE_MESSAGE"

        // ACK 메시지 타입
        const val TYPE_ACK = "ACK"  // 서버에서 보내는 일반 ACK
        const val TYPE_MESSAGE_ACK = "MESSAGE_ACK"
        const val TYPE_EDIT_MESSAGE_ACK = "EDIT_MESSAGE_ACK"
        const val TYPE_DELETE_MESSAGE_ACK = "DELETE_MESSAGE_ACK"

        // FAILED 메시지 타입
        const val TYPE_MESSAGE_FAILED = "MESSAGE_FAILED"
        const val TYPE_EDIT_MESSAGE_FAILED = "EDIT_MESSAGE_FAILED"
        const val TYPE_DELETE_MESSAGE_FAILED = "DELETE_MESSAGE_FAILED"

        // 방 관리 메시지 타입
        const val TYPE_JOIN_ROOM = "JOIN_ROOM"
        const val TYPE_LEAVE_ROOM = "LEAVE_ROOM"
        const val TYPE_JOINED_ROOM = "JOINED_ROOM"
        const val TYPE_LEFT_ROOM = "LEFT_ROOM"

        // 인증 메시지 타입
        const val TYPE_AUTH = "AUTH"
        const val TYPE_AUTH_SUCCESS = "AUTH_SUCCESS"

        // 시스템 메시지 타입
        const val TYPE_ERROR = "ERROR"
        const val TYPE_SYSTEM = WebSocketFieldConstants.MESSAGE_TYPE_SYSTEM


        // 채널 타입 상수 제거: projectId 존재 여부로 구분

        // ================================
        // 메시지 생성 헬퍼 메서드
        // ================================

        /**
         * 채팅 메시지 생성 (MessagePayload 기반)
         */
        fun createChatMessage(
            roomId: String,
            senderId: String,
            messagePayload: com.example.domain.vo.message.MessagePayload,
            messageId: String,
            replyToMessageId: String? = null,
            timestamp: Double? = null,
            projectId: String? = null
        ): WebSocketMessage {
            val payload = try {
                messagePayload.asJsonObject()
            } catch (e: Exception) {
                // fallback: content로 감싸기
                buildJsonObject {
                    put(
                        com.example.domain.vo.message.MessagePayload.KEY_CONTENT,
                        JsonPrimitive(messagePayload.value)
                    )
                }
            }
            return WebSocketMessage(
                type = TYPE_MESSAGE,
                roomId = roomId,
                senderId = senderId,
                message = NestedMessage(
                    id = messageId,
                    messageType = null,
                    payload = payload,
                    senderId = senderId,
                    replyToMessageId = replyToMessageId,
                    timestamp = timestamp
                ),
                projectId = projectId
            )
        }

        /**
         * 채팅 메시지 생성 (MessagePayload 기반, messageType 명시)
         */
        fun createChatMessageWithType(
            roomId: String,
            senderId: String,
            messagePayload: com.example.domain.vo.message.MessagePayload,
            messageId: String,
            messageTypeString: String?,
            replyToMessageId: String? = null,
            timestamp: Double? = null,
            projectId: String? = null
        ): WebSocketMessage {
            val payload = try {
                messagePayload.asJsonObject()
            } catch (e: Exception) {
                buildJsonObject {
                    put(
                        com.example.domain.vo.message.MessagePayload.KEY_CONTENT,
                        JsonPrimitive(messagePayload.value)
                    )
                }
            }
            return WebSocketMessage(
                type = TYPE_MESSAGE,
                roomId = roomId,
                senderId = senderId,
                message = NestedMessage(
                    id = messageId,
                    messageType = messageTypeString,
                    payload = payload,
                    senderId = senderId,
                    replyToMessageId = replyToMessageId,
                    timestamp = timestamp
                ),
                projectId = projectId
            )
        }

        /**
         * 채팅 메시지 생성 (텍스트 content - 하위 호환용)
         */
        fun createChatMessageFromText(
            roomId: String,
            senderId: String,
            textContent: String,
            messageId: String,
            replyToMessageId: String? = null,
            timestamp: Double? = null
        ): WebSocketMessage {
            val messagePayload = com.example.domain.vo.message.MessagePayload.forText(textContent)
            return createChatMessage(
                roomId,
                senderId,
                messagePayload,
                messageId,
                replyToMessageId,
                timestamp
            )
        }

        /**
         * 방 입장 메시지 생성
         */
        fun createJoinRoomMessage(
            roomId: String,
            senderId: String,
            timestamp: Double? = null
        ): WebSocketMessage {
            return WebSocketMessage(
                type = TYPE_JOIN_ROOM,
                roomId = roomId,
                senderId = senderId,
                timestamp = timestamp
            )
        }

        /**
         * 방 퇴장 메시지 생성
         */
        fun createLeaveRoomMessage(
            roomId: String,
            senderId: String,
            timestamp: Double? = null
        ): WebSocketMessage {
            return WebSocketMessage(
                type = TYPE_LEAVE_ROOM,
                roomId = roomId,
                senderId = senderId,
                timestamp = timestamp
            )
        }

        /**
         * 인증 메시지 생성
         */
        fun createAuthMessage(authToken: String): WebSocketMessage {
            return WebSocketMessage(
                type = TYPE_AUTH,
                authToken = authToken
            )
        }

        /**
         * 에러 메시지 생성 (MessagePayload 기반)
         */
        fun createErrorMessage(
            message: String,
            errorCode: String? = null,
            roomId: String? = null
        ): WebSocketMessage {
            val messagePayload = com.example.domain.vo.message.MessagePayload.forText(message)
            val payload = try {
                messagePayload.asJsonObject()
            } catch (e: Exception) {
                buildJsonObject {
                    put(
                        com.example.domain.vo.message.MessagePayload.KEY_CONTENT,
                        JsonPrimitive(message)
                    )
                }
            }
            return WebSocketMessage(
                type = TYPE_ERROR,
                payload = payload,
                errorCode = errorCode,
                roomId = roomId
            )
        }

        /**
         * 메시지 수정 WebSocket 메시지 생성
         */
        fun createEditMessage(
            roomId: String,
            messageId: String,
            newPayload: com.example.domain.vo.message.MessagePayload,
            projectId: String? = null
        ): WebSocketMessage {
            val payload = try {
                newPayload.asJsonObject()
            } catch (e: Exception) {
                buildJsonObject {
                    put(
                        com.example.domain.vo.message.MessagePayload.KEY_CONTENT,
                        JsonPrimitive(newPayload.value)
                    )
                }
            }
            return WebSocketMessage(
                type = TYPE_EDIT_MESSAGE,
                roomId = roomId,
                message = NestedMessage(id = messageId, payload = payload),
                projectId = projectId
            )
        }

        /**
         * 메시지 삭제 WebSocket 메시지 생성
         */
        fun createDeleteMessage(
            roomId: String,
            messageId: String,
            projectId: String? = null
        ): WebSocketMessage {
            return WebSocketMessage(
                type = TYPE_DELETE_MESSAGE,
                roomId = roomId,
                message = NestedMessage(id = messageId),
                projectId = projectId
            )
        }

        /**
         * 프로젝트 초대 WebSocket 메시지 생성
         */
        fun createProjectInviteMessage(
            roomId: String,
            senderId: String,
            projectId: String,
            projectName: String,
            inviterName: String,
            invitationId: String,
            messageId: String,
            timestamp: Double? = null
        ): WebSocketMessage {
            val payload = buildJsonObject {
                put(com.example.domain.vo.message.MessagePayload.KEY_CONTENT, JsonPrimitive(""))
                put(
                    WebSocketFieldConstants.FIELD_PROJECT_NAME,
                    JsonPrimitive(projectName)
                )
                put(
                    WebSocketFieldConstants.FIELD_INVITER_NAME,
                    JsonPrimitive(inviterName)
                )
                put(
                    WebSocketFieldConstants.FIELD_INVITATION_ID,
                    JsonPrimitive(invitationId)
                )
            }

            return WebSocketMessage(
                type = TYPE_MESSAGE,
                roomId = roomId,
                senderId = senderId,
                message = NestedMessage(
                    id = messageId,
                    messageType = WebSocketFieldConstants.MESSAGE_TYPE_PROJECT_INVITE,
                    payload = payload,
                    senderId = senderId,
                    timestamp = timestamp
                ),
                projectId = projectId
            )
        }

    }

    // ================================
    // 유틸리티 메서드
    // ================================

    /**
     * 메시지가 채팅 메시지인지 확인
     */
    fun isChatMessage(): Boolean {
        return type in setOf(TYPE_MESSAGE, TYPE_EDIT_MESSAGE, TYPE_DELETE_MESSAGE)
    }

    /**
     * 메시지가 ACK 메시지인지 확인
     */
    fun isAckMessage(): Boolean {
        return type in setOf(TYPE_MESSAGE_ACK, TYPE_EDIT_MESSAGE_ACK, TYPE_DELETE_MESSAGE_ACK)
    }

    /**
     * 메시지가 FAILED 메시지인지 확인
     */
    fun isFailedMessage(): Boolean {
        return type in setOf(
            TYPE_MESSAGE_FAILED,
            TYPE_EDIT_MESSAGE_FAILED,
            TYPE_DELETE_MESSAGE_FAILED
        )
    }

    /**
     * 메시지가 방 관리 메시지인지 확인
     */
    fun isRoomMessage(): Boolean {
        return type in setOf(TYPE_JOIN_ROOM, TYPE_LEAVE_ROOM, TYPE_JOINED_ROOM, TYPE_LEFT_ROOM)
    }

    /**
     * 메시지가 인증 관련 메시지인지 확인
     */
    fun isAuthMessage(): Boolean {
        return type in setOf(TYPE_AUTH, TYPE_AUTH_SUCCESS)
    }

    /**
     * 메시지가 시스템 메시지인지 확인
     */
    fun isSystemMessage(): Boolean {
        return type in setOf(TYPE_ERROR, TYPE_SYSTEM)
    }

    /**
     * 메시지가 특정 방과 관련이 있는지 확인
     */
    fun isForRoom(targetRoomId: String): Boolean {
        return roomId == targetRoomId
    }

    /**
     * 페이로드에서 텍스트 콘텐츠 추출
     */
    fun getTextContent(): String? {
        try {
            message?.payload?.let { json ->
                return json[com.example.domain.vo.message.MessagePayload.KEY_CONTENT]?.jsonPrimitive?.contentOrNull
            }
        } catch (_: Exception) {
        }
        return payload?.get(com.example.domain.vo.message.MessagePayload.KEY_CONTENT)?.jsonPrimitive?.contentOrNull
    }

    /**
     * JSON 문자열로 변환
     */
    fun toJson(): String = toJson(this)

    /**
     * Map으로 변환
     */
    fun toMap(): Map<String, Any?> = toMap(this)

    /**
     * 메시지의 간단한 문자열 표현
     */
    override fun toString(): String {
        return "WebSocketMessage(type=$type, roomId=$roomId, senderId=$senderId, nestedId=${message?.id})"
    }
}

/**
 * 중첩 도메인 메시지 표현 (WebSocket 전송 전용)
 *
 * 필드 → 도메인 Message 매핑
 * - id                → Message.id
 * - messageType       → Message.messageType (문자열 ↔ enum 변환은 상위 매퍼에서 처리)
 * - payload           → Message.payload (JSON 전체 보존)
 * - senderId          → Message.senderId
 * - replyToMessageId  → Message.replyToMessageId
 * - timestamp         → Message.createdAt (epoch seconds → Instant)
 *
 * 참고: 상위(WebSocketMessage)의 senderId/payload/replyToMessageId/timestamp는 폴백 용도.
 */
@Serializable
data class NestedMessage(
    val id: String? = null,
    val messageType: String? = null,
    val payload: JsonObject? = null,
    val senderId: String? = null,
    val replyToMessageId: String? = null,
    val timestamp: Double? = null
)

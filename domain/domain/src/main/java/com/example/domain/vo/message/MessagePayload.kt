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
     * JSON 키 상수 정의
     */
    companion object {
        // 핵심 키
        const val KEY_CONTENT = "content"
        const val KEY_ATTACHMENTS = "attachments"
        const val KEY_UPLOAD_PROGRESS = "uploadProgress"
        // Removed reply/mentions from payload. These belong to Message fields.

        // 첨부파일 키
        const val KEY_KIND = "kind"
        const val KEY_URL = "url"
        const val KEY_FILENAME = "filename"
        const val KEY_MIME = "mime"
        const val KEY_EXT = "ext"
        const val KEY_INDEX = "index"

        // 특수목적 키 (업로드 상태 관리용)
        const val KEY_UPLOADING = "uploading" // 특수목적: 업로드 진행 중 표시

        // 메타데이터 키 (낙관적 업데이트용)
        const val KEY_META = "_meta" // 특수목적: 낙관적 편집/삭제 메타데이터
        const val KEY_META_PENDING_OP = "pendingOp"
        const val KEY_META_BACKUP_PAYLOAD = "backupPayload"

        // 낙관적 처리 메타 값
        const val OP_EDIT = "edit"
        const val OP_DELETE = "delete"

        /**
         * 일반 텍스트 메시지용 페이로드 생성
         */
        fun forText(content: String): MessagePayload {
            val jsonObject = buildJsonObject {
                put(KEY_CONTENT, content)
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
                put(KEY_CONTENT, content)
                put(KEY_ATTACHMENTS, JsonArray(attachmentJsonObjects))
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
            ext: String? = null,
            index: Int? = null
        ): Map<String, Any?> {
            return buildMap {
                put(KEY_KIND, kind)
                put(KEY_URL, url)
                put(KEY_MIME, mime)
                filename?.let { put(KEY_FILENAME, it) }
                ext?.let { put(KEY_EXT, it) }
                index?.let { put(KEY_INDEX, it) }
            }
        }

        /** 업로드 완료된 이미지 목록을 포함한 payload 생성 */
        fun forImages(content: String, images: List<Map<String, Any?>>): MessagePayload {
            return forTextWithAttachments(content, images)
        }

        /** 단일 이미지 placeholder payload 생성 (로컬 URI, 업로딩 표시, 진행률 0) */
        fun forImagePlaceholder(content: String, localUri: String): MessagePayload {
            val attachment = buildMap<String, Any?> {
                put(KEY_KIND, "image")
                put(KEY_URL, localUri)
                put(KEY_UPLOADING, true)
                put(KEY_INDEX, 0)
            }
            val payload = forTextWithAttachments(content, listOf(attachment))
            return payload.updateValue(KEY_UPLOAD_PROGRESS, 0)
        }

        /** 다중 이미지 placeholder payload 생성 (로컬 URI들, 업로딩 표시, 진행률 0) */
        fun forImagePlaceholders(content: String, localUris: List<String>): MessagePayload {
            val attachments = localUris.mapIndexed { index, uri ->
                buildMap<String, Any?> {
                    put(KEY_KIND, "image")
                    put(KEY_URL, uri)
                    put(KEY_UPLOADING, true)
                    put(KEY_INDEX, index)
                }
            }
            val payload = forTextWithAttachments(content, attachments)
            return payload.updateValue(KEY_UPLOAD_PROGRESS, 0)
        }

        /**
         * 프로젝트 초대 메시지용 페이로드 생성
         */
        fun forProjectInvite(
            projectId: String,
            projectName: String,
            inviterName: String,
            invitationId: String,
            actionText: String = "참여하기"
        ): MessagePayload {
            val jsonObject = buildJsonObject {
                put(KEY_CONTENT, "")
                put("projectId", projectId)
                put("projectName", projectName)
                put("inviterName", inviterName)
                put("invitationId", invitationId)
                put("actionText", actionText)
            }
            return MessagePayload(jsonObject.toString())
        }

        /**
         * 프로젝트 초대(토큰/초대ID 없이)용 간단 페이로드 생성
         * - 고정 포맷: content + projectId + projectName + inviterName + actionText [+ targetUserId(optional)]
         */
        fun forProjectInviteBasic(
            projectId: String,
            projectName: String,
            inviterName: String,
            targetUserId: String? = null,
            actionText: String = "참여하기"
        ): MessagePayload {
            val message = "$inviterName 님이 $projectName 프로젝트에 초대했습니다"
            val jsonObject = buildJsonObject {
                put(KEY_CONTENT, message)
                put("projectId", projectId)
                put("projectName", projectName)
                put("inviterName", inviterName)
                put("actionText", actionText)
                targetUserId?.let { put("targetUserId", it) }
            }
            return MessagePayload(jsonObject.toString())
        }

        // Removed: reply/mentions factories — use Message.replyToMessageId and Message.mentions instead.

    }

    /**
     * content 값을 교체한 새 payload 반환
     */
    fun withContent(newContent: String): MessagePayload {
        return updateValue(KEY_CONTENT, newContent)
    }

    /**
     * 여러 키-값을 병합하여 새 payload 반환
     */
    fun withValues(values: Map<String, Any?>): MessagePayload {
        return try {
            val jsonObject = asJsonObject()
            val updatedJson = buildJsonObject {
                jsonObject.forEach { (key, value) -> put(key, value) }
                values.forEach { (key, value) ->
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
            MessagePayload(updatedJson.toString())
        } catch (_: Exception) {
            this
        }
    }


    /**
     * 낙관적 편집 메타 추가
     */
    fun addOptimisticEdit(backupPayloadJson: String): MessagePayload {
        return addMetadata(OP_EDIT, backupPayloadJson)
    }

    /**
     * 낙관적 삭제 메타 추가
     */
    fun addOptimisticDelete(backupPayloadJson: String): MessagePayload {
        return addMetadata(OP_DELETE, backupPayloadJson)
    }

    /**
     * 낙관적 메타 제거
     */
    fun clearOptimisticMeta(): MessagePayload = removeMetadata()

    /**
     * 낙관적 메타 조회
     */
    fun getOptimisticOp(): String? = getMetaPendingOp()

    fun getOptimisticBackup(): String? = getMetaBackupPayload()

    /**
     * 편집용 낙관적 payload 생성 (content 교체 + 메타 추가)
     */
    fun optimisticEdit(newContent: String, backupPayloadJson: String): MessagePayload {
        return forText(newContent).addOptimisticEdit(backupPayloadJson)
    }

    /**
     * 기존 payload 기반 낙관적 삭제 payload 생성 (메타만 추가)
     */
    fun optimisticDeleteFrom(current: MessagePayload): MessagePayload {
        return current.addOptimisticDelete(current.value)
    }

    /**
     * placeholder 기반 낙관적 삭제 payload 생성 (빈 content + 메타 추가)
     */
    fun optimisticDeletePlaceholder(backupPayloadJson: String = "{}"): MessagePayload {
        return forText("").addOptimisticDelete(backupPayloadJson)
    }

    // Map 변환은 도메인 경계를 벗어난 책임이므로 상위 레이어에서 필요 시 처리하도록 위임한다.

    /**
     * TEXT 타입 메시지의 content 추출
     */
    fun getTextContent(): String? {
        return try {
            asJsonObject()[KEY_CONTENT]?.jsonPrimitive?.contentOrNull
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
            val attachmentsArray = jsonObject[KEY_ATTACHMENTS]?.jsonArray ?: return emptyList()

            android.util.Log.d(
                "MessagePayload",
                "🖼️ [UI표시] Payload에서 첨부파일 추출: ${attachmentsArray.size}개"
            )

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
            asJsonObject().containsKey(KEY_ATTACHMENTS) && getAttachments().isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 특정 키의 값을 수정하는 메서드 (String)
     */
    fun updateValue(key: String, value: String): MessagePayload {
        return try {
            val jsonObject = asJsonObject()
            val updatedJson = buildJsonObject {
                jsonObject.forEach { (existingKey, existingValue) ->
                    if (existingKey == key) {
                        put(key, value)
                    } else {
                        put(existingKey, existingValue)
                    }
                }
                // 키가 존재하지 않는 경우 새로 추가
                if (!jsonObject.containsKey(key)) {
                    put(key, value)
                }
            }
            MessagePayload(updatedJson.toString())
        } catch (e: Exception) {
            this // 실패시 원본 반환
        }
    }

    /**
     * 특정 키의 값을 수정하는 메서드 (Int)
     */
    fun updateValue(key: String, value: Int): MessagePayload {
        return try {
            val jsonObject = asJsonObject()
            val updatedJson = buildJsonObject {
                jsonObject.forEach { (existingKey, existingValue) ->
                    if (existingKey == key) {
                        put(key, value)
                    } else {
                        put(existingKey, existingValue)
                    }
                }
                // 키가 존재하지 않는 경우 새로 추가
                if (!jsonObject.containsKey(key)) {
                    put(key, value)
                }
            }
            MessagePayload(updatedJson.toString())
        } catch (e: Exception) {
            this // 실패시 원본 반환
        }
    }

    /**
     * 특정 키의 값을 수정하는 메서드 (Boolean)
     */
    fun updateValue(key: String, value: Boolean): MessagePayload {
        return try {
            val jsonObject = asJsonObject()
            val updatedJson = buildJsonObject {
                jsonObject.forEach { (existingKey, existingValue) ->
                    if (existingKey == key) {
                        put(key, value)
                    } else {
                        put(existingKey, existingValue)
                    }
                }
                // 키가 존재하지 않는 경우 새로 추가
                if (!jsonObject.containsKey(key)) {
                    put(key, value)
                }
            }
            MessagePayload(updatedJson.toString())
        } catch (e: Exception) {
            this // 실패시 원본 반환
        }
    }

    /**
     * 메타데이터 추가 (낙관적 업데이트용)
     */
    fun addMetadata(pendingOp: String, backupPayload: String): MessagePayload {
        return try {
            val jsonObject = asJsonObject()
            val backupElement = try {
                Json.parseToJsonElement(backupPayload)
            } catch (e: Exception) {
                JsonPrimitive(backupPayload)
            }

            val meta = buildJsonObject {
                put(KEY_META_PENDING_OP, JsonPrimitive(pendingOp))
                put(KEY_META_BACKUP_PAYLOAD, backupElement)
            }

            val updatedJson = buildJsonObject {
                jsonObject.forEach { (key, value) ->
                    if (key != KEY_META) {
                        put(key, value)
                    }
                }
                put(KEY_META, meta)
            }

            MessagePayload(updatedJson.toString())
        } catch (e: Exception) {
            this
        }
    }

    /**
     * 메타데이터 제거
     */
    fun removeMetadata(): MessagePayload {
        return try {
            val jsonObject = asJsonObject()
            val cleanedJson = buildJsonObject {
                jsonObject.forEach { (key, value) ->
                    if (key != KEY_META) {
                        put(key, value)
                    }
                }
            }
            MessagePayload(cleanedJson.toString())
        } catch (e: Exception) {
            this
        }
    }

    /**
     * 메타데이터가 있는지 확인
     */
    fun hasMetadata(): Boolean {
        return try {
            asJsonObject().containsKey(KEY_META)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 메타데이터에서 pendingOp 값 조회
     */
    fun getMetaPendingOp(): String? {
        return try {
            val meta = asJsonObject()[KEY_META]?.jsonObject
            meta?.get(KEY_META_PENDING_OP)?.jsonPrimitive?.content
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 메타데이터에서 백업 페이로드 조회
     */
    fun getMetaBackupPayload(): String? {
        return try {
            val meta = asJsonObject()[KEY_META]?.jsonObject
            val backupElement = meta?.get(KEY_META_BACKUP_PAYLOAD)
            when (backupElement) {
                is JsonObject -> backupElement.toString()
                is JsonPrimitive -> backupElement.content
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 업로드 중인 첨부파일이 있는지 확인 (특수목적)
     */
    fun hasUploadingAttachments(): Boolean {
        return try {
            getAttachments().any { attachment ->
                attachment[KEY_UPLOADING] as? Boolean == true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 업로드 진행률 조회 (0.0 ~ 1.0)
     */
    fun getUploadProgress(): Float {
        return try {
            val json = asJsonObject()
            val progressPrimitive = json[KEY_UPLOAD_PROGRESS]?.jsonPrimitive
            val progress = progressPrimitive?.contentOrNull?.toFloatOrNull()
            when {
                progress != null -> progress.coerceIn(0f, 1f)
                hasUploadingAttachments() -> 0f
                else -> 1f
            }
        } catch (_: Exception) {
            1f
        }
    }

    /**
     * 첨부파일을 업로딩 상태로 표시 (특수목적)
     */
    fun markAsUploading(): MessagePayload {
        return try {
            val jsonObject = asJsonObject()
            val attachmentsArray = jsonObject[KEY_ATTACHMENTS]?.jsonArray ?: return this

            val updatedAttachments = attachmentsArray.map { element ->
                val attachment = element.jsonObject
                buildJsonObject {
                    attachment.forEach { (key, value) ->
                        put(key, value)
                    }
                    put(KEY_UPLOADING, JsonPrimitive(true))
                }
            }

            val updatedJson = buildJsonObject {
                jsonObject.forEach { (key, value) ->
                    if (key == KEY_ATTACHMENTS) {
                        put(key, JsonArray(updatedAttachments))
                    } else {
                        put(key, value)
                    }
                }
            }

            MessagePayload(updatedJson.toString())
        } catch (e: Exception) {
            this
        }
    }

    /**
     * 첨부파일을 업로드 완료 상태로 표시 (특수목적)
     */
    fun markAsUploaded(): MessagePayload {
        return try {
            val jsonObject = asJsonObject()
            val attachmentsArray = jsonObject[KEY_ATTACHMENTS]?.jsonArray ?: return this
            
            val updatedAttachments = attachmentsArray.map { element ->
                val attachment = element.jsonObject
                buildJsonObject {
                    attachment.forEach { (key, value) ->
                        if (key != KEY_UPLOADING) {
                            put(key, value)
                        }
                    }
                }
            }

            val updatedJson = buildJsonObject {
                jsonObject.forEach { (key, value) ->
                    if (key == KEY_ATTACHMENTS) {
                        put(key, JsonArray(updatedAttachments))
                    } else {
                        put(key, value)
                    }
                }
            }

            MessagePayload(updatedJson.toString())
        } catch (e: Exception) {
            this
        }
    }

    /**
     * 첨부파일 추가
     */
    fun addAttachment(
        kind: String,
        url: String,
        filename: String? = null,
        mime: String? = null,
        ext: String? = null,
        index: Int? = null
    ): MessagePayload {
        return try {
            val jsonObject = asJsonObject()
            val existingAttachments =
                jsonObject[KEY_ATTACHMENTS]?.jsonArray ?: JsonArray(emptyList())

            val newAttachment = buildJsonObject {
                put(KEY_KIND, JsonPrimitive(kind))
                put(KEY_URL, JsonPrimitive(url))
                filename?.let { put(KEY_FILENAME, JsonPrimitive(it)) }
                mime?.let { put(KEY_MIME, JsonPrimitive(it)) }
                ext?.let { put(KEY_EXT, JsonPrimitive(it)) }
                index?.let { put(KEY_INDEX, JsonPrimitive(it)) }
            }

            val updatedAttachments = buildList {
                existingAttachments.forEach { add(it) }
                add(newAttachment)
            }
            
            val updatedJson = buildJsonObject {
                jsonObject.forEach { (key, value) ->
                    if (key != KEY_ATTACHMENTS) {
                        put(key, value)
                    }
                }
                put(KEY_ATTACHMENTS, JsonArray(updatedAttachments))
            }

            MessagePayload(updatedJson.toString())
        } catch (e: Exception) {
            this
        }
    }

    /**
     * 첨부파일 URL 업데이트
     */
    fun updateAttachmentUrl(oldUrl: String, newUrl: String): MessagePayload {
        return try {
            val jsonObject = asJsonObject()
            val attachmentsArray = jsonObject[KEY_ATTACHMENTS]?.jsonArray ?: return this
            
            val updatedAttachments = attachmentsArray.map { element ->
                val attachment = element.jsonObject
                val currentUrl = attachment[KEY_URL]?.jsonPrimitive?.content

                if (currentUrl == oldUrl) {
                    buildJsonObject {
                        attachment.forEach { (key, value) ->
                            if (key == KEY_URL) {
                                put(key, JsonPrimitive(newUrl))
                            } else {
                                put(key, value)
                            }
                        }
                    }
                } else {
                    attachment
                }
            }

            val updatedJson = buildJsonObject {
                jsonObject.forEach { (key, value) ->
                    if (key == KEY_ATTACHMENTS) {
                        put(key, JsonArray(updatedAttachments))
                    } else {
                        put(key, value)
                    }
                }
            }

            MessagePayload(updatedJson.toString())
        } catch (e: Exception) {
            this
        }
    }

    // Removed: withReply/withMentions — reply/mentions are not part of payload.

    // Removed: reply/mentions getters — keep these in Message.

}

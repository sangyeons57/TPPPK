package com.example.domain.model.enum

import com.google.firebase.firestore.PropertyName

/**
 * 메시지 첨부 파일의 유형을 나타내는 열거형입니다.
 * Firestore의 `messages/{messageId}/attachments/{attachmentId}.attachmentType` 필드 값과 일치시키거나 매핑됩니다.
 */
enum class MessageAttachmentType(val value: String) {
    /**
     * 이미지 파일 첨부
     */
    @PropertyName("image")
    IMAGE("image"),

    /**
     * 일반 파일 첨부
     */
    @PropertyName("file")
    FILE("file"),

    /**
     * 비디오 파일 첨부
     */
    @PropertyName("video")
    VIDEO("video"),

    /**
     * 오디오 파일 첨부 (DTO 주석에는 없었으나 일반적인 유형)
     */
    @PropertyName("audio")
    AUDIO("audio"),

    /**
     * 링크 첨부 (DTO 주석에는 없었으나 일반적인 유형)
     */
    @PropertyName("link")
    LINK("link"),

    /**
     * 알 수 없거나 정의되지 않은 첨부 파일 유형
     */
    @PropertyName("unknown")
    UNKNOWN("unknown");

    companion object {
        /**
         * 문자열 값으로부터 MessageAttachmentType Enum 상수를 반환합니다.
         * 일치하는 값이 없으면 UNKNOWN을 반환합니다.
         * @param value 찾고자 하는 Enum 상수의 문자열 값
         * @return 매칭되는 MessageAttachmentType 상수, 없으면 MessageAttachmentType.UNKNOWN
         */
        fun fromString(value: String?): MessageAttachmentType {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
} 
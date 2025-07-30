package com.example.domain.model.enum

import com.google.firebase.firestore.PropertyName

/**
 * 메시지 첨부 파일의 업로드 상태를 나타내는 열거형입니다.
 */
enum class MessageAttachmentUploadStatus(val value: String) {
    /**
     * 업로드 대기 중
     */
    @PropertyName("pending")
    PENDING("pending"),

    /**
     * 업로드 진행 중
     */
    @PropertyName("uploading")
    UPLOADING("uploading"),

    /**
     * 업로드 완료
     */
    @PropertyName("completed")
    COMPLETED("completed"),

    /**
     * 업로드 실패
     */
    @PropertyName("failed")
    FAILED("failed");

    companion object {
        /**
         * 문자열 값으로부터 MessageAttachmentUploadStatus Enum 상수를 반환합니다.
         * 일치하는 값이 없으면 PENDING을 반환합니다.
         * @param value 찾고자 하는 Enum 상수의 문자열 값
         * @return 매칭되는 MessageAttachmentUploadStatus 상수, 없으면 MessageAttachmentUploadStatus.PENDING
         */
        fun fromString(value: String?): MessageAttachmentUploadStatus {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}
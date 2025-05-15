package com.example.domain.model

/**
 * 메시지 첨부파일 유형입니다.
 */
enum class AttachmentType(val typeString: String) {
    IMAGE("image"), 
    VIDEO("video"), 
    FILE("file"), 
    AUDIO("audio"), 
    LINK("link"), 
    UNKNOWN("unknown");

    companion object {
        /**
         * 제공된 문자열 값으로부터 해당하는 AttachmentType enum 상수를 반환합니다.
         * 일치하는 typeString을 가진 상수가 없으면 UNKNOWN을 반환합니다.
         * @param typeString 찾고자 하는 enum 상수의 문자열 값 (예: "image", "video")
         * @return 매칭되는 AttachmentType 상수, 없으면 AttachmentType.UNKNOWN
         */
        fun fromString(typeString: String?): AttachmentType {
            return values().find { it.typeString.equals(typeString, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

/**
 * 메시지 첨부파일을 나타내는 클래스입니다.
 */
data class MessageAttachment(
    /**
     * 첨부파일 ID입니다.
     */
    val id: String,
    
    /**
     * 첨부파일 유형입니다.
     */
    val type: AttachmentType,
    
    /**
     * 첨부파일 URL입니다.
     */
    val url: String,
    
    /**
     * 첨부파일 이름입니다.
     */
    val fileName: String,
    
    /**
     * 첨부파일 크기(바이트)입니다.
     */
    val size: Long? = null,
    
    /**
     * 첨부파일의 MIME 타입입니다.
     */
    val mimeType: String? = null,
    
    /**
     * 썸네일 URL입니다. (이미지/비디오인 경우)
     */
    val thumbnailUrl: String? = null
) 
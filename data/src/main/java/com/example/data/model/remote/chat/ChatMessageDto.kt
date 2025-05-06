package com.example.data.model.remote.chat

/**
 * 채팅 메시지 데이터 전송 객체
 * Firebase Firestore와 데이터를 주고받기 위한 모델
 * 
 * @property chatId 메시지의 고유 ID
 * @property channelId 메시지가 속한 채널 ID
 * @property userId 메시지 작성자의 사용자 ID
 * @property userName 메시지 작성자 이름
 * @property userProfileUrl 메시지 작성자 프로필 이미지 URL (nullable)
 * @property message 메시지 내용
 * @property sentAt 메시지 전송 시간 (Unix timestamp)
 * @property isModified 메시지 수정 여부
 * @property attachmentImageUrls 첨부된 이미지 URL 목록 (nullable)
 */
data class ChatMessageDto(
    val chatId: Int,
    val channelId: String,
    val userId: Int,
    val userName: String,
    val userProfileUrl: String? = null,
    val message: String,
    val sentAt: Long, // Unix timestamp (milliseconds)
    val isModified: Boolean = false,
    val attachmentImageUrls: List<String>? = null
) {
    /**
     * Firestore 문서에서 사용할 수 있는 Map으로 변환합니다.
     * 
     * @return Firestore 문서로 저장 가능한 Map
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "chatId" to chatId,
            "channelId" to channelId,
            "userId" to userId,
            "userName" to userName,
            "userProfileUrl" to userProfileUrl,
            "message" to message,
            "sentAt" to sentAt,
            "isModified" to isModified,
            "attachmentImageUrls" to attachmentImageUrls
        )
    }
    
    companion object {
        /**
         * Firestore 문서 데이터에서 ChatMessageDto 객체를 생성합니다.
         * 
         * @param data Firestore 문서 데이터
         * @param id 문서 ID (필요한 경우 사용)
         * @return 생성된 ChatMessageDto 객체
         */
        fun fromMap(data: Map<String, Any?>, id: String? = null): ChatMessageDto {
            return ChatMessageDto(
                chatId = (data["chatId"] as? Number)?.toInt() ?: 0,
                channelId = data["channelId"] as? String ?: "",
                userId = (data["userId"] as? Number)?.toInt() ?: 0,
                userName = data["userName"] as? String ?: "",
                userProfileUrl = data["userProfileUrl"] as? String,
                message = data["message"] as? String ?: "",
                sentAt = (data["sentAt"] as? Number)?.toLong() ?: 0L,
                isModified = data["isModified"] as? Boolean ?: false,
                attachmentImageUrls = (data["attachmentImageUrls"] as? List<*>)?.mapNotNull { it as? String }
            )
        }
    }
} 
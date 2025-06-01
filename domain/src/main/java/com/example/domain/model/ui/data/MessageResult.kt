package com.example.domain.model.ui.search

import java.time.Instant

/**
 * 메시지 검색 결과를 나타내는 UI 모델 클래스
 * 검색 결과 화면에서 표시되는 메시지 정보를 포함합니다.
 * 
 * @property id 메시지의 고유 식별자
 * @property channelId 메시지가 속한 채널 ID
 * @property channelName 메시지가 속한 채널 이름
 * @property messageId 메시지 ID
 * @property messageContent 메시지 내용
 * @property timestamp 메시지 작성 시간
 * @property senderId 발신자 ID
 * @property senderName 발신자 이름
 * @property highlightedContent 검색 키워드가 하이라이트된 메시지 내용 (선택적)
 */
data class MessageResult(
    override val id: String,
    val channelId: String,
    val channelName: String,
    val messageId: String,
    val messageContent: String,
    val timestamp: Instant,
    val senderId: String,
    val senderName: String,
    val highlightedContent: String? = null
) : SearchResultItem {
    override val type: SearchResultType = SearchResultType.MESSAGE
    
    companion object {
        /**
         * 데이터 레이어의 SearchResult.Message 객체를 UI 레이어의 MessageResult로 변환
         *
         * @param message 데이터 모델 메시지 검색 결과
         * @return UI 모델 메시지 검색 결과
         */
        fun fromDataModel(message: com.example.domain.model.data.search.SearchResult.Message): MessageResult {
            return MessageResult(
                id = message.id,
                channelId = message.channelId,
                channelName = message.channelName,
                messageId = message.messageId,
                messageContent = message.content,
                timestamp = message.timestamp,
                senderId = message.senderId,
                senderName = message.senderName
            )
        }
    }
}

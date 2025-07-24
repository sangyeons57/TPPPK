package com.example.feature_chat.util

import com.example.domain.model.vo.DocumentId
import java.util.regex.Pattern

/**
 * 답장 기능을 위한 유틸리티 클래스
 * 메시지에서 답장 정보를 파싱하고 처리
 */
object ReplyParser {

    // 답장 패턴: ">>messageId" 형태
    private val REPLY_PATTERN = Pattern.compile("^>>([a-zA-Z0-9_-]+)")

    /**
     * 메시지 텍스트에서 답장 정보 파싱
     * @param text 파싱할 메시지 텍스트
     * @return 답장 대상 메시지 ID (없으면 null)
     */
    fun parseReplyToMessageId(text: String): DocumentId? {
        val matcher = REPLY_PATTERN.matcher(text.trim())
        return if (matcher.find()) {
            val messageId = matcher.group(1)
            if (messageId != null) DocumentId(messageId) else null
        } else {
            null
        }
    }

    /**
     * 답장 패턴을 제거한 순수 메시지 텍스트 추출
     * @param text 원본 텍스트
     * @return 답장 패턴이 제거된 텍스트
     */
    fun extractMessageContent(text: String): String {
        return text.replace(REPLY_PATTERN.toRegex(), "").trim()
    }

    /**
     * 답장 메시지인지 확인
     * @param text 확인할 텍스트
     * @return 답장 메시지 여부
     */
    fun isReplyMessage(text: String): Boolean {
        return REPLY_PATTERN.matcher(text.trim()).find()
    }

    /**
     * 답장 메시지 텍스트 생성
     * @param replyToMessageId 답장 대상 메시지 ID
     * @param content 실제 메시지 내용
     * @return 답장 패턴이 포함된 완전한 메시지 텍스트
     */
    fun createReplyMessageText(replyToMessageId: DocumentId, content: String): String {
        return ">>${replyToMessageId.value} $content"
    }

    /**
     * 메시지가 특정 메시지에 대한 답장인지 확인
     * @param text 확인할 메시지 텍스트
     * @param targetMessageId 대상 메시지 ID
     * @return 해당 메시지에 대한 답장 여부
     */
    fun isReplyTo(text: String, targetMessageId: DocumentId): Boolean {
        val replyToId = parseReplyToMessageId(text)
        return replyToId?.value == targetMessageId.value
    }
}
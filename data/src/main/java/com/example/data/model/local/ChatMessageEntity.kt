package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.time.Instant

/**
 * 로컬 채팅 메시지 저장을 위한 Room Entity
 * updateAt 필드를 활용한 증분 동기화 지원
 */
@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["channelId", "createdAt"]), // 채널별 시간순 정렬을 위한 인덱스
        Index(value = ["channelId", "updatedAt"]), // 증분 동기화를 위한 인덱스
        Index(value = ["messageId"], unique = true) // 중복 방지를 위한 유니크 인덱스
    ]
)
data class ChatMessageEntity(
    @PrimaryKey
    val messageId: String,

    /**
     * 채널 ID (DM 채널 또는 프로젝트 채널)
     */
    val channelId: String,

    /**
     * 메시지 발송자 ID
     */
    val userId: String,

    /**
     * 메시지 내용
     */
    val content: String,

    /**
     * 메시지 생성 시간 (UTC)
     */
    val createdAt: Instant,

    /**
     * 메시지 최종 수정 시간 (UTC)
     * 🔑 핵심: 증분 동기화의 기준점
     */
    val updatedAt: Instant,

    /**
     * 답장 대상 메시지 ID (optional)
     */
    val replyToMessageId: String? = null,

    /**
     * 멘션 정보 (JSON serialized List<MentionInfo>)
     */
    val mentions: String? = null,

    /**
     * 첨부파일 정보 (JSON serialized)
     */
    val attachments: String? = null,

    /**
     * 삭제 여부
     */
    val isDeleted: Boolean = false,

    /**
     * 로컬 캐시 저장 시간 (동기화 관리용)
     */
    val localSavedAt: Instant = Instant.now()
)
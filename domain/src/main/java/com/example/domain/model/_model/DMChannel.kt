package com.example.domain.model

import com.google.firebase.firestore.DocumentId
import java.time.Instant

/**
 * DM 채널 정보를 나타내는 도메인 모델입니다.
 * Firestore의 DM 채널 데이터를 표현하며, DMChannelDTO와 1:1 매핑됩니다.
 */
data class DMChannel(
    /**
     * DM 채널의 고유 ID (Firestore Document ID)
     */
    @DocumentId
    val id: String,

    /**
     * DM 채널 참여자들의 사용자 ID 목록입니다.
     */
    val participants: List<String>,

    /**
     * 마지막 메시지 미리보기 텍스트입니다. (선택 사항)
     */
    val lastMessagePreview: String?,

    /**
     * 마지막 메시지가 전송된 시간 (UTC, 선택 사항) 입니다.
     */
    val lastMessageTimestamp: Instant?,

    /**
     * DM 채널 생성 시간 (UTC, 선택 사항) 입니다.
     */
    val createdAt: Instant?,

    /**
     * DM 채널 마지막 업데이트 시간 (UTC, 선택 사항) 입니다.
     */
    val updatedAt: Instant?
) 
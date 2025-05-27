package com.example.domain.model

import com.google.firebase.firestore.DocumentId
import java.time.Instant

/**
 * 프로젝트 채널 정보를 나타내는 도메인 모델입니다.
 * Firestore의 ProjectChannelDTO와 1:1 매핑됩니다.
 */
data class ProjectChannel(
    /**
     * 프로젝트 채널의 고유 ID (Firestore Document ID)
     */
    @DocumentId
    val id: String,

    /**
     * 채널의 이름입니다.
     */
    val channelName: String,

    /**
     * 채널의 타입입니다. (예: "MESSAGES", "TASKS")
     * (도메인 레벨에서 Enum으로 변환 고려)
     */
    val channelType: String,

    /**
     * 채널 생성 시간 (UTC, 선택 사항) 입니다.
     */
    val createdAt: Instant?,

    /**
     * 채널 마지막 업데이트 시간 (UTC, 선택 사항) 입니다.
     */
    val updatedAt: Instant?
) 
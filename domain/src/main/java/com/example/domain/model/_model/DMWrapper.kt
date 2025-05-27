package com.example.domain.model

import com.google.firebase.firestore.DocumentId
import java.time.Instant

/**
 * DM 목록 아이템 정보를 나타내는 도메인 모델입니다.
 * Firestore의 DMWrapperDTO와 1:1 매핑됩니다.
 */
data class DMWrapper(
    /**
     * DM 채널의 ID (Firestore Document ID)
     */
    @DocumentId
    val dmChannelId: String,

    /**
     * 대화 상대방 사용자의 ID입니다.
     */
    val otherUserId: String,

    /**
     * 대화 상대방 사용자의 이름입니다.
     */
    val otherUserName: String,

    /**
     * 대화 상대방 사용자의 프로필 이미지 URL입니다. (선택 사항)
     */
    val otherUserProfileImageUrl: String?,

    /**
     * 마지막 메시지 미리보기 텍스트입니다. (선택 사항)
     */
    val lastMessagePreview: String?,

    /**
     * 마지막 메시지가 전송된 시간 (UTC, 선택 사항) 입니다.
     */
    val lastMessageTimestamp: Instant?
) 
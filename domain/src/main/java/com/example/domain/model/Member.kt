package com.example.domain.model

import com.google.firebase.firestore.DocumentId
import java.time.Instant

/**
 * 프로젝트 멤버 정보를 나타내는 도메인 모델입니다.
 * Firestore의 MemberDTO와 1:1 매핑됩니다.
 */
data class Member(
    /**
     * 멤버의 사용자 ID (Firestore Document ID)
     */
    @DocumentId
    val userId: String,

    /**
     * 멤버가 프로젝트에 참여한 시간 (UTC, 선택 사항) 입니다.
     */
    val joinedAt: Instant?,

    /**
     * 멤버가 가진 역할의 ID입니다.
     */
    val roleId: String
) 
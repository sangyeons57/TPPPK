package com.example.domain.model

import com.google.firebase.firestore.DocumentId

/**
 * 권한 정보를 나타내는 도메인 모델입니다.
 * Firestore의 PermissionDTO와 1:1 매핑됩니다.
 */
data class Permission(
    /**
     * 권한의 고유 ID (Firestore Document ID)
     * 권한 이름 자체가 ID로 사용될 수 있습니다 (예: "CAN_EDIT_TASK").
     */
    @DocumentId
    val id: String,

    /**
     * 권한의 이름입니다.
     */
    val name: String,

    /**
     * 권한에 대한 설명입니다.
     */
    val description: String
) 
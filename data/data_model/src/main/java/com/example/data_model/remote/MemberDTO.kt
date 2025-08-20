package com.example.data_model.remote

import com.example.domain.AggregateRoot
import com.example.domain.DTO
import com.example.domain.model.base.Member
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/*
 * 프로젝트 구성원 정보를 나타내는 DTO 클래스
 */
data class MemberDTO(
    @DocumentId override val id: String = "",
    @get:PropertyName(ROLE_ID)
    val roleIds: List<String> = emptyList(),
    @get:PropertyName(STATUS)
    val status: String = "active", // 기존 데이터 호환성을 위한 기본값
    @get:PropertyName(BLOCKED_AT)
    val blockedAt: Date? = null,
    @get:PropertyName(BLOCKED_BY)
    val blockedBy: String? = null,
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null, // Map to joinedAt for compatibility
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {
        const val COLLECTION_NAME = Member.COLLECTION_NAME
        const val ROLE_ID = Member.KEY_ROLE_ID
        const val STATUS = Member.KEY_STATUS
        const val BLOCKED_AT = Member.KEY_BLOCKED_AT
        const val BLOCKED_BY = Member.KEY_BLOCKED_BY

    }
}


package com.example.data.model.remote

import com.example.domain.model.base.Member
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import java.util.Date

import com.google.firebase.firestore.PropertyName
import com.example.core_common.util.DateTimeUtil
import com.example.data.model.DTO
import com.example.domain.model.vo.DocumentId as VODocumentId
/*
 * 프로젝트 구성원 정보를 나타내는 DTO 클래스
 */
data class MemberDTO(
    @DocumentId override val id: String = "",
    @get:PropertyName(JOINED_AT)
    @ServerTimestamp val joinedAt: Date? = null,
    @get:PropertyName(ROLE_ID)
    val roleIds: List<String> = emptyList(),
    @ServerTimestamp override val createdAt: Date? = null, // Map to joinedAt for compatibility
    @ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {
        const val COLLECTION_NAME = Member.COLLECTION_NAME
        const val JOINED_AT = Member.KEY_JOINED_AT
        const val ROLE_ID = Member.KEY_ROLE_ID
        const val UPDATED_AT = Member.KEY_UPDATED_AT

        fun from(member: Member): MemberDTO {
            return MemberDTO(
                id = member.id.value,
                joinedAt = Date.from(member.joinedAt),
                roleIds = member.roleIds.map { it.value },
                createdAt = Date.from(member.createdAt),
                updatedAt = Date.from(member.updatedAt),
            )
        }
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return Member 도메인 모델
     */
    override fun toDomain(): Member {
        return Member.fromDataSource(
            id = VODocumentId(id),
            roleIds = roleIds.map { VODocumentId(it) },
            joinedAt = joinedAt?.toInstant() ?: createdAt?.toInstant() ?: Instant.EPOCH,
            updatedAt = updatedAt?.toInstant() ?: Instant.EPOCH
        )
    }
}

/**
 * Member 도메인 모델을 DTO로 변환하는 확장 함수
 * @return MemberDTO 객체
 */
fun Member.toDto(): MemberDTO {
    return MemberDTO(
        id = id.value,
        joinedAt = Date.from(joinedAt),
        roleIds = roleIds.map { it.value },
        createdAt = Date.from(createdAt),
        updatedAt = Date.from(updatedAt)
    )
}

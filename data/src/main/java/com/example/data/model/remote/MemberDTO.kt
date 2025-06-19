package com.example.data.model.remote

import com.example.domain.model.base.Member
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant

import com.google.firebase.firestore.PropertyName
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.vo.DocumentId as VODocumentId
/*
 * 프로젝트 구성원 정보를 나타내는 DTO 클래스
 */
data class MemberDTO(
    @DocumentId val userId: String = "",
    @get:PropertyName(JOINED_AT)
    @ServerTimestamp val joinedAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(UPDATED_AT)
    @ServerTimestamp val updatedAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(ROLE_ID)
    val roleIds: List<String> = emptyList()
) {

    companion object {
        const val COLLECTION_NAME = Member.COLLECTION_NAME
        const val JOINED_AT = Member.KEY_JOINED_AT
        const val ROLE_ID = Member.KEY_ROLE_ID
        const val UPDATED_AT = Member.KEY_UPDATED_AT
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return Member 도메인 모델
     */
    fun toDomain(): Member {
        return Member.fromDataSource(
            id = VODocumentId(userId),
            roleIds = roleIds.map { VODocumentId(it) },
            joinedAt = joinedAt.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            updatedAt = updatedAt.let{DateTimeUtil.firebaseTimestampToInstant(it)}
        )
    }
}

/**
 * Member 도메인 모델을 DTO로 변환하는 확장 함수
 * @return MemberDTO 객체
 */
fun Member.toDto(): MemberDTO {
    return MemberDTO(
        userId = id.value,
        joinedAt = joinedAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        roleIds = roleIds.map { it.value },
        updatedAt = updatedAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}

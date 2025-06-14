package com.example.data.model.remote

import com.example.domain.model.base.Member
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.core_common.constants.FirestoreConstants
import com.google.firebase.firestore.PropertyName
import com.example.core_common.util.DateTimeUtil

/*
 * 프로젝트 구성원 정보를 나타내는 DTO 클래스
 */
data class MemberDTO(
    @DocumentId val userId: String = "",
    @get:PropertyName(FirestoreConstants.Project.Members.JOINED_AT)
    @ServerTimestamp val joinedAt: Timestamp? = null,
    @get:PropertyName(FirestoreConstants.Project.Members.ROLE_ID)
    val roleIds: List<String> = emptyList()
) {
    /**
     * DTO를 도메인 모델로 변환
     * @return Member 도메인 모델
     */
    fun toDomain(): Member {
        return Member(
            userId = userId,
            joinedAt = joinedAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            roleIds = roleIds
        )
    }
}

/**
 * Member 도메인 모델을 DTO로 변환하는 확장 함수
 * @return MemberDTO 객체
 */
fun Member.toDto(): MemberDTO {
    return MemberDTO(
        userId = userId,
        joinedAt = joinedAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        roleIds = roleIds
    )
}

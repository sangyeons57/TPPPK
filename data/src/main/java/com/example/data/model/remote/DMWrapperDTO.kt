package com.example.data.model.remote


import com.example.core_common.util.DateTimeUtil
import com.example.data.model.DTO
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.Timestamp

/**
 * DM 채널 정보와 상대방 ID를 나타내는 DTO 클래스
 */
data class DMWrapperDTO(
    @DocumentId val id: String = "",
    @get:PropertyName(OTHER_USER_ID) 
    val otherUserId: String = "",
    @get:PropertyName(CREATED_AT)
    @ServerTimestamp val createdAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(UPDATED_AT)
    @ServerTimestamp val updatedAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
) : DTO {

    companion object {
        const val COLLECTION_NAME = DMWrapper.COLLECTION_NAME
        const val OTHER_USER_ID = DMWrapper.KEY_OTHER_USER_ID
        const val CREATED_AT = DMWrapper.KEY_CREATED_AT
        const val UPDATED_AT = DMWrapper.KEY_UPDATED_AT

        fun from (domain: DMWrapper): DMWrapperDTO {
            return DMWrapperDTO(
                id = domain.id.value,
                otherUserId = domain.otherUserId.value,
                createdAt = DateTimeUtil.instantToFirebaseTimestamp(domain.createdAt),
                updatedAt = DateTimeUtil.instantToFirebaseTimestamp(domain.updatedAt),
            )
        }
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return DMWrapper 도메인 모델
     */
    override fun toDomain(): DMWrapper {
        return DMWrapper.fromDataSource(
            id = VODocumentId(id),
            otherUserId = UserId(otherUserId),
            createdAt = DateTimeUtil.firebaseTimestampToInstant(createdAt),
            updatedAt = DateTimeUtil.firebaseTimestampToInstant(updatedAt)
        )
    }
}

/**
 * DMWrapper 도메인 모델을 DTO로 변환하는 확장 함수
 * @return DMWrapperDTO 객체
 */
fun DMWrapper.toDto(): DMWrapperDTO {
    return DMWrapperDTO(
        id = id.value,
        otherUserId = otherUserId.value,
        createdAt = createdAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)}
    )
}

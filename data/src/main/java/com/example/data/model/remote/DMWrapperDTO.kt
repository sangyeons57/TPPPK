package com.example.data.model.remote


import com.example.data.model.DTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.dmchannel.DMChannelLastMessagePreview
import com.example.domain.model.vo.user.UserName
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.Timestamp
import java.util.Date

/**
 * DM 채널 정보와 상대방 ID를 나타내는 DTO 클래스
 */
data class DMWrapperDTO(
    @DocumentId override val id: String = "",
    @get:PropertyName(OTHER_USER_ID) 
    val otherUserId: String = "",
    @get:PropertyName(OTHER_USER_NAME)
    val otherUserName: String = "",
    @get:PropertyName(OTHER_USER_IMAGE_URL)
    val otherUserImageUrl: String? = null,
    @get:PropertyName(LAST_MESSAGE_PREVIEW)
    val lastMessagePreview: String? = null,
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {
        const val COLLECTION_NAME = DMWrapper.COLLECTION_NAME
        const val OTHER_USER_ID = DMWrapper.KEY_OTHER_USER_ID
        const val OTHER_USER_NAME = DMWrapper.KEY_OTHER_USER_NAME
        const val OTHER_USER_IMAGE_URL = DMWrapper.KEY_OTHER_USER_IMAGE_URL
        const val LAST_MESSAGE_PREVIEW = DMWrapper.KEY_LAST_MESSAGE_PREVIEW
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return DMWrapper 도메인 모델
     */
    override fun toDomain(): DMWrapper {
        return DMWrapper.fromDataSource(
            id = VODocumentId(id),
            otherUserId = UserId(otherUserId),
            otherUserName = UserName(otherUserName),
            otherUserImageUrl = otherUserImageUrl?.let { ImageUrl(it) },
            lastMessagePreview = lastMessagePreview?.let { DMChannelLastMessagePreview(it) },
            createdAt = createdAt?.toInstant(),
            updatedAt = updatedAt?.toInstant()
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
        otherUserName = otherUserName.value,
        otherUserImageUrl = otherUserImageUrl?.value,
        lastMessagePreview = lastMessagePreview?.value, // timestamps omitted for ServerTimestamp
    )
}

package com.example.data.model.remote


import com.example.domain.model.AggregateRoot
import com.example.domain.model.DTO
import com.example.domain.model.base.DMWrapper
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
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
}

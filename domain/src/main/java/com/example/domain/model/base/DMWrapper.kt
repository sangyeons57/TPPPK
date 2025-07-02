package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.AggregateRoot
import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.event.dmwrapper.DMWrapperCreatedEvent
import com.example.domain.event.dmwrapper.DMWrapperOtherUserChangedEvent
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.dmchannel.DMChannelLastMessagePreview
import com.example.domain.model.vo.user.UserName
import java.time.Instant

class DMWrapper private constructor(
    initialOtherUserId: UserId,
    initialOtherUserName: UserName,
    initialOtherUserImageUrl: ImageUrl?,
    initialLastMessagePreview: DMChannelLastMessagePreview?,
    override val id: DocumentId,
    override val isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    var otherUserId: UserId = initialOtherUserId
        private set

    var otherUserName: UserName = initialOtherUserName
        private set

    var otherUserImageUrl: ImageUrl? = initialOtherUserImageUrl
        private set

    var lastMessagePreview: DMChannelLastMessagePreview? = initialLastMessagePreview
        private set

    init {
        setOriginalState()
    }

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_OTHER_USER_ID to otherUserId.value,
            KEY_OTHER_USER_NAME to otherUserName.value,
            KEY_OTHER_USER_IMAGE_URL to otherUserImageUrl?.value,
            KEY_LAST_MESSAGE_PREVIEW to lastMessagePreview?.value,
            KEY_CREATED_AT to createdAt,
            KEY_UPDATED_AT to updatedAt,
        )
    }

    fun changeOtherUser(newOtherUserId: UserId) {
        if (this.otherUserId == newOtherUserId) return

        this.otherUserId = newOtherUserId
        this.pushDomainEvent(DMWrapperOtherUserChangedEvent(this.id, newOtherUserId))
    }

    companion object {
        const val COLLECTION_NAME = "dm_wrapper"
        const val KEY_OTHER_USER_ID = "otherUserId"
        const val KEY_OTHER_USER_NAME = "otherUserName"
        const val KEY_OTHER_USER_IMAGE_URL = "otherUserImageUrl"
        const val KEY_LAST_MESSAGE_PREVIEW = "lastMessagePreview"

        fun create(
            otherUserId: UserId,
            otherUserName: UserName,
        ): DMWrapper {
            val dmWrapper = DMWrapper(
                id = DocumentId.EMPTY,
                initialOtherUserId = otherUserId,
                initialOtherUserName = otherUserName,
                initialOtherUserImageUrl = null,
                initialLastMessagePreview = null,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                isNew = true,
            )
            return dmWrapper
        }

        fun fromDataSource(
            id: DocumentId,
            otherUserId: UserId,
            otherUserName: UserName,
            otherUserImageUrl: ImageUrl?,
            lastMessagePreview: DMChannelLastMessagePreview?,
            createdAt: Instant?,
            updatedAt: Instant?
        ): DMWrapper {
            val dmWrapper = DMWrapper(
                id = id,
                initialOtherUserId = otherUserId,
                initialOtherUserName = otherUserName,
                initialOtherUserImageUrl = otherUserImageUrl,
                initialLastMessagePreview = lastMessagePreview,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                isNew = false,
            )
            return dmWrapper
        } 
    }
}

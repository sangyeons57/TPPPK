package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import com.example.domain.AggregateRoot
import com.example.domain.event.dmwrapper.DMWrapperOtherUserChangedEvent
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.user.UserName
import java.time.Instant

class DMWrapper private constructor(
    initialOtherUserId: UserId,
    initialOtherUserName: UserName,
    override val id: DocumentId,
    override val isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    var otherUserId: UserId = initialOtherUserId
        private set

    var otherUserName: UserName = initialOtherUserName
        private set

    init {
        setOriginalState()
    }

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_OTHER_USER_ID to otherUserId.value,
            KEY_OTHER_USER_NAME to otherUserName.value,
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

        fun create(
            otherUserId: UserId,
            otherUserName: UserName,
        ): DMWrapper {
            val dmWrapper = DMWrapper(
                id = DocumentId.EMPTY,
                initialOtherUserId = otherUserId,
                initialOtherUserName = otherUserName,
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
            createdAt: Instant?,
            updatedAt: Instant?
        ): DMWrapper {
            val dmWrapper = DMWrapper(
                id = id,
                initialOtherUserId = otherUserId,
                initialOtherUserName = otherUserName,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                isNew = false,
            )
            return dmWrapper
        } 
    }
}

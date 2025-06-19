package com.example.domain.model.base

import com.example.domain.event.AggregateRoot
import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.event.dmwrapper.DMWrapperCreatedEvent
import com.example.domain.event.dmwrapper.DMWrapperOtherUserChangedEvent
import com.example.domain.model.vo.UserId
import java.time.Instant

class DMWrapper private constructor(
    initialOtherUserId: UserId,
    initialCreatedAt: Instant,
    initialUpdatedAt: Instant,
    override val id: DocumentId,
    override val isNew: Boolean,
) : AggregateRoot() {

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_OTHER_USER_ID to otherUserId,
            KEY_CREATED_AT to createdAt,
            KEY_UPDATED_AT to updatedAt,
        )
    }

    var otherUserId: UserId = initialOtherUserId
        private set
    val createdAt: Instant = initialCreatedAt
    var updatedAt: Instant = initialUpdatedAt
        private set

    fun changeOtherUser(newOtherUserId: UserId) {
        if (this.otherUserId == newOtherUserId) return

        this.otherUserId = newOtherUserId
        this.updatedAt = Instant.now()
        this.pushDomainEvent(DMWrapperOtherUserChangedEvent(this.id, newOtherUserId))
    }

    companion object {
        const val COLLECTION_NAME = "dm_wrapper"
        const val KEY_OTHER_USER_ID = "otherUserId"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_UPDATED_AT = "updatedAt"

        fun create(
            id: DocumentId,
            otherUserId: UserId
        ): DMWrapper {
            val now = Instant.now()
            val dmWrapper = DMWrapper(
                id = id,
                initialOtherUserId = otherUserId,
                initialCreatedAt = now,
                initialUpdatedAt = now,
                isNew = true,
            )
            dmWrapper.pushDomainEvent(DMWrapperCreatedEvent(dmWrapper.id))
            return dmWrapper
        }

        fun fromDataSource(
            id: DocumentId,
            otherUserId: UserId,
            createdAt: Instant,
            updatedAt: Instant
        ): DMWrapper {
            val dmWrapper = DMWrapper(
                id = id,
                initialOtherUserId = otherUserId,
                initialCreatedAt = createdAt,
                initialUpdatedAt = updatedAt,
                isNew = false,
            )
            dmWrapper.pushDomainEvent(DMWrapperCreatedEvent(dmWrapper.id))
            return dmWrapper
        } 
    }
}

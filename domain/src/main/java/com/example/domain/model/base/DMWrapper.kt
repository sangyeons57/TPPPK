package com.example.domain.model.base

import com.example.domain.event.AggregateRoot
import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.dmwrapper.DMWrapperId
import com.example.domain.event.dmwrapper.DMWrapperCreatedEvent
import com.example.domain.event.dmwrapper.DMWrapperOtherUserChangedEvent
import java.time.Instant

class DMWrapper private constructor(
    dmChannelId: DMWrapperId,
    otherUserId: DocumentId,
    createdAt: Instant,
    updatedAt: Instant
) : AggregateRoot {

    private val _domainEvents: MutableList<DomainEvent> = mutableListOf()

    val dmChannelId: DMWrapperId = dmChannelId
    var otherUserId: DocumentId = otherUserId
        private set
    val createdAt: Instant = createdAt
    var updatedAt: Instant = updatedAt
        private set

    override fun pullDomainEvents(): List<DomainEvent> {
        val events = _domainEvents.toList()
        _domainEvents.clear()
        return events
    }

    override fun clearDomainEvents() {
        _domainEvents.clear()
    }

    fun changeOtherUser(newOtherUserId: DocumentId) {
        if (this.otherUserId == newOtherUserId) return

        this.otherUserId = newOtherUserId
        this.updatedAt = Instant.now()
        _domainEvents.add(DMWrapperOtherUserChangedEvent(this.dmChannelId, newOtherUserId))
    }

    companion object {
        fun create(
            dmChannelId: DMWrapperId,
            otherUserId: DocumentId
        ): DMWrapper {
            val now = Instant.now()
            val dmWrapper = DMWrapper(
                dmChannelId = dmChannelId,
                otherUserId = otherUserId,
                createdAt = now,
                updatedAt = now
            )
            dmWrapper._domainEvents.add(DMWrapperCreatedEvent(dmWrapper.dmChannelId))
            return dmWrapper
        }
    }
}

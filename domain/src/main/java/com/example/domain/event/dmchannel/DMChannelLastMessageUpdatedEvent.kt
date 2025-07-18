package com.example.domain.event.dmchannel

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Represents an event that is triggered when the last message of a DMChannel is updated.
 *
 * @property dmChannelId The ID of the DM channel whose last message was updated.
 * @property occurredOn The timestamp when the event occurred.
 */
data class DMChannelLastMessageUpdatedEvent(
    val dmChannelId: DocumentId,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent

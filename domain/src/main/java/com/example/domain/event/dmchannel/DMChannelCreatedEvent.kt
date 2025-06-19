package com.example.domain.event.dmchannel

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Represents an event that is triggered when a new DMChannel is created.
 *
 * @property dmChannelId The ID of the newly created DM channel.
 * @property occurredOn The timestamp when the event occurred.
 */
data class DMChannelCreatedEvent(
    val dmChannelId: DocumentId,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent

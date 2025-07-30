package com.example.domain.event.dmchannel

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import java.time.Instant

/**
 * Represents an event that is triggered when the participant list of a DMChannel changes.
 *
 * @property dmChannelId The ID of the DM channel whose participants changed.
 * @property participants The new list of participants in the DM channel.
 * @property occurredOn The timestamp when the event occurred.
 */
data class DMChannelParticipantsChangedEvent(
    val dmChannelId: DocumentId,
    val participants: List<UserId>,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent

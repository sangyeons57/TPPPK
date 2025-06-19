package com.example.domain.event.projectchannel

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Event indicating that a project channel's name has been updated.
 */
data class ProjectChannelNameUpdatedEvent(
    val channelId: DocumentId,
    val newName: String,
    override val occurredOn: Instant
) : DomainEvent

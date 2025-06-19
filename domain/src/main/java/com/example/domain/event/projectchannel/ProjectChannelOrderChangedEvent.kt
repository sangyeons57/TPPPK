package com.example.domain.event.projectchannel

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Event indicating that a project channel's order has been changed.
 */
data class ProjectChannelOrderChangedEvent(
    val channelId: DocumentId,
    val newOrder: Double,
    override val occurredOn: Instant
) : DomainEvent

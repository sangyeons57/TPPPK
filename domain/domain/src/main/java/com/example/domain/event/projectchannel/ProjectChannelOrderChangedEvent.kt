package com.example.domain.event.projectchannel

import com.example.domain.event.DomainEvent
import com.example.domain.vo.DocumentId
import com.example.domain.vo.projectchannel.ProjectChannelOrder
import java.time.Instant

/**
 * Event indicating that a project channel's order has been changed.
 */
data class ProjectChannelOrderChangedEvent(
    val channelId: DocumentId,
    val newOrder: ProjectChannelOrder,
    override val occurredOn: Instant
) : DomainEvent

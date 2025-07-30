package com.example.domain.event.projectchannel

import com.example.domain.event.DomainEvent
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import java.time.Instant

/**
 * Event indicating that a new project channel has been created.
 */
data class ProjectChannelCreatedEvent(
    val channelId: DocumentId,
    val channelName: Name,
    val channelType: ProjectChannelType,
    val order: ProjectChannelOrder,
    override val occurredOn: Instant
) : DomainEvent

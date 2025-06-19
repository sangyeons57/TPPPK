package com.example.domain.event.projectchannel

import com.example.domain.event.DomainEvent
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Event indicating that a new project channel has been created.
 */
data class ProjectChannelCreatedEvent(
    val channelId: DocumentId,
    val channelName: String,
    val channelType: ProjectChannelType,
    val order: Double,
    override val occurredOn: Instant
) : DomainEvent

package com.example.domain.event.project

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import java.time.Instant

/**
 * Event indicating that a project's image URL has been changed.
 */
data class ProjectImageUrlChangedEvent(
    val projectId: DocumentId,
    val newImageUrl: ImageUrl?,
    override val occurredOn: Instant
) : DomainEvent

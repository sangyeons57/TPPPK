package com.example.domain.event.dmwrapper

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.dmwrapper.DMWrapperId
import com.example.domain.model.vo.DocumentId // Added this import
import java.time.Instant

data class DMWrapperOtherUserChangedEvent(
    val dmWrapperId: DMWrapperId,
    val newOtherUserId: DocumentId, // Changed type to DocumentId
    override val occurredOn: Instant = Instant.now()
) : DomainEvent

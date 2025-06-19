package com.example.domain.event.dmwrapper

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId // Added this import
import com.example.domain.model.vo.UserId
import java.time.Instant

data class DMWrapperOtherUserChangedEvent(
    val dmWrapperId: DocumentId,
    val newOtherUserId: UserId, // Changed type to DocumentId
    override val occurredOn: Instant = Instant.now()
) : DomainEvent

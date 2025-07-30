package com.example.domain.event.dmwrapper

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

data class DMWrapperCreatedEvent(
    val dmWrapperId: DocumentId,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent

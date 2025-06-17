package com.example.domain.event.dmwrapper

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.dmwrapper.DMWrapperId
import java.time.Instant

data class DMWrapperCreatedEvent(
    val dmWrapperId: DMWrapperId,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent

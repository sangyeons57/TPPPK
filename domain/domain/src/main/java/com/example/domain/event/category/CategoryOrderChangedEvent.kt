package com.example.domain.event.category

import com.example.domain.event.DomainEvent
import java.time.Instant

/**
 * Raised when a category's order is changed.
 *
 * @param categoryId The identifier of the category whose order changed.
 * @param occurredOn Timestamp when the event occurred.
 */
data class CategoryOrderChangedEvent(
    val categoryId: String,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent

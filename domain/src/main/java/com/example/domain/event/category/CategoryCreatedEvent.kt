package com.example.domain.event.category

import com.example.domain.event.DomainEvent
import java.time.Instant

/**
 * Raised when a new category is created.
 *
 * @param categoryId The identifier of the newly created category.
 * @param occurredOn The time when the category was created.
 */
data class CategoryCreatedEvent(
    override val occurredOn: Instant = Instant.now()
) : DomainEvent

package com.example.domain.model.base

import com.example.domain.event.AggregateRoot
import com.example.domain.event.DomainEvent
import com.example.domain.event.category.CategoryCreatedEvent // These will be defined in the next step
import com.example.domain.event.category.CategoryNameChangedEvent // These will be defined in the next step
import com.example.domain.event.category.CategoryOrderChangedEvent // These will be defined in the next step
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.category.CategoryId
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.model.vo.category.IsCategoryFlag
import java.time.Instant

/**
 * Represents a Category aggregate in the domain.
 * A category is used to group channels or other items, typically within a project context.
 * Its state changes are tracked via domain events.
 *
 * @property id The unique identifier of the category.
 * @property name The name of the category. Mutable via [changeName].
 * @property order The display order of the category. Mutable via [changeOrder].
 * @property createdBy The identifier of the user/entity that created this category. Immutable.
 * @property createdAt Timestamp of when the category was created. Immutable.
 * @property updatedAt Timestamp of the last update to the category. Mutable.
 * @property isCategory Flag indicating if this entity is a category (true by default). Immutable.
 */
class Category private constructor(
    // Constructor parameters are intentionally without val/var
    // and match the properties declared in the class body.
    id: CategoryId,
    name: CategoryName,
    order: CategoryOrder,
    createdBy: DocumentId,
    createdAt: Instant,
    updatedAt: Instant?,
    isCategory: IsCategoryFlag
) : AggregateRoot {

    val id: CategoryId = id
    var name: CategoryName = name
        private set
    var order: CategoryOrder = order
        private set
    val createdBy: DocumentId = createdBy
    val createdAt: Instant = createdAt
    var updatedAt: Instant? = updatedAt
        private set
    val isCategory: IsCategoryFlag = isCategory

    private val _domainEvents: MutableList<DomainEvent> = mutableListOf()

    /**
     * Retrieves all domain events that have occurred since the last call and clears the list.
     * @return A list of [DomainEvent] objects.
     */
    override fun pullDomainEvents(): List<DomainEvent> {
        val events = _domainEvents.toList() // Make a copy
        _domainEvents.clear()
        return events
    }

    /**
     * Clears all domain events currently held by the aggregate.
     * Typically used by the event dispatcher after processing events.
     */
    override fun clearDomainEvents() {
        _domainEvents.clear()
    }

    /**
     * Changes the name of the category.
     * If the new name is different from the current name, the `name` and `updatedAt` properties are updated,
     * and a [CategoryNameChangedEvent] is raised.
     *
     * @param newName The new name for the category.
     */
    fun changeName(newName: CategoryName) {
        if (this.name == newName) return // No change if the name is the same

        this.name = newName
        this.updatedAt = Instant.now()
        _domainEvents.add(CategoryNameChangedEvent(this.id.value))
    }

    /**
     * Changes the display order of the category.
     * If the new order is different from the current order, the `order` and `updatedAt` properties are updated,
     * and a [CategoryOrderChangedEvent] is raised.
     *
     * @param newOrder The new order for the category.
     */
    fun changeOrder(newOrder: CategoryOrder) {
        if (this.order == newOrder) return // No change if the order is the same

        this.order = newOrder
        this.updatedAt = Instant.now()
        _domainEvents.add(CategoryOrderChangedEvent(this.id.value))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Category
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        /**
         * Creates a new Category instance.
         * This factory method is the designated way to create new categories.
         * It initializes the category's creation and update timestamps and raises a [CategoryCreatedEvent].
         *
         * @param id The unique identifier for the new category.
         * @param name The initial name of the category.
         * @param order The initial display order of the category.
         * @param createdBy The identifier of the user/entity creating the category.
         * @param isCategory Flag indicating if this is a category. Defaults to true.
         * @return A new [Category] instance, ready to be persisted.
         */
        fun create(
            id: CategoryId,
            name: CategoryName,
            order: CategoryOrder,
            createdBy: DocumentId,
            isCategory: IsCategoryFlag = IsCategoryFlag(true)
        ): Category {
            val now = Instant.now()
            val category = Category(
                id = id,
                name = name,
                order = order,
                createdBy = createdBy,
                createdAt = now,
                updatedAt = now,
                isCategory = isCategory
            )
            category._domainEvents.add(CategoryCreatedEvent(id.value))
            return category
        }

        /**
         * Reconstructs a Category instance from a data source (e.g., when loading from a database).
         * This method assumes the data is valid as it's coming from a trusted source.
         * No domain events are raised during reconstruction.
         *
         * @param id The category's identifier.
         * @param name The category's name.
         * @param order The category's display order.
         * @param createdBy Identifier of the creating user/entity.
         * @param createdAt Timestamp of creation.
         * @param updatedAt Timestamp of the last update, can be null if never updated post-creation.
         * @param isCategory Flag indicating if this is a category.
         * @return An instance of [Category] populated with data source values.
         */
        fun fromDataSource(
            id: CategoryId,
            name: CategoryName,
            order: CategoryOrder,
            createdBy: DocumentId,
            createdAt: Instant,
            updatedAt: Instant?,
            isCategory: IsCategoryFlag
        ): Category {
            return Category(
                id = id,
                name = name,
                order = order,
                createdBy = createdBy,
                createdAt = createdAt,
                updatedAt = updatedAt,
                isCategory = isCategory
            )
        }
    }
}

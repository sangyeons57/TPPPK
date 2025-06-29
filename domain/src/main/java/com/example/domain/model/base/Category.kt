package com.example.domain.model.base

import com.example.core_common.constants.Constants
import com.example.domain.model.AggregateRoot
import com.example.domain.event.category.CategoryCreatedEvent // These will be defined in the next step
import com.example.domain.event.category.CategoryNameChangedEvent // These will be defined in the next step
import com.example.domain.event.category.CategoryOrderChangedEvent // These will be defined in the next step
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.model.vo.category.IsCategoryFlag
import java.time.Instant
import com.example.core_common.util.DateTimeUtil

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
    initialName: CategoryName,
    initialOrder: CategoryOrder,
    initialCreatedBy: OwnerId,
    initialIsCategory: IsCategoryFlag,
    override val id: DocumentId,
    override val isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_NAME to name.value,
            KEY_ORDER to order.value,
            KEY_CREATED_BY to createdBy.value,
            KEY_CREATED_AT to createdAt,
            KEY_UPDATED_AT to updatedAt,
            KEY_IS_CATEGORY to isCategory.value
        )
    }

    var name: CategoryName = initialName
        private set
    var order: CategoryOrder = initialOrder
        private set
    val createdBy: OwnerId = initialCreatedBy
    val isCategory: IsCategoryFlag = initialIsCategory

    /**
     * Updates mutable fields (name and/or order). If any field actually changes, `updatedAt` is refreshed and
     * appropriate domain events are raised.
     *
     * @param newName  Optional new name; if null, name is unchanged.
     * @param newOrder Optional new order; if null, order is unchanged.
     */
    fun update(newName: CategoryName? = null, newOrder: CategoryOrder? = null): Category {
        if (newName != null && this.name != newName) {
            this.name = newName
            this.pushDomainEvent(CategoryNameChangedEvent(this.id.value))
        }
        if (newOrder != null && this.order != newOrder) {
            this.order = newOrder
            this.pushDomainEvent(CategoryOrderChangedEvent(this.id.value))
        }
        return this
    }

    /**
     * Updates only the `updatedAt` value (e.g., when the category's child collection changes order).
     */
    fun touch(): Category {
        return this
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
        this.pushDomainEvent(CategoryNameChangedEvent(this.id.value))
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
        this.pushDomainEvent(CategoryOrderChangedEvent(this.id.value))
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
        const val COLLECTION_NAME = "categories"
        const val KEY_NAME = "name"
        const val KEY_ORDER = "order"
        const val KEY_CREATED_BY = "createdBy"
        const val KEY_IS_CATEGORY = "isCategory"
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
            name: CategoryName,
            order: CategoryOrder,
            createdBy: OwnerId,
        ): Category {
            val category = Category(
                id = DocumentId.EMPTY,
                initialName = name,
                initialOrder = order,
                initialCreatedBy = createdBy,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                initialIsCategory = IsCategoryFlag.BASE,
                isNew = true
            )
            return category
        }

        fun createNoCategory(createdBy: OwnerId) : Category {
            val category = Category(
                id = DocumentId(Constants.NO_CATEGORY_ID),
                initialName = CategoryName.NO_CATEGORY_NAME,
                initialOrder = CategoryOrder(Constants.NO_CATEGORY_ORDER),
                initialCreatedBy = createdBy,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                initialIsCategory = IsCategoryFlag.FALSE,
                isNew = true
            )
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
            id: DocumentId,
            name: CategoryName,
            order: CategoryOrder,
            createdBy: OwnerId,
            createdAt: Instant?,
            updatedAt: Instant?,
            isCategory: IsCategoryFlag
        ): Category {
            val category = Category(
                id = id,
                initialName = name,
                initialOrder = order,
                initialCreatedBy = createdBy,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                initialIsCategory = isCategory,
                isNew = false
            )
            return category
        }
    }
}

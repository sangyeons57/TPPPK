package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.enum.ProjectChannelType
import java.time.Instant

import com.example.domain.model.AggregateRoot
import com.example.domain.event.projectchannel.ProjectChannelCreatedEvent
import com.example.domain.event.projectchannel.ProjectChannelNameUpdatedEvent
import com.example.domain.event.projectchannel.ProjectChannelOrderChangedEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder

class ProjectChannel private constructor(
    initialChannelName: Name,
    initialOrder: ProjectChannelOrder,
    initialChannelType: ProjectChannelType,
    initialCategoryId: DocumentId?,
    override val id: DocumentId,
    override val isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    var channelType: ProjectChannelType = initialChannelType
        private set
    var channelName: Name = initialChannelName
        private set
    var order: ProjectChannelOrder = initialOrder
        private set
    var categoryId: DocumentId? = initialCategoryId
        private set

    init {
        setOriginalState()
    }

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_CHANNEL_NAME to this.channelName.value,
            KEY_CHANNEL_TYPE to this.channelType.value,
            KEY_ORDER to this.order.value,
            KEY_CATEGORY_ID to this.categoryId?.value,
            KEY_UPDATED_AT to this.updatedAt,
            KEY_CREATED_AT to this.createdAt
        )
    }

    /**
     * Updates the name of the channel after validation.
     *
     * @throws IllegalArgumentException if the new name is blank.
     */
    fun updateName(newName: Name) {
        if (this.channelName == newName) return
        if (newName.isBlank()) {
            throw IllegalArgumentException("Channel name cannot be empty.")
        }

        this.channelName = newName
        pushDomainEvent(ProjectChannelNameUpdatedEvent(this.id, this.channelName, DateTimeUtil.nowInstant()))
    }

    /**
     * Changes the order of the channel after validation.
     * Note: Uniqueness of the order should be validated by a domain or application service.
     *
     * @throws IllegalArgumentException if the new order is not a positive number.
     */
    fun changeOrder(newOrder: ProjectChannelOrder) {
        if (this.order == newOrder) return
        if (newOrder.value <= 0) {
            throw IllegalArgumentException("Channel order must be a positive number.")
        }

        this.order = newOrder
        pushDomainEvent(ProjectChannelOrderChangedEvent(this.id, this.order, DateTimeUtil.nowInstant()))
    }

    /**
     * Moves the channel to a different category.
     * Use null for NoCategory (project-level channels).
     *
     * @param newCategoryId The ID of the target category, or null for NoCategory
     */
    fun moveToCategory(newCategoryId: DocumentId?) {
        if (this.categoryId == newCategoryId) return
        
        this.categoryId = newCategoryId
        // Note: We could add a domain event here if needed for tracking category moves
    }

    companion object {
        const val COLLECTION_NAME = "project_channels"
        const val KEY_CHANNEL_NAME = "channelName"
        const val KEY_CHANNEL_TYPE = "channelType"
        const val KEY_ORDER = "order"
        const val KEY_CATEGORY_ID = "categoryId"

        /**
         * Factory method for creating a new project channel.
         */
        fun create(
            channelName: Name,
            channelType: ProjectChannelType,
            order: ProjectChannelOrder,
            categoryId: DocumentId? = null
        ): ProjectChannel {
            val channel = ProjectChannel(
                id = DocumentId.EMPTY,
                initialChannelName = channelName,
                initialOrder = order,
                initialChannelType = channelType,
                initialCategoryId = categoryId,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                isNew = true
            )
            return channel
        }

        /**
         * Factory method to reconstitute a ProjectChannel from a data source.
         */
        fun fromDataSource(
            id: DocumentId,
            channelName: Name,
            order: ProjectChannelOrder,
            channelType: ProjectChannelType,
            categoryId: DocumentId? = null,
            createdAt: Instant?,
            updatedAt: Instant?
        ): ProjectChannel {
            return ProjectChannel(
                id = id,
                initialChannelName = channelName,
                initialOrder = order,
                initialChannelType = channelType,
                initialCategoryId = categoryId,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                isNew = false
            )
        }
    }
}


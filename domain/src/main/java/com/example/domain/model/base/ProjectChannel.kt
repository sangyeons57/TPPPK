package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.enum.ProjectChannelStatus
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
    initialStatus: ProjectChannelStatus,
    initialCategoryId: DocumentId,
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
    var status: ProjectChannelStatus = initialStatus
        private set
    var categoryId: DocumentId = initialCategoryId
        private set

    init {
        setOriginalState()
    }

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_CHANNEL_NAME to this.channelName.value,
            KEY_CHANNEL_TYPE to this.channelType.value,
            KEY_ORDER to this.order.value,
            KEY_STATUS to this.status.value,
            KEY_CATEGORY_ID to this.categoryId.value,
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
     * @throws IllegalArgumentException if the new order is invalid.
     */
    fun changeOrder(newOrder: ProjectChannelOrder) {
        if (this.order == newOrder) return
        
        // No_Category 채널은 order 0으로 고정, 다른 채널은 1 이상
        if (this.categoryId.value == Category.NO_CATEGORY_ID) {
            if (newOrder.value != Category.NO_CATEGORY_ORDER) {
                throw IllegalArgumentException("No_Category channel order must be ${Category.NO_CATEGORY_ORDER}")
            }
        } else {
            if (newOrder.value < MIN_CHANNEL_ORDER) {
                throw IllegalArgumentException("Channel order must be ${MIN_CHANNEL_ORDER} or greater (${Category.NO_CATEGORY_ORDER} is reserved for No_Category channels)")
            }
        }

        this.order = newOrder
        pushDomainEvent(ProjectChannelOrderChangedEvent(this.id, this.order, DateTimeUtil.nowInstant()))
    }

    /**
     * Moves the channel to a different category.
     * Use Constants.NO_CATEGORY_ID for NoCategory (project-level channels).
     *
     * @param newCategoryId The ID of the target category, or Constants.NO_CATEGORY_ID for NoCategory
     */
    fun moveToCategory(newCategoryId: DocumentId) {
        if (this.categoryId == newCategoryId) return
        
        this.categoryId = newCategoryId
        // Note: We could add a domain event here if needed for tracking category moves
    }

    /**
     * Archives the project channel, hiding it from the main channel list.
     */
    fun archive(): ProjectChannel {
        if (this.status == ProjectChannelStatus.ARCHIVED) return this
        
        return ProjectChannel(
            initialChannelName = this.channelName,
            initialOrder = this.order,
            initialChannelType = this.channelType,
            initialStatus = ProjectChannelStatus.ARCHIVED,
            initialCategoryId = this.categoryId,
            id = this.id,
            isNew = false,
            createdAt = this.createdAt,
            updatedAt = DateTimeUtil.nowInstant()
        )
    }

    /**
     * Activates the project channel, making it visible in the main channel list.
     */
    fun activate(): ProjectChannel {
        if (this.status == ProjectChannelStatus.ACTIVE) return this
        
        return ProjectChannel(
            initialChannelName = this.channelName,
            initialOrder = this.order,
            initialChannelType = this.channelType,
            initialStatus = ProjectChannelStatus.ACTIVE,
            initialCategoryId = this.categoryId,
            id = this.id,
            isNew = false,
            createdAt = this.createdAt,
            updatedAt = DateTimeUtil.nowInstant()
        )
    }

    /**
     * Disables the project channel temporarily.
     */
    fun disable(): ProjectChannel {
        if (this.status == ProjectChannelStatus.DISABLED) return this
        
        return ProjectChannel(
            initialChannelName = this.channelName,
            initialOrder = this.order,
            initialChannelType = this.channelType,
            initialStatus = ProjectChannelStatus.DISABLED,
            initialCategoryId = this.categoryId,
            id = this.id,
            isNew = false,
            createdAt = this.createdAt,
            updatedAt = DateTimeUtil.nowInstant()
        )
    }

    /**
     * Marks the project channel as deleted (soft delete).
     */
    fun markDeleted(): ProjectChannel {
        if (this.status == ProjectChannelStatus.DELETED) return this
        
        return ProjectChannel(
            initialChannelName = this.channelName,
            initialOrder = this.order,
            initialChannelType = this.channelType,
            initialStatus = ProjectChannelStatus.DELETED,
            initialCategoryId = this.categoryId,
            id = this.id,
            isNew = false,
            createdAt = this.createdAt,
            updatedAt = DateTimeUtil.nowInstant()
        )
    }

    /**
     * Checks if the channel is in an active state.
     */
    fun isActive(): Boolean = status == ProjectChannelStatus.ACTIVE

    /**
     * Checks if the channel is archived.
     */
    fun isArchived(): Boolean = status == ProjectChannelStatus.ARCHIVED

    /**
     * Checks if the channel is disabled.
     */
    fun isDisabled(): Boolean = status == ProjectChannelStatus.DISABLED

    /**
     * Checks if the channel is deleted.
     */
    fun isDeleted(): Boolean = status == ProjectChannelStatus.DELETED

    companion object {
        const val COLLECTION_NAME = "project_channels"
        const val KEY_CHANNEL_NAME = "channelName"
        const val KEY_CHANNEL_TYPE = "channelType"
        const val KEY_ORDER = "order"
        const val KEY_STATUS = "status"
        const val KEY_CATEGORY_ID = "categoryId"
        
        /**
         * Minimum order value for regular channels (non-NoCategory).
         * Regular channels must have order >= MIN_CHANNEL_ORDER
         */
        const val MIN_CHANNEL_ORDER = 1.0
        
        /**
         * Default increment value for channel ordering within categories
         */
        const val CHANNEL_ORDER_INCREMENT = 0.1

        /**
         * Factory method for creating a new project channel.
         */
        fun create(
            channelName: Name,
            channelType: ProjectChannelType,
            order: ProjectChannelOrder,
            status: ProjectChannelStatus = ProjectChannelStatus.ACTIVE,
            categoryId: DocumentId = DocumentId(Category.NO_CATEGORY_ID)
        ): ProjectChannel {
            val channel = ProjectChannel(
                id = DocumentId.EMPTY,
                initialChannelName = channelName,
                initialOrder = order,
                initialChannelType = channelType,
                initialStatus = status,
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
            status: ProjectChannelStatus = ProjectChannelStatus.ACTIVE,
            categoryId: DocumentId = DocumentId(Category.NO_CATEGORY_ID),
            createdAt: Instant?,
            updatedAt: Instant?
        ): ProjectChannel {
            return ProjectChannel(
                id = id,
                initialChannelName = channelName,
                initialOrder = order,
                initialChannelType = channelType,
                initialStatus = status,
                initialCategoryId = categoryId,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                isNew = false
            )
        }
    }
}


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

    init {
        setOriginalState()
    }

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_CHANNEL_NAME to this.channelName.value,
            KEY_CHANNEL_TYPE to this.channelType.value,
            KEY_ORDER to this.order.value,
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

    companion object {
        const val COLLECTION_NAME = "project_channels"
        const val KEY_CHANNEL_NAME = "channelName"
        const val KEY_CHANNEL_TYPE = "channelType"
        const val KEY_ORDER = "order"

        /**
         * Factory method for creating a new project channel.
         */
        fun create(
            channelName: Name,
            channelType: ProjectChannelType,
            order: ProjectChannelOrder
        ): ProjectChannel {
            val channel = ProjectChannel(
                id = DocumentId.EMPTY,
                initialChannelName = channelName,
                initialOrder = order,
                initialChannelType = channelType,
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
            createdAt: Instant?,
            updatedAt: Instant?
        ): ProjectChannel {
            return ProjectChannel(
                id = id,
                initialChannelName = channelName,
                initialOrder = order,
                initialChannelType = channelType,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                isNew = false
            )
        }
    }
}


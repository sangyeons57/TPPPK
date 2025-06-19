package com.example.domain.model.base

import com.example.domain.model.enum.ProjectChannelType
import java.time.Instant

import com.example.domain.event.AggregateRoot
import com.example.domain.event.projectchannel.ProjectChannelCreatedEvent
import com.example.domain.event.projectchannel.ProjectChannelNameUpdatedEvent
import com.example.domain.event.projectchannel.ProjectChannelOrderChangedEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder

class ProjectChannel private constructor(
    initialChannelName: Name,
    initialOrder: ProjectChannelOrder,
    initialChannelType: ProjectChannelType,
    initialCreatedAt: Instant,
    initialUpdatedAt: Instant,
    override val id: DocumentId,
    override val isNew: Boolean
) : AggregateRoot() {

    val createdAt: Instant = initialCreatedAt

    var channelType: ProjectChannelType = initialChannelType
        private set
    var channelName: Name = initialChannelName
        private set
    var order: ProjectChannelOrder = initialOrder
        private set
    var updatedAt: Instant = initialUpdatedAt
        private set

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_CHANNEL_NAME to this.channelName,
            KEY_CHANNEL_TYPE to this.channelType,
            KEY_ORDER to this.order,
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
        this.updatedAt = Instant.now()
        pushDomainEvent(ProjectChannelNameUpdatedEvent(this.id, this.channelName, this.updatedAt))
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
        this.updatedAt = Instant.now()
        pushDomainEvent(ProjectChannelOrderChangedEvent(this.id, this.order, this.updatedAt))
    }

    companion object {
        const val COLLECTION_NAME = "project_channels"
        const val KEY_CHANNEL_NAME = "channelName"
        const val KEY_CHANNEL_TYPE = "channelType"
        const val KEY_ORDER = "order"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_UPDATED_AT = "updatedAt"

        /**
         * Factory method for creating a new project channel.
         */
        fun create(
            id: DocumentId,
            channelName: Name,
            channelType: ProjectChannelType,
            order: ProjectChannelOrder
        ): ProjectChannel {
            val now = Instant.now()
            val channel = ProjectChannel(
                id = id,
                initialChannelName = channelName,
                initialOrder = order,
                initialChannelType = channelType,
                initialCreatedAt = now,
                initialUpdatedAt = now,
                isNew = true
            )
            channel.pushDomainEvent(
                ProjectChannelCreatedEvent(
                    channelId = channel.id,
                    channelName = channel.channelName,
                    channelType = channel.channelType,
                    order = channel.order,
                    occurredOn = now
                )
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
            createdAt: Instant,
            updatedAt: Instant
        ): ProjectChannel {
            return ProjectChannel(
                id = id,
                initialChannelName = channelName,
                initialOrder = order,
                initialChannelType = channelType,
                initialCreatedAt = createdAt,
                initialUpdatedAt = updatedAt,
                isNew = false
            )
        }
    }
}


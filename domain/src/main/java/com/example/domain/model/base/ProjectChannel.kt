package com.example.domain.model.base

import com.example.domain.model.enum.ProjectChannelType
import java.time.Instant

import com.example.domain.event.AggregateRoot
import com.example.domain.event.projectchannel.ProjectChannelCreatedEvent
import com.example.domain.event.projectchannel.ProjectChannelNameUpdatedEvent
import com.example.domain.event.projectchannel.ProjectChannelOrderChangedEvent
import com.example.domain.model.vo.DocumentId

class ProjectChannel private constructor(
    initialChannelName: String,
    initialOrder: Double,
    initialChannelType: ProjectChannelType,
    initialCreatedAt: Instant,
    initialUpdatedAt: Instant,
    override val id: DocumentId,
    override val isNew: Boolean
) : AggregateRoot() {

    val createdAt: Instant = initialCreatedAt

    var channelType: ProjectChannelType = initialChannelType
        private set
    var channelName: String = initialChannelName
        private set
    var order: Double = initialOrder
        private set
    var updatedAt: Instant = initialUpdatedAt
        private set

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            CHANNEL_NAME to this.channelName,
            CHANNEL_TYPE to this.channelType,
            ORDER to this.order,
            UPDATED_AT to this.updatedAt,
            CREATED_AT to this.createdAt
        )
    }

    /**
     * Updates the name of the channel after validation.
     *
     * @throws IllegalArgumentException if the new name is blank.
     */
    fun updateName(newName: String) {
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
    fun changeOrder(newOrder: Double) {
        if (this.order == newOrder) return
        if (newOrder <= 0) {
            throw IllegalArgumentException("Channel order must be a positive number.")
        }

        this.order = newOrder
        this.updatedAt = Instant.now()
        pushDomainEvent(ProjectChannelOrderChangedEvent(this.id, this.order, this.updatedAt))
    }

    companion object {
        const val COLLECTION_NAME = "project_channels"
        const val CHANNEL_NAME = "channelName"
        const val CHANNEL_TYPE = "channelType"
        const val ORDER = "order"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"

        /**
         * Factory method for creating a new project channel.
         */
        fun create(
            id: DocumentId,
            channelName: String,
            channelType: ProjectChannelType,
            order: Double
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
            channelName: String,
            order: Double,
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


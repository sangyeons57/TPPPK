package com.example.domain.model.base

import com.example.domain.event.AggregateRoot
import com.example.domain.event.DomainEvent
import com.example.domain.event.dmchannel.DMChannelCreatedEvent
import com.example.domain.event.dmchannel.DMChannelLastMessageUpdatedEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.dmchannel.DMChannelId
import com.example.domain.model.vo.dmchannel.DMChannelLastMessagePreview
import java.time.Instant

/**
 * Represents a Direct Message (DM) channel between users.
 *
 * A DMChannel is an aggregate root that manages the state and business rules
 * for direct messaging conversations.
 *
 * @property id The unique identifier of the DM channel.
 * @property participants The list of user IDs participating in this DM channel.
 * @property lastMessagePreview A preview of the last message sent in the channel.
 * @property lastMessageTimestamp The timestamp of the last message.
 * @property createdAt The timestamp when the channel was created.
 * @property updatedAt The timestamp when the channel was last updated.
 */
class DMChannel private constructor(
    id: DMChannelId,
    participants: List<DocumentId>,
    lastMessagePreview: DMChannelLastMessagePreview?,
    lastMessageTimestamp: Instant?,
    createdAt: Instant,
    updatedAt: Instant
) : AggregateRoot {

    /** Unique identifier of the DM channel. */
    val id: DMChannelId = id
    /** List of user document IDs participating in this DM channel. */
    var participants: List<DocumentId> = participants
        private set
    /** Preview of the last message sent in the channel. Null if no messages yet. */
    var lastMessagePreview: DMChannelLastMessagePreview? = lastMessagePreview
        private set
    /** Timestamp of the last message sent in the channel. Null if no messages yet. */
    var lastMessageTimestamp: Instant? = lastMessageTimestamp
        private set
    /** Timestamp indicating when the DM channel was created. */
    val createdAt: Instant = createdAt
    /** Timestamp indicating when the DM channel was last updated, either by a new message or participant change. */
    var updatedAt: Instant = updatedAt
        private set

    private val _domainEvents: MutableList<DomainEvent> = mutableListOf()

    override fun pullDomainEvents(): List<DomainEvent> {
        val events = _domainEvents.toList() // Make a copy
        _domainEvents.clear()
        return events
    }

    override fun clearDomainEvents() {
        _domainEvents.clear()
    }

    /**
     * Updates the preview and timestamp of the last message in this DM channel.
     * Generates a [DMChannelLastMessageUpdatedEvent].
     *
     * @param newPreview The new message preview. Can be null.
     * @param newTimestamp The timestamp of the new last message. Can be null.
     */
    fun updateLastMessage(newPreview: DMChannelLastMessagePreview?, newTimestamp: Instant?) {
        // Optional: Add validation, e.g., if newTimestamp is null, newPreview should also be null.
        // Or if newTimestamp is not null, it should be later than or equal to the current lastMessageTimestamp if it exists.
        // For now, direct update as per plan.

        this.lastMessagePreview = newPreview
        this.lastMessageTimestamp = newTimestamp
        this.updatedAt = Instant.now()
        this._domainEvents.add(DMChannelLastMessageUpdatedEvent(this.id))
    }

    // fun addParticipant(participantId: DocumentId, currentTime: Instant)
    // fun removeParticipant(participantId: DocumentId, currentTime: Instant)
    // These methods would also update `updatedAt` and potentially raise domain events.

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DMChannel
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        /**
         * Creates a new DMChannel instance.
         *
         * @param id The unique identifier for the new channel.
         * @param initialParticipants The initial list of participants in the channel. Must contain at least two participants.
         * @param currentTime The current time, used to set creation and update timestamps.
         * @return A new DMChannel instance.
         * @throws IllegalArgumentException if `initialParticipants` has less than two participants.
         */
        fun create(
            id: DMChannelId,
            initialParticipants: List<DocumentId>,
            currentTime: Instant
        ): DMChannel {
            require(initialParticipants.size >= 2) { "DMChannel must have at least two participants." }
            // Ensure participants are distinct, if not already handled or guaranteed by caller
            val distinctParticipants = initialParticipants.distinct()
            require(distinctParticipants.size == initialParticipants.size) { "Participant IDs must be unique."}
            require(distinctParticipants.size >= 2) { "DMChannel must have at least two distinct participants."}


            val channel = DMChannel(
                id = id,
                participants = distinctParticipants, // Use distinct participants
                lastMessagePreview = null,
                lastMessageTimestamp = null,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            channel._domainEvents.add(DMChannelCreatedEvent(channel.id)) // Add this line
            return channel
        }

        /**
         * Reconstructs a DMChannel instance from a data source.
         *
         * This method is intended for use when loading an existing channel from persistence.
         *
         * @param id The unique identifier of the channel.
         * @param participants The list of participants in the channel.
         * @param lastMessagePreview A preview of the last message.
         * @param lastMessageTimestamp The timestamp of the last message.
         * @param createdAt The timestamp when the channel was created.
         * @param updatedAt The timestamp when the channel was last updated.
         * @return A DMChannel instance reconstructed from the provided data.
         */
        fun fromDataSource(
            id: DMChannelId,
            participants: List<DocumentId>,
            lastMessagePreview: DMChannelLastMessagePreview?,
            lastMessageTimestamp: Instant?,
            createdAt: Instant,
            updatedAt: Instant
        ): DMChannel {
            return DMChannel(
                id = id,
                participants = participants,
                lastMessagePreview = lastMessagePreview,
                lastMessageTimestamp = lastMessageTimestamp,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }
}


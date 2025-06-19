package com.example.domain.model.base

import com.example.domain.event.AggregateRoot
import com.example.domain.event.DomainEvent
import com.example.domain.event.dmchannel.DMChannelCreatedEvent
import com.example.domain.event.dmchannel.DMChannelLastMessageUpdatedEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
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
    initialParticipants: List<UserId>,
    initialLastMessagePreview: DMChannelLastMessagePreview?,
    initialLastMessageTimestamp: Instant?,
    initialCreatedAt: Instant,
    initialUpdatedAt: Instant,
    override val id: DocumentId,
    override val isNew: Boolean,
) : AggregateRoot() {

    /** List of user document IDs participating in this DM channel. */
    var participants: List<UserId> = initialParticipants
        private set
    /** Preview of the last message sent in the channel. Null if no messages yet. */
    var lastMessagePreview: DMChannelLastMessagePreview? = initialLastMessagePreview
        private set
    /** Timestamp of the last message sent in the channel. Null if no messages yet. */
    var lastMessageTimestamp: Instant? = initialLastMessageTimestamp
        private set
    /** Timestamp indicating when the DM channel was created. */
    val createdAt: Instant = initialCreatedAt
    /** Timestamp indicating when the DM channel was last updated, either by a new message or participant change. */
    var updatedAt: Instant = initialUpdatedAt
        private set

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
        this.pushDomainEvent(DMChannelLastMessageUpdatedEvent(this.id))
    }

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_PARTICIPANTS to participants,
            KEY_LAST_MESSAGE_PREVIEW to lastMessagePreview,
            KEY_LAST_MESSAGE_TIMESTAMP to lastMessageTimestamp,
            KEY_CREATED_AT to createdAt,
            KEY_UPDATED_AT to updatedAt,
        )
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
        const val COLLECTION_NAME = "dm_channels"
        const val KEY_PARTICIPANTS = "participants" // List<String> = "userId1"
        const val KEY_LAST_MESSAGE_PREVIEW = "lastMessagePreview"
        const val KEY_LAST_MESSAGE_TIMESTAMP = "lastMessageTimestamp"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_UPDATED_AT = "updatedAt"
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
            id: DocumentId,
            initialParticipants: List<UserId>,
            currentTime: Instant
        ): DMChannel {
            require(initialParticipants.size >= 2) { "DMChannel must have at least two participants." }
            // Ensure participants are distinct, if not already handled or guaranteed by caller
            val distinctParticipants = initialParticipants.distinct()
            require(distinctParticipants.size == initialParticipants.size) { "Participant IDs must be unique."}
            require(distinctParticipants.size >= 2) { "DMChannel must have at least two distinct participants."}


            val channel = DMChannel(
                id = id,
                initialParticipants = distinctParticipants, // Use distinct participants
                initialLastMessagePreview = null,
                initialLastMessageTimestamp = null,
                initialCreatedAt = currentTime,
                initialUpdatedAt = currentTime,
                isNew = true,
            )
            channel.pushDomainEvent(DMChannelCreatedEvent(channel.id)) // Add this line
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
            id: DocumentId,
            participants: List<UserId>,
            lastMessagePreview: DMChannelLastMessagePreview?,
            lastMessageTimestamp: Instant?,
            createdAt: Instant,
            updatedAt: Instant
        ): DMChannel {
            val channel = DMChannel(
                id = id,
                initialParticipants = participants,
                initialLastMessagePreview = lastMessagePreview,
                initialLastMessageTimestamp = lastMessageTimestamp,
                initialCreatedAt = createdAt,
                initialUpdatedAt = updatedAt,
                isNew = false,
            )
            channel.pushDomainEvent(DMChannelCreatedEvent(channel.id)) // Add this line
            return channel
        }
    }
}


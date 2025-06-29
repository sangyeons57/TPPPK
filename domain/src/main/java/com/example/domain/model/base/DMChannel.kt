package com.example.domain.model.base

import com.example.domain.model.AggregateRoot
import com.example.domain.event.dmchannel.DMChannelLastMessageUpdatedEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.dmchannel.DMChannelLastMessagePreview
import java.time.Instant
import com.example.core_common.util.DateTimeUtil

/**
 * Represents a Direct Message (DM) channel between users.
 *
 * A DMChannel is an aggregate root that manages the state and business rules
 * for direct messaging conversations.
 *
 * @property id The unique identifier of the DM channel.
 * @property participants The list of user IDs participating in this DM channel.
 * @property createdAt The timestamp when the channel was created.
 * @property updatedAt The timestamp when the channel was last updated.
 */
class DMChannel private constructor(
    initialParticipants: List<UserId>,
    override val id: DocumentId,
    override val isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    /** List of user document IDs participating in this DM channel. */
    var participants: List<UserId> = initialParticipants
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

        this.pushDomainEvent(DMChannelLastMessageUpdatedEvent(this.id))
    }

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_PARTICIPANTS to participants,
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
            initialParticipants: List<UserId>,
        ): DMChannel {
            require(initialParticipants.size >= 2) { "DMChannel must have at least two participants." }
            // Ensure participants are distinct, if not already handled or guaranteed by caller
            val distinctParticipants = initialParticipants.distinct()
            require(distinctParticipants.size == initialParticipants.size) { "Participant IDs must be unique."}
            require(distinctParticipants.size >= 2) { "DMChannel must have at least two distinct participants."}


            val channel = DMChannel(
                id = DocumentId.EMPTY,
                initialParticipants = distinctParticipants, // Use distinct participants
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                isNew = true,
            )
            return channel
        }

        /**
         * Reconstructs a DMChannel instance from a data source.
         *
         * This method is intended for use when loading an existing channel from persistence.
         *
         * @param id The unique identifier of the channel.
         * @param participants The list of participants in the channel.
         * @param createdAt The timestamp when the channel was created.
         * @param updatedAt The timestamp when the channel was last updated.
         * @return A DMChannel instance reconstructed from the provided data.
         */
        fun fromDataSource(
            id: DocumentId,
            participants: List<UserId>,
            createdAt: Instant?,
            updatedAt: Instant?
        ): DMChannel {
            val channel = DMChannel(
                id = id,
                initialParticipants = participants,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                isNew = false,
            )
            return channel
        }
    }
}


package com.example.domain.model.base

import com.example.domain.model.AggregateRoot
import com.example.domain.event.dmchannel.DMChannelLastMessageUpdatedEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.dmchannel.DMChannelLastMessagePreview
import com.example.domain.model.enum.DMChannelStatus
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
 * @property status The current status of the DM channel.
 * @property createdAt The timestamp when the channel was created.
 * @property updatedAt The timestamp when the channel was last updated.
 */
class DMChannel private constructor(
    initialParticipants: List<UserId>,
    initialStatus: DMChannelStatus,
    initialBlockedByMap: Map<UserId, UserId>,
    override val id: DocumentId,
    override val isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    /** List of user document IDs participating in this DM channel. */
    var participants: List<UserId> = initialParticipants
        private set

    /** Current status of the DM channel. */
    var status: DMChannelStatus = initialStatus
        private set

    /** Map of blocked users: key = userId who blocked, value = userId who was blocked */
    var blockedByMap: Map<UserId, UserId> = initialBlockedByMap
        private set

    init {
        setOriginalState()
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

        this.pushDomainEvent(DMChannelLastMessageUpdatedEvent(this.id))
    }

    /**
     * Archives the DM channel, hiding it from the main channel list.
     */
    fun archive(): DMChannel {
        if (this.status == DMChannelStatus.ARCHIVED) return this
        
        return DMChannel(
            initialParticipants = this.participants,
            initialStatus = DMChannelStatus.ARCHIVED,
            initialBlockedByMap = this.blockedByMap,
            id = this.id,
            isNew = false,
            createdAt = this.createdAt,
            updatedAt = DateTimeUtil.nowInstant()
        )
    }

    /**
     * Activates the DM channel, making it visible in the main channel list.
     */
    fun activate(): DMChannel {
        if (this.status == DMChannelStatus.ACTIVE) return this
        
        return DMChannel(
            initialParticipants = this.participants,
            initialStatus = DMChannelStatus.ACTIVE,
            initialBlockedByMap = this.blockedByMap,
            id = this.id,
            isNew = false,
            createdAt = this.createdAt,
            updatedAt = DateTimeUtil.nowInstant()
        )
    }

    /**
     * Blocks the DM channel when one user blocks the other.
     */
    fun blockByUser(blockerUserId: UserId): DMChannel {
        val newBlockedByMap = this.blockedByMap.toMutableMap()
        val otherUserId = getOtherParticipant(blockerUserId)
        
        if (otherUserId != null) {
            newBlockedByMap[blockerUserId] = otherUserId
        }
        
        return DMChannel(
            initialParticipants = this.participants,
            initialStatus = DMChannelStatus.BLOCKED,
            initialBlockedByMap = newBlockedByMap,
            id = this.id,
            isNew = false,
            createdAt = this.createdAt,
            updatedAt = DateTimeUtil.nowInstant()
        )
    }

    /**
     * Unblocks the DM channel when one user unblocks the other.
     */
    fun unblockByUser(unblockerUserId: UserId): DMChannel {
        val newBlockedByMap = this.blockedByMap.toMutableMap()
        newBlockedByMap.remove(unblockerUserId)
        
        // If no one is blocking anyone, activate the channel
        val newStatus = if (newBlockedByMap.isEmpty()) DMChannelStatus.ACTIVE else DMChannelStatus.BLOCKED
        
        return DMChannel(
            initialParticipants = this.participants,
            initialStatus = newStatus,
            initialBlockedByMap = newBlockedByMap,
            id = this.id,
            isNew = false,
            createdAt = this.createdAt,
            updatedAt = DateTimeUtil.nowInstant()
        )
    }

    /**
     * Blocks the DM channel when one user blocks the other.
     */
    fun block(): DMChannel {
        if (this.status == DMChannelStatus.BLOCKED) return this
        
        return DMChannel(
            initialParticipants = this.participants,
            initialStatus = DMChannelStatus.BLOCKED,
            initialBlockedByMap = this.blockedByMap,
            id = this.id,
            isNew = false,
            createdAt = this.createdAt,
            updatedAt = DateTimeUtil.nowInstant()
        )
    }

    /**
     * Marks the DM channel as deleted (soft delete).
     */
    fun markDeleted(): DMChannel {
        if (this.status == DMChannelStatus.DELETED) return this
        
        return DMChannel(
            initialParticipants = this.participants,
            initialStatus = DMChannelStatus.DELETED,
            initialBlockedByMap = this.blockedByMap,
            id = this.id,
            isNew = false,
            createdAt = this.createdAt,
            updatedAt = DateTimeUtil.nowInstant()
        )
    }

    /**
     * Checks if the channel is in an active state.
     */
    fun isActive(): Boolean = status == DMChannelStatus.ACTIVE

    /**
     * Checks if the channel is archived.
     */
    fun isArchived(): Boolean = status == DMChannelStatus.ARCHIVED

    /**
     * Checks if the channel is blocked.
     */
    fun isBlocked(): Boolean = status == DMChannelStatus.BLOCKED

    /**
     * Checks if the channel is deleted.
     */
    fun isDeleted(): Boolean = status == DMChannelStatus.DELETED

    /**
     * Checks if a user has blocked another user.
     */
    fun isBlockedByUser(userId: UserId): Boolean = blockedByMap.containsKey(userId)

    /**
     * Checks if a user is blocked by another user.
     */
    fun isUserBlocked(userId: UserId): Boolean = blockedByMap.containsValue(userId)

    /**
     * Gets the other participant in this DM channel.
     */
    fun getOtherParticipant(userId: UserId): UserId? {
        return participants.find { it != userId }
    }

    /**
     * Gets the list of users who have been blocked.
     */
    fun getBlockedUsers(): List<UserId> = blockedByMap.values.toList()

    /**
     * Gets the list of users who have blocked others.
     */
    fun getBlockerUsers(): List<UserId> = blockedByMap.keys.toList()

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_PARTICIPANTS to participants.map { it.value },
            KEY_STATUS to status.value,
            KEY_BLOCKED_BY_MAP to blockedByMap.mapKeys { it.key.value }.mapValues { it.value.value },
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
        const val KEY_STATUS = "status"
        const val KEY_BLOCKED_BY_MAP = "blockedByMap"
        /**
         * Creates a new DMChannel instance.
         *
         * @param initialParticipants The initial list of participants in the channel. Must contain at least two participants.
         * @param initialStatus The initial status of the channel. Defaults to ACTIVE.
         * @return A new DMChannel instance.
         * @throws IllegalArgumentException if `initialParticipants` has less than two participants.
         */
        fun create(
            initialParticipants: List<UserId>,
            initialStatus: DMChannelStatus = DMChannelStatus.ACTIVE
        ): DMChannel {
            require(initialParticipants.size >= 2) { "DMChannel must have at least two participants." }
            // Ensure participants are distinct, if not already handled or guaranteed by caller
            val distinctParticipants = initialParticipants.distinct()
            require(distinctParticipants.size == initialParticipants.size) { "Participant IDs must be unique."}
            require(distinctParticipants.size >= 2) { "DMChannel must have at least two distinct participants."}


            val channel = DMChannel(
                id = DocumentId.EMPTY,
                initialParticipants = distinctParticipants, // Use distinct participants
                initialStatus = initialStatus,
                initialBlockedByMap = emptyMap(),
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
         * @param status The status of the channel. Defaults to ACTIVE for backward compatibility.
         * @param blockedByMap The map of blocked users. Defaults to empty map.
         * @param createdAt The timestamp when the channel was created.
         * @param updatedAt The timestamp when the channel was last updated.
         * @return A DMChannel instance reconstructed from the provided data.
         */
        fun fromDataSource(
            id: DocumentId,
            participants: List<UserId>,
            status: DMChannelStatus = DMChannelStatus.ACTIVE,
            blockedByMap: Map<UserId, UserId> = emptyMap(),
            createdAt: Instant?,
            updatedAt: Instant?
        ): DMChannel {
            val channel = DMChannel(
                id = id,
                initialParticipants = participants,
                initialStatus = status,
                initialBlockedByMap = blockedByMap,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                isNew = false,
            )
            return channel
        }
    }
}


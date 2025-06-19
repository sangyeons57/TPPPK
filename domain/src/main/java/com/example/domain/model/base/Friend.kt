package com.example.domain.model.base

import com.example.domain.event.AggregateRoot
import com.example.domain.event.DomainEvent
import com.example.domain.event.friend.FriendCreatedEvent
import com.example.domain.event.friend.FriendNameChangedEvent
import com.example.domain.event.friend.FriendProfileImageChangedEvent
import com.example.domain.event.friend.FriendStatusChangedEvent
import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.friend.FriendName
import com.example.domain.model.vo.friend.FriendProfileImageUrl
import java.time.Instant

class Friend private constructor(
    // Immutable properties
    initialRequestedAt: Instant?, // This could be when the friend request was sent by the current user or received
    initialAcceptedAt: Instant?,  // This could be when the friend request was accepted

    // Mutable properties
    initialName: FriendName,
    initialProfileImageUrl: FriendProfileImageUrl?,
    initialStatus: FriendStatus,
    initialCreatedAt: Instant,
    initialUpdatedAt: Instant,
    override val id :DocumentId,
    override val isNew: Boolean

) : AggregateRoot() {

    val requestedAt: Instant? = initialRequestedAt
    var acceptedAt: Instant? = initialAcceptedAt // Made var in case it needs to be set by a method
        private set

    var name: FriendName = initialName
        private set

    var profileImageUrl: FriendProfileImageUrl? = initialProfileImageUrl
        private set

    var status: FriendStatus = initialStatus
        private set

    val createdAt: Instant = initialCreatedAt
    var updatedAt: Instant = initialUpdatedAt
        private set



    override fun getCurrentStateMap(): Map<String, Any?> {
        TODO("Not yet implemented")
    }

    // --- Business Methods ---

    fun changeName(newName: FriendName) {
        if (this.name == newName) return
        this.name = newName
        this.updatedAt = Instant.now()
        this.pushDomainEvent(FriendNameChangedEvent(this.id, newName))
    }

    fun changeProfileImage(newProfileImageUrl: FriendProfileImageUrl?) {
        if (this.profileImageUrl == newProfileImageUrl) return
        this.profileImageUrl = newProfileImageUrl
        this.updatedAt = Instant.now()
        this.pushDomainEvent(FriendProfileImageChangedEvent(this.id, newProfileImageUrl))
    }

    fun acceptRequest() {
        if (this.status == FriendStatus.PENDING || this.status == FriendStatus.REQUESTED) {
            this.status = FriendStatus.ACCEPTED
            this.acceptedAt = Instant.now()
            this.updatedAt = Instant.now()
            this.pushDomainEvent(FriendStatusChangedEvent(this.id, this.status))
        }
        // Consider if any action/event is needed if status is not PENDING/REQUESTED
    }

    fun blockUser() {
        if (this.status == FriendStatus.BLOCKED) return
        this.status = FriendStatus.BLOCKED
        // acceptedAt might be nulled out or kept depending on business rule
        this.updatedAt = Instant.now()
        this.pushDomainEvent(FriendStatusChangedEvent(this.id, this.status))
    }

    fun removeFriend() {
        // This is a conceptual removal. Depending on the system,
        // it might mean setting status to BLOCKED, or a new "REMOVED" status,
        // or even actual deletion which is a repository concern.
        // For now, let's assume it means blocking or changing to a non-interactive status.
        // If actual deletion is implied, this method might not belong here, or should trigger a specific event.
        if (this.status == FriendStatus.BLOCKED && this.name.value == "Unknown") return // Example: already in a removed-like state

        // For this example, let's change status to BLOCKED and clear personal info
        // This is a placeholder for a more defined "removed" state or process
        this.status = FriendStatus.BLOCKED // Or a new FriendStatus.REMOVED
        // this.name = FriendName("Unknown") // Masking name might be a policy
        // this.profileImageUrl = null
        this.updatedAt = Instant.now()
        this.pushDomainEvent(FriendStatusChangedEvent(this.id, this.status))
    }

    fun markAsPending() {
        if (this.status == FriendStatus.PENDING) return
        this.status = FriendStatus.PENDING
        this.updatedAt = Instant.now()
        this.pushDomainEvent(FriendStatusChangedEvent(this.id, this.status))
    }

    fun markAsRequested() {
        // This status might be used if the current user sent the request
        if (this.status == FriendStatus.REQUESTED) return
        this.status = FriendStatus.REQUESTED
        this.updatedAt = Instant.now()
        this.pushDomainEvent(FriendStatusChangedEvent(this.id, this.status))
    }

    companion object {
        const val COLLECTION_NAME = "friends"
        const val KEY_STATUS = "status" // "PENDING_SENT", "PENDING_RECEIVED", "ACCEPTED", "DECLINED", "BLOCKED"
        const val KEY_REQUESTED_AT = "requestedAt"
        const val KEY_ACCEPTED_AT = "acceptedAt"

        fun newRequest(
            id: DocumentId,
            name: FriendName,
            profileImageUrl: FriendProfileImageUrl?,
            requestedAt: Instant // Time the request is made
        ): Friend {
            val now = Instant.now()
            val friend = Friend(
                initialName = name,
                initialProfileImageUrl = profileImageUrl,
                initialStatus = FriendStatus.REQUESTED, // Current user sent the request
                initialRequestedAt = requestedAt,
                initialAcceptedAt = null,
                initialCreatedAt = now,
                initialUpdatedAt = now,
                id = id,
                isNew = true
            )
            friend.pushDomainEvent(FriendCreatedEvent(id))
            // Optionally, add a FriendStatusChangedEvent if REQUESTED is considered a status change from an initial null state
            // friend._domainEvents.add(FriendStatusChangedEvent(id, FriendStatus.REQUESTED))
            return friend
        }

        fun receivedRequest(
            id: DocumentId,
            name: FriendName,
            profileImageUrl: FriendProfileImageUrl?,
            requestedAt: Instant // Time the request was received
        ): Friend {
            val now = Instant.now()
            val friend = Friend(
                initialName = name,
                initialProfileImageUrl = profileImageUrl,
                initialStatus = FriendStatus.PENDING, // Current user received the request, it's pending their action
                initialRequestedAt = requestedAt,
                initialAcceptedAt = null,
                initialCreatedAt = now,
                initialUpdatedAt = now,
                id = id,
                isNew = true
            )
            friend.pushDomainEvent(FriendCreatedEvent(id))
            // friend._domainEvents.add(FriendStatusChangedEvent(id, FriendStatus.PENDING))
            return friend
        }

        // Factory method for reconstructing from data source
        fun fromDataSource(
            id: DocumentId,
            name: FriendName,
            profileImageUrl: FriendProfileImageUrl?,
            status: FriendStatus,
            requestedAt: Instant?,
            acceptedAt: Instant?,
            createdAt: Instant,
            updatedAt: Instant
        ): Friend {
            return Friend(
                initialName = name,
                initialProfileImageUrl = profileImageUrl,
                initialStatus = status,
                initialRequestedAt = requestedAt,
                initialAcceptedAt = acceptedAt,
                initialCreatedAt = createdAt,
                initialUpdatedAt = updatedAt,
                id = id,
                isNew = false
            )
            // No domain event on reconstruction from data source usually, unless specific logic dictates it.
        }
    }
}

package com.example.domain.model.base

import com.example.domain.event.AggregateRoot
import com.example.domain.event.DomainEvent
import com.example.domain.event.friend.FriendCreatedEvent
import com.example.domain.event.friend.FriendNameChangedEvent
import com.example.domain.event.friend.FriendProfileImageChangedEvent
import com.example.domain.event.friend.FriendStatusChangedEvent
import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.vo.friend.FriendId
import com.example.domain.model.vo.friend.FriendName
import com.example.domain.model.vo.friend.FriendProfileImageUrl
import java.time.Instant

class Friend private constructor(
    // Immutable properties
    id: FriendId,
    requestedAt: Instant?, // This could be when the friend request was sent by the current user or received
    acceptedAt: Instant?,  // This could be when the friend request was accepted

    // Mutable properties
    name: FriendName,
    profileImageUrl: FriendProfileImageUrl?,
    status: FriendStatus,
    createdAt: Instant,
    updatedAt: Instant

) : AggregateRoot {

    val id: FriendId = id
    val requestedAt: Instant? = requestedAt
    var acceptedAt: Instant? = acceptedAt // Made var in case it needs to be set by a method
        private set

    var name: FriendName = name
        private set

    var profileImageUrl: FriendProfileImageUrl? = profileImageUrl
        private set

    var status: FriendStatus = status
        private set

    val createdAt: Instant = createdAt
    var updatedAt: Instant = updatedAt
        private set

    private val _domainEvents: MutableList<DomainEvent> = mutableListOf()

    override fun pullDomainEvents(): List<DomainEvent> {
        val events = _domainEvents.toList()
        _domainEvents.clear()
        return events
    }

    override fun clearDomainEvents() {
        _domainEvents.clear()
    }

    // --- Business Methods ---

    fun changeName(newName: FriendName) {
        if (this.name == newName) return
        this.name = newName
        this.updatedAt = Instant.now()
        _domainEvents.add(FriendNameChangedEvent(this.id, newName))
    }

    fun changeProfileImage(newProfileImageUrl: FriendProfileImageUrl?) {
        if (this.profileImageUrl == newProfileImageUrl) return
        this.profileImageUrl = newProfileImageUrl
        this.updatedAt = Instant.now()
        _domainEvents.add(FriendProfileImageChangedEvent(this.id, newProfileImageUrl))
    }

    fun acceptRequest() {
        if (this.status == FriendStatus.PENDING || this.status == FriendStatus.REQUESTED) {
            this.status = FriendStatus.ACCEPTED
            this.acceptedAt = Instant.now()
            this.updatedAt = Instant.now()
            _domainEvents.add(FriendStatusChangedEvent(this.id, this.status))
        }
        // Consider if any action/event is needed if status is not PENDING/REQUESTED
    }

    fun blockUser() {
        if (this.status == FriendStatus.BLOCKED) return
        this.status = FriendStatus.BLOCKED
        // acceptedAt might be nulled out or kept depending on business rule
        this.updatedAt = Instant.now()
        _domainEvents.add(FriendStatusChangedEvent(this.id, this.status))
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
        _domainEvents.add(FriendStatusChangedEvent(this.id, this.status))
    }

    fun markAsPending() {
        if (this.status == FriendStatus.PENDING) return
        this.status = FriendStatus.PENDING
        this.updatedAt = Instant.now()
        _domainEvents.add(FriendStatusChangedEvent(this.id, this.status))
    }

    fun markAsRequested() {
        // This status might be used if the current user sent the request
        if (this.status == FriendStatus.REQUESTED) return
        this.status = FriendStatus.REQUESTED
        this.updatedAt = Instant.now()
        _domainEvents.add(FriendStatusChangedEvent(this.id, this.status))
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Friend
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        fun newRequest(
            id: FriendId,
            name: FriendName,
            profileImageUrl: FriendProfileImageUrl?,
            requestedAt: Instant // Time the request is made
        ): Friend {
            val now = Instant.now()
            val friend = Friend(
                id = id,
                name = name,
                profileImageUrl = profileImageUrl,
                status = FriendStatus.REQUESTED, // Current user sent the request
                requestedAt = requestedAt,
                acceptedAt = null,
                createdAt = now,
                updatedAt = now
            )
            friend._domainEvents.add(FriendCreatedEvent(id))
            // Optionally, add a FriendStatusChangedEvent if REQUESTED is considered a status change from an initial null state
            // friend._domainEvents.add(FriendStatusChangedEvent(id, FriendStatus.REQUESTED))
            return friend
        }

        fun receivedRequest(
            id: FriendId,
            name: FriendName,
            profileImageUrl: FriendProfileImageUrl?,
            requestedAt: Instant // Time the request was received
        ): Friend {
            val now = Instant.now()
            val friend = Friend(
                id = id,
                name = name,
                profileImageUrl = profileImageUrl,
                status = FriendStatus.PENDING, // Current user received the request, it's pending their action
                requestedAt = requestedAt,
                acceptedAt = null,
                createdAt = now,
                updatedAt = now
            )
            friend._domainEvents.add(FriendCreatedEvent(id))
            // friend._domainEvents.add(FriendStatusChangedEvent(id, FriendStatus.PENDING))
            return friend
        }

        // Factory method for reconstructing from data source
        fun fromDataSource(
            id: FriendId,
            name: FriendName,
            profileImageUrl: FriendProfileImageUrl?,
            status: FriendStatus,
            requestedAt: Instant?,
            acceptedAt: Instant?,
            createdAt: Instant,
            updatedAt: Instant
        ): Friend {
            return Friend(
                id = id,
                name = name,
                profileImageUrl = profileImageUrl,
                status = status,
                requestedAt = requestedAt,
                acceptedAt = acceptedAt,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
            // No domain event on reconstruction from data source usually, unless specific logic dictates it.
        }
    }
}

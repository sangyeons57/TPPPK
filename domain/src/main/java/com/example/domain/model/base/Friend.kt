package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.AggregateRoot
import com.example.domain.event.DomainEvent
import com.example.domain.event.friend.FriendCreatedEvent
import com.example.domain.event.friend.FriendNameChangedEvent
import com.example.domain.event.friend.FriendProfileImageChangedEvent
import com.example.domain.event.friend.FriendStatusChangedEvent
import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.user.UserName
import java.time.Instant

class Friend private constructor(
    // Immutable properties
    initialRequestedAt: Instant?, // This could be when the friend request was sent by the current user or received
    initialAcceptedAt: Instant?,  // This could be when the friend request was accepted

    // Mutable properties
    initialName: UserName,
    initialProfileImageUrl: ImageUrl?,
    initialStatus: FriendStatus,
    override val id :DocumentId,
    override val isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,

) : AggregateRoot() {

    val requestedAt: Instant? = initialRequestedAt
    var acceptedAt: Instant? = initialAcceptedAt // Made var in case it needs to be set by a method
        private set


    var name: UserName = initialName
        private set

    var profileImageUrl: ImageUrl? = initialProfileImageUrl
        private set

    var status: FriendStatus = initialStatus
        private set

    init {
        setOriginalState()
    }

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_STATUS to status,
            KEY_REQUESTED_AT to requestedAt,
            KEY_ACCEPTED_AT to acceptedAt,
            KEY_NAME to name.value,
            KEY_PROFILE_IMAGE_URL to profileImageUrl?.value,
            KEY_CREATED_AT to createdAt,
            KEY_UPDATED_AT to updatedAt
        )
    }

    // --- Business Methods ---

    fun changeName(newName: UserName) {
        if (this.name == newName) return
        this.name = newName
        this.pushDomainEvent(FriendNameChangedEvent(this.id, newName))
    }

    fun changeProfileImage(newProfileImageUrl: ImageUrl?) {
        if (this.profileImageUrl == newProfileImageUrl) return
        this.profileImageUrl = newProfileImageUrl
        this.pushDomainEvent(FriendProfileImageChangedEvent(this.id, newProfileImageUrl))
    }

    fun acceptRequest() {
        if (this.status == FriendStatus.PENDING || this.status == FriendStatus.REQUESTED) {
            this.status = FriendStatus.ACCEPTED
            this.acceptedAt = Instant.now()
            this.pushDomainEvent(FriendStatusChangedEvent(this.id, this.status))
        }
        // Consider if any action/event is needed if status is not PENDING/REQUESTED
    }

    fun blockUser() {
        if (this.status == FriendStatus.BLOCKED) return
        this.status = FriendStatus.BLOCKED
        // acceptedAt might be nulled out or kept depending on business rule
        this.pushDomainEvent(FriendStatusChangedEvent(this.id, this.status))
    }

    fun removeFriend() {
        if (this.status == FriendStatus.REMOVED) return
        this.status = FriendStatus.REMOVED
        this.pushDomainEvent(FriendStatusChangedEvent(this.id, this.status))
    }

    fun markAsPending() {
        if (this.status == FriendStatus.PENDING) return
        this.status = FriendStatus.PENDING
        this.pushDomainEvent(FriendStatusChangedEvent(this.id, this.status))
    }

    fun markAsRequested() {
        // This status might be used if the current user sent the request
        if (this.status == FriendStatus.REQUESTED) return
        this.status = FriendStatus.REQUESTED
        this.pushDomainEvent(FriendStatusChangedEvent(this.id, this.status))
    }

    companion object {
        const val COLLECTION_NAME = "friends"
        const val KEY_STATUS = "status" // "PENDING_SENT", "PENDING_RECEIVED", "ACCEPTED", "DECLINED", "BLOCKED"
        const val KEY_REQUESTED_AT = "requestedAt"
        const val KEY_ACCEPTED_AT = "acceptedAt"
        const val KEY_NAME = "name"
        const val KEY_PROFILE_IMAGE_URL = "profileImageUrl"

        fun newRequest(
            id: DocumentId,
            name: UserName,
            profileImageUrl: ImageUrl?,
            requestedAt: Instant // Time the request is made
        ): Friend {
            val friend = Friend(
                initialName = name,
                initialProfileImageUrl = profileImageUrl,
                initialStatus = FriendStatus.REQUESTED, // Current user sent the request
                initialRequestedAt = requestedAt,
                initialAcceptedAt = null,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                id = id,
                isNew = true
            )
            // Optionally, add a FriendStatusChangedEvent if REQUESTED is considered a status change from an initial null state
            // friend._domainEvents.add(FriendStatusChangedEvent(id, FriendStatus.REQUESTED))
            return friend
        }

        fun receivedRequest(
            id: DocumentId,
            name: UserName,
            profileImageUrl: ImageUrl?,
            requestedAt: Instant // Time the request was received
        ): Friend {
            val friend = Friend(
                initialName = name,
                initialProfileImageUrl = profileImageUrl,
                initialStatus = FriendStatus.PENDING, // Current user received the request, it's pending their action
                initialRequestedAt = requestedAt,
                initialAcceptedAt = null,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                id = id,
                isNew = true
            )
            // friend._domainEvents.add(FriendStatusChangedEvent(id, FriendStatus.PENDING))
            return friend
        }

        // Factory method for reconstructing from data source
        fun fromDataSource(
            id: DocumentId,
            name: UserName,
            profileImageUrl: ImageUrl?,
            status: FriendStatus,
            requestedAt: Instant?,
            acceptedAt: Instant?,
            createdAt: Instant?,
            updatedAt: Instant?
        ): Friend {
            return Friend(
                initialName = name,
                initialProfileImageUrl = profileImageUrl,
                initialStatus = status,
                initialRequestedAt = requestedAt,
                initialAcceptedAt = acceptedAt,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                id = id,
                isNew = false
            )
            // No domain event on reconstruction from data source usually, unless specific logic dictates it.
        }
    }
}

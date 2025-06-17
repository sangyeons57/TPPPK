package com.example.domain.model.base

import com.example.domain.event.AggregateRoot
import com.example.domain.event.DomainEvent
import com.example.domain.event.invite.InviteCreatedEvent
import com.example.domain.event.invite.InviteExpiredEvent
import com.example.domain.event.invite.InviteStatusChangedEvent
import com.example.domain.model.enum.InviteStatus // Keep existing import for now, will be com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.DocumentId // For createdBy
import com.example.domain.model.vo.invite.InviteCode
import com.example.domain.model.vo.invite.InviteId
import java.time.Instant

class Invite private constructor(
    id: InviteId,
    inviteCode: InviteCode,
    status: InviteStatus,
    createdBy: DocumentId,
    createdAt: Instant,
    updatedAt: Instant,
    expiresAt: Instant?
) : AggregateRoot {

    // Immutable properties
    val id: InviteId = id
    val inviteCode: InviteCode = inviteCode
    val createdBy: DocumentId = createdBy
    val createdAt: Instant = createdAt
    val expiresAt: Instant? = expiresAt

    // Mutable properties
    var status: InviteStatus = status
        private set

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

    // Business logic methods for state changes will be added in a later step
    // (e.g., changeStatus, expire)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Invite
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    // Add new methods here:
    fun changeStatus(newStatus: InviteStatus) {
        if (this.status == newStatus) return // No change if status is the same
        val oldStatus = this.status // Capture old status before changing
        if (this.status == InviteStatus.EXPIRED && newStatus != InviteStatus.EXPIRED) {
            // Potentially disallow changing status away from EXPIRED, depending on business rules
            // For now, let's assume it's possible but log an event.
        }

        this.status = newStatus
        this.updatedAt = Instant.now()
        _domainEvents.add(InviteStatusChangedEvent(this.id, newStatus, oldStatus, this.updatedAt))
    }

    fun expire() {
        if (this.status == InviteStatus.EXPIRED) return // Already expired

        this.status = InviteStatus.EXPIRED
        this.updatedAt = Instant.now()
        _domainEvents.add(InviteExpiredEvent(this.id, this.updatedAt))
    }

    // Method to check if invite is still valid (not expired and active)
    fun isActive(): Boolean {
        val now = Instant.now()
        return this.status == InviteStatus.ACTIVE && (this.expiresAt == null || this.expiresAt.isAfter(now))
    }


    companion object {
        fun create(
            id: InviteId,
            inviteCode: InviteCode,
            createdBy: DocumentId,
            expiresAt: Instant? // Optional expiration
        ): Invite {
            val now = Instant.now()
            var initialStatus = InviteStatus.ACTIVE
            if (expiresAt != null && expiresAt.isBefore(now)) {
                initialStatus = InviteStatus.EXPIRED
            }

            val invite = Invite(
                id = id,
                inviteCode = inviteCode,
                status = initialStatus,
                createdBy = createdBy,
                createdAt = now,
                updatedAt = now,
                expiresAt = expiresAt
            )
            invite._domainEvents.add(InviteCreatedEvent(invite.id, invite.createdBy, invite.status, invite.expiresAt, invite.createdAt)) // Use invite.createdAt for occurredOn consistency
            return invite
        }

        // Factory method for reconstituting from data source
        fun fromDataSource(
            id: InviteId,
            inviteCode: InviteCode,
            status: InviteStatus,
            createdBy: DocumentId,
            createdAt: Instant,
            updatedAt: Instant,
            expiresAt: Instant?
        ): Invite {
            return Invite(
                id = id,
                inviteCode = inviteCode,
                status = status,
                createdBy = createdBy,
                createdAt = createdAt,
                updatedAt = updatedAt,
                expiresAt = expiresAt
            )
        }
    }
}

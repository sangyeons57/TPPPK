package com.example.domain.model.base

import com.example.domain.model.AggregateRoot
import com.example.domain.event.DomainEvent
import com.example.domain.event.invite.InviteCreatedEvent
import com.example.domain.event.invite.InviteExpiredEvent
import com.example.domain.event.invite.InviteStatusChangedEvent
import com.example.domain.model.enum.InviteStatus // Keep existing import for now, will be com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.DocumentId // For createdBy
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.invite.InviteCode
import java.time.Instant

class Invite private constructor(
    initialInviteCode: InviteCode,
    initialStatus: InviteStatus,
    initialCreatedBy: OwnerId,
    initialCreatedAt: Instant,
    initialUpdateAt: Instant,
    initialExpiresAt: Instant?,
    override val id: DocumentId,
    override var isNew: Boolean
) : AggregateRoot() {

    // Immutable properties
    val inviteCode: InviteCode = initialInviteCode
    val createdBy: OwnerId = initialCreatedBy
    override val createdAt: Instant = initialCreatedAt
    val expiresAt: Instant? = initialExpiresAt

    // Mutable properties
    var status: InviteStatus = initialStatus
        private set

    override var updatedAt: Instant = initialUpdateAt
        private set

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_INVITE_LINK to this.inviteCode,
            KEY_STATUS to this.status,
            KEY_CREATED_BY to this.createdBy,
            KEY_CREATED_AT to this.createdAt,
            KEY_EXPIRES_AT to this.expiresAt,
            KEY_UPDATED_AT to this.updatedAt
        )
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
        this.pushDomainEvent(InviteStatusChangedEvent(this.id, newStatus, oldStatus, this.updatedAt))
    }

    fun expire() {
        if (this.status == InviteStatus.EXPIRED) return // Already expired

        this.status = InviteStatus.EXPIRED
        this.updatedAt = Instant.now()
        this.pushDomainEvent(InviteExpiredEvent(this.id, this.updatedAt))
    }

    // Method to check if invite is still valid (not expired and active)
    fun isActive(): Boolean {
        val now = Instant.now()
        return this.status == InviteStatus.ACTIVE && (this.expiresAt == null || this.expiresAt.isAfter(now))
    }


    companion object {
        const val COLLECTION_NAME = "invites"
        const val KEY_INVITE_LINK = "inviteCode"
        const val KEY_STATUS = "status"
        const val KEY_CREATED_BY = "createdBy"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_UPDATED_AT = "updatedAt"
        const val KEY_EXPIRES_AT = "expiresAt"

        fun create(
            id: DocumentId,
            inviteCode: InviteCode,
            createdBy: OwnerId,
            expiresAt: Instant? // Optional expiration
        ): Invite {
            val now = Instant.now()
            var initialStatus = InviteStatus.ACTIVE
            if (expiresAt != null && expiresAt.isBefore(now)) {
                initialStatus = InviteStatus.EXPIRED
            }

            val invite = Invite(
                initialInviteCode = inviteCode,
                initialStatus = initialStatus,
                initialCreatedBy = createdBy,
                initialCreatedAt = now,
                initialUpdateAt = now,
                initialExpiresAt = expiresAt,
                id = id,
                isNew = true
            )
            invite.pushDomainEvent(InviteCreatedEvent(invite.id, invite.createdBy, invite.status, invite.expiresAt, invite.createdAt)) // Use invite.createdAt for occurredOn consistency
            return invite
        }

        // Factory method for reconstituting from data source
        fun fromDataSource(
            id: DocumentId,
            inviteCode: InviteCode,
            status: InviteStatus,
            createdBy: OwnerId,
            createdAt: Instant,
            updatedAt: Instant,
            expiresAt: Instant?
        ): Invite {
            return Invite(
                initialInviteCode = inviteCode,
                initialStatus = status,
                initialCreatedBy = createdBy,
                initialCreatedAt = createdAt,
                initialUpdateAt = updatedAt,
                initialExpiresAt = expiresAt,
                id = id,
                isNew = false
            )
        }
    }
}

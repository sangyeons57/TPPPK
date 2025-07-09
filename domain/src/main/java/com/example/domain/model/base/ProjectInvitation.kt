package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.AggregateRoot
import com.example.domain.event.DomainEvent
import com.example.domain.event.invite.InviteCreatedEvent
import com.example.domain.event.invite.InviteExpiredEvent
import com.example.domain.event.invite.InviteStatusChangedEvent
import com.example.domain.model.enum.ProjectInvitationStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.invite.InviteCode
import java.time.Instant
import java.util.Date

class ProjectInvitation private constructor(
    initialStatus: ProjectInvitationStatus,
    initialCreatedBy: UserId,
    initialProjectId: DocumentId,
    initialInviteeId: UserId,
    initialMessage: String?,
    initialExpiresAt: Instant?,
    override val id: DocumentId, // This IS the invite code
    override var isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    // Immutable properties
    val inviterId: UserId = initialCreatedBy
    val projectId: DocumentId = initialProjectId
    val inviteeId: UserId = initialInviteeId
    val message: String? = initialMessage

    // Mutable properties
    var status: ProjectInvitationStatus = initialStatus
        private set
    var expiresAt: Instant? = initialExpiresAt
        private set

    init {
        setOriginalState()
    }

    /**
     * Gets the invite code (which is the same as the document ID)
     */
    val inviteCode: InviteCode
        get() = InviteCode(id.value)

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_INVITE_LINK to this.inviteCode.value,
            KEY_STATUS to this.status,
            KEY_INVITER_ID to this.inviterId.value,
            KEY_PROJECT_ID to this.projectId.value,
            KEY_INVITEE_ID to this.inviteeId.value,
            KEY_MESSAGE to this.message,
            KEY_CREATED_AT to this.createdAt,
            KEY_EXPIRES_AT to this.expiresAt,
            KEY_UPDATED_AT to this.updatedAt
        )
    }

    // Add new methods here:
    fun changeStatus(newStatus: ProjectInvitationStatus) {
        if (this.status == newStatus) return // No change if status is the same
        val oldStatus = this.status // Capture old status before changing
        if (this.status == ProjectInvitationStatus.EXPIRED && newStatus != ProjectInvitationStatus.EXPIRED) {
            // Potentially disallow changing status away from EXPIRED, depending on business rules
            // For now, let's assume it's possible but log an event.
        }

        this.status = newStatus
        this.pushDomainEvent(InviteStatusChangedEvent(this.id, newStatus, oldStatus, DateTimeUtil.nowInstant()))
    }

    fun expire() {
        if (this.status == ProjectInvitationStatus.EXPIRED) return // Already expired

        this.status = ProjectInvitationStatus.EXPIRED
        this.pushDomainEvent(InviteExpiredEvent(this.id, DateTimeUtil.nowInstant()))
    }

    // Method to check if invite is still valid (not expired and active)
    fun isActive(): Boolean {
        return this.status == ProjectInvitationStatus.PENDING && (this.expiresAt == null || this.expiresAt!!.isAfter(DateTimeUtil.nowInstant()))
    }

    // Method to accept the invitation
    fun accept() {
        if (this.status != ProjectInvitationStatus.PENDING) {
            throw IllegalStateException("Cannot accept invitation with status: ${this.status}")
        }
        if (this.expiresAt != null && this.expiresAt!!.isBefore(DateTimeUtil.nowInstant())) {
            throw IllegalStateException("Cannot accept expired invitation")
        }
        
        this.status = ProjectInvitationStatus.ACCEPTED
        this.pushDomainEvent(InviteStatusChangedEvent(this.id, ProjectInvitationStatus.ACCEPTED, ProjectInvitationStatus.PENDING, DateTimeUtil.nowInstant()))
    }

    // Method to reject the invitation
    fun reject() {
        if (this.status != ProjectInvitationStatus.PENDING) {
            throw IllegalStateException("Cannot reject invitation with status: ${this.status}")
        }
        
        this.status = ProjectInvitationStatus.REJECTED
        this.pushDomainEvent(InviteStatusChangedEvent(this.id, ProjectInvitationStatus.REJECTED, ProjectInvitationStatus.PENDING, DateTimeUtil.nowInstant()))
    }

    // Method to cancel the invitation (by inviter)
    fun cancel() {
        if (this.status != ProjectInvitationStatus.PENDING) {
            throw IllegalStateException("Cannot cancel invitation with status: ${this.status}")
        }
        
        this.status = ProjectInvitationStatus.CANCELLED
        this.pushDomainEvent(InviteStatusChangedEvent(this.id, ProjectInvitationStatus.CANCELLED, ProjectInvitationStatus.PENDING, DateTimeUtil.nowInstant()))
    }


    companion object {
        const val COLLECTION_NAME = "project_invitations"
        const val KEY_INVITE_LINK = "inviteCode"
        const val KEY_STATUS = "status"
        const val KEY_INVITER_ID = "inviterId"
        const val KEY_PROJECT_ID = "projectId"
        const val KEY_INVITEE_ID = "inviteeId"
        const val KEY_MESSAGE = "message"
        const val KEY_EXPIRES_AT = "expiresAt"

        fun create(
            inviteCodeId: DocumentId, // The invite code that will be used as document ID
            inviterId: UserId,
            projectId: DocumentId,
            inviteeId: UserId,
            message: String? = null,
            expiresAt: Instant? = null // Optional expiration
        ): ProjectInvitation {
            var initialStatus = ProjectInvitationStatus.PENDING
            if (expiresAt != null && expiresAt.isBefore(DateTimeUtil.nowInstant())) {
                initialStatus = ProjectInvitationStatus.EXPIRED
            }

            val invitation = ProjectInvitation(
                initialStatus = initialStatus,
                initialCreatedBy = inviterId,
                initialProjectId = projectId,
                initialInviteeId = inviteeId,
                initialMessage = message,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                initialExpiresAt = expiresAt,
                id = inviteCodeId, // ID is the invite code
                isNew = true
            )
            
            // Fire domain event for invitation creation
            invitation.pushDomainEvent(InviteCreatedEvent(invitation.id, invitation.inviterId, invitation.projectId, invitation.inviteeId, DateTimeUtil.nowInstant()))
            
            return invitation
        }

        // Factory method for reconstituting from data source
        fun fromDataSource(
            id: DocumentId, // This IS the invite code
            status: ProjectInvitationStatus,
            inviterId: UserId,
            projectId: DocumentId,
            inviteeId: UserId,
            message: String?,
            createdAt: Instant?,
            updatedAt: Instant?,
            expiresAt: Instant?
        ): ProjectInvitation {
            return ProjectInvitation(
                initialStatus = status,
                initialCreatedBy = inviterId,
                initialProjectId = projectId,
                initialInviteeId = inviteeId,
                initialMessage = message,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                initialExpiresAt = expiresAt,
                id = id, // ID is the invite code
                isNew = false
            )
        }
    }
}

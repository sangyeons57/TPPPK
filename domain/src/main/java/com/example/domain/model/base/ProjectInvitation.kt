package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.AggregateRoot
import com.example.domain.event.DomainEvent
import com.example.domain.event.invite.InviteCreatedEvent
import com.example.domain.event.invite.InviteExpiredEvent
import com.example.domain.event.invite.InviteStatusChangedEvent
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.invite.InviteCode
import java.time.Instant
import java.util.Date

class ProjectInvitation private constructor(
    initialStatus: InviteStatus,
    initialInviterId: UserId,
    initialProjectId: DocumentId,
    initialExpiresAt: Instant?,
    override val id: DocumentId, // This IS the invite code
    override var isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    // Immutable properties
    val inviterId: UserId = initialInviterId
    val projectId: DocumentId = initialProjectId

    // Mutable properties
    var status: InviteStatus = initialStatus
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
            KEY_INVITE_CODE to this.inviteCode.value,
            KEY_STATUS to this.status.value,
            KEY_INVITER_ID to this.inviterId.value,
            KEY_PROJECT_ID to this.projectId.value,
            KEY_CREATED_AT to this.createdAt,
            KEY_EXPIRES_AT to this.expiresAt,
            KEY_UPDATED_AT to this.updatedAt
        )
    }

    // Business methods for invite link management
    fun changeStatus(newStatus: InviteStatus) {
        if (this.status == newStatus) return // No change if status is the same
        val oldStatus = this.status // Capture old status before changing
        
        // Only allow certain transitions
        if (oldStatus.isCompleted() && newStatus != oldStatus) {
            throw IllegalStateException("Cannot change status from completed state: $oldStatus to $newStatus")
        }

        this.status = newStatus
        this.pushDomainEvent(InviteStatusChangedEvent(this.id, newStatus, oldStatus, DateTimeUtil.nowInstant()))
    }

    fun expire() {
        if (this.status == InviteStatus.EXPIRED) return // Already expired

        val oldStatus = this.status
        this.status = InviteStatus.EXPIRED
        this.pushDomainEvent(InviteExpiredEvent(this.id, DateTimeUtil.nowInstant()))
        this.pushDomainEvent(InviteStatusChangedEvent(this.id, InviteStatus.EXPIRED, oldStatus, DateTimeUtil.nowInstant()))
    }

    fun revoke() {
        if (this.status == InviteStatus.REVOKED) return // Already revoked
        if (this.status.isCompleted()) {
            throw IllegalStateException("Cannot revoke invite with status: ${this.status}")
        }

        val oldStatus = this.status
        this.status = InviteStatus.REVOKED
        this.pushDomainEvent(InviteStatusChangedEvent(this.id, InviteStatus.REVOKED, oldStatus, DateTimeUtil.nowInstant()))
    }

    // Method to check if invite is still valid and usable
    fun isActive(): Boolean {
        return this.status == InviteStatus.ACTIVE && 
               (this.expiresAt == null || this.expiresAt!!.isAfter(DateTimeUtil.nowInstant()))
    }

    // Method to check if invite can be used
    fun canBeUsed(): Boolean {
        return isActive()
    }


    companion object {
        const val COLLECTION_NAME = "project_invitations"
        const val KEY_INVITE_CODE = "inviteCode"
        const val KEY_STATUS = "status"
        const val KEY_INVITER_ID = "inviterId"
        const val KEY_PROJECT_ID = "projectId"
        const val KEY_EXPIRES_AT = "expiresAt"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_UPDATED_AT = "updatedAt"

        fun create(
            inviteCodeId: DocumentId, // The invite code that will be used as document ID
            inviterId: UserId,
            projectId: DocumentId,
            expiresAt: Instant? = null // Optional expiration
        ): ProjectInvitation {
            var initialStatus = InviteStatus.ACTIVE
            if (expiresAt != null && expiresAt.isBefore(DateTimeUtil.nowInstant())) {
                initialStatus = InviteStatus.EXPIRED
            }

            val invitation = ProjectInvitation(
                initialStatus = initialStatus,
                initialInviterId = inviterId,
                initialProjectId = projectId,
                initialExpiresAt = expiresAt,
                id = inviteCodeId, // ID is the invite code
                isNew = true,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant()
            )
            
            // Fire domain event for invitation creation
            invitation.pushDomainEvent(InviteCreatedEvent(invitation.id, invitation.inviterId, invitation.projectId))
            
            return invitation
        }

        /**
         * DDD 패턴을 위한 팩토리 메서드 - 새로운 초대 링크 생성
         * ID는 Firebase Functions에서 자동 생성되므로 임시 ID를 사용합니다.
         */
        fun createNew(
            inviterId: UserId,
            projectId: DocumentId,
            expiresInHours: Long = 24L
        ): ProjectInvitation {
            val expiresAt = DateTimeUtil.nowInstant().plusSeconds(expiresInHours * 3600)
            
            val invitation = ProjectInvitation(
                initialStatus = InviteStatus.ACTIVE,
                initialInviterId = inviterId,
                initialProjectId = projectId,
                initialExpiresAt = expiresAt,
                id = DocumentId.EMPTY,
                isNew = true,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant()
            )
            
            // Fire domain event for invitation creation
            invitation.pushDomainEvent(InviteCreatedEvent(invitation.id, invitation.inviterId, invitation.projectId, DateTimeUtil.nowInstant()))
            
            return invitation
        }

        // Factory method for reconstituting from data source
        fun fromDataSource(
            id: DocumentId, // This IS the invite code
            status: InviteStatus,
            inviterId: UserId,
            projectId: DocumentId,
            createdAt: Instant?,
            updatedAt: Instant?,
            expiresAt: Instant?
        ): ProjectInvitation {
            return ProjectInvitation(
                initialStatus = status,
                initialInviterId = inviterId,
                initialProjectId = projectId,
                initialExpiresAt = expiresAt,
                id = id, // ID is the invite code
                isNew = false,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant()
            )
        }
    }
}

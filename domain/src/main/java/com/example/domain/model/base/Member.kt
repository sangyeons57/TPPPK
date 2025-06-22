package com.example.domain.model.base

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant

import com.example.domain.event.AggregateRoot
import com.example.domain.event.member.MemberJoinedEvent
import com.example.domain.event.member.MemberRolesUpdatedEvent
import com.example.domain.model.vo.DocumentId // For Role IDs and Member ID
import com.example.domain.model.vo.UserId

class Member private constructor(
    initialRoleIds: List<DocumentId>,
    initialJoinedAt: Instant,
    initialUpdatedAt: Instant,
    override val id: DocumentId,
    override var isNew: Boolean
) : AggregateRoot() {

    // Immutable properties
    val joinedAt: Instant = initialJoinedAt

    // Mutable properties
    var roleIds: List<DocumentId> = initialRoleIds
        private set
    var updatedAt: Instant = initialUpdatedAt
        private set

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_ROLE_ID to this.roleIds,
            KEY_JOINED_AT to this.joinedAt,
            KEY_UPDATED_AT to this.updatedAt
        )
    }

    /**
     * Assigns a new role to the member if they don't already have it.
     */
    fun assignRole(roleId: DocumentId) {
        if (roleIds.contains(roleId)) return

        this.roleIds += roleId
        this.updatedAt = Instant.now()
        pushDomainEvent(MemberRolesUpdatedEvent(this.id, this.roleIds, this.updatedAt))
    }

    fun updateRoles(roleIds: List<DocumentId>) {
        this.roleIds = roleIds
        this.updatedAt = Instant.now()
        pushDomainEvent(MemberRolesUpdatedEvent(this.id, this.roleIds, this.updatedAt))
    }

    /**
     * Revokes a role from the member if they have it.
     */
    fun revokeRole(roleId: DocumentId) {
        if (!roleIds.contains(roleId)) return

        this.roleIds = this.roleIds - roleId
        this.updatedAt = Instant.now()
        pushDomainEvent(MemberRolesUpdatedEvent(this.id, this.roleIds, this.updatedAt))
    }

    companion object {
        const val COLLECTION_NAME = "members"
        const val KEY_JOINED_AT = "joinedAt"
        const val KEY_ROLE_ID = "roleIds" // List<String>
        const val KEY_UPDATED_AT = "updatedAt"
        /**
         * Factory method for a new member joining.
         */
        fun create(
            memberId: UserId,
            initialRoleIds: List<DocumentId>): Member {
            val now = Instant.now()
            val member = Member(
                initialRoleIds = initialRoleIds,
                initialJoinedAt = now,
                initialUpdatedAt = now,
                id = DocumentId.from(memberId),
                isNew = true
            )
            return member
        }

        private val PROJECT_OWNER_MEMBER = DocumentId("OWNER")
        fun createOwnerMember() : Member {
            val now = Instant.now()
            val member = Member(
                initialRoleIds = emptyList(),
                initialJoinedAt = now,
                initialUpdatedAt = now,
                id = PROJECT_OWNER_MEMBER,
                isNew = true
            )
            return member
        }

        /**
         * Factory method to reconstitute a Member from a data source.
         */
        fun fromDataSource(
            id: DocumentId,
            roleIds: List<DocumentId>,
            joinedAt: Instant,
            updatedAt: Instant
        ): Member {
            return Member(
                initialRoleIds = roleIds,
                initialJoinedAt = joinedAt,
                initialUpdatedAt = updatedAt,
                id = id,
                isNew = false
            )
        }
    }
}


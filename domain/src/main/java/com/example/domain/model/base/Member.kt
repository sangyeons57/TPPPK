package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.AggregateRoot
import com.example.domain.event.member.MemberRolesUpdatedEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import java.time.Instant

class Member private constructor(
    initialRoleIds: List<DocumentId>,
    override val id: DocumentId,
    override var isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    // Immutable properties
    // Mutable properties
    var roleIds: List<DocumentId> = initialRoleIds
        private set

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_ROLE_ID to this.roleIds,
            KEY_CREATED_AT to this.createdAt,
            KEY_UPDATED_AT to this.updatedAt
        )
    }

    /**
     * Assigns a new role to the member if they don't already have it.
     */
    fun assignRole(roleId: DocumentId) {
        if (roleIds.contains(roleId)) return

        this.roleIds += roleId
        pushDomainEvent(MemberRolesUpdatedEvent(this.id, this.roleIds, DateTimeUtil.nowInstant()))
    }

    fun updateRoles(roleIds: List<DocumentId>) {
        this.roleIds = roleIds
        pushDomainEvent(MemberRolesUpdatedEvent(this.id, this.roleIds, DateTimeUtil.nowInstant()))
    }

    /**
     * Revokes a role from the member if they have it.
     */
    fun revokeRole(roleId: DocumentId) {
        if (!roleIds.contains(roleId)) return

        this.roleIds = this.roleIds - roleId
        pushDomainEvent(MemberRolesUpdatedEvent(this.id, this.roleIds, DateTimeUtil.nowInstant()))
    }

    companion object {
        const val COLLECTION_NAME = "members"
        const val KEY_ROLE_ID = "roleIds" // List<String>


        /**
         * Factory method for a new member joining.
         */
        fun create(
            id: DocumentId,
            roleIds: List<DocumentId>
        ): Member {

            val member = Member(
                id = id,
                initialRoleIds = roleIds,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                isNew = true
            )
            return member
        }
        fun create(
            roleIds: List<DocumentId>
        ): Member {

            val member = Member(
                id = DocumentId.EMPTY,
                initialRoleIds = roleIds,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
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
            createdAt: Instant?,
            updatedAt: Instant?
        ): Member {
            return Member(
                initialRoleIds = roleIds,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                id = id,
                isNew = false
            )
        }
    }
}


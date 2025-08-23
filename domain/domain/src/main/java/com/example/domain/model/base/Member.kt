package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import com.example.domain.AggregateRoot
import com.example.domain.event.member.MemberRolesUpdatedEvent
import com.example.domain.vo.DocumentId
import java.time.Instant

class Member private constructor(
    initialRoleIds: List<DocumentId>,
    initialStatus: MemberStatus,
    initialBlockedAt: Instant?,
    initialBlockedBy: DocumentId?,
    override val id: DocumentId,
    override var isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    // Immutable properties
    // Mutable properties
    var roleIds: List<DocumentId> = initialRoleIds
        private set

    var status: MemberStatus = initialStatus
        private set

    var blockedAt: Instant? = initialBlockedAt
        private set

    var blockedBy: DocumentId? = initialBlockedBy
        private set

    init {
        setOriginalState()
    }

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_ROLE_ID to this.roleIds.map { it.value },
            KEY_STATUS to MemberStatus.toString(this.status),
            KEY_BLOCKED_AT to this.blockedAt,
            KEY_BLOCKED_BY to this.blockedBy?.value,
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

    /**
     * 멤버를 차단합니다.
     */
    fun block(blockedBy: DocumentId) {
        this.status = MemberStatus.BLOCKED
        this.blockedAt = DateTimeUtil.nowInstant()
        this.blockedBy = blockedBy
    }

    /**
     * 멤버가 프로젝트에서 나간 상태로 변경합니다.
     */
    fun leave() {
        this.status = MemberStatus.LEAVE
        this.blockedAt = null
        this.blockedBy = null
    }

    /**
     * 멤버의 차단 상태를 해제하거나 프로젝트에 재참여 시 활성 상태로 변경합니다.
     */
    fun activate() {
        this.status = MemberStatus.ACTIVE
        this.blockedAt = null
        this.blockedBy = null
    }

    /**
     * 멤버가 활성 상태인지 확인합니다.
     */
    fun isActive(): Boolean = status == MemberStatus.ACTIVE

    /**
     * 멤버가 차단된 상태인지 확인합니다.
     */
    fun isBlocked(): Boolean = status == MemberStatus.BLOCKED

    /**
     * 멤버가 프로젝트를 나간 상태인지 확인합니다.
     */
    fun hasLeft(): Boolean = status == MemberStatus.LEAVE

    companion object {
        const val COLLECTION_NAME = "members"
        const val KEY_ROLE_ID = "roleIds" // List<String>
        const val KEY_STATUS = "status" // String
        const val KEY_BLOCKED_AT = "blockedAt" // Timestamp
        const val KEY_BLOCKED_BY = "blockedBy" // String


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
                initialStatus = MemberStatus.ACTIVE,
                initialBlockedAt = null,
                initialBlockedBy = null,
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
            status: MemberStatus? = null,
            blockedAt: Instant? = null,
            blockedBy: DocumentId? = null,
            createdAt: Instant?,
            updatedAt: Instant?
        ): Member {
            return Member(
                initialRoleIds = roleIds,
                initialStatus = status ?: MemberStatus.ACTIVE, // 기존 데이터 호환성
                initialBlockedAt = blockedAt,
                initialBlockedBy = blockedBy,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                id = id,
                isNew = false
            )
        }
    }
}


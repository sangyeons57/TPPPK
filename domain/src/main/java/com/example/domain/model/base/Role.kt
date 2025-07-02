package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.data.project.RolePermission // Added import

import com.example.domain.model.AggregateRoot
import com.example.domain.event.role.RoleCreatedEvent
import com.example.domain.event.role.RoleDefaultStatusChangedEvent
import com.example.domain.event.role.RoleNameChangedEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.role.RoleIsDefault
import java.time.Instant
import java.util.Date

class Role private constructor(
    // Constructor parameters to initialize the state
    initialName: Name,
    initialIsDefault: RoleIsDefault,
    override val id: DocumentId,
    override val isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    // Immutable properties

    // Mutable properties with private setters
    var name: Name = initialName
        private set
    var isDefault: RoleIsDefault = initialIsDefault
        private set

    init {
        setOriginalState()
    }

    // Implementation of abstract method from AggregateRoot
    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_NAME to this.name.value,
            KEY_IS_DEFAULT to this.isDefault.value,
            KEY_UPDATED_AT to this.updatedAt,
            KEY_CREATED_AT to this.createdAt,
        )
    }

    /**
     * Changes the name of the role, firing a domain event if the name is actually different.
     */
    fun changeName(newName: Name) {
        if (this.name == newName) return // No change, no event

        val oldName = this.name
        this.name = newName
        pushDomainEvent(RoleNameChangedEvent(this.id, oldName, newName, DateTimeUtil.nowInstant()))
    }

    /**
     * Changes the default status of the role, firing a domain event if the status changes.
     */
    fun setDefault(newDefaultStatus: RoleIsDefault) {
        if (this.isDefault == newDefaultStatus) return // No change, no event

        this.isDefault = newDefaultStatus
        pushDomainEvent(RoleDefaultStatusChangedEvent(this.id, newDefaultStatus, DateTimeUtil.nowInstant()))
    }

    companion object {
        const val COLLECTION_NAME = "roles"
        const val KEY_NAME = "name"
        const val KEY_IS_DEFAULT = "isDefault"

        // ===== 시스템 역할 상수 =====
        /**
         * 프로젝트 소유자 역할
         * - 프로젝트 생성자에게 자동으로 할당
         * - 최고 권한을 가지며 삭제하거나 편집할 수 없음
         */
        const val OWNER = "OWNER"
        
        /**
         * 시스템 역할 목록
         * 새로운 시스템 역할이 추가되면 이 목록에도 추가해야 함
         */
        val SYSTEM_ROLES = setOf(OWNER)
        
        // 하위 호환성을 위해 유지
        @Deprecated("Use OWNER instead", ReplaceWith("OWNER"))
        val PROJECT_ROLE_OWNER = OWNER
        /**
         * Factory method to create a new Role.
         * This method encapsulates the creation logic and fires a domain event.
         */
        fun create(
            name: Name,
            isDefault: RoleIsDefault
        ): Role {
            val role = Role(
                id = DocumentId.EMPTY,
                initialName = name,
                initialIsDefault = isDefault,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                isNew = true
            )
            return role
        }

        /**
         * 주어진 역할 ID가 시스템 역할인지 확인
         *
         * @param roleId 확인할 역할 ID
         * @return 시스템 역할이면 true, 아니면 false
         */
        fun isSystemRole(roleId: String): Boolean {
            return SYSTEM_ROLES.contains(roleId)
        }

        fun createOwner(
        ) : Role {
            val role = Role(
                id = DocumentId(OWNER),
                initialName = Name(OWNER),
                initialIsDefault = RoleIsDefault.NON_DEFAULT,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                isNew = true
            )
            return role
        }

        /**
         * Factory method to reconstitute a Role from a data source.
         * This method does not fire any domain events as it represents an existing entity.
         */
        fun fromDataSource(
            id: DocumentId,
            name: Name,
            isDefault: RoleIsDefault,
            createdAt: Instant?,
            updatedAt: Instant?
        ): Role {
            return Role(
                id = id,
                initialName = name,
                initialIsDefault = isDefault,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                isNew = false
            )
        }
    }
}

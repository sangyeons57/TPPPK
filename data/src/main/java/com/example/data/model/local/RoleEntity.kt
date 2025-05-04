package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Role
import com.example.domain.model.RolePermission

/**
 * 프로젝트 역할 데이터를 저장하기 위한 Room 엔티티
 */
@Entity(tableName = "roles")
data class RoleEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String? = null,
    val memberCount: Int? = null
) {
    /**
     * RoleEntity를 도메인 모델 Role로 변환
     * 
     * @param permissions 이 역할과 연관된 권한 맵
     * @return Role 도메인 모델
     */
    fun toDomain(permissions: Map<RolePermission, Boolean>): Role {
        return Role(
            id = id,
            projectId = projectId,
            name = name,
            permissions = permissions,
            memberCount = memberCount
        )
    }

    companion object {
        /**
         * 도메인 모델 Role을 RoleEntity로 변환
         * 
         * @param role 변환할 Role 객체
         * @return RoleEntity
         */
        fun fromDomain(role: Role): RoleEntity {
            return RoleEntity(
                id = role.id ?: throw IllegalArgumentException("Role ID cannot be null"),
                projectId = role.projectId,
                name = role.name,
                memberCount = role.memberCount
            )
        }
    }
} 
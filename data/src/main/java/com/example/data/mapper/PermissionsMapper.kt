package com.example.data.mapper

import com.example.data.model.local.PermissionsEntity
import com.example.domain.model.base.Permission
import com.example.domain.model.data.project.RolePermission

/**
 * PermissionsEntity와 Permission 도메인 모델 간의 변환을 담당하는 매퍼
 * 3-tier 동기화 아키텍처에서 Entity와 Domain 모델 간 변환을 처리합니다
 */
object PermissionsMapper {

    /**
     * Permission 도메인 모델을 PermissionsEntity로 변환
     * @param permission 변환할 Permission 도메인 모델
     * @param roleId 권한이 속한 역할 ID
     * @param projectId 권한이 속한 프로젝트 ID
     * @return PermissionsEntity
     */
    fun toEntity(permission: Permission, roleId: String, projectId: String): PermissionsEntity {
        return PermissionsEntity(
            id = permission.id.value,
            roleId = roleId,
            projectId = projectId,
            createdAt = permission.createdAt,
            updatedAt = permission.updatedAt
        )
    }

    /**
     * PermissionsEntity를 Permission 도메인 모델로 변환
     * @param entity 변환할 PermissionsEntity
     * @return Permission 도메인 모델
     */
    fun toDomain(entity: PermissionsEntity): Permission {
        val rolePermission = RolePermission.valueOf(entity.id)
        return Permission.fromDataSource(
            id = rolePermission,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Permission 도메인 모델 리스트를 PermissionsEntity 리스트로 변환
     * @param permissions 변환할 Permission 도메인 모델 리스트
     * @param roleId 권한들이 속한 역할 ID
     * @param projectId 권한들이 속한 프로젝트 ID
     * @return PermissionsEntity 리스트
     */
    fun toEntityList(
        permissions: List<Permission>,
        roleId: String,
        projectId: String
    ): List<PermissionsEntity> {
        return permissions.map { toEntity(it, roleId, projectId) }
    }

    /**
     * PermissionsEntity 리스트를 Permission 도메인 모델 리스트로 변환
     * @param entities 변환할 PermissionsEntity 리스트
     * @return Permission 도메인 모델 리스트
     */
    fun toDomainList(entities: List<PermissionsEntity>): List<Permission> {
        return entities.map { toDomain(it) }
    }
}
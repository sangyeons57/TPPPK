package com.example.mapper

import com.example.data_core.model.local.PermissionsEntity
import com.example.domain.model.base.Permission
import com.example.domain.model.data.project.RolePermission
import com.example.domain.model.vo.DocumentId
import com.example.mapper.base.BaseEntityMapper
import javax.inject.Inject

interface PermissionEntityMapper : BaseEntityMapper<Permission, PermissionsEntity>

class PermissionEntityMapperImpl @Inject constructor() : PermissionEntityMapper {
    override fun toDomain(entity: PermissionsEntity): Permission {
        return Permission.fromDataSource(
            id = DocumentId(entity.id),
            roleId = DocumentId(entity.roleId),
            permissionType = RolePermission.valueOf(entity.permissionType),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(domain: Permission): PermissionsEntity {
        return PermissionsEntity(
            id = domain.id.value,
            roleId = domain.roleId.value,
            permissionType = domain.permissionType.name,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}

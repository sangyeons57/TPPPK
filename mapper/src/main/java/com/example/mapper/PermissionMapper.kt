package com.example.mapper

import com.example.data.model.remote.PermissionDTO
import com.example.domain.model.base.Permission
import com.example.domain.model.data.project.RolePermission
import com.example.mapper.base.BaseMapper

interface PermissionMapper : BaseMapper<Permission, PermissionDTO>

class PermissionMapperImpl : PermissionMapper {
    override fun fromDto(dto: PermissionDTO): Permission {
        return Permission.fromDataSource(
            id = RolePermission.from(dto.id),
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun toDto(domain: Permission): PermissionDTO {
        return PermissionDTO(
            id = domain.id.value,
            createdAt = null,
            updatedAt = null
        )
    }

    override fun domainToMap(domain: Permission): Map<String, Any?> {
        return mapOf()
    }

    override fun dataToMap(data: PermissionDTO): Map<String, Any?> {
        return mapOf()
    }
}

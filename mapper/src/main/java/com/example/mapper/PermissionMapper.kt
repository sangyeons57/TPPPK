package com.example.mapper

import com.example.data.model.remote.PermissionDTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Permission
import com.example.domain.model.data.project.RolePermission
import com.example.mapper.base.BaseMapper
import java.util.Date

interface PermissionMapper : BaseMapper<Permission, PermissionDTO>

class PermissionMapperImpl : PermissionMapper {
    override fun toDomain(dto: PermissionDTO): Permission {
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

    override fun mapToDto(map: Map<String, Any?>): PermissionDTO {
        return PermissionDTO(
            id = map["id"] as? String ?: "",
            createdAt = (map[AggregateRoot.KEY_CREATED_AT] as? Date),
            updatedAt = (map[AggregateRoot.KEY_UPDATED_AT] as? Date)
        )
    }
}

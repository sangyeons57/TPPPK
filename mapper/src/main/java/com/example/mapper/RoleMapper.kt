package com.example.mapper

import com.example.data_core.model.remote.RoleDTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Role
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.role.RoleIsDefault
import com.example.mapper.base.BaseMapper
import java.util.Date

interface RoleMapper : BaseMapper<Role, RoleDTO>

class RoleMapperImpl : RoleMapper {
    override fun toDomain(dto: RoleDTO): Role {
        return Role.fromDataSource(
            id = DocumentId(dto.id),
            name = Name(dto.name),
            isDefault = RoleIsDefault(dto.isDefault),
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun toDto(domain: Role): RoleDTO {
        return RoleDTO(
            id = domain.id.value,
            name = domain.name.value,
            isDefault = domain.isDefault.value,
            createdAt = null, // Let Firestore set server timestamp
            updatedAt = null
        )
    }

    override fun domainToMap(domain: Role): Map<String, Any?> {
        return mapOf(
            Role.KEY_NAME to domain.name.value,
            Role.KEY_IS_DEFAULT to domain.isDefault.value,
        )
    }

    override fun dataToMap(data: RoleDTO): Map<String, Any?> {
        return mapOf(
            Role.KEY_NAME to data.name,
            Role.KEY_IS_DEFAULT to data.isDefault,
        )
    }

    override fun mapToDto(map: Map<String, Any?>): RoleDTO {
        return RoleDTO(
            id = map["id"] as? String ?: "",
            name = map[Role.KEY_NAME] as? String ?: "",
            isDefault = map[Role.KEY_IS_DEFAULT] as? Boolean ?: false,
            createdAt = (map[AggregateRoot.KEY_CREATED_AT] as? Date),
            updatedAt = (map[AggregateRoot.KEY_UPDATED_AT] as? Date)
        )
    }
}

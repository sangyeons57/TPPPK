package com.example.mapper

import com.example.data_core.model.local.RolesEntity
import com.example.domain.model.base.Role
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.ProjectId
import com.example.domain.model.vo.role.RoleIsDefault
import com.example.mapper.base.BaseEntityMapper
import javax.inject.Inject

interface RoleEntityMapper : BaseEntityMapper<Role, RolesEntity>

class RoleEntityMapperImpl @Inject constructor() : RoleEntityMapper {
    override fun toDomain(entity: RolesEntity): Role {
        return Role.fromDataSource(
            id = DocumentId(entity.id),
            projectId = ProjectId(entity.projectId),
            name = Name(entity.name),
            isDefault = RoleIsDefault(entity.isDefault),
            isSystem = entity.isSystem,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(domain: Role): RolesEntity {
        return RolesEntity(
            id = domain.id.value,
            projectId = domain.projectId.value,
            name = domain.name.value,
            isDefault = domain.isDefault.value,
            isSystem = domain.isSystem,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}

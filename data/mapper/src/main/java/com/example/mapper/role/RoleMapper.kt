package com.example.mapper.role

import com.example.data_model.remote.RoleDTO
import com.example.domain.model.base.Role
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.role.RoleIsDefault
import com.example.mapper.DtoMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Role 관련 Domain과 DTO 간의 매핑을 담당하는 Mapper
 */
@Singleton
class RoleMapper @Inject constructor() : DtoMapper<Role, RoleDTO> {

    override fun dtoToDomain(dto: RoleDTO): Role {
        return Role.fromDataSource(
            id = DocumentId(dto.id),
            name = Name(dto.name),
            isDefault = RoleIsDefault(dto.isDefault),
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun domainToDto(domain: Role): RoleDTO {
        return RoleDTO(
            id = domain.id.value,
            name = domain.name.value,
            isDefault = domain.isDefault.value,
            createdAt = null, // ServerTimestamp가 처리
            updatedAt = null  // ServerTimestamp가 처리
        )
    }
}
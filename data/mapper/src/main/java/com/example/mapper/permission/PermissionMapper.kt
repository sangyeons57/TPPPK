package com.example.mapper.permission

import com.example.data_model.remote.PermissionDTO
import com.example.domain.model.base.Permission
import com.example.domain.model.data.project.RolePermission
import com.example.mapper.DtoMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Permission 관련 Domain과 DTO 간의 매핑을 담당하는 Mapper
 */
@Singleton
class PermissionMapper @Inject constructor() : DtoMapper<Permission, PermissionDTO> {

    override fun dtoToDomain(dto: PermissionDTO): Permission {
        return Permission.fromDataSource(
            id = RolePermission.from(dto.id),
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun domainToDto(domain: Permission): PermissionDTO {
        return PermissionDTO(
            id = domain.id.value,
            createdAt = null, // ServerTimestamp가 처리
            updatedAt = null  // ServerTimestamp가 처리
        )
    }
}
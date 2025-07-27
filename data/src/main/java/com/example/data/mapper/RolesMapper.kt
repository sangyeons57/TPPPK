package com.example.data.mapper

import com.example.data.model.local.RolesEntity
import com.example.domain.model.base.Role
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.role.RoleIsDefault

/**
 * RolesEntity와 Role 도메인 모델 간의 변환을 담당하는 매퍼
 * 3-tier 동기화 아키텍처에서 Entity와 Domain 모델 간 변환을 처리합니다
 */
object RolesMapper {

    /**
     * Role 도메인 모델을 RolesEntity로 변환
     * @param role 변환할 Role 도메인 모델
     * @param projectId 역할이 속한 프로젝트 ID
     * @return RolesEntity
     */
    fun toEntity(role: Role, projectId: String): RolesEntity {
        return RolesEntity(
            id = role.id.value,
            projectId = projectId,
            name = role.name.value,
            isDefault = role.isDefault.value,
            createdAt = role.createdAt,
            updatedAt = role.updatedAt
        )
    }

    /**
     * RolesEntity를 Role 도메인 모델로 변환
     * @param entity 변환할 RolesEntity
     * @return Role 도메인 모델
     */
    fun toDomain(entity: RolesEntity): Role {
        return Role.fromDataSource(
            id = DocumentId(entity.id),
            name = Name(entity.name),
            isDefault = RoleIsDefault.fromBoolean(entity.isDefault),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Role 도메인 모델 리스트를 RolesEntity 리스트로 변환
     * @param roles 변환할 Role 도메인 모델 리스트
     * @param projectId 역할들이 속한 프로젝트 ID
     * @return RolesEntity 리스트
     */
    fun toEntityList(roles: List<Role>, projectId: String): List<RolesEntity> {
        return roles.map { toEntity(it, projectId) }
    }

    /**
     * RolesEntity 리스트를 Role 도메인 모델 리스트로 변환
     * @param entities 변환할 RolesEntity 리스트
     * @return Role 도메인 모델 리스트
     */
    fun toDomainList(entities: List<RolesEntity>): List<Role> {
        return entities.map { toDomain(it) }
    }
}
package com.example.mapper

import com.example.data_core.model.local.MembersEntity
import com.example.domain.model.base.Member
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ProjectId
import com.example.domain.model.vo.UserId
import com.example.mapper.base.BaseEntityMapper
import javax.inject.Inject

interface MemberEntityMapper : BaseEntityMapper<Member, MembersEntity>

class MemberEntityMapperImpl @Inject constructor() : MemberEntityMapper {
    override fun toDomain(entity: MembersEntity): Member {
        return Member.fromDataSource(
            id = DocumentId(entity.id),
            projectId = ProjectId(entity.projectId),
            userId = UserId(entity.userId),
            roleIds = entity.roleIds.map { DocumentId(it) },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(domain: Member): MembersEntity {
        return MembersEntity(
            id = domain.id.value,
            projectId = domain.projectId.value,
            userId = domain.userId.value,
            roleIds = domain.roleIds.map { it.value },
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}

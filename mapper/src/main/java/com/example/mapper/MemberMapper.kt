package com.example.mapper

import com.example.data.model.remote.MemberDTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Member
import com.example.domain.model.vo.DocumentId
import com.example.mapper.base.BaseMapper
import java.util.Date

interface MemberMapper : BaseMapper<Member, MemberDTO>

class MemberMapperImpl : MemberMapper {
    override fun toDomain(dto: MemberDTO): Member {
        return Member.fromDataSource(
            id = DocumentId(dto.id),
            roleIds = dto.roleIds.map { DocumentId(it) },
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun toDto(domain: Member): MemberDTO {
        return MemberDTO(
            id = domain.id.value,
            roleIds = domain.roleIds.map { it.value },
            createdAt = null,
            updatedAt = null
        )
    }

    override fun domainToMap(domain: Member): Map<String, Any?> {
        return mapOf(
            Member.KEY_ROLE_ID to domain.roleIds.map { it.value },
        )
    }

    override fun dataToMap(data: MemberDTO): Map<String, Any?> {
        return mapOf(
            Member.KEY_ROLE_ID to data.roleIds,
        )
    }

    override fun mapToDto(map: Map<String, Any?>): MemberDTO {
        return MemberDTO(
            id = map["id"] as? String ?: "",
            roleIds = map[Member.KEY_ROLE_ID] as? List<String> ?: emptyList(),
            createdAt = (map[AggregateRoot.KEY_CREATED_AT] as? Date),
            updatedAt = (map[AggregateRoot.KEY_UPDATED_AT] as? Date)
        )
    }
}

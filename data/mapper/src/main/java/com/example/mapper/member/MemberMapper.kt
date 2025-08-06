package com.example.mapper.member

import com.example.data_model.remote.MemberDTO
import com.example.domain.model.base.Member
import com.example.domain.vo.DocumentId
import com.example.mapper.DtoMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Member 관련 Domain과 DTO 간의 매핑을 담당하는 Mapper
 */
@Singleton
class MemberMapper @Inject constructor() : DtoMapper<Member, MemberDTO> {

    override fun dtoToDomain(dto: MemberDTO): Member {
        return Member.fromDataSource(
            id = DocumentId(dto.id),
            roleIds = dto.roleIds.map { DocumentId(it) },
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun domainToDto(domain: Member): MemberDTO {
        return MemberDTO(
            id = domain.id.value,
            roleIds = domain.roleIds.map { it.value },
            createdAt = null, // ServerTimestamp가 처리
            updatedAt = null  // ServerTimestamp가 처리
        )
    }
}
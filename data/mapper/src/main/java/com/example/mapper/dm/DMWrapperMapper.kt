package com.example.mapper.dm

import com.example.data_model.remote.DMWrapperDTO
import com.example.domain.model.base.DMWrapper
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.user.UserName
import com.example.mapper.DtoMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DMWrapper 관련 Domain과 DTO 간의 매핑을 담당하는 Mapper
 */
@Singleton
class DMWrapperMapper @Inject constructor() : DtoMapper<DMWrapper, DMWrapperDTO> {

    override fun dtoToDomain(dto: DMWrapperDTO): DMWrapper {
        return DMWrapper.fromDataSource(
            id = DocumentId(dto.id),
            otherUserId = UserId(dto.otherUserId),
            otherUserName = UserName(dto.otherUserName),
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun domainToDto(domain: DMWrapper): DMWrapperDTO {
        return DMWrapperDTO(
            id = domain.id.value,
            otherUserId = domain.otherUserId.value,
            otherUserName = domain.otherUserName.value,
            createdAt = null, // ServerTimestamp가 처리
            updatedAt = null  // ServerTimestamp가 처리
        )
    }
}
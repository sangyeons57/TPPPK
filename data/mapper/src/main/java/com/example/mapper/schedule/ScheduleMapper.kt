package com.example.mapper.schedule

import com.example.data_model.remote.ScheduleDTO
import com.example.domain.model.base.Schedule
import com.example.domain.vo.DocumentId
import com.example.domain.vo.OwnerId
import com.example.domain.vo.ProjectId
import com.example.domain.vo.schedule.ScheduleContent
import com.example.domain.vo.schedule.ScheduleTitle
import com.example.mapper.DtoMapper
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedule 관련 Domain과 DTO 간의 매핑을 담당하는 Mapper
 */
@Singleton
class ScheduleMapper @Inject constructor() : DtoMapper<Schedule, ScheduleDTO> {

    override fun dtoToDomain(dto: ScheduleDTO): Schedule {
        return Schedule.fromDataSource(
            id = DocumentId(dto.id),
            title = ScheduleTitle(dto.title),
            content = ScheduleContent(dto.content),
            startTime = dto.startTime?.toInstant()
                ?: throw IllegalArgumentException("startTime cannot be null"),
            endTime = dto.endTime?.toInstant()
                ?: throw IllegalArgumentException("endTime cannot be null"),
            projectId = dto.projectId?.let { ProjectId(it) },
            creatorId = OwnerId(dto.creatorId),
            status = dto.status,
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun domainToDto(domain: Schedule): ScheduleDTO {
        return ScheduleDTO(
            id = domain.id.value,
            title = domain.title.value,
            content = domain.content.value,
            startTime = Date.from(domain.startTime),
            endTime = Date.from(domain.endTime),
            projectId = domain.projectId?.value,
            creatorId = domain.creatorId.value,
            status = domain.status,
            createdAt = null, // ServerTimestamp가 처리
            updatedAt = null  // ServerTimestamp가 처리
        )
    }
}
package com.example.mapper

import com.example.data.model.remote.ScheduleDTO
import com.example.domain.model.base.Schedule
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.ProjectId
import com.example.domain.model.vo.schedule.ScheduleContent
import com.example.domain.model.vo.schedule.ScheduleTitle
import com.example.mapper.base.BaseMapper
import java.util.Date

interface ScheduleMapper : BaseMapper<Schedule, ScheduleDTO>

class ScheduleMapperImpl : ScheduleMapper {
    override fun fromDto(dto: ScheduleDTO): Schedule {
        requireNotNull(dto.startTime) { "startTime is null in ScheduleDTO with id=${dto.id}" }
        requireNotNull(dto.endTime) { "endTime is null in ScheduleDTO with id=${dto.id}" }

        return Schedule.fromDataSource(
            id = DocumentId(dto.id),
            title = ScheduleTitle(dto.title),
            content = ScheduleContent(dto.content),
            startTime = dto.startTime.toInstant(),
            endTime = dto.endTime.toInstant(),
            projectId = ProjectId(dto.projectId ?: "-1"),
            creatorId = OwnerId(dto.creatorId),
            status = dto.status,
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant(),
        )
    }

    override fun toDto(domain: Schedule): ScheduleDTO {
        return ScheduleDTO(
            id = domain.id.value,
            title = domain.title.value,
            content = domain.content.value,
            startTime = Date.from(domain.startTime),
            endTime = Date.from(domain.endTime),
            projectId = domain.projectId?.value,
            creatorId = domain.creatorId.value,
            status = domain.status,
            createdAt = null,
            updatedAt = null,
        )
    }

    override fun domainToMap(domain: Schedule): Map<String, Any?> {
        return mapOf(
            Schedule.KEY_PROJECT_ID to domain.projectId?.value,
            Schedule.KEY_CREATOR_ID to domain.creatorId.value,
            Schedule.KEY_TITLE to domain.title.value,
            Schedule.KEY_CONTENT to domain.content.value,
            Schedule.KEY_START_TIME to domain.startTime,
            Schedule.KEY_END_TIME to domain.endTime,
            Schedule.KEY_STATUS to domain.status,
        )
    }

    override fun dataToMap(data: ScheduleDTO): Map<String, Any?> {
        return mapOf(
            Schedule.KEY_PROJECT_ID to data.projectId,
            Schedule.KEY_CREATOR_ID to data.creatorId,
            Schedule.KEY_TITLE to data.title,
            Schedule.KEY_CONTENT to data.content,
            Schedule.KEY_START_TIME to data.startTime,
            Schedule.KEY_END_TIME to data.endTime,
            Schedule.KEY_STATUS to data.status,
        )
    }
}

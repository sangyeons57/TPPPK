package com.example.mapper

import com.example.data_core.model.local.SchedulesEntity
import com.example.domain.model.base.Schedule
import com.example.domain.model.enum.ScheduleStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.ProjectId
import com.example.domain.model.vo.schedule.ScheduleContent
import com.example.domain.model.vo.schedule.ScheduleTitle
import com.example.mapper.base.BaseEntityMapper
import javax.inject.Inject

interface ScheduleEntityMapper : BaseEntityMapper<Schedule, SchedulesEntity>

class ScheduleEntityMapperImpl @Inject constructor() : ScheduleEntityMapper {
    override fun toDomain(entity: SchedulesEntity): Schedule {
        return Schedule.fromDataSource(
            id = DocumentId(entity.id),
            projectId = ProjectId(entity.projectId),
            ownerId = OwnerId(entity.ownerId),
            title = ScheduleTitle(entity.title),
            content = ScheduleContent(entity.content),
            startTime = entity.startTime,
            endTime = entity.endTime,
            status = ScheduleStatus.valueOf(entity.status),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(domain: Schedule): SchedulesEntity {
        return SchedulesEntity(
            id = domain.id.value,
            projectId = domain.projectId.value,
            ownerId = domain.ownerId.value,
            title = domain.title.value,
            content = domain.content.value,
            startTime = domain.startTime,
            endTime = domain.endTime,
            status = domain.status.name,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}

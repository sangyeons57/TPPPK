package com.example.data.mapper

import com.example.data.model.local.SchedulesEntity
import com.example.domain.model.base.Schedule
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.ProjectId
import com.example.domain.model.vo.schedule.ScheduleTitle
import com.example.domain.model.vo.schedule.ScheduleContent
import com.example.domain.model.enum.ScheduleStatus

/**
 * SchedulesEntity와 Schedule 도메인 모델 간의 변환을 담당하는 매퍼
 * 3-tier 동기화 아키텍처에서 Entity와 Domain 모델 간 변환을 처리합니다
 */
object SchedulesMapper {

    /**
     * Schedule 도메인 모델을 SchedulesEntity로 변환
     * @param schedule 변환할 Schedule 도메인 모델
     * @return SchedulesEntity
     */
    fun toEntity(schedule: Schedule): SchedulesEntity {
        return SchedulesEntity(
            id = schedule.id.value,
            projectId = schedule.projectId?.value,
            creatorId = schedule.creatorId.value,
            title = schedule.title.value,
            content = schedule.content.value,
            startTime = schedule.startTime,
            endTime = schedule.endTime,
            status = schedule.status.value,
            createdAt = schedule.createdAt,
            updatedAt = schedule.updatedAt
        )
    }

    /**
     * SchedulesEntity를 Schedule 도메인 모델로 변환
     * @param entity 변환할 SchedulesEntity
     * @return Schedule 도메인 모델
     */
    fun toDomain(entity: SchedulesEntity): Schedule {
        return Schedule.fromDataSource(
            id = DocumentId(entity.id),
            projectId = entity.projectId?.let { ProjectId(it) },
            creatorId = OwnerId(entity.creatorId),
            title = ScheduleTitle(entity.title),
            content = ScheduleContent(entity.content),
            startTime = entity.startTime,
            endTime = entity.endTime,
            status = ScheduleStatus.fromString(entity.status),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Schedule 도메인 모델 리스트를 SchedulesEntity 리스트로 변환
     * @param schedules 변환할 Schedule 도메인 모델 리스트
     * @return SchedulesEntity 리스트
     */
    fun toEntityList(schedules: List<Schedule>): List<SchedulesEntity> {
        return schedules.map { toEntity(it) }
    }

    /**
     * SchedulesEntity 리스트를 Schedule 도메인 모델 리스트로 변환
     * @param entities 변환할 SchedulesEntity 리스트
     * @return Schedule 도메인 모델 리스트
     */
    fun toDomainList(entities: List<SchedulesEntity>): List<Schedule> {
        return entities.map { toDomain(it) }
    }
}
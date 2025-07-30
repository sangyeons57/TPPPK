package com.example.data_converter

import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Schedule
import com.example.domain.model.enum.ScheduleStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.ProjectId
import com.example.domain.model.vo.schedule.ScheduleContent
import com.example.domain.model.vo.schedule.ScheduleTitle
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedule Domain Model과 JSON 간의 변환을 담당하는 Converter
 */
@Singleton
class ScheduleJsonConverter @Inject constructor(
    private val gson: Gson
) : JsonConverter<Schedule> {

    override fun toJson(data: Schedule): String {
        return try {
            val scheduleData = mapOf(
                AggregateRoot.KEY_ID to data.id.value,
                Schedule.KEY_TITLE to data.title.value,
                Schedule.KEY_CONTENT to data.content.value,
                Schedule.KEY_START_TIME to data.startTime.toEpochMilli(),
                Schedule.KEY_END_TIME to data.endTime.toEpochMilli(),
                Schedule.KEY_PROJECT_ID to data.projectId?.value,
                Schedule.KEY_CREATOR_ID to data.creatorId.value,
                Schedule.KEY_STATUS to data.status.name,
                AggregateRoot.KEY_CREATED_AT to data.createdAt?.toEpochMilli(),
                AggregateRoot.KEY_UPDATED_AT to data.updatedAt?.toEpochMilli()
            )
            gson.toJson(scheduleData)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert Schedule to JSON: ${e.message}", e)
        }
    }

    override fun fromJson(json: String): Schedule {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val scheduleData: Map<String, Any?> = gson.fromJson(json, type)

            Schedule.fromDataSource(
                id = DocumentId(scheduleData[AggregateRoot.KEY_ID] as String),
                title = ScheduleTitle(scheduleData[Schedule.KEY_TITLE] as String),
                content = ScheduleContent(scheduleData[Schedule.KEY_CONTENT] as String),
                startTime = Instant.ofEpochMilli((scheduleData[Schedule.KEY_START_TIME] as Double).toLong()),
                endTime = Instant.ofEpochMilli((scheduleData[Schedule.KEY_END_TIME] as Double).toLong()),
                projectId = (scheduleData[Schedule.KEY_PROJECT_ID] as? String)?.let { ProjectId(it) },
                creatorId = OwnerId(scheduleData[Schedule.KEY_CREATOR_ID] as String),
                status = ScheduleStatus.valueOf(scheduleData[Schedule.KEY_STATUS] as String),
                createdAt = (scheduleData[AggregateRoot.KEY_CREATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                updatedAt = (scheduleData[AggregateRoot.KEY_UPDATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) }
            )
        } catch (e: JsonSyntaxException) {
            throw JsonConversionException("Invalid JSON format for Schedule: ${e.message}", e)
        } catch (e: ClassCastException) {
            throw JsonConversionException("JSON structure mismatch for Schedule: ${e.message}", e)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert JSON to Schedule: ${e.message}", e)
        }
    }
}
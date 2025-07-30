package com.example.data_converter

import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Task
import com.example.domain.model.vo.task.TaskOrder
import com.example.domain.model.vo.task.TaskStatus
import com.example.domain.model.vo.task.TaskType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.task.TaskContent
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Task Domain Model과 JSON 간의 변환을 담당하는 Converter
 */
@Singleton
class TaskJsonConverter @Inject constructor(
    private val gson: Gson
) : JsonConverter<Task> {

    override fun toJson(data: Task): String {
        return try {
            val taskData = mapOf(
                AggregateRoot.KEY_ID to data.id.value,
                Task.KEY_TASK_TYPE to data.taskType.value,
                Task.KEY_STATUS to data.status.value,
                Task.KEY_CONTENT to data.content.value,
                Task.KEY_ORDER to data.order.value,
                Task.KEY_CHECKED_BY to data.checkedBy?.value,
                Task.KEY_CHECKED_AT to data.checkedAt?.toEpochMilli(),
                AggregateRoot.KEY_CREATED_AT to data.createdAt?.toEpochMilli(),
                AggregateRoot.KEY_UPDATED_AT to data.updatedAt?.toEpochMilli()
            )
            gson.toJson(taskData)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert Task to JSON: ${e.message}", e)
        }
    }

    override fun fromJson(json: String): Task {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val taskData: Map<String, Any?> = gson.fromJson(json, type)

            Task.fromDataSource(
                id = DocumentId(taskData[AggregateRoot.KEY_ID] as String),
                taskType = TaskType.fromValue(taskData[Task.KEY_TASK_TYPE] as String),
                status = TaskStatus.fromValue(taskData[Task.KEY_STATUS] as String),
                content = TaskContent(taskData[Task.KEY_CONTENT] as String),
                order = TaskOrder((taskData[Task.KEY_ORDER] as Double).toInt()),
                checkedBy = (taskData[Task.KEY_CHECKED_BY] as? String)?.let { UserId(it) },
                checkedAt = (taskData[Task.KEY_CHECKED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                createdAt = (taskData[AggregateRoot.KEY_CREATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                updatedAt = (taskData[AggregateRoot.KEY_UPDATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) }
            )
        } catch (e: JsonSyntaxException) {
            throw JsonConversionException("Invalid JSON format for Task: ${e.message}", e)
        } catch (e: ClassCastException) {
            throw JsonConversionException("JSON structure mismatch for Task: ${e.message}", e)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert JSON to Task: ${e.message}", e)
        }
    }
}
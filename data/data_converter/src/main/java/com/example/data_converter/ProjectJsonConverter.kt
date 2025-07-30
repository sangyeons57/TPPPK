package com.example.data_converter

import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Project
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.project.ProjectStatus
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Project Domain Model과 JSON 간의 변환을 담당하는 Converter
 */
@Singleton
class ProjectJsonConverter @Inject constructor(
    private val gson: Gson
) : JsonConverter<Project> {

    override fun toJson(data: Project): String {
        return try {
            val projectData = mapOf(
                AggregateRoot.KEY_ID to data.id.value,
                Project.KEY_NAME to data.name.value,
                Project.KEY_OWNER_ID to data.ownerId.value,
                Project.KEY_STATUS to data.status.name,
                AggregateRoot.KEY_CREATED_AT to data.createdAt.toEpochMilli(),
                AggregateRoot.KEY_UPDATED_AT to data.updatedAt.toEpochMilli()
            )
            gson.toJson(projectData)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert Project to JSON: ${e.message}", e)
        }
    }

    override fun fromJson(json: String): Project {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val projectData: Map<String, Any?> = gson.fromJson(json, type)

            Project.fromDataSource(
                id = DocumentId(projectData[AggregateRoot.KEY_ID] as String),
                name = ProjectName(projectData[Project.KEY_NAME] as String),
                ownerId = OwnerId(projectData[Project.KEY_OWNER_ID] as String),
                status = ProjectStatus.valueOf(projectData[Project.KEY_STATUS] as String),
                createdAt = Instant.ofEpochMilli((projectData[AggregateRoot.KEY_CREATED_AT] as Double).toLong()),
                updatedAt = Instant.ofEpochMilli((projectData[AggregateRoot.KEY_UPDATED_AT] as Double).toLong())
            )
        } catch (e: JsonSyntaxException) {
            throw JsonConversionException("Invalid JSON format for Project: ${e.message}", e)
        } catch (e: ClassCastException) {
            throw JsonConversionException("JSON structure mismatch for Project: ${e.message}", e)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert JSON to Project: ${e.message}", e)
        }
    }
}
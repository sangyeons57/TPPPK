package com.example.data_converter

import com.example.domain.AggregateRoot
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.vo.DocumentId
import com.example.domain.vo.project.ProjectName
import com.example.domain.vo.projectwrapper.ProjectWrapperOrder
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProjectsWrapper Domain Model과 JSON 간의 변환을 담당하는 Converter
 */
@Singleton
class ProjectsWrapperJsonConverter @Inject constructor(
    private val gson: Gson
) : JsonConverter<ProjectsWrapper> {

    override fun toJson(data: ProjectsWrapper): String {
        return try {
            val wrapperData = mapOf(
                AggregateRoot.KEY_ID to data.id.value,
                ProjectsWrapper.KEY_ORDER to data.order.value,
                ProjectsWrapper.KEY_PROJECT_NAME to data.projectName.value,
                AggregateRoot.KEY_CREATED_AT to data.createdAt.toEpochMilli(),
                AggregateRoot.KEY_UPDATED_AT to data.updatedAt.toEpochMilli()
            )
            gson.toJson(wrapperData)
        } catch (e: Exception) {
            throw JsonConversionException(
                "Failed to convert ProjectsWrapper to JSON: ${e.message}",
                e
            )
        }
    }

    override fun fromJson(json: String): ProjectsWrapper {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val wrapperData: Map<String, Any?> = gson.fromJson(json, type)

            ProjectsWrapper.fromDataSource(
                id = DocumentId(wrapperData[AggregateRoot.KEY_ID] as String),
                order = ProjectWrapperOrder((wrapperData[ProjectsWrapper.KEY_ORDER] as Double).toInt()),
                projectName = ProjectName(wrapperData[ProjectsWrapper.KEY_PROJECT_NAME] as String),
                createdAt = (wrapperData[AggregateRoot.KEY_CREATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                updatedAt = (wrapperData[AggregateRoot.KEY_UPDATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) }
            )
        } catch (e: JsonSyntaxException) {
            throw JsonConversionException(
                "Invalid JSON format for ProjectsWrapper: ${e.message}",
                e
            )
        } catch (e: ClassCastException) {
            throw JsonConversionException(
                "JSON structure mismatch for ProjectsWrapper: ${e.message}",
                e
            )
        } catch (e: Exception) {
            throw JsonConversionException(
                "Failed to convert JSON to ProjectsWrapper: ${e.message}",
                e
            )
        }
    }
}
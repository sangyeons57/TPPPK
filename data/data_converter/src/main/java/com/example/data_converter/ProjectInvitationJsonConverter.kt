package com.example.data_converter

import com.example.domain.AggregateRoot
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.enum.InviteStatus
import com.example.domain.vo.DocumentId
import com.example.domain.vo.ProjectId
import com.example.domain.vo.UserId
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProjectInvitation Domain Model과 JSON 간의 변환을 담당하는 Converter
 */
@Singleton
class ProjectInvitationJsonConverter @Inject constructor(
    private val gson: Gson
) : JsonConverter<ProjectInvitation> {

    override fun toJson(data: ProjectInvitation): String {
        return try {
            val invitationData = mapOf(
                AggregateRoot.KEY_ID to data.id.value,
                ProjectInvitation.KEY_INVITER_ID to data.inviterId.value,
                ProjectInvitation.KEY_PROJECT_ID to data.projectId.value,
                ProjectInvitation.KEY_STATUS to data.status.name,
                ProjectInvitation.KEY_EXPIRES_AT to data.expiresAt?.toEpochMilli(),
                AggregateRoot.KEY_CREATED_AT to data.createdAt.toEpochMilli(),
                AggregateRoot.KEY_UPDATED_AT to data.updatedAt.toEpochMilli()
            )
            gson.toJson(invitationData)
        } catch (e: Exception) {
            throw JsonConversionException(
                "Failed to convert ProjectInvitation to JSON: ${e.message}",
                e
            )
        }
    }

    override fun fromJson(json: String): ProjectInvitation {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val invitationData: Map<String, Any?> = gson.fromJson(json, type)

            ProjectInvitation.fromDataSource(
                id = DocumentId(invitationData[AggregateRoot.KEY_ID] as String),
                inviterId = UserId(invitationData[ProjectInvitation.KEY_INVITER_ID] as String),
                projectId = ProjectId(invitationData[ProjectInvitation.KEY_PROJECT_ID] as String),
                status = InviteStatus.valueOf(invitationData[ProjectInvitation.KEY_STATUS] as String),
                expiresAt = (invitationData[ProjectInvitation.KEY_EXPIRES_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                createdAt = (invitationData[AggregateRoot.KEY_CREATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                updatedAt = (invitationData[AggregateRoot.KEY_UPDATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) }
            )
        } catch (e: JsonSyntaxException) {
            throw JsonConversionException(
                "Invalid JSON format for ProjectInvitation: ${e.message}",
                e
            )
        } catch (e: ClassCastException) {
            throw JsonConversionException(
                "JSON structure mismatch for ProjectInvitation: ${e.message}",
                e
            )
        } catch (e: Exception) {
            throw JsonConversionException(
                "Failed to convert JSON to ProjectInvitation: ${e.message}",
                e
            )
        }
    }
}
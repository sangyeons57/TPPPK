package com.example.data_converter

import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.enum.ProjectChannelStatus
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProjectChannel Domain Model과 JSON 간의 변환을 담당하는 Converter
 */
@Singleton
class ProjectChannelJsonConverter @Inject constructor(
    private val gson: Gson
) : JsonConverter<ProjectChannel> {

    override fun toJson(data: ProjectChannel): String {
        return try {
            val channelData = mapOf(
                AggregateRoot.KEY_ID to data.id.value,
                ProjectChannel.KEY_CHANNEL_NAME to data.channelName.value,
                ProjectChannel.KEY_CHANNEL_TYPE to data.channelType.name,
                ProjectChannel.KEY_ORDER to data.order.value,
                ProjectChannel.KEY_STATUS to data.status.name,
                ProjectChannel.KEY_CATEGORY_ID to data.categoryId?.value,
                AggregateRoot.KEY_CREATED_AT to data.createdAt.toEpochMilli(),
                AggregateRoot.KEY_UPDATED_AT to data.updatedAt.toEpochMilli()
            )
            gson.toJson(channelData)
        } catch (e: Exception) {
            throw JsonConversionException(
                "Failed to convert ProjectChannel to JSON: ${e.message}",
                e
            )
        }
    }

    override fun fromJson(json: String): ProjectChannel {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val channelData: Map<String, Any?> = gson.fromJson(json, type)

            ProjectChannel.fromDataSource(
                id = DocumentId(channelData[AggregateRoot.KEY_ID] as String),
                channelName = Name(channelData[ProjectChannel.KEY_CHANNEL_NAME] as String),
                channelType = ProjectChannelType.valueOf(channelData[ProjectChannel.KEY_CHANNEL_TYPE] as String),
                order = ProjectChannelOrder((channelData[ProjectChannel.KEY_ORDER] as Double).toInt()),
                status = ProjectChannelStatus.valueOf(channelData[ProjectChannel.KEY_STATUS] as String),
                categoryId = (channelData[ProjectChannel.KEY_CATEGORY_ID] as? String)
                    ?.let { DocumentId(it) }
                    ?: DocumentId(Category.NO_CATEGORY_ID),
                createdAt = (channelData[AggregateRoot.KEY_CREATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                updatedAt = (channelData[AggregateRoot.KEY_UPDATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) }
            )
        } catch (e: JsonSyntaxException) {
            throw JsonConversionException("Invalid JSON format for ProjectChannel: ${e.message}", e)
        } catch (e: ClassCastException) {
            throw JsonConversionException(
                "JSON structure mismatch for ProjectChannel: ${e.message}",
                e
            )
        } catch (e: Exception) {
            throw JsonConversionException(
                "Failed to convert JSON to ProjectChannel: ${e.message}",
                e
            )
        }
    }
}
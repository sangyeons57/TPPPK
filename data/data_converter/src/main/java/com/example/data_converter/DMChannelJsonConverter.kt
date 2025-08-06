package com.example.data_converter

import com.example.domain.AggregateRoot
import com.example.domain.model.base.DMChannel
import com.example.domain.model.enum.DMChannelStatus
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DMChannel Domain Model과 JSON 간의 변환을 담당하는 Converter
 */
@Singleton
class DMChannelJsonConverter @Inject constructor(
    private val gson: Gson
) : JsonConverter<DMChannel> {

    override fun toJson(data: DMChannel): String {
        return try {
            val channelData = mapOf(
                AggregateRoot.KEY_ID to data.id.value,
                DMChannel.KEY_PARTICIPANTS to data.participants.map { it.value },
                DMChannel.KEY_STATUS to data.status.name,
                DMChannel.KEY_BLOCKED_BY_MAP to data.blockedByMap.mapKeys { it.key.value }
                    .mapValues { it.value.value },
                AggregateRoot.KEY_CREATED_AT to data.createdAt.toEpochMilli(),
                AggregateRoot.KEY_UPDATED_AT to data.updatedAt.toEpochMilli()
            )
            gson.toJson(channelData)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert DMChannel to JSON: ${e.message}", e)
        }
    }

    override fun fromJson(json: String): DMChannel {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val channelData: Map<String, Any?> = gson.fromJson(json, type)

            @Suppress("UNCHECKED_CAST")
            val participantStrings = channelData[DMChannel.KEY_PARTICIPANTS] as List<String>
            val participants = participantStrings.map { UserId(it) }

            @Suppress("UNCHECKED_CAST")
            val blockedByMapData =
                channelData[DMChannel.KEY_BLOCKED_BY_MAP] as? Map<String, String> ?: emptyMap()
            val blockedByMap =
                blockedByMapData.mapKeys { UserId(it.key) }.mapValues { UserId(it.value) }

            DMChannel.fromDataSource(
                id = DocumentId(channelData[AggregateRoot.KEY_ID] as String),
                participants = participants,
                status = DMChannelStatus.valueOf(channelData[DMChannel.KEY_STATUS] as String),
                blockedByMap = blockedByMap,
                createdAt = (channelData[AggregateRoot.KEY_CREATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                updatedAt = (channelData[AggregateRoot.KEY_UPDATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) }
            )
        } catch (e: JsonSyntaxException) {
            throw JsonConversionException("Invalid JSON format for DMChannel: ${e.message}", e)
        } catch (e: ClassCastException) {
            throw JsonConversionException("JSON structure mismatch for DMChannel: ${e.message}", e)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert JSON to DMChannel: ${e.message}", e)
        }
    }
}
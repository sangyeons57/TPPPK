package com.example.data_converter

import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Reaction
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.reaction.Emoji
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reaction Domain Model과 JSON 간의 변환을 담당하는 Converter
 */
@Singleton
class ReactionJsonConverter @Inject constructor(
    private val gson: Gson
) : JsonConverter<Reaction> {

    override fun toJson(data: Reaction): String {
        return try {
            val reactionData = mapOf(
                AggregateRoot.KEY_ID to data.id.value,
                Reaction.KEY_USER_ID to data.userId.value,
                Reaction.KEY_EMOJI to data.emoji.value,
                AggregateRoot.KEY_CREATED_AT to data.createdAt.toEpochMilli(),
                AggregateRoot.KEY_UPDATED_AT to data.updatedAt.toEpochMilli()
            )
            gson.toJson(reactionData)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert Reaction to JSON: ${e.message}", e)
        }
    }

    override fun fromJson(json: String): Reaction {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val reactionData: Map<String, Any?> = gson.fromJson(json, type)

            Reaction.fromDataSource(
                id = DocumentId(reactionData[AggregateRoot.KEY_ID] as String),
                userId = UserId(reactionData[Reaction.KEY_USER_ID] as String),
                emoji = Emoji(reactionData[Reaction.KEY_EMOJI] as String),
                createdAt = (reactionData[AggregateRoot.KEY_CREATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                updatedAt = (reactionData[AggregateRoot.KEY_UPDATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) }
            )
        } catch (e: JsonSyntaxException) {
            throw JsonConversionException("Invalid JSON format for Reaction: ${e.message}", e)
        } catch (e: ClassCastException) {
            throw JsonConversionException("JSON structure mismatch for Reaction: ${e.message}", e)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert JSON to Reaction: ${e.message}", e)
        }
    }
}
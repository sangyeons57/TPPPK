package com.example.data_converter

import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Friend
import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.user.UserName
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Friend Domain Model과 JSON 간의 변환을 담당하는 Converter
 */
@Singleton
class FriendJsonConverter @Inject constructor(
    private val gson: Gson
) : JsonConverter<Friend> {

    override fun toJson(data: Friend): String {
        return try {
            val friendData = mapOf(
                AggregateRoot.KEY_ID to data.id.value,
                Friend.KEY_NAME to data.name.value,
                Friend.KEY_PROFILE_IMAGE_URL to data.profileImageUrl?.value,
                Friend.KEY_STATUS to data.status.name,
                Friend.KEY_REQUESTED_AT to data.requestedAt?.toEpochMilli(),
                Friend.KEY_ACCEPTED_AT to data.acceptedAt?.toEpochMilli(),
                AggregateRoot.KEY_CREATED_AT to data.createdAt.toEpochMilli(),
                AggregateRoot.KEY_UPDATED_AT to data.updatedAt.toEpochMilli()
            )
            gson.toJson(friendData)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert Friend to JSON: ${e.message}", e)
        }
    }

    override fun fromJson(json: String): Friend {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val friendData: Map<String, Any?> = gson.fromJson(json, type)

            Friend.fromDataSource(
                id = DocumentId(friendData[AggregateRoot.KEY_ID] as String),
                name = UserName(friendData[Friend.KEY_NAME] as String),
                profileImageUrl = (friendData[Friend.KEY_PROFILE_IMAGE_URL] as? String)?.let {
                    ImageUrl(
                        it
                    )
                },
                status = FriendStatus.valueOf(friendData[Friend.KEY_STATUS] as String),
                requestedAt = (friendData[Friend.KEY_REQUESTED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                acceptedAt = (friendData[Friend.KEY_ACCEPTED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                createdAt = (friendData[AggregateRoot.KEY_CREATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                updatedAt = (friendData[AggregateRoot.KEY_UPDATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) }
            )
        } catch (e: JsonSyntaxException) {
            throw JsonConversionException("Invalid JSON format for Friend: ${e.message}", e)
        } catch (e: ClassCastException) {
            throw JsonConversionException("JSON structure mismatch for Friend: ${e.message}", e)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert JSON to Friend: ${e.message}", e)
        }
    }
}
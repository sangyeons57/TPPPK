package com.example.data_converter

import com.example.domain.AggregateRoot
import com.example.domain.model.base.DMWrapper
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.user.UserName
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DMWrapper Domain Model과 JSON 간의 변환을 담당하는 Converter
 */
@Singleton
class DMWrapperJsonConverter @Inject constructor(
    private val gson: Gson
) : JsonConverter<DMWrapper> {

    override fun toJson(data: DMWrapper): String {
        return try {
            val wrapperData = mapOf(
                AggregateRoot.KEY_ID to data.id.value,
                DMWrapper.KEY_OTHER_USER_ID to data.otherUserId.value,
                DMWrapper.KEY_OTHER_USER_NAME to data.otherUserName.value,
                AggregateRoot.KEY_CREATED_AT to data.createdAt.toEpochMilli(),
                AggregateRoot.KEY_UPDATED_AT to data.updatedAt.toEpochMilli()
            )
            gson.toJson(wrapperData)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert DMWrapper to JSON: ${e.message}", e)
        }
    }

    override fun fromJson(json: String): DMWrapper {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val wrapperData: Map<String, Any?> = gson.fromJson(json, type)

            DMWrapper.fromDataSource(
                id = DocumentId(wrapperData[AggregateRoot.KEY_ID] as String),
                otherUserId = UserId(wrapperData[DMWrapper.KEY_OTHER_USER_ID] as String),
                otherUserName = UserName(wrapperData[DMWrapper.KEY_OTHER_USER_NAME] as String),
                createdAt = (wrapperData[AggregateRoot.KEY_CREATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                updatedAt = (wrapperData[AggregateRoot.KEY_UPDATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) }
            )
        } catch (e: JsonSyntaxException) {
            throw JsonConversionException("Invalid JSON format for DMWrapper: ${e.message}", e)
        } catch (e: ClassCastException) {
            throw JsonConversionException("JSON structure mismatch for DMWrapper: ${e.message}", e)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert JSON to DMWrapper: ${e.message}", e)
        }
    }
}
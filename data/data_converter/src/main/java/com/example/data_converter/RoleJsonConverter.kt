package com.example.data_converter

import com.example.domain.AggregateRoot
import com.example.domain.model.base.Role
import com.example.domain.vo.DocumentId
import com.example.domain.vo.Name
import com.example.domain.vo.role.RoleIsDefault
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Role Domain Model과 JSON 간의 변환을 담당하는 Converter
 */
@Singleton
class RoleJsonConverter @Inject constructor(
    private val gson: Gson
) : JsonConverter<Role> {

    override fun toJson(data: Role): String {
        return try {
            val roleData = mapOf(
                AggregateRoot.KEY_ID to data.id.value,
                Role.KEY_NAME to data.name.value,
                Role.KEY_IS_DEFAULT to data.isDefault.value,
                AggregateRoot.KEY_CREATED_AT to data.createdAt.toEpochMilli(),
                AggregateRoot.KEY_UPDATED_AT to data.updatedAt.toEpochMilli()
            )
            gson.toJson(roleData)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert Role to JSON: ${e.message}", e)
        }
    }

    override fun fromJson(json: String): Role {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val roleData: Map<String, Any?> = gson.fromJson(json, type)

            Role.fromDataSource(
                id = DocumentId(roleData[AggregateRoot.KEY_ID] as String),
                name = Name(roleData[Role.KEY_NAME] as String),
                isDefault = RoleIsDefault(roleData[Role.KEY_IS_DEFAULT] as Boolean),
                createdAt = (roleData[AggregateRoot.KEY_CREATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                updatedAt = (roleData[AggregateRoot.KEY_UPDATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) }
            )
        } catch (e: JsonSyntaxException) {
            throw JsonConversionException("Invalid JSON format for Role: ${e.message}", e)
        } catch (e: ClassCastException) {
            throw JsonConversionException("JSON structure mismatch for Role: ${e.message}", e)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert JSON to Role: ${e.message}", e)
        }
    }
}
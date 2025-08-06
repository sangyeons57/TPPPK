package com.example.data_converter

import com.example.domain.AggregateRoot
import com.example.domain.model.base.Permission
import com.example.domain.model.data.project.RolePermission
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Permission Domain Model과 JSON 간의 변환을 담당하는 Converter
 */
@Singleton
class PermissionJsonConverter @Inject constructor(
    private val gson: Gson
) : JsonConverter<Permission> {

    override fun toJson(data: Permission): String {
        return try {
            val permissionData = mapOf(
                AggregateRoot.KEY_ID to data.id.value, // RolePermission enum의 name
                "permission" to data.getPermissionRole().name,
                AggregateRoot.KEY_CREATED_AT to data.createdAt.toEpochMilli(),
                AggregateRoot.KEY_UPDATED_AT to data.updatedAt.toEpochMilli()
            )
            gson.toJson(permissionData)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert Permission to JSON: ${e.message}", e)
        }
    }

    override fun fromJson(json: String): Permission {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val permissionData: Map<String, Any?> = gson.fromJson(json, type)

            val permission = RolePermission.valueOf(permissionData["permission"] as String)

            Permission.fromDataSource(
                id = permission,
                createdAt = (permissionData[AggregateRoot.KEY_CREATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                updatedAt = (permissionData[AggregateRoot.KEY_UPDATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) }
            )
        } catch (e: JsonSyntaxException) {
            throw JsonConversionException("Invalid JSON format for Permission: ${e.message}", e)
        } catch (e: ClassCastException) {
            throw JsonConversionException("JSON structure mismatch for Permission: ${e.message}", e)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert JSON to Permission: ${e.message}", e)
        }
    }
}
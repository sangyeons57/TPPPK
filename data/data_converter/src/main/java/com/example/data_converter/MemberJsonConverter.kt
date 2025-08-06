package com.example.data_converter

import com.example.domain.AggregateRoot
import com.example.domain.model.base.Member
import com.example.domain.vo.DocumentId
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Member Domain Model과 JSON 간의 변환을 담당하는 Converter
 */
@Singleton
class MemberJsonConverter @Inject constructor(
    private val gson: Gson
) : JsonConverter<Member> {

    override fun toJson(data: Member): String {
        return try {
            val memberData = mapOf(
                AggregateRoot.KEY_ID to data.id.value,
                Member.KEY_ROLE_ID to data.roleIds.map { it.value },
                AggregateRoot.KEY_CREATED_AT to data.createdAt.toEpochMilli(),
                AggregateRoot.KEY_UPDATED_AT to data.updatedAt.toEpochMilli()
            )
            gson.toJson(memberData)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert Member to JSON: ${e.message}", e)
        }
    }

    override fun fromJson(json: String): Member {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val memberData: Map<String, Any?> = gson.fromJson(json, type)

            @Suppress("UNCHECKED_CAST")
            val roleIdStrings = memberData[Member.KEY_ROLE_ID] as List<String>
            val roleIds = roleIdStrings.map { DocumentId(it) }

            Member.fromDataSource(
                id = DocumentId(memberData[AggregateRoot.KEY_ID] as String),
                roleIds = roleIds,
                createdAt = (memberData[AggregateRoot.KEY_CREATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                updatedAt = (memberData[AggregateRoot.KEY_UPDATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) }
            )
        } catch (e: JsonSyntaxException) {
            throw JsonConversionException("Invalid JSON format for Member: ${e.message}", e)
        } catch (e: ClassCastException) {
            throw JsonConversionException("JSON structure mismatch for Member: ${e.message}", e)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert JSON to Member: ${e.message}", e)
        }
    }
}
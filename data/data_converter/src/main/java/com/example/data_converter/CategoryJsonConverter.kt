package com.example.data_converter

import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Category
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.model.vo.category.IsCategoryFlag
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Category Domain Model과 JSON 간의 변환을 담당하는 Converter
 */
@Singleton
class CategoryJsonConverter @Inject constructor(
    private val gson: Gson
) : JsonConverter<Category> {

    override fun toJson(data: Category): String {
        return try {
            val categoryData = mapOf(
                AggregateRoot.KEY_ID to data.id.value,
                Category.KEY_NAME to data.name.value,
                Category.KEY_ORDER to data.order.toDouble(),
                Category.KEY_CREATED_BY to data.createdBy.value,
                Category.KEY_IS_CATEGORY to data.isCategory.value,
                AggregateRoot.KEY_CREATED_AT to data.createdAt?.toEpochMilli(),
                AggregateRoot.KEY_UPDATED_AT to data.updatedAt?.toEpochMilli()
            )
            gson.toJson(categoryData)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert Category to JSON: ${e.message}", e)
        }
    }

    override fun fromJson(json: String): Category {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val categoryData: Map<String, Any?> = gson.fromJson(json, type)

            Category.fromDataSource(
                id = DocumentId(categoryData[AggregateRoot.KEY_ID] as String),
                name = CategoryName(categoryData[Category.KEY_NAME] as String),
                order = CategoryOrder.fromDouble(categoryData[Category.KEY_ORDER] as Double),
                createdBy = OwnerId(categoryData[Category.KEY_CREATED_BY] as String),
                isCategory = IsCategoryFlag(categoryData[Category.KEY_IS_CATEGORY] as Boolean),
                createdAt = (categoryData[AggregateRoot.KEY_CREATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                updatedAt = (categoryData[AggregateRoot.KEY_UPDATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) }
            )
        } catch (e: JsonSyntaxException) {
            throw JsonConversionException("Invalid JSON format for Category: ${e.message}", e)
        } catch (e: ClassCastException) {
            throw JsonConversionException("JSON structure mismatch for Category: ${e.message}", e)
        } catch (e: Exception) {
            throw JsonConversionException("Failed to convert JSON to Category: ${e.message}", e)
        }
    }
}
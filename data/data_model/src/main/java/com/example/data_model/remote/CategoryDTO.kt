package com.example.data_model.remote

import com.example.domain.DTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Category
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * 카테고리 정보를 나타내는 DTO 클래스
 */
data class CategoryDTO(
    @get:DocumentId
    override val id: String = "",
    @get:PropertyName(NAME)
    val name: String = "",
    // 순서를 소수점으로 관리하면 정수보다 유연하게 아이템 사이에 삽입할 수 있습니다.
    @get:PropertyName(ORDER)
    val order: Double = Category.NO_CATEGORY_ORDER.toDouble(),
    @get:PropertyName(CREATED_BY)
    val createdBy: String = "",
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null,
    @get:PropertyName(IS_CATEGORY) 
    val isCategory: Boolean = true
) : DTO {

    companion object {
        const val COLLECTION_NAME = Category.COLLECTION_NAME
        const val NAME = Category.KEY_NAME
        const val ORDER = Category.KEY_ORDER
        const val CREATED_BY = Category.KEY_CREATED_BY
        const val IS_CATEGORY = Category.KEY_IS_CATEGORY
    }
}


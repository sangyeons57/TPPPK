package com.example.data.model.remote

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.base.Category
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

import java.time.Instant

/**
 * 카테고리 정보를 나타내는 DTO 클래스
 */
data class CategoryDTO(
    @DocumentId val id: String = "",
    @get:PropertyName(NAME)
    val name: String = "",
    // 순서를 소수점으로 관리하면 정수보다 유연하게 아이템 사이에 삽입할 수 있습니다.
    @get:PropertyName(ORDER)
    val order: Double = 0.0,
    @get:PropertyName(CREATED_BY)
    val createdBy: String = "",
    @get:PropertyName(CREATED_AT)
    @ServerTimestamp val createdAt: Timestamp? = null,
    @get:PropertyName(UPDATED_AT)
    @ServerTimestamp val updatedAt: Timestamp? = null,
    @get:PropertyName(IS_CATEGORY) 
    val isCategory: Boolean = true
) {

    companion object {
        const val COLLECTION_NAME = "categories"
        const val NAME = "name"
        const val ORDER = "order"
        const val CREATED_BY = "createdBy"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val IS_CATEGORY = "isCategory"
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return Category 도메인 모델
     */
    fun toDomain(): Category {
        return Category(
            id = id,
            name = name,
            order = order,
            createdBy = createdBy,
            createdAt = createdAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            updatedAt = updatedAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            isCategory= isCategory
        )
    }
}

/**
 * Category 도메인 모델을 DTO로 변환하는 확장 함수
 * @return CategoryDTO 객체
 */
fun Category.toDto(): CategoryDTO {
    return CategoryDTO(
        id = id,
        name = name,
        order = order,
        createdBy = createdBy,
        createdAt = createdAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        isCategory = isCategory
    )
}

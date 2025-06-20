package com.example.data.model.remote

import com.example.core_common.util.DateTimeUtil
import com.example.data.model.DTO
import com.example.domain.model.base.Category
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.model.vo.category.IsCategoryFlag
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

import java.time.Instant

/**
 * 카테고리 정보를 나타내는 DTO 클래스
 */
data class CategoryDTO(
    @com.google.firebase.firestore.DocumentId
    val id: String = "",
    @get:PropertyName(NAME)
    val name: String = "",
    // 순서를 소수점으로 관리하면 정수보다 유연하게 아이템 사이에 삽입할 수 있습니다.
    @get:PropertyName(ORDER)
    val order: Double = 0.0,
    @get:PropertyName(CREATED_BY)
    val createdBy: String = "",
    @get:PropertyName(CREATED_AT)
    @ServerTimestamp val createdAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(UPDATED_AT)
    @ServerTimestamp val updatedAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(IS_CATEGORY) 
    val isCategory: Boolean = true
) : DTO {

    companion object {
        const val COLLECTION_NAME = Category.COLLECTION_NAME
        const val NAME = Category.KEY_NAME
        const val ORDER = Category.KEY_ORDER
        const val CREATED_BY = Category.KEY_CREATED_BY
        const val CREATED_AT = Category.KEY_CREATED_AT
        const val UPDATED_AT = Category.KEY_UPDATED_AT
        const val IS_CATEGORY = Category.KEY_IS_CATEGORY
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return Category 도메인 모델
     */
    override fun toDomain(): Category {
        return Category.fromDataSource(
            id = DocumentId(id),
            name = CategoryName(name),
            order = CategoryOrder(order),
            createdBy = OwnerId(createdBy),
            createdAt = DateTimeUtil.firebaseTimestampToInstant(createdAt),
            updatedAt = DateTimeUtil.firebaseTimestampToInstant(updatedAt),
            isCategory= IsCategoryFlag(isCategory)
        )
    }
}

/**
 * Category 도메인 모델을 DTO로 변환하는 확장 함수
 * @return CategoryDTO 객체
 */
fun Category.toDto(): CategoryDTO {
    return CategoryDTO(
        id = id.value,
        name = name.value,
        order = order.value,
        createdBy = createdBy.value,
        createdAt = createdAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        isCategory = isCategory.value
    )
}

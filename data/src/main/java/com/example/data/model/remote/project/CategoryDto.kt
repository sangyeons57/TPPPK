package com.example.data.model.remote.project

import com.example.core_common.constants.FirestoreConstants
import com.example.domain.model.Category
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.time.Instant

/**
 * 프로젝트 카테고리 정보를 표현하는 데이터 전송 객체(DTO)
 * Firebase Firestore의 'projects/{projectId}/categories' 서브컬렉션과 매핑됩니다.
 */
data class CategoryDto(
    /**
     * 카테고리 ID (Firestore 문서 ID)
     */
    @DocumentId
    val categoryId: String = "",
    
    /**
     * 카테고리 이름
     */
    @PropertyName(FirestoreConstants.CategoryFields.NAME)
    val name: String = "",
    
    /**
     * 카테고리 표시 순서
     */
    @PropertyName(FirestoreConstants.CategoryFields.ORDER)
    val order: Int = 0,
    
    /**
     * 카테고리 생성 시간
     */
    @PropertyName(FirestoreConstants.CategoryFields.CREATED_AT)
    val createdAt: Timestamp? = null,
    
    /**
     * 카테고리 생성자 ID
     */
    @PropertyName(FirestoreConstants.CategoryFields.CREATED_BY)
    val createdBy: String? = null,

    /**
     * 카테고리 마지막 수정 시간
     */
    @PropertyName(FirestoreConstants.CategoryFields.UPDATED_AT)
    val updatedAt: Timestamp? = null,

    /**
     * 카테고리 마지막 수정자 ID
     */
    @PropertyName(FirestoreConstants.CategoryFields.UPDATED_BY)
    val updatedBy: String? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            FirestoreConstants.CategoryFields.NAME to name,
            FirestoreConstants.CategoryFields.ORDER to order,
            FirestoreConstants.CategoryFields.CREATED_AT to createdAt,
            FirestoreConstants.CategoryFields.CREATED_BY to createdBy,
            FirestoreConstants.CategoryFields.UPDATED_AT to updatedAt,
            FirestoreConstants.CategoryFields.UPDATED_BY to updatedBy
        ).filterValues { it != null }
    }

    /**
     * CategoryDto를 기본적인 Category 도메인 모델로 변환합니다.
     * projectId는 이 DTO에 없으므로, 호출하는 쪽에서 제공해야 합니다.
     * 시간 관련 필드는 Epoch으로 초기화되며, Mapper에서 실제 값으로 변환합니다.
     */
    fun toBasicDomainModel(projectId: String): Category {
        return Category(
            id = this.categoryId,
            projectId = projectId,
            name = this.name,
            order = this.order,
            channels = emptyList(),
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
            createdBy = this.createdBy,
            updatedBy = this.updatedBy
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>, documentId: String): CategoryDto {
            return CategoryDto(
                categoryId = documentId,
                name = map[FirestoreConstants.CategoryFields.NAME] as? String ?: "",
                order = (map[FirestoreConstants.CategoryFields.ORDER] as? Number)?.toInt() ?: 0,
                createdAt = map[FirestoreConstants.CategoryFields.CREATED_AT] as? Timestamp,
                createdBy = map[FirestoreConstants.CategoryFields.CREATED_BY] as? String,
                updatedAt = map[FirestoreConstants.CategoryFields.UPDATED_AT] as? Timestamp,
                updatedBy = map[FirestoreConstants.CategoryFields.UPDATED_BY] as? String
            )
        }

        /**
         * 기본적인 Category 도메인 모델로부터 CategoryDto 객체를 생성합니다.
         * projectId는 DTO에 포함되지 않습니다.
         * 시간 관련 필드는 null로 초기화되며, Mapper에서 실제 Timestamp로 변환합니다.
         */
        fun fromBasicDomainModel(domain: Category): CategoryDto {
            return CategoryDto(
                categoryId = domain.id,
                name = domain.name,
                order = domain.order,
                createdAt = null,
                createdBy = domain.createdBy,
                updatedAt = null,
                updatedBy = domain.updatedBy
            )
        }
    }
} 
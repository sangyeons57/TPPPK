package com.example.data.model.local

import androidx.room.Entity
import androidx.room.Index
import com.example.domain.model.Category

/**
 * 카테고리 데이터를 로컬 데이터베이스에 저장하기 위한 엔티티
 */
@Entity(
    tableName = "categories",
    primaryKeys = ["id"],
    indices = [
        Index(value = ["projectId"])
    ]
)
data class CategoryEntity(
    /**
     * 카테고리 ID
     */
    val id: String,
    
    /**
     * 프로젝트 ID
     */
    val projectId: String,
    
    /**
     * 카테고리 이름
     */
    val name: String,
    
    /**
     * 카테고리 순서
     */
    val order: Int,
    
    /**
     * 로컬 캐시 저장 시간
     */
    val cachedAt: Long = System.currentTimeMillis()
) {
    /**
     * CategoryEntity를 도메인 모델 Category로 변환
     * 
     * @return Category 도메인 모델
     */
    fun toDomain(): Category {
        return Category(
            id = id,
            projectId = projectId,
            name = name,
            order = order
        )
    }

    companion object {
        /**
         * 도메인 모델 Category를 CategoryEntity로 변환
         * 
         * @param category 변환할 Category 객체
         * @return CategoryEntity
         */
        fun fromDomain(category: Category): CategoryEntity {
            return CategoryEntity(
                id = category.id,
                projectId = category.projectId,
                name = category.name,
                order = category.order
            )
        }
    }
} 
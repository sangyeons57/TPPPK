package com.example.data.model.mapper

import com.example.core_common.util.DateTimeUtil
import com.example.data.model.local.CategoryEntity // Keep for Room mapping
import com.example.data.model.remote.project.CategoryDto
import com.example.domain.model.Category
import com.google.firebase.firestore.DocumentSnapshot // Added for potential future use
import java.time.Instant
import javax.inject.Inject

/**
 * CategoryDto, CategoryEntity, Category 도메인 모델 간의 변환을 담당합니다.
 * DateTimeUtil을 주입받아 Firestore Timestamp 관련 변환을 처리합니다.
 */
class CategoryMapper @Inject constructor(
    private val dateTimeUtil: DateTimeUtil
) {

    /**
     * Firestore DocumentSnapshot을 Category 도메인 모델로 변환합니다.
     * projectId는 상위 경로에서 가져와야 하므로 매개변수로 전달받습니다.
     */
    fun mapToDomain(document: DocumentSnapshot, projectId: String): Category? {
        return try {
            val dto = document.toObject(CategoryDto::class.java)
            // categoryId는 dto.categoryId에 자동으로 채워짐 (@DocumentId)
            dto?.toDomainModelWithTime(projectId, dateTimeUtil)
        } catch (e: Exception) {
            // Log error
            null
        }
    }

    /**
     * CategoryDto (Firestore 데이터)와 projectId를 사용하여 Category 도메인 모델로 변환합니다.
     */
    fun mapToDomain(dto: CategoryDto, projectId: String): Category {
        return dto.toDomainModelWithTime(projectId, dateTimeUtil)
    }

    /**
     * Category 도메인 모델을 Firestore에 저장하기 위한 CategoryDto로 변환합니다.
     * CategoryDto는 projectId를 포함하지 않습니다.
     */
    fun mapToDto(domain: Category): CategoryDto {
        return domain.toDtoWithTime(dateTimeUtil)
    }

    // --- Room Entity <-> Domain Model Mappers --- //

    /**
     * CategoryEntity (Room)를 Category 도메인 모델로 변환합니다.
     * CategoryEntity는 createdAt/updatedAt 등의 타임스탬프 필드가 없다고 가정합니다.
     * 만약 있다면, 여기서 Instant로 변환해야 합니다.
     */
    fun toDomain(entity: CategoryEntity): Category {
        return Category(
            id = entity.id,
            projectId = entity.projectId,
            name = entity.name,
            order = entity.order,
            channels = emptyList(), // Default
            // Assuming CategoryEntity does not have timestamp fields that map to domain's createdAt/updatedAt
            // If it did, they would be mapped here, e.g.:
            // createdAt = entity.createdAtMillis?.let { Instant.ofEpochMilli(it) } ?: Instant.EPOCH,
            // updatedAt = entity.updatedAtMillis?.let { Instant.ofEpochMilli(it) } ?: Instant.EPOCH,
            // createdBy = entity.createdBy, (if exists in entity)
            // updatedBy = entity.updatedBy  (if exists in entity)
            // For now, use defaults or nulls as per current domain model definition for these fields
            createdAt = null, // Or Instant.EPOCH if non-null is required by domain and not in entity
            updatedAt = null, // Or Instant.EPOCH
            createdBy = null, // If not in entity
            updatedBy = null  // If not in entity
        )
    }

    /**
     * Category 도메인 모델을 CategoryEntity (Room)로 변환합니다.
     * Category 도메인 모델의 channels 목록은 무시합니다.
     * 타임스탬프 필드 (createdAt, updatedAt)도 현재 CategoryEntity에 없다고 가정하고 저장하지 않습니다.
     */
    fun toEntity(domain: Category): CategoryEntity {
        return CategoryEntity(
            id = domain.id,
            projectId = domain.projectId,
            name = domain.name,
            order = domain.order
            // Assuming CategoryEntity does not store these timestamp/user fields:
            // createdAtMillis = domain.createdAt?.toEpochMilli(),
            // updatedAtMillis = domain.updatedAt?.toEpochMilli(),
            // createdBy = domain.createdBy,
            // updatedBy = domain.updatedBy
        )
    }
}

/**
 * CategoryDto를 Category 도메인 모델로 변환합니다.
 * projectId는 DTO에 없으므로 매개변수로 받아야 합니다.
 * DateTimeUtil을 사용하여 Timestamp 필드를 Instant로 변환합니다.
 */
fun CategoryDto.toDomainModelWithTime(projectId: String, dateTimeUtil: DateTimeUtil): Category {
    val basicDomain = this.toBasicDomainModel(projectId)
    return basicDomain.copy(
        createdAt = this.createdAt?.let { dateTimeUtil.firebaseTimestampToInstant(it) } ?: Instant.EPOCH,
        updatedAt = this.updatedAt?.let { dateTimeUtil.firebaseTimestampToInstant(it) } ?: Instant.EPOCH
        // createdBy and updatedBy are already set by toBasicDomainModel from DTO
    )
}

/**
 * Category 도메인 모델을 CategoryDto로 변환합니다.
 * DateTimeUtil을 사용하여 Instant 필드를 Timestamp로 변환합니다.
 * projectId는 CategoryDto에 포함되지 않습니다.
 */
fun Category.toDtoWithTime(dateTimeUtil: DateTimeUtil): CategoryDto {
    val basicDto = CategoryDto.fromBasicDomainModel(this)
    return basicDto.copy(
        createdAt = this.createdAt?.let { dateTimeUtil.instantToFirebaseTimestamp(it) }, // Handle nullable Instant
        updatedAt = this.updatedAt?.let { dateTimeUtil.instantToFirebaseTimestamp(it) }  // Handle nullable Instant
        // createdBy and updatedBy are already set by fromBasicDomainModel from domain
    )
} 
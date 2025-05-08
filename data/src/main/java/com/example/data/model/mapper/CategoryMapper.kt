package com.example.data.model.mapper

import com.example.data.model.remote.project.CategoryDto
import com.example.domain.model.Category
import com.google.firebase.Timestamp
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CategoryDto와 Category 도메인 모델 간의 변환을 담당하는 매퍼 클래스
 */
@Singleton
class CategoryMapper @Inject constructor() {

    /**
     * CategoryDto를 Category 도메인 모델로 변환합니다.
     *
     * @param dto 변환할 CategoryDto 객체
     * @return 변환된 Category 도메인 모델
     */
    fun mapToDomain(dto: CategoryDto): Category {
        return Category(
            id = dto.categoryId,
            projectId = "", // projectId는 상위 경로에서 정해져 있으므로 사용 시 설정 필요
            name = dto.name,
            order = dto.order
        )
    }

    /**
     * CategoryDto를 Category 도메인 모델로 변환합니다. projectId를 함께 전달받습니다.
     *
     * @param dto 변환할 CategoryDto 객체
     * @param projectId 카테고리가 속한 프로젝트 ID
     * @return 변환된 Category 도메인 모델
     */
    fun mapToDomain(dto: CategoryDto, projectId: String): Category {
        return Category(
            id = dto.categoryId,
            projectId = projectId,
            name = dto.name,
            order = dto.order
        )
    }

    /**
     * Category 도메인 모델을 CategoryDto로 변환합니다.
     *
     * @param domainModel 변환할 Category 도메인 모델
     * @param userId 현재 로그인한 사용자 ID (새 카테고리 생성 시 createdBy 필드에 사용)
     * @return 변환된 CategoryDto 객체
     */
    fun mapToDto(domainModel: Category, userId: String): CategoryDto {
        return CategoryDto(
            categoryId = domainModel.id,
            name = domainModel.name,
            order = domainModel.order,
            createdAt = Timestamp.now(),
            createdBy = userId
        )
    }

    /**
     * CategoryDto 목록을 Category 도메인 모델 목록으로 변환합니다.
     *
     * @param dtoList 변환할 CategoryDto 목록
     * @param projectId 카테고리가 속한 프로젝트 ID
     * @return 변환된 Category 도메인 모델 목록
     */
    fun mapToDomainList(dtoList: List<CategoryDto>, projectId: String): List<Category> {
        return dtoList.map { mapToDomain(it, projectId) }
    }
} 
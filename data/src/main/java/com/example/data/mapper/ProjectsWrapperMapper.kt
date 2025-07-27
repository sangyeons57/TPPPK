package com.example.data.mapper

import com.example.data.model.local.ProjectsWrapperEntity
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.projectwrapper.ProjectWrapperOrder

/**
 * ProjectsWrapperEntity와 ProjectsWrapper 도메인 모델 간의 변환을 담당하는 매퍼
 * 3-tier 동기화 아키텍처에서 Entity와 Domain 모델 간 변환을 처리합니다
 */
object ProjectsWrapperMapper {

    /**
     * ProjectsWrapper 도메인 모델을 ProjectsWrapperEntity로 변환
     * @param projectsWrapper 변환할 ProjectsWrapper 도메인 모델
     * @param userId 래퍼가 속한 사용자 ID
     * @return ProjectsWrapperEntity
     */
    fun toEntity(projectsWrapper: ProjectsWrapper, userId: String): ProjectsWrapperEntity {
        return ProjectsWrapperEntity(
            id = projectsWrapper.id.value,
            userId = userId,
            order = projectsWrapper.order.value,
            projectName = projectsWrapper.projectName.value,
            projectImageUrl = projectsWrapper.projectImageUrl?.value,
            createdAt = projectsWrapper.createdAt,
            updatedAt = projectsWrapper.updatedAt
        )
    }

    /**
     * ProjectsWrapperEntity를 ProjectsWrapper 도메인 모델로 변환
     * @param entity 변환할 ProjectsWrapperEntity
     * @return ProjectsWrapper 도메인 모델
     */
    fun toDomain(entity: ProjectsWrapperEntity): ProjectsWrapper {
        return ProjectsWrapper.fromDataSource(
            id = DocumentId(entity.id),
            order = ProjectWrapperOrder(entity.order),
            projectName = ProjectName(entity.projectName),
            projectImageUrl = entity.projectImageUrl?.let { ImageUrl(it) },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * ProjectsWrapper 도메인 모델 리스트를 ProjectsWrapperEntity 리스트로 변환
     * @param projectsWrappers 변환할 ProjectsWrapper 도메인 모델 리스트
     * @param userId 래퍼들이 속한 사용자 ID
     * @return ProjectsWrapperEntity 리스트
     */
    fun toEntityList(
        projectsWrappers: List<ProjectsWrapper>,
        userId: String
    ): List<ProjectsWrapperEntity> {
        return projectsWrappers.map { toEntity(it, userId) }
    }

    /**
     * ProjectsWrapperEntity 리스트를 ProjectsWrapper 도메인 모델 리스트로 변환
     * @param entities 변환할 ProjectsWrapperEntity 리스트
     * @return ProjectsWrapper 도메인 모델 리스트
     */
    fun toDomainList(entities: List<ProjectsWrapperEntity>): List<ProjectsWrapper> {
        return entities.map { toDomain(it) }
    }
}
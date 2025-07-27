package com.example.data.mapper

import com.example.data.model.local.ProjectsEntity
import com.example.domain.model.base.Project
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.project.ProjectStatus

/**
 * ProjectsEntity와 Project 도메인 모델 간의 변환을 담당하는 매퍼
 * 3-tier 동기화 아키텍처에서 Entity와 Domain 모델 간 변환을 처리합니다
 */
object ProjectsMapper {

    /**
     * Project 도메인 모델을 ProjectsEntity로 변환
     * @param project 변환할 Project 도메인 모델
     * @return ProjectsEntity
     */
    fun toEntity(project: Project): ProjectsEntity {
        return ProjectsEntity(
            id = project.id.value,
            name = project.name.value,
            imageUrl = project.imageUrl?.value,
            status = project.status.value,
            ownerId = project.ownerId.value,
            createdAt = project.createdAt,
            updatedAt = project.updatedAt
        )
    }

    /**
     * ProjectsEntity를 Project 도메인 모델로 변환
     * @param entity 변환할 ProjectsEntity
     * @return Project 도메인 모델
     */
    fun toDomain(entity: ProjectsEntity): Project {
        return Project.fromDataSource(
            id = DocumentId(entity.id),
            name = ProjectName(entity.name),
            imageUrl = entity.imageUrl?.let { ImageUrl(it) },
            ownerId = OwnerId(entity.ownerId),
            status = ProjectStatus.fromString(entity.status),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Project 도메인 모델 리스트를 ProjectsEntity 리스트로 변환
     * @param projects 변환할 Project 도메인 모델 리스트
     * @return ProjectsEntity 리스트
     */
    fun toEntityList(projects: List<Project>): List<ProjectsEntity> {
        return projects.map { toEntity(it) }
    }

    /**
     * ProjectsEntity 리스트를 Project 도메인 모델 리스트로 변환
     * @param entities 변환할 ProjectsEntity 리스트
     * @return Project 도메인 모델 리스트
     */
    fun toDomainList(entities: List<ProjectsEntity>): List<Project> {
        return entities.map { toDomain(it) }
    }
}
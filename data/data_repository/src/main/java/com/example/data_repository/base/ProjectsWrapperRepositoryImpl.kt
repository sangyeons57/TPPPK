package com.example.data_repository.base

import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.ProjectsWrapperRemoteDataSource
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.vo.DocumentId
import com.example.domain_repository.base.ProjectsWrapperRepository
import com.example.mapper.project.ProjectsWrapperMapper
import javax.inject.Inject

class ProjectsWrapperRepositoryImpl @Inject constructor(
    private val projectsWrapperRemoteDataSource: ProjectsWrapperRemoteDataSource,
    private val projectsWrapperMapper: ProjectsWrapperMapper,
) : DefaultRepositoryImpl(projectsWrapperRemoteDataSource), ProjectsWrapperRepository {
    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is ProjectsWrapper)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type ProjectsWrapper"))
        ensureCollection()

        return if (entity.isNew) {
            projectsWrapperRemoteDataSource.create(projectsWrapperMapper.domainToDto(entity))
        } else {
            projectsWrapperRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }

}

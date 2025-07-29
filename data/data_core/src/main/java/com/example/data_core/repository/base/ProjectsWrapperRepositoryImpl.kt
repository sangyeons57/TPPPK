package com.example.data_core.repository.base

import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.remote.ProjectsWrapperRemoteDataSource
import com.example.data_core.repository.DefaultRepositoryImpl
import com.example.data_model.remote.toDto
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.ProjectsWrapperRepository
import com.example.domain.repository.factory.context.ProjectsWrapperRepositoryFactoryContext
import javax.inject.Inject

class ProjectsWrapperRepositoryImpl @Inject constructor(
    private val projectsWrapperRemoteDataSource: ProjectsWrapperRemoteDataSource,
    override val factoryContext: ProjectsWrapperRepositoryFactoryContext
) : DefaultRepositoryImpl(projectsWrapperRemoteDataSource, factoryContext), ProjectsWrapperRepository {
    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is ProjectsWrapper)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type ProjectsWrapper"))
        ensureCollection()

        return if (entity.isNew) {
            projectsWrapperRemoteDataSource.create(entity.toDto())
        } else {
            projectsWrapperRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }

}

package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.ProjectsWrapperRemoteDataSource
import com.example.data.model.DTO
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.repository.base.ProjectsWrapperRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.example.data.model.remote.ProjectsWrapperDTO // Added for DTO mapping
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.factory.context.ProjectsWrapperRepositoryFactoryContext
import com.google.firebase.firestore.Source

class ProjectsWrapperRepositoryImpl @Inject constructor(
    private val projectsWrapperRemoteDataSource: ProjectsWrapperRemoteDataSource,
    override val factoryContext: ProjectsWrapperRepositoryFactoryContext
) : DefaultRepositoryImpl(projectsWrapperRemoteDataSource, factoryContext.collectionPath), ProjectsWrapperRepository {

    override fun observeAll(): Flow<CustomResult<List<ProjectsWrapper>, Exception>> {
        return projectsWrapperRemoteDataSource.observeAll().map { result : CustomResult<List<DTO>, Exception> ->
            when (result) {
                is CustomResult.Success -> CustomResult.Success(result.data.map { (it as ProjectsWrapperDTO).toDomain() })
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is ProjectsWrapper)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type ProjectsWrapper"))

        return if (entity.isNew) {
            projectsWrapperRemoteDataSource.create(entity.toDto())
        } else {
            projectsWrapperRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }

}

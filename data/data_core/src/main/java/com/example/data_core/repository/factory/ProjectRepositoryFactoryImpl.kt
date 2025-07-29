package com.example.data_core.repository.factory

import com.example.data_core.datasource.remote.ProjectRemoteDataSource
import com.example.data_core.datasource.remote.special.FunctionsRemoteDataSource
import com.example.data_core.repository.base.ProjectRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.ProjectRepository
import com.example.domain.repository.factory.context.ProjectRepositoryFactoryContext
import javax.inject.Inject

class ProjectRepositoryFactoryImpl @Inject constructor(
    private val projectRemoteDataSource: ProjectRemoteDataSource,
    private val functionsRemoteDataSource: FunctionsRemoteDataSource,
) : RepositoryFactory<ProjectRepositoryFactoryContext, ProjectRepository> {

    override fun create(input: ProjectRepositoryFactoryContext): ProjectRepository {
        return ProjectRepositoryImpl(
            projectRemoteDataSource = projectRemoteDataSource,
            functionsRemoteDataSource = functionsRemoteDataSource,
            factoryContext = input,
        )
    }
}

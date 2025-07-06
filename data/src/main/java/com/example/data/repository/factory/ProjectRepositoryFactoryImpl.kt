package com.example.data.repository.factory

import com.example.data.datasource.remote.CategoryRemoteDataSource
import com.example.data.datasource.remote.MemberRemoteDataSource
import com.example.data.datasource.remote.ProjectRemoteDataSource
import com.example.data.datasource.remote.special.FunctionsRemoteDataSource
import com.example.data.repository.base.ProjectRepositoryImpl
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

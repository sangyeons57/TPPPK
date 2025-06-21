package com.example.data.repository.factory

import com.example.data.datasource.remote.ProjectsWrapperRemoteDataSource
import com.example.data.repository.base.ProjectsWrapperRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.ProjectsWrapperRepository
import com.example.domain.repository.factory.context.ProjectsWrapperRepositoryFactoryContext
import javax.inject.Inject

class ProjectsWrapperRepositoryFactoryImpl @Inject constructor(
    private val projectsWrapperRemoteDataSource: ProjectsWrapperRemoteDataSource
) : RepositoryFactory<ProjectsWrapperRepositoryFactoryContext, ProjectsWrapperRepository> {

    override fun create(input: ProjectsWrapperRepositoryFactoryContext): ProjectsWrapperRepository {
        return ProjectsWrapperRepositoryImpl(
            projectsWrapperRemoteDataSource = projectsWrapperRemoteDataSource
        )
    }
}

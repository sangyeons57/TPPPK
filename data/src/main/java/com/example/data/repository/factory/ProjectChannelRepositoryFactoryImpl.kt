package com.example.data.repository.factory

import com.example.data.datasource.remote.ProjectChannelRemoteDataSource
import com.example.data.repository.base.ProjectChannelRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.ProjectChannelRepository
import com.example.domain.repository.factory.context.ProjectChannelRepositoryFactoryContext
import javax.inject.Inject

class ProjectChannelRepositoryFactoryImpl @Inject constructor(
    private val projectChannelRemoteDataSource: ProjectChannelRemoteDataSource
) : RepositoryFactory<ProjectChannelRepositoryFactoryContext, ProjectChannelRepository> {

    override fun create(input: ProjectChannelRepositoryFactoryContext): ProjectChannelRepository {
        return ProjectChannelRepositoryImpl(
            projectChannelRemoteDataSource = projectChannelRemoteDataSource,
            factoryContext = input,
        )
    }
}

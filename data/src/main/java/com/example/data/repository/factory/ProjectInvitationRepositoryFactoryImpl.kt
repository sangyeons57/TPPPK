package com.example.data.repository.factory

import com.example.data.datasource.remote.ProjectInvitationRemoteDataSource
import com.example.data.repository.base.ProjectInvitationRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.ProjectInvitationRepository
import com.example.domain.repository.factory.context.ProjectInvitationRepositoryFactoryContext
import javax.inject.Inject

class ProjectInvitationRepositoryFactoryImpl @Inject constructor(
    private val projectInvitationRemoteDataSource: ProjectInvitationRemoteDataSource,
) : RepositoryFactory<ProjectInvitationRepositoryFactoryContext, ProjectInvitationRepository> {

    override fun create(input: ProjectInvitationRepositoryFactoryContext): ProjectInvitationRepository {
        return ProjectInvitationRepositoryImpl(
            projectInvitationRemoteDataSource = projectInvitationRemoteDataSource,
            factoryContext = input,
        )
    }
} 
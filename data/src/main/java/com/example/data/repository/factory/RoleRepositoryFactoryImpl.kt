package com.example.data.repository.factory

import com.example.data.datasource.remote.PermissionRemoteDataSource
import com.example.data.datasource.remote.RoleRemoteDataSource
import com.example.data.repository.base.ProjectRoleRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.ProjectRoleRepository
import com.example.domain.repository.factory.context.ProjectRoleRepositoryFactoryContext
import javax.inject.Inject

class RoleRepositoryFactoryImpl @Inject constructor(
    private val roleRemoteDataSource: RoleRemoteDataSource,
    private val permissionRemoteDataSource: PermissionRemoteDataSource
) : RepositoryFactory<ProjectRoleRepositoryFactoryContext, ProjectRoleRepository> {

    override fun create(input: ProjectRoleRepositoryFactoryContext): ProjectRoleRepository {
        return ProjectRoleRepositoryImpl(
            roleRemoteDataSource = roleRemoteDataSource,
            factoryContext = input,
        )
    }
}

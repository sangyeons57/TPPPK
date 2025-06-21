package com.example.data.repository.factory

import com.example.data.datasource.remote.PermissionRemoteDataSource
import com.example.data.datasource.remote.RoleRemoteDataSource
import com.example.data.repository.base.RoleRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.RoleRepository
import com.example.domain.repository.factory.context.RoleRepositoryFactoryContext
import javax.inject.Inject

class RoleRepositoryFactoryImpl @Inject constructor(
    private val roleRemoteDataSource: RoleRemoteDataSource,
    private val permissionRemoteDataSource: PermissionRemoteDataSource
) : RepositoryFactory<RoleRepositoryFactoryContext, RoleRepository> {

    override fun create(input: RoleRepositoryFactoryContext): RoleRepository {
        return RoleRepositoryImpl(
            roleRemoteDataSource = roleRemoteDataSource,
            permissionRemoteDataSource = permissionRemoteDataSource
        )
    }
}

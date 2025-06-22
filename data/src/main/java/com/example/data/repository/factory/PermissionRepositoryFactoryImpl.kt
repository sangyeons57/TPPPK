package com.example.data.repository.factory

import com.example.data.datasource.remote.PermissionRemoteDataSource
import com.example.data.repository.base.PermissionRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.PermissionRepository
import com.example.domain.repository.factory.context.PermissionRepositoryFactoryContext
import javax.inject.Inject

class PermissionRepositoryFactoryImpl @Inject constructor(
    private val permissionRemoteDataSource: PermissionRemoteDataSource,
) : RepositoryFactory<PermissionRepositoryFactoryContext, PermissionRepository> {

    override fun create(input: PermissionRepositoryFactoryContext): PermissionRepository {
        return PermissionRepositoryImpl(
            permissionRemoteDataSource = permissionRemoteDataSource,
            factoryContext = input,
        )
    }
}

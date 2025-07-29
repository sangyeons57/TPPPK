package com.example.domain.repository.base

import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.PermissionRepositoryFactoryContext

interface PermissionRepository : DefaultRepository {
    override val factoryContext: PermissionRepositoryFactoryContext
}

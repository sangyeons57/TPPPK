package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Permission
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.PermissionRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow

interface PermissionRepository : DefaultRepository {
    override val factoryContext: PermissionRepositoryFactoryContext
}

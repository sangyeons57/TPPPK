package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.ProjectsWrapperRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow

interface ProjectsWrapperRepository : DefaultRepository {
    override val factoryContext: ProjectsWrapperRepositoryFactoryContext
}

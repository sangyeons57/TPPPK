package com.example.domain.repository.base

import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.ProjectsWrapperRepositoryFactoryContext

interface ProjectsWrapperRepository : DefaultRepository {
    override val factoryContext: ProjectsWrapperRepositoryFactoryContext
}

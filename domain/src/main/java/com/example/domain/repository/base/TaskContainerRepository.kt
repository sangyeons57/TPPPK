package com.example.domain.repository.base

import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.TaskContainerRepositoryFactoryContext

interface TaskContainerRepository : DefaultRepository {
    override val factoryContext: TaskContainerRepositoryFactoryContext
}
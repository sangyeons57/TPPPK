package com.example.domain.repository.base

import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.TaskRepositoryFactoryContext

interface TaskRepository : DefaultRepository {
    override val factoryContext: TaskRepositoryFactoryContext
}
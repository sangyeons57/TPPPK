package com.example.domain.repository.base

import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.ProjectChannelRepositoryFactoryContext

interface ProjectChannelRepository : DefaultRepository {
    override val factoryContext: ProjectChannelRepositoryFactoryContext
}

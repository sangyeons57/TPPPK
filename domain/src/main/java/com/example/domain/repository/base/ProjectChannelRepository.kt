package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectChannel
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.ProjectChannelRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow

interface ProjectChannelRepository : DefaultRepository {
    override val factoryContext: ProjectChannelRepositoryFactoryContext
}

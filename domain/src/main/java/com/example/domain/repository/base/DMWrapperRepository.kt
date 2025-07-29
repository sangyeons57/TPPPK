package com.example.domain.repository.base

import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.DMWrapperRepositoryFactoryContext

interface DMWrapperRepository : DefaultRepository {
    override val factoryContext: DMWrapperRepositoryFactoryContext


}

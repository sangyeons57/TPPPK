package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.vo.UserId
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.DMWrapperRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow

interface DMWrapperRepository : DefaultRepository {
    override val factoryContext: DMWrapperRepositoryFactoryContext


}

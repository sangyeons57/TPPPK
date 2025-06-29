package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Member
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.MemberRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow

interface MemberRepository : DefaultRepository {
    override val factoryContext: MemberRepositoryFactoryContext


}

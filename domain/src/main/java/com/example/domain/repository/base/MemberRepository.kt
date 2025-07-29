package com.example.domain.repository.base

import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.MemberRepositoryFactoryContext

interface MemberRepository : DefaultRepository {
    override val factoryContext: MemberRepositoryFactoryContext


}

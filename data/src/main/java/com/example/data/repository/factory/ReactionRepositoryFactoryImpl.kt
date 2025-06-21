package com.example.data.repository.factory

import com.example.data.repository.base.ReactionRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.ReactionRepository
import com.example.domain.repository.factory.context.ReactionRepositoryFactoryContext
import javax.inject.Inject

class ReactionRepositoryFactoryImpl @Inject constructor(
) : RepositoryFactory<ReactionRepositoryFactoryContext, ReactionRepository> {

    override fun create(input: ReactionRepositoryFactoryContext): ReactionRepository {
        return ReactionRepositoryImpl()
    }
}

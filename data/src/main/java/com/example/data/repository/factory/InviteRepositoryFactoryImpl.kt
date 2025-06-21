package com.example.data.repository.factory

import com.example.data.datasource.remote.InviteRemoteDataSource
import com.example.data.repository.base.InviteRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.InviteRepository
import com.example.domain.repository.factory.context.InviteRepositoryFactoryContext
import javax.inject.Inject

class InviteRepositoryFactoryImpl @Inject constructor(
    private val inviteRemoteDataSource: InviteRemoteDataSource
) : RepositoryFactory<InviteRepositoryFactoryContext, InviteRepository> {

    override fun create(input: InviteRepositoryFactoryContext): InviteRepository {
        return InviteRepositoryImpl(
            inviteRemoteDataSource = inviteRemoteDataSource
        )
    }
}

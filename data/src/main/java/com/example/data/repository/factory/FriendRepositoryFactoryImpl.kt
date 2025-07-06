package com.example.data.repository.factory

import com.example.data.datasource.remote.FriendRemoteDataSource
import com.example.data.datasource.remote.UserRemoteDataSource
import com.example.data.datasource.remote.special.FunctionsRemoteDataSource
import com.example.data.repository.base.FriendRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.FriendRepository
import com.example.domain.repository.factory.context.FriendRepositoryFactoryContext
import javax.inject.Inject

class FriendRepositoryFactoryImpl @Inject constructor(
    private val friendRemoteDataSource: FriendRemoteDataSource,
    private val functionRemoteDataSource: FunctionsRemoteDataSource,
    private val userRemoteDataSource: UserRemoteDataSource
) : RepositoryFactory<FriendRepositoryFactoryContext, FriendRepository> {

    override fun create(input: FriendRepositoryFactoryContext): FriendRepository {
        return FriendRepositoryImpl(
            friendRemoteDataSource = friendRemoteDataSource,
            functionsRemoteDataSource = functionRemoteDataSource,
            factoryContext = input
        )
    }
}

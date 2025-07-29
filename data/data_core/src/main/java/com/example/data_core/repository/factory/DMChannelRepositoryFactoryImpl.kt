package com.example.data_core.repository.factory

import com.example.data_core.datasource.remote.DMChannelRemoteDataSource
import com.example.data_core.datasource.remote.special.AuthRemoteDataSource
import com.example.data_core.datasource.remote.special.FunctionsRemoteDataSource
import com.example.data_core.repository.base.DMChannelRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.DMChannelRepository
import com.example.domain.repository.factory.context.DMChannelRepositoryFactoryContext
import javax.inject.Inject

class DMChannelRepositoryFactoryImpl @Inject constructor(
    private val dmChannelRemoteDataSource: DMChannelRemoteDataSource,
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val functionsRemoteDataSource: FunctionsRemoteDataSource,
) : RepositoryFactory<DMChannelRepositoryFactoryContext, DMChannelRepository> {

    override fun create(input: DMChannelRepositoryFactoryContext): DMChannelRepository {
        return DMChannelRepositoryImpl(
            dmChannelRemoteDataSource = dmChannelRemoteDataSource,
            authRemoteDataSource = authRemoteDataSource,
            functionsRemoteDataSource = functionsRemoteDataSource,
            factoryContext = input,
        )
    }
}

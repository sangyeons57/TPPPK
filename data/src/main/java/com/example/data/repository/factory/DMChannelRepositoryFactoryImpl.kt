package com.example.data.repository.factory

import com.example.data.datasource.remote.DMChannelRemoteDataSource
import com.example.data.datasource.remote.UserRemoteDataSource
import com.example.data.datasource.remote.special.AuthRemoteDataSource
import com.example.data.repository.base.DMChannelRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.DMChannelRepository
import com.example.domain.repository.factory.context.DMChannelRepositoryFactoryContext
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class DMChannelRepositoryFactoryImpl @Inject constructor(
    private val dmChannelRemoteDataSource: DMChannelRemoteDataSource,
    private val authRemoteDataSource: AuthRemoteDataSource,
) : RepositoryFactory<DMChannelRepositoryFactoryContext, DMChannelRepository> {

    override fun create(input: DMChannelRepositoryFactoryContext): DMChannelRepository {
        return DMChannelRepositoryImpl(
            dmChannelRemoteDataSource = dmChannelRemoteDataSource,
            authRemoteDataSource = authRemoteDataSource,
            factoryContext = input,

        )
    }
}

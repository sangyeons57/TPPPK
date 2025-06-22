package com.example.data.repository.factory

import com.example.data.datasource.remote.DMWrapperRemoteDataSource
import com.example.data.repository.base.DMWrapperRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.DMWrapperRepository
import com.example.domain.repository.factory.context.DMWrapperRepositoryFactoryContext
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class DMWrapperRepositoryFactoryImpl @Inject constructor(
    private val dmWrapperRemoteDataSource: DMWrapperRemoteDataSource,
) : RepositoryFactory<DMWrapperRepositoryFactoryContext, DMWrapperRepository> {

    override fun create(input: DMWrapperRepositoryFactoryContext): DMWrapperRepository {
        return DMWrapperRepositoryImpl(
            dmWrapperRemoteDataSource = dmWrapperRemoteDataSource,
            factoryContext = input,
        )
    }
}

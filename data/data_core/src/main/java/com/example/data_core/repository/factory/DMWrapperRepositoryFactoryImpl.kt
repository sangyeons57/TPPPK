package com.example.data_core.repository.factory

import com.example.data_core.datasource.remote.DMWrapperRemoteDataSource
import com.example.data_core.repository.base.DMWrapperRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.DMWrapperRepository
import com.example.domain.repository.factory.context.DMWrapperRepositoryFactoryContext
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

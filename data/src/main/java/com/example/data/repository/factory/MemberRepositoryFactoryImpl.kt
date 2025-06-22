package com.example.data.repository.factory

import com.example.data.datasource.remote.MemberRemoteDataSource
import com.example.data.repository.base.MemberRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.MemberRepository
import com.example.domain.repository.factory.context.MemberRepositoryFactoryContext
import javax.inject.Inject

class MemberRepositoryFactoryImpl @Inject constructor(
    private val memberRemoteDataSource: MemberRemoteDataSource
) : RepositoryFactory<MemberRepositoryFactoryContext, MemberRepository> {

    override fun create(input: MemberRepositoryFactoryContext): MemberRepository {
        return MemberRepositoryImpl(
            memberRemoteDataSource = memberRemoteDataSource,
            factoryContext = input,
        )
    }
}

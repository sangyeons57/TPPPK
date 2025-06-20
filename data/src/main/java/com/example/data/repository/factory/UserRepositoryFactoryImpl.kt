package com.example.data.repository.factory

import com.example.data.datasource.remote.UserRemoteDataSource
import com.example.data.repository.base.UserRepositoryImpl
import com.example.domain.model.base.User
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.MediaRepository
import com.example.domain.repository.base.UserRepository
import com.example.domain.repository.factory.context.UserRepositoryFactoryContext

class UserRepositoryFactoryImpl(
    private val userRemoteDataSource: UserRemoteDataSource,
): RepositoryFactory<UserRepositoryFactoryContext, UserRepository>{

    override fun create(input: UserRepositoryFactoryContext): UserRepository {
        return UserRepositoryImpl(
            userRemoteDataSource = userRemoteDataSource,
            collectionPath = input.collectionPath
        )
    }
}
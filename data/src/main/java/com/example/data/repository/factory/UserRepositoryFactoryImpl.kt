package com.example.data.repository.factory

import android.util.Log
import com.example.data.datasource.remote.RoleRemoteDataSource
import com.example.data.datasource.remote.UserRemoteDataSource
import com.example.data.datasource.remote.special.FunctionsRemoteDataSource
import com.example.data.repository.base.UserRepositoryImpl
import com.example.domain.model.base.User
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.MediaRepository
import com.example.domain.repository.base.UserRepository
import com.example.domain.repository.factory.context.UserRepositoryFactoryContext
import javax.inject.Inject

class UserRepositoryFactoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val functionsRemoteDataSource: FunctionsRemoteDataSource,
): RepositoryFactory<UserRepositoryFactoryContext, UserRepository>{

    override fun create(input: UserRepositoryFactoryContext): UserRepository {
        Log.d("UserRepositoryFactoryImpl", input.toString())
        return UserRepositoryImpl(
            userRemoteDataSource = userRemoteDataSource,
            functionsRemoteDataSource = functionsRemoteDataSource,
            factoryContext = input,
        )
    }
}
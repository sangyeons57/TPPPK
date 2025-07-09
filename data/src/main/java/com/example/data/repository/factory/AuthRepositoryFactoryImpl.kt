package com.example.data.repository.factory

import com.example.data.datasource.remote.special.AuthRemoteDataSource
import com.example.data.repository.base.AuthRepositoryImpl
import com.example.data.service.CacheService
import com.example.data.util.FirebaseAuthWrapper
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class AuthRepositoryFactoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val authWrapper: FirebaseAuthWrapper,
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val cacheService: CacheService,
) : RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository> {

    override fun create(input: AuthRepositoryFactoryContext): AuthRepository {
        return AuthRepositoryImpl(
            authWrapper = authWrapper,
            authRemoteDataSource = authRemoteDataSource,
            cacheService = cacheService
        )
    }
}

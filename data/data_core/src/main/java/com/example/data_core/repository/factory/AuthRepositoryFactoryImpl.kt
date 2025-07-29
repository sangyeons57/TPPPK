package com.example.data_core.repository.factory

import com.example.data_core.datasource.remote.special.AuthRemoteDataSource
import com.example.data_core.repository.base.AuthRepositoryImpl
import com.example.data_core.util.FirebaseAuthWrapper
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class AuthRepositoryFactoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val authWrapper: FirebaseAuthWrapper,
    private val authRemoteDataSource: AuthRemoteDataSource,
) : RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository> {

    override fun create(input: AuthRepositoryFactoryContext): AuthRepository {
        return AuthRepositoryImpl(
            authWrapper = authWrapper,
            authRemoteDataSource = authRemoteDataSource
        )
    }
}

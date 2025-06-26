package com.example.data.repository.factory

import com.example.data.repository.base.FunctionsRepositoryImpl
import com.example.data.datasource.remote.special.FunctionsRemoteDataSourceImpl
import com.example.domain.repository.FunctionsRepository
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.factory.context.FunctionsRepositoryFactoryContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FunctionsRepository를 생성하는 Factory 구현체
 * Clean Architecture의 Repository Factory 패턴을 따릅니다.
 */
@Singleton
class FunctionsRepositoryFactoryImpl @Inject constructor(
    private val firebaseFunctions: FirebaseFunctions,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseStorage: FirebaseStorage
) : RepositoryFactory<FunctionsRepositoryFactoryContext, FunctionsRepository> {
    
    override fun create(input: FunctionsRepositoryFactoryContext): FunctionsRepository {
        val functionsRemoteDataSource = FunctionsRemoteDataSourceImpl(
            functions = firebaseFunctions,
            auth = firebaseAuth,
            storage = firebaseStorage
        )
        
        return FunctionsRepositoryImpl(functionsRemoteDataSource)
    }
}
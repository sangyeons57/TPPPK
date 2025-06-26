package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.special.FunctionsRemoteDataSource
import com.example.domain.repository.FunctionsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FunctionsRepository의 구현체
 * FunctionsRemoteDataSource를 사용하여 Firebase Functions과 통신합니다.
 */
@Singleton
class FunctionsRepositoryImpl @Inject constructor(
    private val functionsRemoteDataSource: FunctionsRemoteDataSource
) : FunctionsRepository {

    override suspend fun callFunction(
        functionName: String,
        data: Map<String, Any?>?
    ): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.callFunction(functionName, data)
    }

    override suspend fun getHelloWorld(): CustomResult<String, Exception> {
        return functionsRemoteDataSource.getHelloWorld()
    }

    override suspend fun callFunctionWithUserData(
        functionName: String,
        userId: String,
        customData: Map<String, Any?>?
    ): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.callFunctionWithUserData(functionName, userId, customData)
    }
}
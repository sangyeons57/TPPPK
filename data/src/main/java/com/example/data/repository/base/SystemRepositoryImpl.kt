package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.special.FunctionsRemoteDataSource
import com.example.domain.repository.base.SystemRepository
import javax.inject.Inject

/**
 * 시스템 공통 기능 및 Firebase Functions 호출과 관련된 Repository 구현체입니다.
 */
class SystemRepositoryImpl @Inject constructor(
    private val functionsRemoteDataSource: FunctionsRemoteDataSource
) : SystemRepository {

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
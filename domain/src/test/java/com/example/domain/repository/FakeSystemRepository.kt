package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.SystemRepository

class FakeSystemRepository : SystemRepository {
    private var shouldThrowError = false
    private var helloWorldMessage = "Hello from Firebase!"
    private val functionResults = mutableMapOf<String, Map<String, Any?>>()

    fun setShouldThrowError(shouldThrow: Boolean) {
        shouldThrowError = shouldThrow
    }

    fun setHelloWorldMessage(message: String) {
        helloWorldMessage = message
    }

    fun setFunctionResult(functionName: String, result: Map<String, Any?>) {
        functionResults[functionName] = result
    }

    override suspend fun callFunction(
        functionName: String,
        data: Map<String, Any?>?
    ): CustomResult<Map<String, Any?>, Exception> {
        return if (shouldThrowError) {
            CustomResult.Failure(Exception("Function call failed"))
        } else {
            CustomResult.Success(functionResults[functionName] ?: mapOf("result" to "default"))
        }
    }

    override suspend fun getHelloWorld(): CustomResult<String, Exception> {
        return if (shouldThrowError) {
            CustomResult.Failure(Exception("Hello World function failed"))
        } else {
            CustomResult.Success(helloWorldMessage)
        }
    }

    override suspend fun callFunctionWithUserData(
        functionName: String,
        userId: String,
        customData: Map<String, Any?>?
    ): CustomResult<Map<String, Any?>, Exception> {
        return if (shouldThrowError) {
            CustomResult.Failure(Exception("Function call with user data failed"))
        } else {
            CustomResult.Success(functionResults[functionName] ?: mapOf("result" to "default"))
        }
    }
}
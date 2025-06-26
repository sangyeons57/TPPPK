package com.example.domain.repository

import com.example.core_common.result.CustomResult

class FakeFunctionsRepository : FunctionsRepository {

    private var shouldThrowError = false
    private var helloWorldMessage = "Hello from Firebase!"
    private var functionResults = mutableMapOf<String, Map<String, Any?>>()

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
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Function call failed"))
        }
        
        val result = functionResults[functionName] ?: mapOf("result" to "Default response")
        return CustomResult.Success(result)
    }

    override suspend fun getHelloWorld(): CustomResult<String, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Hello World function failed"))
        }
        return CustomResult.Success(helloWorldMessage)
    }

    override suspend fun callFunctionWithUserData(
        functionName: String,
        userId: String,
        customData: Map<String, Any?>?
    ): CustomResult<Map<String, Any?>, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Function with user data failed"))
        }
        
        val result = functionResults[functionName] ?: mapOf(
            "result" to "Default user response",
            "userId" to userId,
            "customData" to customData
        )
        return CustomResult.Success(result)
    }
}
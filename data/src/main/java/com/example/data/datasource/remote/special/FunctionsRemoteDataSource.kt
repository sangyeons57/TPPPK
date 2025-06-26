package com.example.data.datasource.remote.special

import com.example.core_common.result.CustomResult
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

interface FunctionsRemoteDataSource {

    /**
     * Firebase Functions의 callable function을 호출합니다.
     * 
     * @param functionName 호출할 함수 이름
     * @param data 함수에 전달할 데이터 (nullable)
     * @return 함수 실행 결과를 담은 CustomResult
     */
    suspend fun callFunction(
        functionName: String,
        data: Map<String, Any?>? = null
    ): CustomResult<Map<String, Any?>, Exception>

    /**
     * "Hello World" 메시지를 반환하는 함수를 호출합니다.
     * 
     * @return Hello World 메시지를 담은 CustomResult
     */
    suspend fun getHelloWorld(): CustomResult<String, Exception>

    /**
     * 사용자 정의 데이터와 함께 함수를 호출합니다.
     * 
     * @param functionName 호출할 함수 이름
     * @param userId 사용자 ID
     * @param customData 사용자 정의 데이터
     * @return 함수 실행 결과를 담은 CustomResult
     */
    suspend fun callFunctionWithUserData(
        functionName: String,
        userId: String,
        customData: Map<String, Any?>? = null
    ): CustomResult<Map<String, Any?>, Exception>
}

@Singleton
class FunctionsRemoteDataSourceImpl @Inject constructor(
    private val functions: FirebaseFunctions
) : FunctionsRemoteDataSource {

    companion object {
        private const val DEFAULT_TIMEOUT_MS = 30000L
    }

    override suspend fun callFunction(
        functionName: String,
        data: Map<String, Any?>?
    ): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            val callable = functions.getHttpsCallable(functionName)
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                if (data != null) {
                    callable.call(data).await()
                } else {
                    callable.call().await()
                }
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                CustomResult.Failure(Exception("Function call timed out after ${DEFAULT_TIMEOUT_MS}ms"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun getHelloWorld(): CustomResult<String, Exception> = withContext(Dispatchers.IO) {
        try {
            val callable = functions.getHttpsCallable("helloWorld")
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call().await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val responseData = result.data as? Map<String, Any?>
                val message = responseData?.get("message") as? String 
                    ?: result.data as? String 
                    ?: result.data.toString()
                CustomResult.Success(message)
            } else {
                CustomResult.Failure(Exception("Hello World function call timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun callFunctionWithUserData(
        functionName: String,
        userId: String,
        customData: Map<String, Any?>?
    ): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            val requestData = mutableMapOf<String, Any?>("userId" to userId)
            customData?.let { requestData.putAll(it) }

            val callable = functions.getHttpsCallable(functionName)
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                CustomResult.Failure(Exception("Function call with user data timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }
}
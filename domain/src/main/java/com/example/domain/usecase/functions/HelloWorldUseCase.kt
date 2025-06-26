package com.example.domain.usecase.functions

import com.example.core_common.result.CustomResult
import com.example.domain.repository.FunctionsRepository
import javax.inject.Inject

/**
 * Firebase Functions의 "Hello World" 함수를 호출하는 UseCase
 * Firebase Functions 통합의 기본적인 동작을 테스트하고 데모하는 용도로 사용됩니다.
 */
interface HelloWorldUseCase {
    /**
     * Firebase Functions의 helloWorld 함수를 호출하여 "Hello World" 메시지를 가져옵니다.
     * 
     * @return Hello World 메시지를 담은 CustomResult
     */
    suspend operator fun invoke(): CustomResult<String, Exception>
    
    /**
     * 사용자 정의 데이터와 함께 Functions을 호출합니다.
     * 
     * @param customMessage 사용자가 전달할 메시지
     * @return Functions에서 처리된 응답을 담은 CustomResult
     */
    suspend fun callWithCustomMessage(customMessage: String): CustomResult<String, Exception>
}

/**
 * HelloWorldUseCase의 구현체
 * FunctionsRepository를 사용하여 Firebase Functions과 통신합니다.
 */
class HelloWorldUseCaseImpl @Inject constructor(
    private val functionsRepository: FunctionsRepository
) : HelloWorldUseCase {

    override suspend fun invoke(): CustomResult<String, Exception> {
        return functionsRepository.getHelloWorld()
    }

    override suspend fun callWithCustomMessage(customMessage: String): CustomResult<String, Exception> {
        return try {
            val data = mapOf("message" to customMessage)
            val result = functionsRepository.callFunction("customHelloWorld", data)
            
            when (result) {
                is CustomResult.Success -> {
                    val responseMessage = result.data["message"] as? String 
                        ?: result.data["result"] as? String 
                        ?: "No message received"
                    CustomResult.Success(responseMessage)
                }
                is CustomResult.Failure -> {
                    CustomResult.Failure(result.error)
                }
                is CustomResult.Loading -> {
                    CustomResult.Loading
                }
                is CustomResult.Initial -> {
                    CustomResult.Initial
                }
                is CustomResult.Progress -> {
                    CustomResult.Loading
                }
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}
#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

import kotlinx.coroutines.runBlocking
import com.example.domain.usecase.functions.HelloWorldUseCaseImpl
import com.example.domain.repository.FunctionsRepository
import com.example.core_common.result.CustomResult

// Simple test to verify our Functions functionality
fun main() {
    println("🧪 Testing Firebase Functions Hello World...")
    
    // Create a simple fake repository for testing
    val fakeRepository = object : FunctionsRepository {
        override suspend fun callFunction(functionName: String, data: Map<String, Any?>?): CustomResult<Map<String, Any?>, Exception> {
            return CustomResult.Success(mapOf("result" to "Function called: $functionName"))
        }
        
        override suspend fun getHelloWorld(): CustomResult<String, Exception> {
            return CustomResult.Success("Hello from Firebase!")
        }
        
        override suspend fun callFunctionWithUserData(functionName: String, userId: String, customData: Map<String, Any?>?): CustomResult<Map<String, Any?>, Exception> {
            return CustomResult.Success(mapOf("message" to "Hello $userId!"))
        }
    }
    
    val useCase = HelloWorldUseCaseImpl(fakeRepository)
    
    runBlocking {
        // Test 1: Basic Hello World
        println("Test 1: Basic Hello World")
        when (val result = useCase()) {
            is CustomResult.Success -> println("✅ SUCCESS: ${result.data}")
            is CustomResult.Failure -> println("❌ FAILURE: ${result.error.message}")
            else -> println("⚠️  UNEXPECTED: $result")
        }
        
        // Test 2: Custom Message
        println("\nTest 2: Custom Message")
        when (val result = useCase.callWithCustomMessage("Test message")) {
            is CustomResult.Success -> println("✅ SUCCESS: ${result.data}")
            is CustomResult.Failure -> println("❌ FAILURE: ${result.error.message}")
            else -> println("⚠️  UNEXPECTED: $result")
        }
    }
    
    println("\n🎉 All tests completed!")
}
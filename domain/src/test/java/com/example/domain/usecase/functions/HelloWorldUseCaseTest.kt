package com.example.domain.usecase.functions

import com.example.core_common.result.CustomResult
import com.example.domain.repository.FakeFunctionsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HelloWorldUseCaseTest {

    private lateinit var helloWorldUseCase: HelloWorldUseCase
    private lateinit var fakeFunctionsRepository: FakeFunctionsRepository

    @Before
    fun setUp() {
        fakeFunctionsRepository = FakeFunctionsRepository()
        helloWorldUseCase = HelloWorldUseCaseImpl(fakeFunctionsRepository)
    }

    @Test
    fun `getHelloWorld returns success with default message`() = runTest {
        // When
        val result = helloWorldUseCase()

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals("Hello from Firebase!", (result as CustomResult.Success).data)
    }

    @Test
    fun `getHelloWorld returns success with custom message`() = runTest {
        // Given
        val customMessage = "Custom Hello World!"
        fakeFunctionsRepository.setHelloWorldMessage(customMessage)

        // When
        val result = helloWorldUseCase()

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals(customMessage, (result as CustomResult.Success).data)
    }

    @Test
    fun `getHelloWorld returns failure when repository throws error`() = runTest {
        // Given
        fakeFunctionsRepository.setShouldThrowError(true)

        // When
        val result = helloWorldUseCase()

        // Then
        assertTrue(result is CustomResult.Failure)
        assertEquals("Hello World function failed", (result as CustomResult.Failure).error.message)
    }

    @Test
    fun `callWithCustomMessage returns success with processed message`() = runTest {
        // Given
        val customMessage = "Test message"
        val expectedResponse = mapOf("message" to "Processed: $customMessage")
        fakeFunctionsRepository.setFunctionResult("customHelloWorld", expectedResponse)

        // When
        val result = helloWorldUseCase.callWithCustomMessage(customMessage)

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals("Processed: $customMessage", (result as CustomResult.Success).data)
    }

    @Test
    fun `callWithCustomMessage returns success with result field when no message field`() = runTest {
        // Given
        val customMessage = "Test message"
        val expectedResponse = mapOf("result" to "Result response")
        fakeFunctionsRepository.setFunctionResult("customHelloWorld", expectedResponse)

        // When
        val result = helloWorldUseCase.callWithCustomMessage(customMessage)

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals("Result response", (result as CustomResult.Success).data)
    }

    @Test
    fun `callWithCustomMessage returns default message when no message or result field`() = runTest {
        // Given
        val customMessage = "Test message"
        val expectedResponse = mapOf("data" to "Some other data")
        fakeFunctionsRepository.setFunctionResult("customHelloWorld", expectedResponse)

        // When
        val result = helloWorldUseCase.callWithCustomMessage(customMessage)

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals("No message received", (result as CustomResult.Success).data)
    }

    @Test
    fun `callWithCustomMessage returns failure when repository throws error`() = runTest {
        // Given
        val customMessage = "Test message"
        fakeFunctionsRepository.setShouldThrowError(true)

        // When
        val result = helloWorldUseCase.callWithCustomMessage(customMessage)

        // Then
        assertTrue(result is CustomResult.Failure)
        assertEquals("Function call failed", (result as CustomResult.Failure).error.message)
    }

    @Test
    fun `callWithCustomMessage handles exception and returns failure`() = runTest {
        // Given
        val customMessage = "Test message"
        fakeFunctionsRepository.setFunctionResult("customHelloWorld", mapOf("message" to null))

        // When
        val result = helloWorldUseCase.callWithCustomMessage(customMessage)

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals("No message received", (result as CustomResult.Success).data)
    }
}
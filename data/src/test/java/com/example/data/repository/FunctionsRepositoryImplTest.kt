package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.special.FunctionsRemoteDataSource
import com.example.data.repository.base.FunctionsRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FunctionsRepositoryImplTest {

    private lateinit var functionsRepositoryImpl: FunctionsRepositoryImpl
    private lateinit var mockFunctionsRemoteDataSource: FunctionsRemoteDataSource

    @Before
    fun setUp() {
        mockFunctionsRemoteDataSource = mockk()
        functionsRepositoryImpl = FunctionsRepositoryImpl(mockFunctionsRemoteDataSource)
    }

    @Test
    fun `callFunction returns success when remote data source succeeds`() = runTest {
        // Given
        val functionName = "testFunction"
        val data = mapOf("key" to "value")
        val expectedResult = mapOf("result" to "success")
        coEvery { 
            mockFunctionsRemoteDataSource.callFunction(functionName, data) 
        } returns CustomResult.Success(expectedResult)

        // When
        val result = functionsRepositoryImpl.callFunction(functionName, data)

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals(expectedResult, (result as CustomResult.Success).data)
        coVerify { mockFunctionsRemoteDataSource.callFunction(functionName, data) }
    }

    @Test
    fun `callFunction returns failure when remote data source fails`() = runTest {
        // Given
        val functionName = "testFunction"
        val data = mapOf("key" to "value")
        val expectedError = Exception("Function call failed")
        coEvery { 
            mockFunctionsRemoteDataSource.callFunction(functionName, data) 
        } returns CustomResult.Failure(expectedError)

        // When
        val result = functionsRepositoryImpl.callFunction(functionName, data)

        // Then
        assertTrue(result is CustomResult.Failure)
        assertEquals(expectedError, (result as CustomResult.Failure).error)
        coVerify { mockFunctionsRemoteDataSource.callFunction(functionName, data) }
    }

    @Test
    fun `callFunction with null data returns success when remote data source succeeds`() = runTest {
        // Given
        val functionName = "testFunction"
        val expectedResult = mapOf("result" to "success")
        coEvery { 
            mockFunctionsRemoteDataSource.callFunction(functionName, null) 
        } returns CustomResult.Success(expectedResult)

        // When
        val result = functionsRepositoryImpl.callFunction(functionName, null)

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals(expectedResult, (result as CustomResult.Success).data)
        coVerify { mockFunctionsRemoteDataSource.callFunction(functionName, null) }
    }

    @Test
    fun `getHelloWorld returns success when remote data source succeeds`() = runTest {
        // Given
        val expectedMessage = "Hello from Firebase!"
        coEvery { 
            mockFunctionsRemoteDataSource.getHelloWorld() 
        } returns CustomResult.Success(expectedMessage)

        // When
        val result = functionsRepositoryImpl.getHelloWorld()

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals(expectedMessage, (result as CustomResult.Success).data)
        coVerify { mockFunctionsRemoteDataSource.getHelloWorld() }
    }

    @Test
    fun `getHelloWorld returns failure when remote data source fails`() = runTest {
        // Given
        val expectedError = Exception("Hello World function failed")
        coEvery { 
            mockFunctionsRemoteDataSource.getHelloWorld() 
        } returns CustomResult.Failure(expectedError)

        // When
        val result = functionsRepositoryImpl.getHelloWorld()

        // Then
        assertTrue(result is CustomResult.Failure)
        assertEquals(expectedError, (result as CustomResult.Failure).error)
        coVerify { mockFunctionsRemoteDataSource.getHelloWorld() }
    }

    @Test
    fun `callFunctionWithUserData returns success when remote data source succeeds`() = runTest {
        // Given
        val functionName = "testFunction"
        val userId = "user123"
        val customData = mapOf("key" to "value")
        val expectedResult = mapOf("result" to "success", "userId" to userId)
        coEvery { 
            mockFunctionsRemoteDataSource.callFunctionWithUserData(functionName, userId, customData) 
        } returns CustomResult.Success(expectedResult)

        // When
        val result = functionsRepositoryImpl.callFunctionWithUserData(functionName, userId, customData)

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals(expectedResult, (result as CustomResult.Success).data)
        coVerify { mockFunctionsRemoteDataSource.callFunctionWithUserData(functionName, userId, customData) }
    }

    @Test
    fun `callFunctionWithUserData returns failure when remote data source fails`() = runTest {
        // Given
        val functionName = "testFunction"
        val userId = "user123"
        val customData = mapOf("key" to "value")
        val expectedError = Exception("Function with user data failed")
        coEvery { 
            mockFunctionsRemoteDataSource.callFunctionWithUserData(functionName, userId, customData) 
        } returns CustomResult.Failure(expectedError)

        // When
        val result = functionsRepositoryImpl.callFunctionWithUserData(functionName, userId, customData)

        // Then
        assertTrue(result is CustomResult.Failure)
        assertEquals(expectedError, (result as CustomResult.Failure).error)
        coVerify { mockFunctionsRemoteDataSource.callFunctionWithUserData(functionName, userId, customData) }
    }

    @Test
    fun `callFunctionWithUserData with null customData returns success when remote data source succeeds`() = runTest {
        // Given
        val functionName = "testFunction"
        val userId = "user123"
        val expectedResult = mapOf("result" to "success", "userId" to userId)
        coEvery { 
            mockFunctionsRemoteDataSource.callFunctionWithUserData(functionName, userId, null) 
        } returns CustomResult.Success(expectedResult)

        // When
        val result = functionsRepositoryImpl.callFunctionWithUserData(functionName, userId, null)

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals(expectedResult, (result as CustomResult.Success).data)
        coVerify { mockFunctionsRemoteDataSource.callFunctionWithUserData(functionName, userId, null) }
    }
}
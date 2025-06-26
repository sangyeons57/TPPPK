package com.example.core_common.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomResultTest {

    @Test
    fun `Success result creation and access`() {
        // Given
        val data = "test data"

        // When
        val result = CustomResult.Success(data)

        // Then
        assertTrue("Result should be Success", result is CustomResult.Success)
        assertEquals("Data should match", data, result.data)
    }

    @Test
    fun `Failure result creation and access`() {
        // Given
        val error = Exception("test error")

        // When
        val result = CustomResult.Failure(error)

        // Then
        assertTrue("Result should be Failure", result is CustomResult.Failure)
        assertEquals("Error should match", error, result.error)
    }

    @Test
    fun `Loading result creation`() {
        // When
        val result = CustomResult.Loading<String>()

        // Then
        assertTrue("Result should be Loading", result is CustomResult.Loading)
    }

    @Test
    fun `Initial result creation`() {
        // When
        val result = CustomResult.Initial<String>()

        // Then
        assertTrue("Result should be Initial", result is CustomResult.Initial)
    }

    @Test
    fun `Progress result creation and access`() {
        // Given
        val progress = 50

        // When
        val result = CustomResult.Progress<String>(progress)

        // Then
        assertTrue("Result should be Progress", result is CustomResult.Progress)
        assertEquals("Progress should match", progress, result.progress)
    }

    @Test
    fun `Success fold behavior`() {
        // Given
        val data = "test data"
        val result = CustomResult.Success(data)
        var onSuccessCalled = false
        var onFailureCalled = false

        // When
        result.fold(
            onSuccess = { onSuccessCalled = true },
            onFailure = { onFailureCalled = true }
        )

        // Then
        assertTrue("onSuccess should be called", onSuccessCalled)
        assertFalse("onFailure should not be called", onFailureCalled)
    }

    @Test
    fun `Failure fold behavior`() {
        // Given
        val error = Exception("test error")
        val result = CustomResult.Failure<String, Exception>(error)
        var onSuccessCalled = false
        var onFailureCalled = false

        // When
        result.fold(
            onSuccess = { onSuccessCalled = true },
            onFailure = { onFailureCalled = true }
        )

        // Then
        assertFalse("onSuccess should not be called", onSuccessCalled)
        assertTrue("onFailure should be called", onFailureCalled)
    }

    @Test
    fun `onSuccess extension function`() {
        // Given
        val data = "test data"
        val result = CustomResult.Success(data)
        var callbackData: String? = null

        // When
        result.onSuccess { callbackData = it }

        // Then
        assertEquals("Callback should receive data", data, callbackData)
    }

    @Test
    fun `onSuccess extension function with Failure`() {
        // Given
        val error = Exception("test error")
        val result = CustomResult.Failure<String, Exception>(error)
        var callbackCalled = false

        // When
        result.onSuccess { callbackCalled = true }

        // Then
        assertFalse("Callback should not be called", callbackCalled)
    }

    @Test
    fun `onFailure extension function`() {
        // Given
        val error = Exception("test error")
        val result = CustomResult.Failure<String, Exception>(error)
        var callbackError: Exception? = null

        // When
        result.onFailure { callbackError = it }

        // Then
        assertEquals("Callback should receive error", error, callbackError)
    }

    @Test
    fun `onFailure extension function with Success`() {
        // Given
        val data = "test data"
        val result = CustomResult.Success(data)
        var callbackCalled = false

        // When
        result.onFailure { callbackCalled = true }

        // Then
        assertFalse("Callback should not be called", callbackCalled)
    }

    @Test
    fun `getOrNull extension function with Success`() {
        // Given
        val data = "test data"
        val result = CustomResult.Success(data)

        // When
        val retrieved = result.getOrNull()

        // Then
        assertEquals("Should return data", data, retrieved)
    }

    @Test
    fun `getOrNull extension function with Failure`() {
        // Given
        val error = Exception("test error")
        val result = CustomResult.Failure<String, Exception>(error)

        // When
        val retrieved = result.getOrNull()

        // Then
        assertNull("Should return null", retrieved)
    }

    @Test
    fun `getOrNull extension function with Loading`() {
        // Given
        val result = CustomResult.Loading<String>()

        // When
        val retrieved = result.getOrNull()

        // Then
        assertNull("Should return null", retrieved)
    }

    @Test
    fun `getOrDefault extension function with Success`() {
        // Given
        val data = "test data"
        val default = "default data"
        val result = CustomResult.Success(data)

        // When
        val retrieved = result.getOrDefault(default)

        // Then
        assertEquals("Should return data", data, retrieved)
    }

    @Test
    fun `getOrDefault extension function with Failure`() {
        // Given
        val error = Exception("test error")
        val default = "default data"
        val result = CustomResult.Failure<String, Exception>(error)

        // When
        val retrieved = result.getOrDefault(default)

        // Then
        assertEquals("Should return default", default, retrieved)
    }

    @Test
    fun `isSuccess extension function`() {
        // Given
        val successResult = CustomResult.Success("data")
        val failureResult = CustomResult.Failure<String, Exception>(Exception())
        val loadingResult = CustomResult.Loading<String>()

        // Then
        assertTrue("Success should be success", successResult.isSuccess())
        assertFalse("Failure should not be success", failureResult.isSuccess())
        assertFalse("Loading should not be success", loadingResult.isSuccess())
    }

    @Test
    fun `isFailure extension function`() {
        // Given
        val successResult = CustomResult.Success("data")
        val failureResult = CustomResult.Failure<String, Exception>(Exception())
        val loadingResult = CustomResult.Loading<String>()

        // Then
        assertFalse("Success should not be failure", successResult.isFailure())
        assertTrue("Failure should be failure", failureResult.isFailure())
        assertFalse("Loading should not be failure", loadingResult.isFailure())
    }

    @Test
    fun `isLoading extension function`() {
        // Given
        val successResult = CustomResult.Success("data")
        val failureResult = CustomResult.Failure<String, Exception>(Exception())
        val loadingResult = CustomResult.Loading<String>()

        // Then
        assertFalse("Success should not be loading", successResult.isLoading())
        assertFalse("Failure should not be loading", failureResult.isLoading())
        assertTrue("Loading should be loading", loadingResult.isLoading())
    }

    @Test
    fun `map extension function with Success`() {
        // Given
        val data = "test"
        val result = CustomResult.Success(data)

        // When
        val mappedResult = result.map { it.uppercase() }

        // Then
        assertTrue("Mapped result should be Success", mappedResult is CustomResult.Success)
        assertEquals("Mapped data should be uppercase", "TEST", (mappedResult as CustomResult.Success).data)
    }

    @Test
    fun `map extension function with Failure`() {
        // Given
        val error = Exception("test error")
        val result = CustomResult.Failure<String, Exception>(error)

        // When
        val mappedResult = result.map { it.uppercase() }

        // Then
        assertTrue("Mapped result should be Failure", mappedResult is CustomResult.Failure)
        assertEquals("Error should be preserved", error, (mappedResult as CustomResult.Failure).error)
    }

    @Test
    fun `flatMap extension function with Success to Success`() {
        // Given
        val data = "test"
        val result = CustomResult.Success(data)

        // When
        val flatMappedResult = result.flatMap { CustomResult.Success(it.uppercase()) }

        // Then
        assertTrue("Result should be Success", flatMappedResult is CustomResult.Success)
        assertEquals("Data should be uppercase", "TEST", (flatMappedResult as CustomResult.Success).data)
    }

    @Test
    fun `flatMap extension function with Success to Failure`() {
        // Given
        val data = "test"
        val error = Exception("mapping error")
        val result = CustomResult.Success(data)

        // When
        val flatMappedResult = result.flatMap { CustomResult.Failure<String, Exception>(error) }

        // Then
        assertTrue("Result should be Failure", flatMappedResult is CustomResult.Failure)
        assertEquals("Error should match", error, (flatMappedResult as CustomResult.Failure).error)
    }

    @Test
    fun `flatMap extension function with Failure`() {
        // Given
        val error = Exception("test error")
        val result = CustomResult.Failure<String, Exception>(error)

        // When
        val flatMappedResult = result.flatMap { CustomResult.Success(it.uppercase()) }

        // Then
        assertTrue("Result should be Failure", flatMappedResult is CustomResult.Failure)
        assertEquals("Error should be preserved", error, (flatMappedResult as CustomResult.Failure).error)
    }
}
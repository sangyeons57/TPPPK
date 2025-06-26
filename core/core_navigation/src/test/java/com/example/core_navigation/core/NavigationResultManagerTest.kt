package com.example.core_navigation.core

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class NavigationResultManagerTest {

    private lateinit var navigationResultManager: NavigationResultManager
    private lateinit var mockNavController: NavHostController
    private lateinit var mockCurrentEntry: NavBackStackEntry
    private lateinit var mockPreviousEntry: NavBackStackEntry
    private lateinit var mockCurrentSavedStateHandle: SavedStateHandle
    private lateinit var mockPreviousSavedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        navigationResultManager = NavigationResultManager()
        mockNavController = mock(NavHostController::class.java)
        mockCurrentEntry = mock(NavBackStackEntry::class.java)
        mockPreviousEntry = mock(NavBackStackEntry::class.java)
        mockCurrentSavedStateHandle = mock(SavedStateHandle::class.java)
        mockPreviousSavedStateHandle = mock(SavedStateHandle::class.java)
    }

    @Test
    fun `setResult stores value in previous entry savedStateHandle`() {
        // Given
        val key = "test_key"
        val value = "test_value"
        
        `when`(mockNavController.previousBackStackEntry).thenReturn(mockPreviousEntry)
        `when`(mockPreviousEntry.savedStateHandle).thenReturn(mockPreviousSavedStateHandle)

        // When
        navigationResultManager.setResult(mockNavController, key, value)

        // Then
        verify(mockPreviousSavedStateHandle)[key] = value
    }

    @Test(expected = IllegalStateException::class)
    fun `setResult throws exception when no previous entry`() {
        // Given
        val key = "test_key"
        val value = "test_value"
        
        `when`(mockNavController.previousBackStackEntry).thenReturn(null)

        // When
        navigationResultManager.setResult(mockNavController, key, value)

        // Then - exception should be thrown
    }

    @Test
    fun `getResult retrieves value from current entry savedStateHandle`() {
        // Given
        val key = "test_key"
        val expectedValue = "test_value"
        
        `when`(mockNavController.currentBackStackEntry).thenReturn(mockCurrentEntry)
        `when`(mockCurrentEntry.savedStateHandle).thenReturn(mockCurrentSavedStateHandle)
        `when`(mockCurrentSavedStateHandle.get<String>(key)).thenReturn(expectedValue)

        // When
        val result = navigationResultManager.getResult<String>(mockNavController, key)

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun `getResult returns null when no current entry`() {
        // Given
        val key = "test_key"
        
        `when`(mockNavController.currentBackStackEntry).thenReturn(null)

        // When
        val result = navigationResultManager.getResult<String>(mockNavController, key)

        // Then
        assertNull(result)
    }

    @Test
    fun `getResult from SavedStateHandle returns value`() {
        // Given
        val key = "test_key"
        val expectedValue = "test_value"
        val savedStateHandle = SavedStateHandle()
        savedStateHandle[key] = expectedValue

        // When
        val result = navigationResultManager.getResult<String>(savedStateHandle, key)

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun `observeResult from SavedStateHandle emits value`() = runTest {
        // Given
        val key = "test_key"
        val expectedValue = "test_value"
        val savedStateHandle = SavedStateHandle()
        savedStateHandle[key] = expectedValue

        // When
        val flow = navigationResultManager.observeResult<String>(savedStateHandle, key)
        val result = flow.first()

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun `observeNonNullResult from SavedStateHandle emits non-null value`() = runTest {
        // Given
        val key = "test_key"
        val expectedValue = "test_value"
        val savedStateHandle = SavedStateHandle()
        savedStateHandle[key] = expectedValue

        // When
        val flow = navigationResultManager.observeNonNullResult<String>(savedStateHandle, key)
        val result = flow.first()

        // Then
        assertEquals(expectedValue, result)
    }

    @Test(expected = IllegalStateException::class)
    fun `observeNonNullResult throws exception when value is null`() = runTest {
        // Given
        val key = "test_key"
        val savedStateHandle = SavedStateHandle()
        // No value set, so it will be null

        // When
        val flow = navigationResultManager.observeNonNullResult<String>(savedStateHandle, key)
        flow.first() // This should throw

        // Then - exception should be thrown
    }

    @Test
    fun `consumeResult retrieves and removes value from SavedStateHandle`() {
        // Given
        val key = "test_key"
        val expectedValue = "test_value"
        val savedStateHandle = SavedStateHandle()
        savedStateHandle[key] = expectedValue

        // When
        val result = navigationResultManager.consumeResult<String>(savedStateHandle, key)

        // Then
        assertEquals(expectedValue, result)
        assertFalse("Key should be removed", savedStateHandle.contains(key))
    }

    @Test
    fun `consumeResult returns null when key not found`() {
        // Given
        val key = "test_key"
        val savedStateHandle = SavedStateHandle()

        // When
        val result = navigationResultManager.consumeResult<String>(savedStateHandle, key)

        // Then
        assertNull(result)
    }

    @Test
    fun `hasResult returns true when key exists in SavedStateHandle`() {
        // Given
        val key = "test_key"
        val value = "test_value"
        val savedStateHandle = SavedStateHandle()
        savedStateHandle[key] = value

        // When
        val hasResult = navigationResultManager.hasResult(savedStateHandle, key)

        // Then
        assertTrue(hasResult)
    }

    @Test
    fun `hasResult returns false when key does not exist in SavedStateHandle`() {
        // Given
        val key = "test_key"
        val savedStateHandle = SavedStateHandle()

        // When
        val hasResult = navigationResultManager.hasResult(savedStateHandle, key)

        // Then
        assertFalse(hasResult)
    }

    @Test
    fun `clearAllResults removes all keys from SavedStateHandle`() {
        // Given
        val savedStateHandle = SavedStateHandle()
        savedStateHandle["key1"] = "value1"
        savedStateHandle["key2"] = "value2"
        savedStateHandle["key3"] = "value3"
        
        `when`(mockNavController.currentBackStackEntry).thenReturn(mockCurrentEntry)
        `when`(mockCurrentEntry.savedStateHandle).thenReturn(savedStateHandle)

        // When
        navigationResultManager.clearAllResults(mockNavController)

        // Then
        assertFalse("key1 should be removed", savedStateHandle.contains("key1"))
        assertFalse("key2 should be removed", savedStateHandle.contains("key2"))
        assertFalse("key3 should be removed", savedStateHandle.contains("key3"))
    }

    @Test
    fun `setResultAndNavigateBack sets result and navigates back`() {
        // Given
        val key = "test_key"
        val value = "test_value"
        
        `when`(mockNavController.previousBackStackEntry).thenReturn(mockPreviousEntry)
        `when`(mockPreviousEntry.savedStateHandle).thenReturn(mockPreviousSavedStateHandle)
        `when`(mockNavController.popBackStack()).thenReturn(true)

        // When
        val result = navigationResultManager.setResultAndNavigateBack(mockNavController, key, value)

        // Then
        assertTrue("Should return true for successful navigation", result)
        verify(mockPreviousSavedStateHandle)[key] = value
        verify(mockNavController).popBackStack()
    }

    @Test
    fun `setResultAndNavigateBack returns false when navigation fails`() {
        // Given
        val key = "test_key"
        val value = "test_value"
        
        `when`(mockNavController.previousBackStackEntry).thenReturn(mockPreviousEntry)
        `when`(mockPreviousEntry.savedStateHandle).thenReturn(mockPreviousSavedStateHandle)
        `when`(mockNavController.popBackStack()).thenReturn(false)

        // When
        val result = navigationResultManager.setResultAndNavigateBack(mockNavController, key, value)

        // Then
        assertFalse("Should return false for failed navigation", result)
    }

    @Test
    fun `NavigationResultKeys constants are properly defined`() {
        // Test that all constants are non-empty strings
        assertTrue("RESULT_SUCCESS should not be empty", NavigationResultKeys.RESULT_SUCCESS.isNotEmpty())
        assertTrue("RESULT_ERROR should not be empty", NavigationResultKeys.RESULT_ERROR.isNotEmpty())
        assertTrue("RESULT_CANCELLED should not be empty", NavigationResultKeys.RESULT_CANCELLED.isNotEmpty())
        assertTrue("PROJECT_CREATED should not be empty", NavigationResultKeys.PROJECT_CREATED.isNotEmpty())
        assertTrue("MEMBER_ADDED should not be empty", NavigationResultKeys.MEMBER_ADDED.isNotEmpty())
        assertTrue("CATEGORY_CREATED should not be empty", NavigationResultKeys.CATEGORY_CREATED.isNotEmpty())
        assertTrue("FRIEND_ADDED should not be empty", NavigationResultKeys.FRIEND_ADDED.isNotEmpty())
        assertTrue("PROFILE_UPDATED should not be empty", NavigationResultKeys.PROFILE_UPDATED.isNotEmpty())
    }

    @Test
    fun `NavigationResultKeys constants are unique`() {
        // Collect all constants into a set to check for uniqueness
        val allKeys = setOf(
            NavigationResultKeys.RESULT_SUCCESS,
            NavigationResultKeys.RESULT_ERROR,
            NavigationResultKeys.RESULT_CANCELLED,
            NavigationResultKeys.PROJECT_CREATED,
            NavigationResultKeys.PROJECT_UPDATED,
            NavigationResultKeys.PROJECT_DELETED,
            NavigationResultKeys.MEMBER_ADDED,
            NavigationResultKeys.MEMBER_UPDATED,
            NavigationResultKeys.CATEGORY_CREATED,
            NavigationResultKeys.CHANNEL_CREATED,
            NavigationResultKeys.FRIEND_ADDED,
            NavigationResultKeys.PROFILE_UPDATED
        )

        // Count all constants
        val totalConstants = 12 // Update this if more constants are added

        assertEquals("All result keys should be unique", totalConstants, allKeys.size)
    }
}
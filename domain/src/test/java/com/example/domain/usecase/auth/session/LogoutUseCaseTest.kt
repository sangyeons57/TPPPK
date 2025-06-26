package com.example.domain.usecase.auth.session

import com.example.core_common.result.CustomResult
import com.example.domain.repository.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LogoutUseCaseTest {

    private lateinit var logoutUseCase: LogoutUseCase
    private lateinit var fakeAuthRepository: FakeAuthRepository

    @Before
    fun setUp() {
        fakeAuthRepository = FakeAuthRepository()
        logoutUseCase = LogoutUseCase(fakeAuthRepository)
    }

    @Test
    fun `logout success`() = runTest {
        // Given - user is logged in
        fakeAuthRepository.setUserSession(
            com.example.domain.util.TestDataBuilder.createTestUserSession()
        )

        // When
        val result = logoutUseCase()

        // Then
        assertTrue("Logout should succeed", result is CustomResult.Success)
        assertTrue("User should not be logged in after logout", !fakeAuthRepository.isLoggedIn())
    }

    @Test
    fun `logout when not logged in still succeeds`() = runTest {
        // Given - user is not logged in
        fakeAuthRepository.setUserSession(null)

        // When
        val result = logoutUseCase()

        // Then
        assertTrue("Logout should succeed even when not logged in", result is CustomResult.Success)
    }

    @Test
    fun `logout failure due to repository error`() = runTest {
        // Given
        fakeAuthRepository.setShouldThrowError(true)

        // When
        val result = logoutUseCase()

        // Then
        assertTrue("Logout should fail when repository throws error", result is CustomResult.Failure)
    }

    @Test
    fun `logout clears user session`() = runTest {
        // Given - user is logged in
        val userSession = com.example.domain.util.TestDataBuilder.createTestUserSession()
        fakeAuthRepository.setUserSession(userSession)
        assertTrue("User should be logged in initially", fakeAuthRepository.isLoggedIn())

        // When
        val result = logoutUseCase()

        // Then
        assertTrue("Logout should succeed", result is CustomResult.Success)
        assertTrue("User should not be logged in after logout", !fakeAuthRepository.isLoggedIn())
        
        // Verify session is actually cleared
        val sessionResult = fakeAuthRepository.getCurrentUserSession()
        assertTrue("Session should not exist after logout", sessionResult is CustomResult.Failure)
    }
}
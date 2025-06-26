package com.example.domain.usecase.auth.session

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.data.UserSession
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Token
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.FakeAuthRepository
import com.example.domain.repository.FakeUserRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class LoginUseCaseTest {

    private lateinit var loginUseCase: LoginUseCase
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeUserRepository: FakeUserRepository

    @Before
    fun setUp() {
        fakeAuthRepository = FakeAuthRepository()
        fakeUserRepository = FakeUserRepository()
        loginUseCase = LoginUseCase(fakeAuthRepository, fakeUserRepository)
    }

    @Test
    fun `login success`() = runTest {
        // Given
        val userId = "test_user_id"
        val userEmail = "test@example.com"
        val userPassword = "password"
        val userSession = UserSession(UserId(userId), Token("test_token"), Email(userEmail))
        val user = User.create(
            id = DocumentId(userId),
            email = UserEmail(userEmail),
            name = UserName("Test User"),
            consentTimeStamp = Instant.now()
        )

        fakeAuthRepository.setUserSession(userSession)
        fakeUserRepository.addUser(user)

        // When
        val result = loginUseCase(userEmail, userPassword)

        // Then
        assertTrue(result is CustomResult.Success)
        assertEquals(userSession, (result as CustomResult.Success).data)
    }

    @Test
    fun `login failure due to withdrawn account`() = runTest {
        // Given
        val userId = "test_user_id"
        val userEmail = "test@example.com"
        val userPassword = "password"
        val userSession = UserSession(UserId(userId), Token("test_token"), Email(userEmail))
        val user = User.create(
            id = DocumentId(userId),
            email = UserEmail(userEmail),
            name = UserName("Test User"),
            consentTimeStamp = Instant.now()
        ).apply { markAsWithdrawn() }

        fakeAuthRepository.setUserSession(userSession)
        fakeUserRepository.addUser(user)

        // When
        val result = loginUseCase(userEmail, userPassword)

        // Then
        assertTrue(result is CustomResult.Failure)
        assertTrue((result as CustomResult.Failure).error is WithdrawnAccountException)
    }

    @Test
    fun `login failure due to auth error`() = runTest {
        // Given
        val userEmail = "test@example.com"
        val userPassword = "password"
        fakeAuthRepository.setShouldThrowError(true)

        // When
        val result = loginUseCase(userEmail, userPassword)

        // Then
        assertTrue(result is CustomResult.Failure)
    }

    @Test
    fun `login failure due to user not found`() = runTest {
        // Given
        val userId = "test_user_id"
        val userEmail = "test@example.com"
        val userPassword = "password"
        val userSession = UserSession(UserId(userId), Token("test_token"), Email(userEmail))

        fakeAuthRepository.setUserSession(userSession)
        // User is not added to the fakeUserRepository

        // When
        val result = loginUseCase(userEmail, userPassword)

        // Then
        assertTrue(result is CustomResult.Failure)
    }
}
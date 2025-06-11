package com.example.domain.usecase.user

import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class WithdrawUserUseCaseImplTest {

    @Mock
    private lateinit var mockAuthRepository: AuthRepository

    @Mock
    private lateinit var mockUserRepository: UserRepository

    private lateinit var withdrawUserUseCaseImpl: WithdrawUserUseCaseImpl

    @Before
    fun setUp() {
        withdrawUserUseCaseImpl = WithdrawUserUseCaseImpl(mockAuthRepository, mockUserRepository)
    }

    @Test
    fun `invoke success should call both repositories and return success`() = runTest {
        // Arrange
        `when`(mockAuthRepository.withdrawCurrentUser()).thenReturn(Result.success(Unit))
        `when`(mockUserRepository.clearSensitiveUserDataAndMarkAsWithdrawn()).thenReturn(Result.success(Unit))

        // Act
        val result = withdrawUserUseCaseImpl()

        // Assert
        val inOrder = inOrder(mockAuthRepository, mockUserRepository)
        inOrder.verify(mockAuthRepository).withdrawCurrentUser()
        inOrder.verify(mockUserRepository).clearSensitiveUserDataAndMarkAsWithdrawn()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke when deleteCurrentUser fails should not call clearSensitiveUserData and return failure`() = runTest {
        // Arrange
        val authException = Exception("Auth error")
        `when`(mockAuthRepository.withdrawCurrentUser()).thenReturn(Result.failure(authException))

        // Act
        val result = withdrawUserUseCaseImpl()

        // Assert
        verify(mockAuthRepository).withdrawCurrentUser()
        verify(mockUserRepository, never()).clearSensitiveUserDataAndMarkAsWithdrawn()
        assertTrue(result.isFailure)
        assertEquals(authException, result.exceptionOrNull())
    }

    @Test
    fun `invoke when clearSensitiveUserData fails should call both repositories and return failure`() = runTest {
        // Arrange
        val dbException = Exception("DB error")
        `when`(mockAuthRepository.withdrawCurrentUser()).thenReturn(Result.success(Unit))
        `when`(mockUserRepository.clearSensitiveUserDataAndMarkAsWithdrawn()).thenReturn(Result.failure(dbException))

        // Act
        val result = withdrawUserUseCaseImpl()

        // Assert
        verify(mockAuthRepository).withdrawCurrentUser()
        verify(mockUserRepository).clearSensitiveUserDataAndMarkAsWithdrawn()
        assertTrue(result.isFailure)
        assertEquals(dbException, result.exceptionOrNull())
    }
}

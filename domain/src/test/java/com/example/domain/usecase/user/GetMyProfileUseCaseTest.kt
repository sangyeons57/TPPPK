package com.example.domain.usecase.user

import com.example.domain.model.User // Changed from UserProfileData
import com.example.domain.repository.base.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class GetMyProfileUseCaseTest {

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var getMyProfileUseCase: GetMyProfileUseCase

    @Before
    fun setUp() {
        getMyProfileUseCase = GetMyProfileUseCase(userRepository)
    }

    @Test
    fun `invoke calls userRepository getMyProfile and returns its result`() = runTest {
        val expectedUser = User(id = "id1", name = "Test User", email = "test@example.com") // Using User model
        val expectedResult = Result.Success(expectedUser)

        `when`(userRepository.getMyProfile()).thenReturn(expectedResult)

        val result = getMyProfileUseCase()

        assertEquals(expectedResult, result)
    }

    @Test
    fun `invoke returns error when userRepository getMyProfile fails`() = runTest {
        val exception = Exception("Network error")
        val expectedResult = Result.Error(exception, "Network error")

        `when`(userRepository.getMyProfile()).thenReturn(expectedResult)

        val result = getMyProfileUseCase()

        assertEquals(expectedResult, result)
    }
}

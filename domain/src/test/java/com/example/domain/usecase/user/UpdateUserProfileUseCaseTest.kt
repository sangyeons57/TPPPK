package com.example.domain.usecase.user

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
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class UpdateUserProfileUseCaseTest {

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var updateUserProfileUseCase: UpdateUserProfileUseCase

    @Before
    fun setUp() {
        updateUserProfileUseCase = UpdateUserProfileUseCase(userRepository)
    }

    @Test
    fun `invoke calls userRepository updateUserProfile with correct params and returns its result`() = runTest {
        val params = UpdateUserProfileParams(name = "New Name", profileImageUrl = "new_image_url")
        val expectedResult = Result.Success(Unit)

        `when`(userRepository.updateUserProfile(params.name, params.profileImageUrl)).thenReturn(expectedResult)

        val result = updateUserProfileUseCase(params)

        verify(userRepository).updateUserProfile(params.name, params.profileImageUrl)
        assertEquals(expectedResult, result)
    }

    @Test
    fun `invoke returns error when userRepository updateUserProfile fails`() = runTest {
        val params = UpdateUserProfileParams(name = "New Name", profileImageUrl = "new_image_url")
        val exception = Exception("Update failed")
        val expectedResult = Result.Error(exception, "Update failed")

        `when`(userRepository.updateUserProfile(params.name, params.profileImageUrl)).thenReturn(expectedResult)

        val result = updateUserProfileUseCase(params)

        assertEquals(expectedResult, result)
    }
}

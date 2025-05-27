package com.example.domain.usecase.user

import android.net.Uri
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class UploadProfileImageUseCaseTest {

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var uploadProfileImageUseCase: UploadProfileImageUseCase

    @Before
    fun setUp() {
        uploadProfileImageUseCase = UploadProfileImageUseCase(userRepository)
    }

    @Test
    fun `invoke calls userRepository uploadProfileImage with correct URI and returns its result`() = runTest {
        val mockUri: Uri = mock()
        val expectedUrl = "http://example.com/image.jpg"
        val expectedResult = Result.Success(expectedUrl)

        `when`(userRepository.uploadProfileImage(mockUri)).thenReturn(expectedResult)

        val result = uploadProfileImageUseCase(mockUri)

        verify(userRepository).uploadProfileImage(mockUri)
        assertEquals(expectedResult, result)
    }

    @Test
    fun `invoke returns error when userRepository uploadProfileImage fails`() = runTest {
        val mockUri: Uri = mock()
        val exception = Exception("Upload failed")
        val expectedResult = Result.Error(exception, "Upload failed")

        `when`(userRepository.uploadProfileImage(mockUri)).thenReturn(expectedResult)

        val result = uploadProfileImageUseCase(mockUri)

        assertEquals(expectedResult, result)
    }
}

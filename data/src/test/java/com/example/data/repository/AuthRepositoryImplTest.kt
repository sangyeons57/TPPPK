package com.example.data.repository

import com.example.data.datasource.remote.auth.AuthRemoteDataSource
import com.example.data.model.mapper.UserMapper
import com.example.data.util.FirebaseAuthWrapper
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
class AuthRepositoryImplTest {

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    @Mock
    private lateinit var mockFirebaseUser: FirebaseUser

    @Mock
    private lateinit var mockAuthWrapper: FirebaseAuthWrapper // Not used in deleteCurrentUser but part of constructor

    @Mock
    private lateinit var mockAuthRemoteDataSource: AuthRemoteDataSource // Not used in deleteCurrentUser but part of constructor
    
    @Mock
    private lateinit var mockUserMapper: UserMapper // Not used in deleteCurrentUser but part of constructor


    private lateinit var authRepositoryImpl: AuthRepositoryImpl

    @Before
    fun setUp() {
        authRepositoryImpl = AuthRepositoryImpl(
            mockFirebaseAuth,
            mockAuthWrapper,
            mockAuthRemoteDataSource,
            mockUserMapper
        )
    }

    @Test
    fun `deleteCurrentUser success should call firebaseUser delete and return success`() = runTest {
        // Arrange
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        `when`(mockFirebaseUser.delete()).thenReturn(Tasks.forResult(null))

        // Act
        val result = authRepositoryImpl.withdrawCurrentUser()

        // Assert
        verify(mockFirebaseUser).delete()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteCurrentUser failure when user not logged in should return failure`() = runTest {
        // Arrange
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)

        // Act
        val result = authRepositoryImpl.withdrawCurrentUser()

        // Assert
        verify(mockFirebaseUser, never()).delete()
        assertTrue(result.isFailure)
        assertEquals("User not logged in", result.exceptionOrNull()?.message)
    }

    @Test
    fun `deleteCurrentUser failure when Firebase error occurs should return failure`() = runTest {
        // Arrange
        val firebaseException = Exception("Firebase delete error")
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        `when`(mockFirebaseUser.delete()).thenReturn(Tasks.forException(firebaseException))

        // Act
        val result = authRepositoryImpl.withdrawCurrentUser()

        // Assert
        verify(mockFirebaseUser).delete()
        assertTrue(result.isFailure)
        assertEquals(firebaseException, result.exceptionOrNull())
    }
}

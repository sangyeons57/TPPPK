package com.example.data.repository

import android.net.Uri
import com.example.core_common.dispatcher.DispatcherProvider
import com.example.data.datasource.remote.user.UserRemoteDataSource
import com.example.data.model.mapper.UserMapper // Keep if existing tests need it, not for new methods
import com.example.domain.model.User // Changed from UserProfileData
import com.google.firebase.auth.FirebaseAuth // Keep if existing tests need it
import com.google.firebase.firestore.FirebaseFirestore // Keep if existing tests need it
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import com.example.core_common.constants.FirestoreConstants // Added
import com.example.domain.model.AccountStatus // Added
import com.google.android.gms.tasks.Tasks // Added
import com.google.firebase.auth.FirebaseUser // Added
import com.google.firebase.firestore.CollectionReference // Added
import com.google.firebase.firestore.DocumentReference // Added
import org.junit.Assert.assertFalse // Added
import org.junit.Assert.assertNull // Added
import org.mockito.ArgumentCaptor // Added
import org.mockito.Captor // Added
import org.mockito.Mockito.anyMap // Added
import org.mockito.Mockito.never // Added

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class UserRepositoryImplTest {

    @Mock
    private lateinit var userRemoteDataSource: UserRemoteDataSource

    @Mock private lateinit var userMapper: UserMapper
    @Mock private lateinit var firestore: FirebaseFirestore
    @Mock private lateinit var firebaseAuth: FirebaseAuth
    @Mock private lateinit var firebaseUser: FirebaseUser // Added
    @Mock private lateinit var usersCollection: CollectionReference // Added
    @Mock private lateinit var userDocument: DocumentReference // Added

    @Captor
    private lateinit var updateDataCaptor: ArgumentCaptor<Map<String, Any?>> // Added

    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var userRepositoryImpl: UserRepositoryImpl

    private val testUserId = "testUserId123" // Added

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        dispatcherProvider = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
            override val unconfined = testDispatcher
        }
        userRepositoryImpl = UserRepositoryImpl(
            userRemoteDataSource,
            userMapper,
            firestore,
            firebaseAuth,
            dispatcherProvider
        )
        // Common mock setup for clearSensitiveUserDataAndMarkAsWithdrawn tests
        `when`(firestore.collection(FirestoreConstants.Collections.USERS)).thenReturn(usersCollection)
        `when`(usersCollection.document(testUserId)).thenReturn(userDocument)
        `when`(firebaseAuth.currentUser).thenReturn(firebaseUser)
        `when`(firebaseUser.uid).thenReturn(testUserId)
    }

    @Test
    fun `getMyProfile success delegates to dataSource and returns its success result`() = runTest {
        val expectedUser = User(id = "id1", name = "Test User", email = "test@example.com", profileImageUrl = "url", statusMessage = "status")
        val dataSourceResult = Result.Success(expectedUser)
        `when`(userRemoteDataSource.getMyProfile()).thenReturn(dataSourceResult)

        val result = userRepositoryImpl.getMyProfile()

        verify(userRemoteDataSource).getMyProfile()
        assertEquals(dataSourceResult, result)
        assertTrue(result is Result.Success && result.data == expectedUser)
    }

    @Test
    fun `getMyProfile failure delegates to dataSource and returns its error result`() = runTest {
        val exception = Exception("DataSource error")
        val dataSourceResult = Result.Error(exception, "DataSource error")
        `when`(userRemoteDataSource.getMyProfile()).thenReturn(dataSourceResult)

        val result = userRepositoryImpl.getMyProfile()

        verify(userRemoteDataSource).getMyProfile()
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }

    @Test
    fun `getUserProfileImageUrl success delegates to dataSource`() = runTest {
        val userId = "userId1"
        val expectedUrl = "http://example.com/image.jpg"
        val dataSourceResult = Result.Success(expectedUrl)
        `when`(userRemoteDataSource.getUserProfileImageUrl(userId)).thenReturn(dataSourceResult)

        val result = userRepositoryImpl.getUserProfileImageUrl(userId)

        verify(userRemoteDataSource).getUserProfileImageUrl(userId)
        assertEquals(dataSourceResult, result)
    }
    
    @Test
    fun `getUserProfileImageUrl failure delegates to dataSource`() = runTest {
        val userId = "userId1"
        val exception = Exception("DataSource error for image URL")
        val dataSourceResult = Result.Error(exception, "DataSource error for image URL")
        `when`(userRemoteDataSource.getUserProfileImageUrl(userId)).thenReturn(dataSourceResult)

        val result = userRepositoryImpl.getUserProfileImageUrl(userId)
        
        verify(userRemoteDataSource).getUserProfileImageUrl(userId)
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }


    @Test
    fun `updateUserProfile success delegates to dataSource`() = runTest {
        val name = "New Name"
        val imageUrl = "http://example.com/new.jpg"
        val dataSourceResult = Result.Success(Unit)
        `when`(userRemoteDataSource.updateUserProfile(name, imageUrl)).thenReturn(dataSourceResult)

        val result = userRepositoryImpl.updateUserProfile(name, imageUrl)

        verify(userRemoteDataSource).updateUserProfile(name, imageUrl)
        assertEquals(dataSourceResult, result)
    }
    
    @Test
    fun `updateUserProfile failure delegates to dataSource`() = runTest {
        val name = "New Name"
        val imageUrl = "http://example.com/new.jpg"
        val exception = Exception("DataSource error for update")
        val dataSourceResult = Result.Error(exception, "DataSource error for update")
        `when`(userRemoteDataSource.updateUserProfile(name, imageUrl)).thenReturn(dataSourceResult)

        val result = userRepositoryImpl.updateUserProfile(name, imageUrl)
        
        verify(userRemoteDataSource).updateUserProfile(name, imageUrl)
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }

    @Test
    fun `uploadProfileImage success delegates to dataSource`() = runTest {
        val mockUri: Uri = mock()
        val expectedUrl = "http://example.com/uploaded.jpg"
        val dataSourceResult = Result.Success(expectedUrl)
        `when`(userRemoteDataSource.uploadProfileImage(mockUri)).thenReturn(dataSourceResult)

        val result = userRepositoryImpl.uploadProfileImage(mockUri)

        verify(userRemoteDataSource).uploadProfileImage(mockUri)
        assertEquals(dataSourceResult, result)
    }
    
    @Test
    fun `uploadProfileImage failure delegates to dataSource`() = runTest {
        val mockUri: Uri = mock()
        val exception = Exception("DataSource error for upload")
        val dataSourceResult = Result.Error(exception, "DataSource error for upload")
        `when`(userRemoteDataSource.uploadProfileImage(mockUri)).thenReturn(dataSourceResult)

        val result = userRepositoryImpl.uploadProfileImage(mockUri)
        
        verify(userRemoteDataSource).uploadProfileImage(mockUri)
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }

    // Tests for clearSensitiveUserDataAndMarkAsWithdrawn
    @Test
    fun `clearSensitiveUserDataAndMarkAsWithdrawn success should update Firestore and return success`() = runTest {
        // Arrange
        `when`(userDocument.update(anyMap())).thenReturn(Tasks.forResult(null))

        // Act
        val result = userRepositoryImpl.clearSensitiveUserDataAndMarkAsWithdrawn()

        // Assert
        verify(userDocument).update(updateDataCaptor.capture())
        val capturedData = updateDataCaptor.value
        
        assertNull(capturedData[FirestoreConstants.UserFields.EMAIL])
        assertEquals("DEFAULT_PROFILE_IMAGE_MARKER", capturedData[FirestoreConstants.UserFields.PROFILE_IMAGE_URL])
        assertNull(capturedData[FirestoreConstants.UserFields.STATUS_MESSAGE])
        assertNull(capturedData[FirestoreConstants.UserFields.FCM_TOKEN])
        assertEquals(AccountStatus.WITHDRAWN.name, capturedData[FirestoreConstants.UserFields.ACCOUNT_STATUS])
        assertNull(capturedData[FirestoreConstants.UserFields.MEMO])
        
        assertFalse(capturedData.containsKey(FirestoreConstants.UserFields.NAME))
        assertFalse(capturedData.containsKey(FirestoreConstants.UserFields.USER_ID))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `clearSensitiveUserDataAndMarkAsWithdrawn failure when user not logged in should return failure`() = runTest {
        // Arrange
        `when`(firebaseAuth.currentUser).thenReturn(null)

        // Act
        val result = userRepositoryImpl.clearSensitiveUserDataAndMarkAsWithdrawn()

        // Assert
        verify(userDocument, never()).update(anyMap())
        assertTrue(result.isFailure)
        assertEquals("User not logged in or UID not available", result.exceptionOrNull()?.message)
    }

    @Test
    fun `clearSensitiveUserDataAndMarkAsWithdrawn failure when Firestore error occurs should return failure`() = runTest {
        // Arrange
        val firestoreException = Exception("Firestore update error")
        `when`(userDocument.update(anyMap())).thenReturn(Tasks.forException(firestoreException))

        // Act
        val result = userRepositoryImpl.clearSensitiveUserDataAndMarkAsWithdrawn()

        // Assert
        verify(userDocument).update(anyMap())
        assertTrue(result.isFailure)
        assertEquals(firestoreException, result.exceptionOrNull())
    }
}

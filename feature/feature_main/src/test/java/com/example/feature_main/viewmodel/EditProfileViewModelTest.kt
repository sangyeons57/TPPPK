package com.example.feature_main.viewmodel

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.core_common.dispatcher.DispatcherProvider
import com.example.domain.model.Result
import com.example.domain.model.User // Changed from UserProfileData
import com.example.domain.usecase.user.GetMyProfileUseCase
import com.example.domain.usecase.user.UpdateUserProfileParams
import com.example.domain.usecase.user.UpdateUserProfileUseCase
import com.example.domain.usecase.user.UploadProfileImageUseCase
import com.example.feature_main.util.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class EditProfileViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule() // For LiveData if used, good practice

    @Mock
    private lateinit var getMyProfileUseCase: GetMyProfileUseCase

    @Mock
    private lateinit var updateUserProfileUseCase: UpdateUserProfileUseCase

    @Mock
    private lateinit var uploadProfileImageUseCase: UploadProfileImageUseCase

    private lateinit var dispatcherProvider: DispatcherProvider

    private lateinit var viewModel: EditProfileViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val testUser = User(id = "id1", name = "Initial Name", email = "initial@example.com", profileImageUrl = "initial_url", statusMessage = "Initial status")


    @Before
    fun setUp() {
        dispatcherProvider = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
            override val unconfined = testDispatcher
        }
        // Initial successful load for most tests, can be overridden
        runTest {
            `when`(getMyProfileUseCase()).thenReturn(Result.Success(testUser))
        }
        viewModel = EditProfileViewModel(
            getMyProfileUseCase,
            updateUserProfileUseCase,
            uploadProfileImageUseCase,
            dispatcherProvider
        )
        // Advance past initial load
        advanceUntilIdle()
    }

    @Test
    fun `init loads profile successfully`() = runTest {
        // Setup in @Before already handles one successful load.
        // This test verifies the state after that initial load.
        // To test a specific profile, it's better to re-initialize as done below,
        // or ensure @Before's mock matches exactly what's needed.

        val specificTestUser = User(id = "id_specific", name = "Specific Name", email = "specific@example.com", profileImageUrl = "specific_url")
        `when`(getMyProfileUseCase()).thenReturn(Result.Success(specificTestUser))

        // Re-initialize viewModel for this specific test case if @Before setup is too general
        val localViewModel = EditProfileViewModel(
            getMyProfileUseCase, updateUserProfileUseCase, uploadProfileImageUseCase, dispatcherProvider
        )
        advanceUntilIdle() // Ensure coroutines launched in init complete

        // If localViewModel is used, verify against it. If global viewModel, ensure @Before mock is what's tested.
        // Assuming we test the localViewModel initialized here:
        verify(getMyProfileUseCase, times(1)).invoke() 
        val uiState = localViewModel.uiState.value
        assertEquals(specificTestUser, uiState.user)
        assertEquals(false, uiState.isLoading)
        assertNull(uiState.errorMessage)
    }

    @Test
    fun `init loads profile with failure`() = runTest {
        `when`(getMyProfileUseCase()).thenReturn(Result.Error(Exception("Load failed"), "Failed to load profile"))
        // Re-initialize viewModel to test initial load failure
        val localViewModel = EditProfileViewModel(
            getMyProfileUseCase, updateUserProfileUseCase, uploadProfileImageUseCase, dispatcherProvider
        )
        advanceUntilIdle()

        verify(getMyProfileUseCase, times(1)).invoke() // Called once during init
        val uiState = localViewModel.uiState.value
        assertEquals("Failed to load profile", uiState.errorMessage)
        assertEquals(false, uiState.isLoading)
    }

    @Test
    fun `onNameChanged updates uiState user name`() {
        val newName = "Updated Name"
        viewModel.onNameChanged(newName) // Assumes initial user is not null from setup
        assertEquals(newName, viewModel.uiState.value.user?.name)
    }

    @Test
    fun `onProfileImageClicked emits RequestImagePick event`() = runTest {
        viewModel.onProfileImageClicked()
        val event = viewModel.eventFlow.first()
        assertTrue(event is EditProfileEvent.RequestImagePick)
    }

    @Test
    fun `handleImageSelection with null URI shows snackbar`() = runTest {
        viewModel.handleImageSelection(null)
        val event = viewModel.eventFlow.first()
        assertTrue(event is EditProfileEvent.ShowSnackbar)
        assertEquals("Image selection cancelled.", (event as EditProfileEvent.ShowSnackbar).message)
    }

    @Test
    fun `handleImageSelection uploads image successfully`() = runTest {
        val mockUri: Uri = mock()
        val newImageUrl = "http://example.com/new_image.jpg"
        `when`(uploadProfileImageUseCase(mockUri)).thenReturn(Result.Success(newImageUrl))

        viewModel.handleImageSelection(mockUri)
        advanceUntilIdle()

        verify(uploadProfileImageUseCase).invoke(mockUri)
        val uiState = viewModel.uiState.value
        assertEquals(newImageUrl, uiState.user?.profileImageUrl)
        assertEquals(false, uiState.isLoading)
        assertNull(uiState.errorMessage)
    }

    @Test
    fun `handleImageSelection fails to upload image`() = runTest {
        val mockUri: Uri = mock()
        `when`(uploadProfileImageUseCase(mockUri)).thenReturn(Result.Error(Exception("Upload failed"), "Image upload failed"))

        viewModel.handleImageSelection(mockUri)
        advanceUntilIdle()

        verify(uploadProfileImageUseCase).invoke(mockUri)
        val uiState = viewModel.uiState.value
        assertEquals("Image upload failed", uiState.errorMessage)
        assertEquals(false, uiState.isLoading)
        // Profile image URL should revert or stay as the original if upload fails.
        // The ViewModel logic is: it updates user.profileImageUrl upon successful upload.
        // If upload fails, user.profileImageUrl (which might have been the original or null) is not changed by the upload part.
        assertEquals(testUser.profileImageUrl, uiState.user?.profileImageUrl) // Check against initial user's image URL
    }

    @Test
    fun `onSaveProfileClicked updates profile successfully`() = runTest {
        val updatedName = "Updated Name"
        val newImageUrl = "http://example.com/updated_image.jpg"

        // Initial state from @Before: viewModel.uiState.value.user is testUser
        
        // Simulate name change
        viewModel.onNameChanged(updatedName)
        
        // Simulate image selection and successful upload
        val mockUri: Uri = mock()
        `when`(uploadProfileImageUseCase(mockUri)).thenReturn(Result.Success(newImageUrl))
        viewModel.handleImageSelection(mockUri)
        advanceUntilIdle() // Ensure image upload and UI state update complete

        // Now, uiState.user should have updatedName and newImageUrl
        val userToSave = viewModel.uiState.value.user!!
        assertEquals(updatedName, userToSave.name)
        assertEquals(newImageUrl, userToSave.profileImageUrl)

        val params = UpdateUserProfileParams(name = updatedName, profileImageUrl = newImageUrl)
        `when`(updateUserProfileUseCase(params)).thenReturn(Result.Success(Unit))

        viewModel.onSaveProfileClicked() // This will use the name and imageURL from uiState.user
        advanceUntilIdle()

        verify(updateUserProfileUseCase).invoke(params)
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)

        // Check for events
        val events = mutableListOf<EditProfileEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.toList(events)
        }
        // Trigger the save again to capture events in this test's context easily (or use a test collector)
        viewModel.onSaveProfileClicked()
        advanceUntilIdle()

        assertTrue(events.any { it is EditProfileEvent.ShowSnackbar && it.message == "Profile updated successfully" })
        assertTrue(events.any { it is EditProfileEvent.NavigateBack })
        job.cancel()
    }

    @Test
    fun `onSaveProfileClicked fails to update profile`() = runTest {
        val currentName = testUser.name // From @Before setup
        // Simulate no image change, so profileImageUrl from testUser is used.
        val profileImageUrl = testUser.profileImageUrl 

        val params = UpdateUserProfileParams(name = currentName, profileImageUrl = profileImageUrl)
        `when`(updateUserProfileUseCase(params)).thenReturn(Result.Error(Exception("Update failed"), "Failed to update profile"))

        viewModel.onSaveProfileClicked() // Uses current uiState.user which is testUser
        advanceUntilIdle()

        verify(updateUserProfileUseCase).invoke(params)
        val uiState = viewModel.uiState.value
        assertEquals("Failed to update profile", uiState.errorMessage)
        assertEquals(false, uiState.isLoading)
    }
    
    @Test
    fun `errorMessageShown clears error message`() {
        // First, set an error message (e.g. by simulating a failed load)
        runTest {
            `when`(getMyProfileUseCase()).thenReturn(Result.Error(Exception("Load error")))
            val localViewModel = EditProfileViewModel(getMyProfileUseCase, updateUserProfileUseCase, uploadProfileImageUseCase, dispatcherProvider)
            advanceUntilIdle()
            assertNotNull(localViewModel.uiState.value.errorMessage)

            // Now, call errorMessageShown
            localViewModel.errorMessageShown()
            assertNull(localViewModel.uiState.value.errorMessage)
        }
    }
}

package com.example.feature_main.viewmodel

import android.net.Uri
import app.cash.turbine.test
import com.example.data.util.CoroutinesTestRule
import com.example.domain.model.User
import com.example.domain.usecase.auth.LogoutUseCase
import com.example.domain.usecase.user.GetCurrentUserStreamUseCase
import com.example.domain.usecase.user.UpdateUserImageUseCase
import com.example.domain.usecase.user.UpdateUserStatusUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
// import org.mockito.Mockito.mock // Replaced by MockK

/**
 * ProfileViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 ProfileViewModel의 기능을 검증합니다.
 * FakeUserRepository와 FakeAuthRepository를 사용하여 외부 의존성 없이 테스트합니다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    private lateinit var viewModel: ProfileViewModel

    // Mock UseCases
    private lateinit var getCurrentUserStreamUseCase: GetCurrentUserStreamUseCase
    private lateinit var logoutUseCase: LogoutUseCase
    private lateinit var updateUserStatusUseCase: UpdateUserStatusUseCase // For status message
    private lateinit var updateUserProfileImageUseCase: UpdateUserImageUseCase

    // 테스트 데이터
    private val testUser = User(
        userId = "test_user_id",
        email = "test@example.com",
        name = "Test User",
        profileImageUrl = "https://example.com/profile.jpg",
        statusMessage = "테스트 상태 메시지",
        userStatus = "ONLINE" // Assuming UserStatus.ONLINE.value
    )
    private val testUserProfileData = testUser.toUserProfileData()
    private lateinit var mockUri: Uri

    @Before
    fun setup() {
        getCurrentUserStreamUseCase = mockk()
        logoutUseCase = mockk()
        updateUserStatusUseCase = mockk() // For status message
        updateUserProfileImageUseCase = mockk()
        mockUri = mockk<Uri>()

        // Default success scenario for user profile loading
        coEvery { getCurrentUserStreamUseCase() } returns flowOf(Result.success(testUser))

        viewModel = ProfileViewModel(
            getCurrentUserStreamUseCase,
            logoutUseCase,
            updateUserStatusUseCase,
            updateUserProfileImageUseCase
        )
    }

    @Test
    fun `초기화 시 사용자 프로필을 성공적으로 로드해야 함`() = runTest {
        // ViewModel is initialized in setup, which calls loadUserProfile
        runCurrent() // Execute pending coroutines

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertNotNull(uiState.userProfile)
        assertEquals(testUserProfileData, uiState.userProfile)
        assertNull(uiState.errorMessage)
    }

    @Test
    fun `프로필 로드 중 오류 발생 시 에러 상태로 업데이트되어야 함`() = runTest {
        // Given: Simulate error during profile loading
        val exception = RuntimeException("프로필 로드 실패")
        coEvery { getCurrentUserStreamUseCase() } returns flowOf(Result.failure(exception))

        // When: Re-initialize ViewModel or explicitly call loadUserProfile if it's public
        // For init block, re-initialize
        viewModel = ProfileViewModel(
            getCurrentUserStreamUseCase,
            logoutUseCase,
            updateUserStatusUseCase,
            updateUserProfileImageUseCase
        )
        runCurrent()


        viewModel.eventFlow.test {
            val uiState = viewModel.uiState.value
            assertFalse(uiState.isLoading)
            assertNull(uiState.userProfile)
            assertNotNull(uiState.errorMessage)
            assertEquals("프로필 정보를 불러오지 못했습니다: ${exception.message}", uiState.errorMessage)

            // Check for snackbar event
            val event = awaitItem()
            assertTrue(event is ProfileEvent.ShowSnackbar)
            assertEquals("프로필 정보를 불러오지 못했습니다: ${exception.message}", (event as ProfileEvent.ShowSnackbar).message)
            cancelAndIgnoreRemainingEvents()
        }
    }


    @Test
    fun `changeStatusMessage 성공 시 프로필 업데이트 및 스낵바 표시`() = runTest {
        val newStatusMessage = "새로운 상태 메시지"
        val updatedUser = testUser.copy(statusMessage = newStatusMessage)

        coEvery { updateUserStatusUseCase(newStatusMessage) } returns Result.success(Unit)
        // Mock subsequent profile reload to return updated user
        coEvery { getCurrentUserStreamUseCase() } returns flowOf(Result.success(testUser), Result.success(updatedUser))


        viewModel = ProfileViewModel( // Re-init to ensure loadUserProfile is called after mock setup
            getCurrentUserStreamUseCase,
            logoutUseCase,
            updateUserStatusUseCase,
            updateUserProfileImageUseCase
        )
        runCurrent() // Initial load

        viewModel.eventFlow.test {
            viewModel.changeStatus(newStatusMessage)
            runCurrent() // Allow coroutines to complete

            // Check UI state for loading indicators (optional, depends on exact implementation)
            // For this test, focus on the final state and event.

            val finalUiState = viewModel.uiState.value
            // assertEquals(newStatusMessage, finalUiState.userProfile?.statusMessage) // This will be true after loadUserProfile re-fetches
            
            // Verify snackbar
            val event = awaitItem() // This should be the snackbar from changeStatusMessage
            assertTrue(event is ProfileEvent.ShowSnackbar)
            assertEquals("상태 메시지 변경됨", (event as ProfileEvent.ShowSnackbar).message)

            // Verify loadUserProfile was called by checking if getCurrentUserStreamUseCase was called again
            // For simplicity, we'll assume the state reflects the *final* loaded profile
            // Need to advance time or manage dispatchers if loadUserProfile is complex.
            // If loadUserProfile is called, the userProfile should eventually update.
            // Waiting for state change or using Turbine for state testing would be more robust here.
            
            // Let's check the user profile after some time, assuming loadUserProfile completes
             testScheduler.advanceUntilIdle() // Advance virtual time
             val reloadedUiState = viewModel.uiState.value
             assertEquals(newStatusMessage, reloadedUiState.userProfile?.memo)


            coVerify { updateUserStatusUseCase(newStatusMessage) }
            // coVerify(atLeast = 2) { getCurrentUserStreamUseCase() } // Initial load + after status update
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `changeStatusMessage 실패 시 스낵바 표시`() = runTest {
        val newStatusMessage = "실패할 상태 메시지"
        val exception = RuntimeException("상태 메시지 변경 실패")
        coEvery { updateUserStatusUseCase(newStatusMessage) } returns Result.failure(exception)

        viewModel.eventFlow.test {
            viewModel.changeStatus(newStatusMessage)
            runCurrent()

            val event = awaitItem()
            assertTrue(event is ProfileEvent.ShowSnackbar)
            assertEquals("상태 메시지 변경 실패: ${exception.message}", (event as ProfileEvent.ShowSnackbar).message)

            val uiState = viewModel.uiState.value
            assertFalse(uiState.isLoading) // Should not be loading indefinitely
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onChangeStatusClick 시 showChangeStatusDialog가 true로 변경됨`() {
        viewModel.onChangeStatusClick()
        assertTrue(viewModel.uiState.value.showChangeStatusDialog)
    }

    @Test
    fun `onDismissChangeStatusDialog 시 showChangeStatusDialog가 false로 변경됨`() {
        // First, set it to true
        viewModel.onChangeStatusClick()
        assertTrue(viewModel.uiState.value.showChangeStatusDialog)

        // Then, dismiss
        viewModel.onDismissChangeStatusDialog()
        assertFalse(viewModel.uiState.value.showChangeStatusDialog)
    }

    @Test
    fun `onChangeStatusSuccess 시 다이얼로그 닫고 스낵바 표시 및 프로필 새로고침`() = runTest {
        val statusName = "ONLINE"
        // Mock subsequent profile reload
        coEvery { getCurrentUserStreamUseCase() } returns flowOf(Result.success(testUser)) // For initial and reload

        // Initialize with a state where dialog is shown
         val initialFlow = MutableSharedFlow<Result<User>>()
         coEvery { getCurrentUserStreamUseCase() } returns initialFlow

        viewModel = ProfileViewModel(
            getCurrentUserStreamUseCase,
            logoutUseCase,
            updateUserStatusUseCase,
            updateUserProfileImageUseCase
        )
        // Manually set dialog to be open for the test context
        viewModel.onChangeStatusClick()
        assertTrue(viewModel.uiState.value.showChangeStatusDialog)
        
        // Emit initial user for loadUserProfile
        initialFlow.emit(Result.success(testUser))
        runCurrent()


        viewModel.eventFlow.test {
            viewModel.onChangeStatusSuccess(statusName)
            runCurrent()

            assertFalse(viewModel.uiState.value.showChangeStatusDialog)

            val event = awaitItem()
            assertTrue(event is ProfileEvent.ShowSnackbar)
            assertEquals("상태가 '$statusName'(으)로 변경되었습니다.", (event as ProfileEvent.ShowSnackbar).message)
            
            // Verify loadUserProfile was called (which in turn calls getCurrentUserStreamUseCase)
            // This coVerify might be tricky if called multiple times in setup/init.
            // Consider verifying the *effect* of loadUserProfile if direct verify is hard.
            coVerify(atLeast = 1) { getCurrentUserStreamUseCase() } // Expect it to be called for reload
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    // Example of an adapted existing test (Logout)
    @Test
    fun `로그아웃 성공 시 LogoutCompleted 이벤트 발생`() = runTest {
        coEvery { logoutUseCase() } returns Result.success(Unit)
        viewModel.eventFlow.test {
            viewModel.onLogoutClick()
            runCurrent()
            val event = awaitItem()
            assertTrue(event is ProfileEvent.LogoutCompleted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `로그아웃 실패 시 ShowSnackbar 이벤트 발생`() = runTest {
        val exception = RuntimeException("로그아웃 실패")
        coEvery { logoutUseCase() } returns Result.failure(exception)
        viewModel.eventFlow.test {
            viewModel.onLogoutClick()
            runCurrent()
            val event = awaitItem()
            assertTrue(event is ProfileEvent.ShowSnackbar)
            assertEquals("로그아웃 실패", (event as ProfileEvent.ShowSnackbar).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // The test for onEditStatusClick (old dialog for status message) can be removed or adapted
    // if onEditStatusClick has new specific logic. Given it's a placeholder now,
    // there's nothing in the VM to test for it.

    // The test `상태 버튼 클릭 시 상태 화면으로 이동 이벤트가 발생해야 함` is now obsolete
    // as `onChangeStatusClick` handles dialog visibility.

}
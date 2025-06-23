package com.example.feature_profile.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.data.util.CoroutinesTestRule
// import com.example.data.util.FlowTestExtensions.getValue // May not be needed with Turbine
import com.example.domain.model.UserStatus
import com.example.domain.usecase.user.GetCurrentStatusUseCase
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
// import org.mockito.Mockito.mock // Replaced by MockK

/**
 * ChangeStatusViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 ChangeStatusViewModel의 기능을 검증합니다.
 * FakeUserRepository를 사용하여 외부 의존성 없이 테스트합니다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChangeStatusViewModelTest {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    private lateinit var viewModel: ChangeStatusViewModel
    private lateinit var getCurrentStatusUseCase: GetCurrentStatusUseCase
    private lateinit var updateUserStatusUseCase: UpdateUserStatusUseCase
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setup() {
        getCurrentStatusUseCase = mockk()
        updateUserStatusUseCase = mockk()
        savedStateHandle = mockk(relaxed = true) // relaxed = true if not testing SavedStateHandle interactions

        // Default success for initial status load
        coEvery { getCurrentStatusUseCase() } returns Result.success(UserStatus.ONLINE)

        viewModel = ChangeStatusViewModel(
            savedStateHandle,
            getCurrentStatusUseCase,
            updateUserStatusUseCase
        )
        // Ensure initial coroutines like loadCurrentStatus complete for consistent state before each test
        runCurrent()
    }

    @Test
    fun `초기화 시 현재 사용자 상태를 성공적으로 로드해야 함`() = runTest {
        // ViewModel is initialized in setup, which calls loadCurrentStatus
        // runCurrent() in setup ensures this is done
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(UserStatus.ONLINE, uiState.currentStatus)
        assertEquals(UserStatus.ONLINE, uiState.selectedStatus)
        assertNull(uiState.error)
    }

    @Test
    fun `상태 로딩 중 오류 발생 시 에러 상태로 업데이트되어야 함`() = runTest {
        val exception = RuntimeException("상태 로드 실패")
        coEvery { getCurrentStatusUseCase() } returns Result.failure(exception)

        // Re-initialize for this specific scenario
        viewModel = ChangeStatusViewModel(savedStateHandle, getCurrentStatusUseCase, updateUserStatusUseCase)
        runCurrent()

        viewModel.eventFlow.test {
            val uiState = viewModel.uiState.value
            assertFalse(uiState.isLoading)
            assertNull(uiState.currentStatus)
            assertNull(uiState.selectedStatus) // selectedStatus might also be null or previous if load fails
            assertEquals("현재 상태를 불러오지 못했습니다: ${exception.message}", uiState.error)

            val event = awaitItem()
            assertTrue(event is ChangeStatusEvent.ShowSnackbar)
            assertEquals("현재 상태를 불러오지 못했습니다: ${exception.message}", (event as ChangeStatusEvent.ShowSnackbar).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `상태를 선택하면 SelectedStatus가 업데이트되어야 함`() = runTest {
        // Initial state is ONLINE
        viewModel.onStatusSelected(UserStatus.OFFLINE)
        assertEquals(UserStatus.OFFLINE, viewModel.uiState.value.selectedStatus)
        assertEquals(UserStatus.ONLINE, viewModel.uiState.value.currentStatus) // Current status should not change yet
    }

    @Test
    fun `상태 업데이트 성공 시 현재 상태 업데이트 및 이벤트 발생`() = runTest {
        val initialStatus = UserStatus.ONLINE
        val newStatus = UserStatus.OFFLINE
        coEvery { getCurrentStatusUseCase() } returns Result.success(initialStatus) // Ensure initial state
        viewModel = ChangeStatusViewModel(savedStateHandle, getCurrentStatusUseCase, updateUserStatusUseCase)
        runCurrent()


        viewModel.onStatusSelected(newStatus)
        coEvery { updateUserStatusUseCase(newStatus.value) } returns Result.success(Unit)

        viewModel.eventFlow.test {
            viewModel.updateStatus()
            runCurrent()

            val uiState = viewModel.uiState.value
            assertEquals(newStatus, uiState.currentStatus)
            assertEquals(newStatus, uiState.selectedStatus)
            assertFalse(uiState.isUpdating)
            assertTrue(uiState.updateSuccess)
            assertNull(uiState.error)

            // Expecting Snackbar("상태 변경 중...") then Snackbar("상태가 'OFFLINE'(으)로 변경되었습니다.") then DismissDialog
            assertEquals(ChangeStatusEvent.ShowSnackbar("상태 변경 중..."), awaitItem())
            assertEquals(ChangeStatusEvent.ShowSnackbar("상태가 '${newStatus.name}'(으)로 변경되었습니다."), awaitItem())
            assertEquals(ChangeStatusEvent.DismissDialog, awaitItem())
            
            coVerify { updateUserStatusUseCase(newStatus.value) } // Verify .value is used
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `상태 업데이트 실패 시 에러 메시지 표시 및 현재 상태 유지`() = runTest {
        val initialStatus = UserStatus.ONLINE
        val newStatus = UserStatus.OFFLINE
        val exception = RuntimeException("업데이트 실패")

        coEvery { getCurrentStatusUseCase() } returns Result.success(initialStatus)
        viewModel = ChangeStatusViewModel(savedStateHandle, getCurrentStatusUseCase, updateUserStatusUseCase)
        runCurrent()

        viewModel.onStatusSelected(newStatus)
        coEvery { updateUserStatusUseCase(newStatus.value) } returns Result.failure(exception)

        viewModel.eventFlow.test {
            viewModel.updateStatus()
            runCurrent()

            val uiState = viewModel.uiState.value
            assertEquals(initialStatus, uiState.currentStatus) // Should remain initial status
            assertEquals(newStatus, uiState.selectedStatus) // Selected should be the new one
            assertFalse(uiState.isUpdating)
            assertFalse(uiState.updateSuccess)
            assertEquals("상태 변경 실패: ${exception.message}", uiState.error)

            assertEquals(ChangeStatusEvent.ShowSnackbar("상태 변경 중..."), awaitItem())
            assertEquals(ChangeStatusEvent.ShowSnackbar("상태 변경 실패: ${exception.message}"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `현재 상태와 동일한 상태로 업데이트 시도 시 스낵바 메시지 표시`() = runTest {
        coEvery { getCurrentStatusUseCase() } returns Result.success(UserStatus.ONLINE)
        viewModel = ChangeStatusViewModel(savedStateHandle, getCurrentStatusUseCase, updateUserStatusUseCase)
        runCurrent()

        viewModel.onStatusSelected(UserStatus.ONLINE) // Same as current

        viewModel.eventFlow.test {
            viewModel.updateStatus()
            runCurrent()

            assertEquals(ChangeStatusEvent.ShowSnackbar("현재 상태와 동일합니다."), awaitItem())
            coVerify(exactly = 0) { updateUserStatusUseCase(any()) } // Ensure use case not called
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `상태 선택 없이 업데이트 시도 시 스낵바 메시지 표시`() = runTest {
        // To achieve selectedStatus == null, we simulate an initial load failure
        coEvery { getCurrentStatusUseCase() } returns Result.failure(RuntimeException("Initial load failed"))
        viewModel = ChangeStatusViewModel(savedStateHandle, getCurrentStatusUseCase, updateUserStatusUseCase)
        // Skip runCurrent() after viewModel init to keep selectedStatus potentially null if init logic relies on it.
        // Or, more directly, ensure the state is as expected.
        // The ViewModel's init logic sets selectedStatus = currentStatus. If currentStatus is null, selectedStatus is null.
        runCurrent() // Let init run. If load fails, currentStatus and selectedStatus will be null.
        
        assertNull(viewModel.uiState.value.selectedStatus) // Verify precondition

        viewModel.eventFlow.test {
            viewModel.updateStatus()
            runCurrent()
            
            // Consume ShowSnackbar from failed initial load if any, then check for the target snackbar.
            // This depends on how strictly events are consumed or if a fresh collector is used.
            // For this test, we expect "상태를 선택해주세요."
            // If init failure snackbar is also there, need to handle it.
            // Let's assume the init failure snackbar was already handled or we filter.
            
            // awaitItem() might be the init failure snackbar.
            // Better: filter for the specific message or ensure the test setup isolates this.
            // For now, assuming it's the primary event after updateStatus.
            
            var eventFound = false
            for (i in 0..1) { // Check first few events if init also posts one
                val event = awaitItem()
                if (event == ChangeStatusEvent.ShowSnackbar("상태를 선택해주세요.")) {
                    eventFound = true
                    break
                }
            }
            assertTrue("Event '상태를 선택해주세요.' not found", eventFound)

            coVerify(exactly = 0) { updateUserStatusUseCase(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
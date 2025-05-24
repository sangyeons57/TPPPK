package com.example.feature_settings.presentation

import app.cash.turbine.test
import com.example.domain.usecase.user.WithdrawUserUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SettingsViewModelTest {

    // Rule for main dispatcher substitution
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    private lateinit var mockWithdrawUserUseCase: WithdrawUserUseCase

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        viewModel = SettingsViewModel(mockWithdrawUserUseCase)
    }

    @Test
    fun `onWithdrawAccountClick should show withdrawal dialog`() = runTest {
        assertFalse(viewModel.showWithdrawalDialog.value) // Initial state
        viewModel.onWithdrawAccountClick()
        assertTrue(viewModel.showWithdrawalDialog.value)
    }

    @Test
    fun `dismissWithdrawalDialog should hide withdrawal dialog`() = runTest {
        // First show it
        viewModel.onWithdrawAccountClick()
        assertTrue(viewModel.showWithdrawalDialog.value)

        // Then dismiss
        viewModel.dismissWithdrawalDialog()
        assertFalse(viewModel.showWithdrawalDialog.value)
    }
    
    @Test
    fun `dismissWithdrawalDialog when already hidden should keep dialog hidden`() = runTest {
        assertFalse(viewModel.showWithdrawalDialog.value) // Initial state
        viewModel.dismissWithdrawalDialog()
        assertFalse(viewModel.showWithdrawalDialog.value)
    }

    @Test
    fun `confirmWithdrawal success should call use case and hide dialog`() = runTest {
        // Arrange
        `when`(mockWithdrawUserUseCase.invoke()).thenReturn(Result.success(Unit))
        viewModel.onWithdrawAccountClick() // Show dialog first

        // Act
        viewModel.confirmWithdrawal()

        // Assert
        verify(mockWithdrawUserUseCase).invoke()
        assertFalse(viewModel.showWithdrawalDialog.value)
        // TODO: Verify success event/state if implemented later
    }

    @Test
    fun `confirmWithdrawal failure should call use case and hide dialog`() = runTest {
        // Arrange
        val testException = Exception("Test withdrawal error")
        `when`(mockWithdrawUserUseCase.invoke()).thenReturn(Result.failure(testException))
        viewModel.onWithdrawAccountClick() // Show dialog first

        // Act
        viewModel.confirmWithdrawal()

        // Assert
        verify(mockWithdrawUserUseCase).invoke()
        assertFalse(viewModel.showWithdrawalDialog.value)
        // TODO: Verify error event/state if implemented later (e.g., snackbar message)
    }
}

// Helper class for main dispatcher substitution in tests
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : org.junit.rules.TestWatcher() {
    override fun starting(description: org.junit.runner.Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: org.junit.runner.Description) {
        Dispatchers.resetMain()
    }
}

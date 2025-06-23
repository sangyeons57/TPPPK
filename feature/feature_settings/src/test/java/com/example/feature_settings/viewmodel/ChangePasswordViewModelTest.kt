package com.example.feature_settings.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.data.util.CoroutinesTestRule
import com.example.data.util.FlowTestExtensions.EventCollector
import com.example.data.util.FlowTestExtensions.getValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * ChangePasswordViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 ChangePasswordViewModel의 기능을 검증합니다.
 * 외부 의존성 없이 ViewModel의 상태 및 이벤트 처리를 테스트합니다.
 */
@ExperimentalCoroutinesApi
class ChangePasswordViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: com.example.feature_change_password.viewmodel.ChangePasswordViewModel
    
    // SavedStateHandle Mock
    private lateinit var savedStateHandle: SavedStateHandle

    /**
     * 테스트 초기화
     */
    @Before
    fun setup() {
        // SavedStateHandle Mock 설정
        savedStateHandle = mock(SavedStateHandle::class.java)
        
        // ViewModel 초기화
        viewModel =
            com.example.feature_change_password.viewmodel.ChangePasswordViewModel(savedStateHandle)
    }

    /**
     * 초기 상태 테스트
     */
    @Test
    fun `초기 상태는 비어 있어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        
        // When: 초기 상태 가져오기
        val initialState = viewModel.uiState.getValue()
        
        // Then: 초기 상태 확인
        assertEquals("", initialState.currentPassword)
        assertEquals("", initialState.newPassword)
        assertEquals("", initialState.confirmPassword)
        assertFalse(initialState.isLoading)
        assertNull(initialState.currentPasswordError)
        assertNull(initialState.newPasswordError)
        assertNull(initialState.confirmPasswordError)
        assertFalse(initialState.changeSuccess)
    }

    /**
     * 현재 비밀번호 입력 테스트
     */
    @Test
    fun `현재 비밀번호 입력 시 UI 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        
        // When: 현재 비밀번호 입력
        val currentPassword = "current"
        viewModel.onCurrentPasswordChange(currentPassword)
        
        // Then: UI 상태 업데이트 확인
        val uiState = viewModel.uiState.getValue()
        assertEquals(currentPassword, uiState.currentPassword)
        assertNull(uiState.currentPasswordError) // 에러는 초기화됨
    }

    /**
     * 새 비밀번호 입력 테스트
     */
    @Test
    fun `새 비밀번호 입력 시 UI 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        
        // When: 새 비밀번호 입력
        val newPassword = "new123"
        viewModel.onNewPasswordChange(newPassword)
        
        // Then: UI 상태 업데이트 확인
        val uiState = viewModel.uiState.getValue()
        assertEquals(newPassword, uiState.newPassword)
        assertNull(uiState.newPasswordError) // 에러는 초기화됨
    }

    /**
     * 비밀번호 확인 입력 테스트
     */
    @Test
    fun `비밀번호 확인 입력 시 UI 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        
        // When: 비밀번호 확인 입력
        val confirmPassword = "new123"
        viewModel.onConfirmPasswordChange(confirmPassword)
        
        // Then: UI 상태 업데이트 확인
        val uiState = viewModel.uiState.getValue()
        assertEquals(confirmPassword, uiState.confirmPassword)
        assertNull(uiState.confirmPasswordError) // 에러는 초기화됨
    }

    /**
     * 현재 비밀번호 누락 시 유효성 검사 테스트
     */
    @Test
    fun `현재 비밀번호 누락 시 에러 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel에 현재 비밀번호 미입력
        viewModel.onNewPasswordChange("new123")
        viewModel.onConfirmPasswordChange("new123")
        
        // 이벤트 수집기 설정
        val eventCollector =
            EventCollector<com.example.feature_change_password.viewmodel.ChangePasswordEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 비밀번호 변경 시도
        viewModel.changePassword()
        
        // Then: 현재 비밀번호 에러 확인
        val uiState = viewModel.uiState.getValue()
        assertNotNull(uiState.currentPasswordError)
        assertTrue(uiState.currentPasswordError!!.contains("현재 비밀번호를 입력해주세요"))
        assertFalse(uiState.isLoading)
        assertFalse(uiState.changeSuccess)
        
        // 이벤트 발생하지 않음
        assertTrue(eventCollector.events.isEmpty())
    }

    /**
     * 새 비밀번호 길이 부족 시 유효성 검사 테스트
     */
    @Test
    fun `새 비밀번호가 6자 미만일 때 에러 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel에 새 비밀번호 짧게 입력
        viewModel.onCurrentPasswordChange("current")
        viewModel.onNewPasswordChange("new") // 3자리
        viewModel.onConfirmPasswordChange("new")
        
        // When: 비밀번호 변경 시도
        viewModel.changePassword()
        
        // Then: 새 비밀번호 길이 에러 확인
        val uiState = viewModel.uiState.getValue()
        assertNotNull(uiState.newPasswordError)
        assertTrue(uiState.newPasswordError!!.contains("6자 이상"))
        assertFalse(uiState.isLoading)
        assertFalse(uiState.changeSuccess)
    }

    /**
     * 비밀번호 확인 불일치 테스트
     */
    @Test
    fun `새 비밀번호와 확인 비밀번호가 일치하지 않을 때 에러 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel에 일치하지 않는 비밀번호 확인 입력
        viewModel.onCurrentPasswordChange("current")
        viewModel.onNewPasswordChange("newpass123")
        viewModel.onConfirmPasswordChange("newpass456") // 불일치
        
        // When: 비밀번호 변경 시도
        viewModel.changePassword()
        
        // Then: 비밀번호 불일치 에러 확인
        val uiState = viewModel.uiState.getValue()
        assertNotNull(uiState.confirmPasswordError)
        assertTrue(uiState.confirmPasswordError!!.contains("일치하지 않습니다"))
        assertFalse(uiState.isLoading)
        assertFalse(uiState.changeSuccess)
    }

    /**
     * 현재 비밀번호와 새 비밀번호 동일 시 유효성 검사 테스트
     */
    @Test
    fun `현재 비밀번호와 새 비밀번호가 동일할 때 에러 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel에 현재 비밀번호와 동일한 새 비밀번호 입력
        val samePassword = "current"
        viewModel.onCurrentPasswordChange(samePassword)
        viewModel.onNewPasswordChange(samePassword)
        viewModel.onConfirmPasswordChange(samePassword)
        
        // When: 비밀번호 변경 시도
        viewModel.changePassword()
        
        // Then: 동일 비밀번호 에러 확인
        val uiState = viewModel.uiState.getValue()
        assertNotNull(uiState.newPasswordError)
        assertTrue(uiState.newPasswordError!!.contains("현재 비밀번호와 다른"))
        assertFalse(uiState.isLoading)
        assertFalse(uiState.changeSuccess)
    }

    /**
     * 비밀번호 변경 성공 테스트
     */
    @Test
    fun `유효한 입력으로 비밀번호 변경 시 성공해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel과 유효한 입력값
        viewModel.onCurrentPasswordChange("current") // 성공 시나리오 예상 값
        viewModel.onNewPasswordChange("newpass123")
        viewModel.onConfirmPasswordChange("newpass123")
        
        // 이벤트 수집기 설정
        val eventCollector =
            EventCollector<com.example.feature_change_password.viewmodel.ChangePasswordEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 비밀번호 변경 시도
        viewModel.changePassword()
        coroutinesTestRule.testCoroutineDispatcher.advanceTimeBy(1500) // 지연 시간 건너뛰기
        
        // Then: 성공 상태 확인
        val uiState = viewModel.uiState.getValue()
        assertFalse(uiState.isLoading)
        assertTrue(uiState.changeSuccess)
        
        // 이벤트 확인
        assertTrue(eventCollector.events.size >= 2)
        assertTrue(eventCollector.events.any { it is com.example.feature_change_password.viewmodel.ChangePasswordEvent.ClearFocus })
        assertTrue(eventCollector.events.any {
            it is com.example.feature_change_password.viewmodel.ChangePasswordEvent.ShowSnackbar &&
            it.message.contains("비밀번호가 변경되었습니다") 
        })
    }

    /**
     * 현재 비밀번호 불일치 테스트
     */
    @Test
    fun `현재 비밀번호가 일치하지 않을 때 에러가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel과 올바르지 않은 현재 비밀번호
        viewModel.onCurrentPasswordChange("wrong") // 잘못된 비밀번호
        viewModel.onNewPasswordChange("newpass123")
        viewModel.onConfirmPasswordChange("newpass123")
        
        // 이벤트 수집기 설정
        val eventCollector =
            EventCollector<com.example.feature_change_password.viewmodel.ChangePasswordEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 비밀번호 변경 시도
        viewModel.changePassword()
        coroutinesTestRule.testCoroutineDispatcher.advanceTimeBy(1500) // 지연 시간 건너뛰기
        
        // Then: 인증 실패 상태 확인
        val uiState = viewModel.uiState.getValue()
        assertFalse(uiState.isLoading)
        assertFalse(uiState.changeSuccess)
        assertNotNull(uiState.currentPasswordError)
        assertTrue(uiState.currentPasswordError!!.contains("현재 비밀번호가 일치하지 않습니다"))
        
        // 이벤트 확인
        assertTrue(eventCollector.events.size >= 2)
        assertTrue(eventCollector.events.any { it is com.example.feature_change_password.viewmodel.ChangePasswordEvent.ClearFocus })
        assertTrue(eventCollector.events.any {
            it is com.example.feature_change_password.viewmodel.ChangePasswordEvent.ShowSnackbar &&
            it.message.contains("현재 비밀번호가 일치하지 않습니다") 
        })
    }
} 
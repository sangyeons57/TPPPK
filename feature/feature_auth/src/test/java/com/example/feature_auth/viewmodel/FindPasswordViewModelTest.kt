package com.example.feature_auth.viewmodel

import com.example.data.repository.FakeAuthRepository
import com.example.data.util.CoroutinesTestRule
import com.example.data.util.FlowTestExtensions.EventCollector
import com.example.data.util.FlowTestExtensions.getValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * FindPasswordViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 FindPasswordViewModel의 기능을 검증합니다.
 * FakeAuthRepository를 사용하여 외부 의존성 없이 테스트합니다.
 */
@ExperimentalCoroutinesApi
class FindPasswordViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: FindPasswordViewModel

    // Fake Repository
    private lateinit var fakeAuthRepository: FakeAuthRepository

    // 테스트 데이터
    private val testEmail = "test@example.com"
    private val testAuthCode = "123456"
    private val testNewPassword = "P@ssw0rd123"

    /**
     * 테스트 초기화
     */
    @Before
    fun setup() {
        // Fake Repository 초기화
        fakeAuthRepository = FakeAuthRepository()
        
        // ViewModel 초기화 (의존성 주입)
        // 현재 FindPasswordViewModel의 구현에서는 authRepository가 주석 처리되어 있으므로,
        // 테스트에서는 이 점을 고려해야 함
        viewModel = FindPasswordViewModel(/*fakeAuthRepository*/)
    }

    /**
     * 초기 상태 테스트
     */
    @Test
    fun `초기 상태는 모든 필드가 비어있고 상태값이 초기화되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel (setup에서 이미 초기화됨)

        // When: UI 상태 가져오기
        val initialState = viewModel.uiState.getValue()

        // Then: 초기 상태 확인
        assertEquals("", initialState.email)
        assertEquals("", initialState.authCode)
        assertEquals("", initialState.newPassword)
        assertEquals("", initialState.newPasswordConfirm)
        assertFalse(initialState.isPasswordVisible)
        assertFalse(initialState.isEmailSent)
        assertFalse(initialState.isEmailVerified)
        assertFalse(initialState.isLoading)
        assertNull(initialState.errorMessage)
        assertFalse(initialState.passwordChangeSuccess)
    }

    /**
     * 이메일 입력 테스트
     */
    @Test
    fun `이메일 입력 시 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 상태의 ViewModel
        val testEmail = "test@example.com"

        // When: 이메일 입력
        viewModel.onEmailChange(testEmail)

        // Then: 상태 업데이트 확인
        val state = viewModel.uiState.getValue()
        assertEquals(testEmail, state.email)
        assertNull(state.errorMessage)
    }

    /**
     * 인증코드 입력 테스트
     */
    @Test
    fun `인증코드 입력 시 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 상태의 ViewModel
        val testAuthCode = "123456"

        // When: 인증코드 입력
        viewModel.onAuthCodeChange(testAuthCode)

        // Then: 상태 업데이트 확인
        val state = viewModel.uiState.getValue()
        assertEquals(testAuthCode, state.authCode)
        assertNull(state.errorMessage)
    }

    /**
     * 새 비밀번호 입력 테스트
     */
    @Test
    fun `새 비밀번호 입력 시 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 상태의 ViewModel
        val testPassword = "P@ssw0rd123"

        // When: 새 비밀번호 입력
        viewModel.onNewPasswordChange(testPassword)

        // Then: 상태 업데이트 확인
        val state = viewModel.uiState.getValue()
        assertEquals(testPassword, state.newPassword)
        assertNull(state.errorMessage)
    }

    /**
     * 새 비밀번호 확인 입력 테스트
     */
    @Test
    fun `새 비밀번호 확인 입력 시 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 상태의 ViewModel
        val testPasswordConfirm = "P@ssw0rd123"

        // When: 새 비밀번호 확인 입력
        viewModel.onNewPasswordConfirmChange(testPasswordConfirm)

        // Then: 상태 업데이트 확인
        val state = viewModel.uiState.getValue()
        assertEquals(testPasswordConfirm, state.newPasswordConfirm)
        assertNull(state.errorMessage)
    }

    /**
     * 비밀번호 가시성 토글 테스트
     */
    @Test
    fun `비밀번호 가시성 토글 시 상태가 전환되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 상태의 ViewModel
        assertFalse(viewModel.uiState.getValue().isPasswordVisible)

        // When: 비밀번호 가시성 토글
        viewModel.onPasswordVisibilityToggle()

        // Then: 가시성 상태 변경 확인
        assertTrue(viewModel.uiState.getValue().isPasswordVisible)

        // When: 다시 토글
        viewModel.onPasswordVisibilityToggle()

        // Then: 다시 원래 상태로 돌아옴
        assertFalse(viewModel.uiState.getValue().isPasswordVisible)
    }

    /**
     * 잘못된 이메일 형식으로 인증번호 요청 테스트
     */
    @Test
    fun `잘못된 이메일 형식으로 인증번호 요청 시 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 유효하지 않은 이메일 설정
        viewModel.onEmailChange("invalid-email")

        // When: 인증번호 전송 요청
        viewModel.onSendAuthCodeClick()

        // Then: 오류 메시지 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.errorMessage)
        assertEquals("올바른 이메일 주소를 입력해주세요.", state.errorMessage)
        assertFalse(state.isEmailSent)
    }

    /**
     * 성공적인 인증번호 요청 테스트
     */
    @Test
    fun `올바른 이메일로 인증번호 요청 시 성공 상태가 설정되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 올바른 이메일 입력
        val eventCollector = EventCollector<FindPasswordEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        viewModel.onEmailChange(testEmail)

        // When: 인증번호 전송 요청
        viewModel.onSendAuthCodeClick()

        // Then: 성공 상태 확인
        val state = viewModel.uiState.getValue()
        assertTrue(state.isEmailSent)
        assertFalse(state.isLoading)
        
        // 스낵바 이벤트 발생 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is FindPasswordEvent.ShowSnackbar)
        assertEquals("인증번호가 전송되었습니다.", (event as FindPasswordEvent.ShowSnackbar).message)
    }

    /**
     * 인증코드 미입력 검증 테스트
     */
    @Test
    fun `인증코드 미입력 시 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이메일 입력 및 인증번호 전송 상태 설정
        viewModel.onEmailChange(testEmail)
        viewModel.onSendAuthCodeClick() // isEmailSent를 true로 설정
        
        // 인증번호 미입력 (빈 상태 유지)

        // When: 인증번호 확인 요청
        viewModel.onConfirmAuthCodeClick()

        // Then: 오류 메시지 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.errorMessage)
        assertEquals("인증번호를 입력해주세요.", state.errorMessage)
        assertFalse(state.isEmailVerified)
    }

    /**
     * 성공적인 인증코드 확인 테스트
     */
    @Test
    fun `올바른 인증코드 입력 시 인증 성공 상태가 설정되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 이메일, 인증코드 입력
        val eventCollector = EventCollector<FindPasswordEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        viewModel.onEmailChange(testEmail)
        viewModel.onSendAuthCodeClick() // isEmailSent를 true로 설정
        viewModel.onAuthCodeChange(testAuthCode)

        // When: 인증번호 확인 요청
        viewModel.onConfirmAuthCodeClick()

        // Then: 성공 상태 확인
        val state = viewModel.uiState.getValue()
        assertTrue(state.isEmailVerified)
        assertFalse(state.isLoading)
        
        // 스낵바 이벤트 발생 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.last() // 마지막 이벤트(인증 성공 메시지)
        assertTrue(event is FindPasswordEvent.ShowSnackbar)
        assertEquals("인증되었습니다. 새 비밀번호를 입력하세요.", (event as FindPasswordEvent.ShowSnackbar).message)
    }

    /**
     * 비밀번호 미입력 검증 테스트
     */
    @Test
    fun `새 비밀번호 미입력 시 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이메일 인증까지 완료된 상태 설정
        viewModel.onEmailChange(testEmail)
        viewModel.onSendAuthCodeClick()
        viewModel.onAuthCodeChange(testAuthCode)
        viewModel.onConfirmAuthCodeClick()
        
        // 비밀번호 미입력 (빈 상태 유지)

        // When: 비밀번호 변경 요청
        viewModel.onChangePasswordClick()

        // Then: 오류 메시지 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.errorMessage)
        assertEquals("새 비밀번호를 모두 입력해주세요.", state.errorMessage)
        assertFalse(state.passwordChangeSuccess)
    }

    /**
     * 비밀번호 불일치 테스트
     */
    @Test
    fun `새 비밀번호와 확인이 일치하지 않을 때 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이메일 인증까지 완료된 상태 및 불일치하는 비밀번호 설정
        viewModel.onEmailChange(testEmail)
        viewModel.onSendAuthCodeClick()
        viewModel.onAuthCodeChange(testAuthCode)
        viewModel.onConfirmAuthCodeClick()
        
        viewModel.onNewPasswordChange(testNewPassword)
        viewModel.onNewPasswordConfirmChange("DifferentPassword123!")

        // When: 비밀번호 변경 요청
        viewModel.onChangePasswordClick()

        // Then: 오류 메시지 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.errorMessage)
        assertEquals("새 비밀번호가 일치하지 않습니다.", state.errorMessage)
        assertFalse(state.passwordChangeSuccess)
    }

    /**
     * 성공적인 비밀번호 변경 테스트
     */
    @Test
    fun `올바른 정보로 비밀번호 변경 시 성공 상태가 설정되고 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 모든 필수 정보 입력
        val eventCollector = EventCollector<FindPasswordEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        viewModel.onEmailChange(testEmail)
        viewModel.onSendAuthCodeClick()
        viewModel.onAuthCodeChange(testAuthCode)
        viewModel.onConfirmAuthCodeClick()
        viewModel.onNewPasswordChange(testNewPassword)
        viewModel.onNewPasswordConfirmChange(testNewPassword)

        // When: 비밀번호 변경 요청
        viewModel.onChangePasswordClick()

        // Then: 성공 상태 확인
        val state = viewModel.uiState.getValue()
        assertTrue(state.passwordChangeSuccess)
        assertFalse(state.isLoading)
        
        // 이벤트 발생 확인 (최소 2개: 스낵바 메시지 + 네비게이션)
        assertTrue(eventCollector.events.size >= 2)
        
        // 스낵바 이벤트 확인
        val snackbarEvent = eventCollector.events.findLast { it is FindPasswordEvent.ShowSnackbar }
        assertNotNull(snackbarEvent)
        assertEquals("비밀번호가 성공적으로 변경되었습니다.", (snackbarEvent as FindPasswordEvent.ShowSnackbar).message)
        
        // 네비게이션 이벤트 확인
        val navigationEvent = eventCollector.events.findLast { it is FindPasswordEvent.NavigateBack }
        assertNotNull(navigationEvent)
        assertTrue(navigationEvent is FindPasswordEvent.NavigateBack)
    }
} 
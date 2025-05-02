package com.example.feature_auth.viewmodel

import com.example.data.repository.FakeAuthRepository
import com.example.data.util.CoroutinesTestRule
import com.example.data.util.FlowTestExtensions.EventCollector
import com.example.data.util.FlowTestExtensions.getValue
import com.example.domain.model.LoginFormFocusTarget
import com.example.domain.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * LoginViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 LoginViewModel의 기능을 검증합니다.
 * FakeAuthRepository를 사용하여 외부 의존성 없이 테스트합니다.
 */
@ExperimentalCoroutinesApi
class LoginViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: LoginViewModel

    // Fake Repository
    private lateinit var fakeAuthRepository: FakeAuthRepository

    // 테스트 데이터
    private val testUser = User(
        userId = "test_user_id",
        email = "test@example.com",
        name = "Test User",
        profileImageUrl = null,
        statusMessage = null
    )
    private val testPassword = "password123"

    /**
     * 테스트 초기화
     */
    @Before
    fun setup() {
        // Fake Repository 초기화
        fakeAuthRepository = FakeAuthRepository()
        
        // 테스트 데이터 설정
        fakeAuthRepository.addUser(testUser, testPassword)

        // ViewModel 초기화 (의존성 주입)
        viewModel = LoginViewModel(fakeAuthRepository)
    }

    /**
     * 초기 상태 테스트
     */
    @Test
    fun `초기 상태는 모든 필드가 비어있고 로그인 버튼이 비활성화되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel (setup에서 이미 초기화됨)

        // When: UI 상태 가져오기
        val initialState = viewModel.uiState.getValue()

        // Then: 초기 상태 확인
        assertEquals("", initialState.email)
        assertEquals("", initialState.password)
        assertFalse(initialState.isPasswordVisible)
        assertFalse(initialState.isLoginEnabled)
        assertFalse(initialState.isLoading)
        assertNull(initialState.emailError)
        assertNull(initialState.passwordError)
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
        assertNull(state.emailError)
        // 비밀번호가 비어있으므로 로그인 버튼 비활성화 상태 유지
        assertFalse(state.isLoginEnabled)
    }

    /**
     * 비밀번호 입력 테스트
     */
    @Test
    fun `비밀번호 입력 시 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 상태의 ViewModel
        val testPassword = "password123"

        // When: 비밀번호 입력
        viewModel.onPasswordChange(testPassword)

        // Then: 상태 업데이트 확인
        val state = viewModel.uiState.getValue()
        assertEquals(testPassword, state.password)
        assertNull(state.passwordError)
        // 이메일이 비어있으므로 로그인 버튼 비활성화 상태 유지
        assertFalse(state.isLoginEnabled)
    }

    /**
     * 모든 필드 입력 시 로그인 버튼 활성화 테스트
     */
    @Test
    fun `이메일과 비밀번호가 모두 입력되면 로그인 버튼이 활성화되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 상태의 ViewModel
        val testEmail = "test@example.com"
        val testPassword = "password123"

        // When: 이메일과 비밀번호 입력
        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword)

        // Then: 로그인 버튼 활성화 확인
        val state = viewModel.uiState.getValue()
        assertTrue(state.isLoginEnabled)
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
     * 유효하지 않은 이메일 입력 시 로그인 실패 테스트
     */
    @Test
    fun `유효하지 않은 이메일 형식으로 로그인 시도 시 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 유효하지 않은 이메일과 비밀번호 설정
        viewModel.onEmailChange("invalid-email")
        viewModel.onPasswordChange("password123")

        // When: 로그인 시도
        viewModel.onLoginClick()

        // Then: 이메일 에러 메시지 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.emailError)
        assertEquals("올바른 이메일 형식이 아닙니다.", state.emailError)
    }

    /**
     * 비밀번호 누락 시 로그인 실패 테스트
     */
    @Test
    fun `비밀번호를 입력하지 않고 로그인 시도 시 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이메일만 설정
        viewModel.onEmailChange("test@example.com")

        // When: 로그인 시도
        viewModel.onLoginClick()

        // Then: 비밀번호 에러 메시지 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.passwordError)
        assertEquals("비밀번호를 입력해주세요.", state.passwordError)
    }

    /**
     * 올바른 자격 증명으로 로그인 성공 테스트
     */
    @Test
    fun `올바른 이메일과 비밀번호로 로그인 시 성공 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 올바른 이메일/비밀번호 입력
        val eventCollector = EventCollector<LoginEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        viewModel.onEmailChange(testUser.email)
        viewModel.onPasswordChange(testPassword)

        // When: 로그인 시도
        viewModel.onLoginClick()

        // Then: 로그인 성공 이벤트 발생 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is LoginEvent.LoginSuccess)
        assertEquals(testUser.userId, (event as LoginEvent.LoginSuccess).userId)
    }

    /**
     * 로그인 에러 테스트
     */
    @Test
    fun `로그인 중 오류 발생 시 스낵바 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 레포지토리 에러 설정
        val eventCollector = EventCollector<LoginEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        fakeAuthRepository.setShouldSimulateError(true)
        
        viewModel.onEmailChange(testUser.email)
        viewModel.onPasswordChange(testPassword)

        // When: 로그인 시도
        viewModel.onLoginClick()

        // Then: 스낵바 이벤트 발생 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is LoginEvent.ShowSnackbar)
        assertTrue((event as LoginEvent.ShowSnackbar).message.isNotEmpty())
    }

    /**
     * 잘못된 비밀번호로 로그인 실패 테스트
     */
    @Test
    fun `잘못된 비밀번호로 로그인 시 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 올바른 이메일/잘못된 비밀번호 입력
        val eventCollector = EventCollector<LoginEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        viewModel.onEmailChange(testUser.email)
        viewModel.onPasswordChange("wrong_password")

        // When: 로그인 시도
        viewModel.onLoginClick()

        // Then: 스낵바 이벤트 발생 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is LoginEvent.ShowSnackbar)
        assertEquals("비밀번호가 일치하지 않습니다", (event as LoginEvent.ShowSnackbar).message)
    }

    /**
     * 비밀번호 찾기 버튼 클릭 테스트
     */
    @Test
    fun `비밀번호 찾기 버튼 클릭 시 NavigateToFindPassword 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정
        val eventCollector = EventCollector<LoginEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)

        // When: 비밀번호 찾기 버튼 클릭
        viewModel.onFindPasswordClick()

        // Then: 이벤트 발생 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is LoginEvent.NavigateToFindPassword)
    }

    /**
     * 회원가입 버튼 클릭 테스트
     */
    @Test
    fun `회원가입 버튼 클릭 시 NavigateToSignUp 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정
        val eventCollector = EventCollector<LoginEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)

        // When: 회원가입 버튼 클릭
        viewModel.onSignUpClick()

        // Then: 이벤트 발생 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is LoginEvent.NavigateToSignUp)
    }

    /**
     * 이메일 포커스 요청 테스트
     */
    @Test
    fun `이메일 유효성 검사 실패 시 이메일 필드로 포커스 이동 요청이 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 유효하지 않은 이메일 입력
        val eventCollector = EventCollector<LoginEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        viewModel.onEmailChange("invalid-email")
        viewModel.onPasswordChange("password123")

        // When: 로그인 시도
        viewModel.onLoginClick()

        // Then: 포커스 이동 이벤트 발생 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is LoginEvent.RequestFocus)
        assertEquals(LoginFormFocusTarget.EMAIL, (event as LoginEvent.RequestFocus).target)
    }

    /**
     * 비밀번호 포커스 요청 테스트
     */
    @Test
    fun `비밀번호 유효성 검사 실패 시 비밀번호 필드로 포커스 이동 요청이 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 비밀번호 누락
        val eventCollector = EventCollector<LoginEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        viewModel.onEmailChange("valid@example.com")
        // 비밀번호 입력하지 않음

        // When: 로그인 시도
        viewModel.onLoginClick()

        // Then: 포커스 이동 이벤트 발생 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is LoginEvent.RequestFocus)
        assertEquals(LoginFormFocusTarget.PASSWORD, (event as LoginEvent.RequestFocus).target)
    }
} 
package com.example.feature_auth.viewmodel

import androidx.compose.ui.focus.FocusState
import com.example.data.repository.FakeAuthRepository
import com.example.data.util.CoroutinesTestRule
import com.example.data.util.FlowTestExtensions.EventCollector
import com.example.data.util.FlowTestExtensions.getValue
import com.example.domain.model.SignUpFormFocusTarget
import com.example.domain.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * SignUpViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 SignUpViewModel의 기능을 검증합니다.
 * FakeAuthRepository를 사용하여 외부 의존성 없이 테스트합니다.
 */
@ExperimentalCoroutinesApi
class SignUpViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: SignUpViewModel

    // Fake Repository
    private lateinit var fakeAuthRepository: FakeAuthRepository

    // 테스트 데이터
    private val testUser = User(
        userId = "test_user_id",
        email = "new_user@example.com",
        name = "New User",
        profileImageUrl = null,
        statusMessage = null
    )
    private val testPassword = "P@ssw0rd123"

    // FocusState 목 객체
    private lateinit var focusedState: FocusState
    private lateinit var unfocusedState: FocusState

    /**
     * 테스트 초기화
     */
    @Before
    fun setup() {
        // Fake Repository 초기화
        fakeAuthRepository = FakeAuthRepository()
        
        // FocusState 목 객체 설정
        focusedState = mock(FocusState::class.java)
        unfocusedState = mock(FocusState::class.java)
        `when`(focusedState.isFocused).thenReturn(true)
        `when`(unfocusedState.isFocused).thenReturn(false)

        // ViewModel 초기화 (의존성 주입)
        viewModel = SignUpViewModel(fakeAuthRepository)
    }

    /**
     * 초기 상태 테스트
     */
    @Test
    fun `초기 상태는 모든 필드가 비어있고 에러가 없어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel (setup에서 이미 초기화됨)

        // When: UI 상태 가져오기
        val initialState = viewModel.uiState.getValue()

        // Then: 초기 상태 확인
        assertEquals("", initialState.email)
        assertEquals("", initialState.password)
        assertEquals("", initialState.passwordConfirm)
        assertEquals("", initialState.name)
        assertFalse(initialState.isPasswordVisible)
        assertFalse(initialState.isLoading)
        assertFalse(initialState.signUpSuccess)
        assertNull(initialState.emailError)
        assertNull(initialState.passwordError)
        assertNull(initialState.passwordConfirmError)
        assertNull(initialState.nameError)
        assertFalse(initialState.isEmailTouched)
        assertFalse(initialState.isPasswordTouched)
        assertFalse(initialState.isPasswordConfirmTouched)
        assertFalse(initialState.isNameTouched)
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
    }

    /**
     * 비밀번호 입력 테스트
     */
    @Test
    fun `비밀번호 입력 시 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 상태의 ViewModel
        val testPassword = "P@ssw0rd123"

        // When: 비밀번호 입력
        viewModel.onPasswordChange(testPassword)

        // Then: 상태 업데이트 확인
        val state = viewModel.uiState.getValue()
        assertEquals(testPassword, state.password)
        assertNull(state.passwordError)
    }

    /**
     * 비밀번호 확인 입력 테스트
     */
    @Test
    fun `비밀번호 확인 입력 시 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 상태의 ViewModel
        val testPasswordConfirm = "P@ssw0rd123"

        // When: 비밀번호 확인 입력
        viewModel.onPasswordConfirmChange(testPasswordConfirm)

        // Then: 상태 업데이트 확인
        val state = viewModel.uiState.getValue()
        assertEquals(testPasswordConfirm, state.passwordConfirm)
        assertNull(state.passwordConfirmError)
    }

    /**
     * 이름 입력 테스트
     */
    @Test
    fun `이름 입력 시 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 상태의 ViewModel
        val testName = "New User"

        // When: 이름 입력
        viewModel.onNameChange(testName)

        // Then: 상태 업데이트 확인
        val state = viewModel.uiState.getValue()
        assertEquals(testName, state.name)
        assertNull(state.nameError)
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
     * 이메일 포커스 테스트
     */
    @Test
    fun `이메일 필드 포커스 시 터치 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 상태의 ViewModel
        assertFalse(viewModel.uiState.getValue().isEmailTouched)

        // When: 이메일 필드 포커스
        viewModel.onEmailFocus(focusedState)

        // Then: 이메일 터치 상태 확인
        assertTrue(viewModel.uiState.getValue().isEmailTouched)
    }

    /**
     * 이메일 포커스 아웃 테스트
     */
    @Test
    fun `이메일 필드 포커스 아웃 시 유효성 검사가 수행되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이메일 필드 터치 상태 설정
        viewModel.onEmailFocus(focusedState)
        viewModel.onEmailChange("invalid-email")

        // When: 이메일 필드 포커스 아웃
        viewModel.onEmailFocus(unfocusedState)

        // Then: 유효성 검사 에러 메시지 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.emailError)
        assertEquals("올바른 이메일을 입력해주세요.", state.emailError)
    }

    /**
     * 비밀번호 유효성 검사 테스트 - 길이 부족
     */
    @Test
    fun `비밀번호가 8자 미만인 경우 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 비밀번호 필드 터치 상태 설정
        viewModel.onPasswordFocus(focusedState)
        viewModel.onPasswordChange("Pw@1")

        // When: 비밀번호 필드 포커스 아웃
        viewModel.onPasswordFocus(unfocusedState)

        // Then: 유효성 검사 에러 메시지 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.passwordError)
        assertEquals("비밀번호는 최소 8자 이상이어야 합니다.", state.passwordError)
    }

    /**
     * 비밀번호 유효성 검사 테스트 - 영문자 누락
     */
    @Test
    fun `비밀번호에 영문자가 없는 경우 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 비밀번호 필드 터치 상태 설정
        viewModel.onPasswordFocus(focusedState)
        viewModel.onPasswordChange("12345678@")

        // When: 비밀번호 필드 포커스 아웃
        viewModel.onPasswordFocus(unfocusedState)

        // Then: 유효성 검사 에러 메시지 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.passwordError)
        assertEquals("비밀번호에 최소 하나 이상의 영문자가 포함되어야 합니다.", state.passwordError)
    }

    /**
     * 비밀번호 유효성 검사 테스트 - 숫자 누락
     */
    @Test
    fun `비밀번호에 숫자가 없는 경우 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 비밀번호 필드 터치 상태 설정
        viewModel.onPasswordFocus(focusedState)
        viewModel.onPasswordChange("Password@")

        // When: 비밀번호 필드 포커스 아웃
        viewModel.onPasswordFocus(unfocusedState)

        // Then: 유효성 검사 에러 메시지 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.passwordError)
        assertEquals("비밀번호에 최소 하나 이상의 숫자가 포함되어야 합니다.", state.passwordError)
    }

    /**
     * 비밀번호 유효성 검사 테스트 - 특수문자 누락
     */
    @Test
    fun `비밀번호에 특수문자가 없는 경우 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 비밀번호 필드 터치 상태 설정
        viewModel.onPasswordFocus(focusedState)
        viewModel.onPasswordChange("Password123")

        // When: 비밀번호 필드 포커스 아웃
        viewModel.onPasswordFocus(unfocusedState)

        // Then: 유효성 검사 에러 메시지 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.passwordError)
        assertEquals("비밀번호에 최소 하나 이상의 특수문자가 포함되어야 합니다.", state.passwordError)
    }

    /**
     * 비밀번호 확인 불일치 테스트
     */
    @Test
    fun `비밀번호와 확인이 일치하지 않는 경우 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 비밀번호와 확인 필드 설정
        viewModel.onPasswordChange("P@ssw0rd123")
        viewModel.onPasswordConfirmFocus(focusedState)
        viewModel.onPasswordConfirmChange("DifferentP@ss123")

        // When: 비밀번호 확인 필드 포커스 아웃
        viewModel.onPasswordConfirmFocus(unfocusedState)

        // Then: 유효성 검사 에러 메시지 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.passwordConfirmError)
        assertEquals("비밀번호가 일치하지 않습니다.", state.passwordConfirmError)
    }

    /**
     * 이름 필드 누락 테스트
     */
    @Test
    fun `이름이 비어있는 경우 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이름 필드 터치 상태 설정
        viewModel.onNameFocus(focusedState)
        viewModel.onNameChange("")

        // When: 이름 필드 포커스 아웃
        viewModel.onNameFocus(unfocusedState)

        // Then: 유효성 검사 에러 메시지 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.nameError)
        assertEquals("이름을 입력해주세요.", state.nameError)
    }

    /**
     * 회원가입 성공 테스트
     */
    @Test
    fun `유효한 정보로 회원가입 시 성공 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 유효한 회원가입 정보 입력
        val eventCollector = EventCollector<SignUpEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        viewModel.onEmailChange(testUser.email)
        viewModel.onPasswordChange(testPassword)
        viewModel.onPasswordConfirmChange(testPassword)
        viewModel.onNameChange(testUser.name)

        // When: 회원가입 시도
        viewModel.signUp()

        // Then: 회원가입 성공 스낵바 이벤트 및 로그인 화면 이동 이벤트 확인
        assertTrue(eventCollector.events.size >= 2)
        val snackbarEvent = eventCollector.events.find { it is SignUpEvent.ShowSnackbar }
        val navigateEvent = eventCollector.events.find { it is SignUpEvent.NavigateToLogin }
        
        assertNotNull(snackbarEvent)
        assertNotNull(navigateEvent)
        assertEquals("회원가입 성공! 로그인해주세요.", (snackbarEvent as SignUpEvent.ShowSnackbar).message)
        assertTrue(navigateEvent is SignUpEvent.NavigateToLogin)
        
        // UI 상태 확인
        val finalState = viewModel.uiState.getValue()
        assertTrue(finalState.signUpSuccess)
        assertFalse(finalState.isLoading)
    }

    /**
     * 회원가입 오류 테스트
     */
    @Test
    fun `회원가입 중 오류 발생 시 스낵바 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 레포지토리 에러 설정
        val eventCollector = EventCollector<SignUpEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        fakeAuthRepository.setShouldSimulateError(true)
        
        viewModel.onEmailChange(testUser.email)
        viewModel.onPasswordChange(testPassword)
        viewModel.onPasswordConfirmChange(testPassword)
        viewModel.onNameChange(testUser.name)

        // When: 회원가입 시도
        viewModel.signUp()

        // Then: 스낵바 이벤트 발생 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is SignUpEvent.ShowSnackbar)
        assertTrue((event as SignUpEvent.ShowSnackbar).message.isNotEmpty())
        
        // UI 상태 확인
        val finalState = viewModel.uiState.getValue()
        assertFalse(finalState.signUpSuccess)
        assertFalse(finalState.isLoading)
    }

    /**
     * 이메일 중복 테스트
     */
    @Test
    fun `이미 사용 중인 이메일로 회원가입 시 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 이미 존재하는 사용자 추가
        val eventCollector = EventCollector<SignUpEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        val existingUser = User(
            userId = "existing_user_id",
            email = "existing@example.com",
            name = "Existing User",
            profileImageUrl = null,
            statusMessage = null
        )
        fakeAuthRepository.addUser(existingUser, "existing_password")
        
        viewModel.onEmailChange(existingUser.email)
        viewModel.onPasswordChange(testPassword)
        viewModel.onPasswordConfirmChange(testPassword)
        viewModel.onNameChange("New Name")

        // When: 회원가입 시도
        viewModel.signUp()

        // Then: 스낵바 이벤트 발생 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is SignUpEvent.ShowSnackbar)
        assertEquals("이미 사용 중인 이메일입니다", (event as SignUpEvent.ShowSnackbar).message)
    }

    /**
     * 잘못된 이메일 형식 회원가입 테스트
     */
    @Test
    fun `잘못된 이메일 형식으로 회원가입 시 포커스 요청이 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 잘못된 이메일 입력
        val eventCollector = EventCollector<SignUpEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        viewModel.onEmailChange("invalid-email")
        viewModel.onPasswordChange(testPassword)
        viewModel.onPasswordConfirmChange(testPassword)
        viewModel.onNameChange(testUser.name)

        // When: 회원가입 시도
        viewModel.signUp()

        // Then: 포커스 이동 이벤트 발생 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is SignUpEvent.RequestFocus)
        assertEquals(SignUpFormFocusTarget.EMAIL, (event as SignUpEvent.RequestFocus).target)
        
        // 이메일 에러 메시지 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.emailError)
    }
} 
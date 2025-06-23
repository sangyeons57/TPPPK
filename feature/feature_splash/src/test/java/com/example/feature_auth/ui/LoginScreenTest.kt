package com.example.feature_auth.ui

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.feature_login.LoginUiState
import org.junit.Rule
import org.junit.Test

/**
 * LoginScreen UI 테스트
 *
 * 이 테스트는 LoginScreen의 UI 컴포넌트 렌더링과 기본적인 상호작용을 검증합니다.
 * 테스트에서는 stateless 컴포넌트인 LoginContent를 직접 테스트합니다.
 */
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * 기본 로그인 화면 렌더링 테스트
     */
    @Test
    fun loginContent_renders_allBasicElements() {
        // Given: 기본 UI 상태
        val uiState = com.example.feature_login.LoginUiState(
            email = "",
            password = "",
            isPasswordVisible = false
        )
        
        // When: LoginContent 렌더링
        composeTestRule.setContent {
            com.example.feature_login.LoginContent(
                uiState = uiState,
                emailFocusRequester = FocusRequester(),
                passwordFocusRequester = FocusRequester(),
                onEmailChange = {},
                onPasswordChange = {},
                onPasswordVisibilityToggle = {},
                onLoginClick = {},
                onFindPasswordClick = {},
                onSignUpClick = {}
            )
        }
        
        // Then: 모든 필수 UI 요소 확인
        // 앱 제목
        composeTestRule.onNodeWithText("프로젝팅").assertIsDisplayed()
        
        // 이메일 필드
        composeTestRule.onNodeWithText("이메일").assertIsDisplayed()
        composeTestRule.onNodeWithText("이메일 주소를 입력하세요").assertIsDisplayed()
        
        // 비밀번호 필드
        composeTestRule.onNodeWithText("비밀번호").assertIsDisplayed()
        composeTestRule.onNodeWithText("비밀번호를 입력하세요").assertIsDisplayed()
        
        // 로그인 버튼
        composeTestRule.onNodeWithText("로그인").assertIsDisplayed()
        
        // 비밀번호 찾기 버튼
        composeTestRule.onNodeWithText("비밀번호 찾기").assertIsDisplayed()
        
        // 회원가입 버튼
        composeTestRule.onNodeWithText("회원가입").assertIsDisplayed()
    }
    
    /**
     * 이메일 입력 테스트
     */
    @Test
    fun loginContent_whenEmailEntered_shouldUpdateUI() {
        // Given: 이메일이 입력된 상태
        val testEmail = "test@example.com"
        val uiState = com.example.feature_login.LoginUiState(
            email = testEmail,
            password = "",
            isPasswordVisible = false
        )
        
        // When: LoginContent 렌더링
        composeTestRule.setContent {
            com.example.feature_login.LoginContent(
                uiState = uiState,
                emailFocusRequester = FocusRequester(),
                passwordFocusRequester = FocusRequester(),
                onEmailChange = {},
                onPasswordChange = {},
                onPasswordVisibilityToggle = {},
                onLoginClick = {},
                onFindPasswordClick = {},
                onSignUpClick = {}
            )
        }
        
        // Then: 이메일 필드에 값이 표시되는지 확인
        composeTestRule.onNodeWithText(testEmail).assertIsDisplayed()
    }
    
    /**
     * 비밀번호 입력 및 가시성 토글 테스트
     */
    @Test
    fun loginContent_whenPasswordVisible_shouldShowPasswordText() {
        // Given: 비밀번호가 입력되고 가시성이 켜진 상태
        val testPassword = "password123"
        val uiState = com.example.feature_login.LoginUiState(
            email = "",
            password = testPassword,
            isPasswordVisible = true
        )
        
        // When: LoginContent 렌더링
        composeTestRule.setContent {
            com.example.feature_login.LoginContent(
                uiState = uiState,
                emailFocusRequester = FocusRequester(),
                passwordFocusRequester = FocusRequester(),
                onEmailChange = {},
                onPasswordChange = {},
                onPasswordVisibilityToggle = {},
                onLoginClick = {},
                onFindPasswordClick = {},
                onSignUpClick = {}
            )
        }
        
        // Then: 비밀번호 필드에 값이 표시되는지 확인
        composeTestRule.onNodeWithText(testPassword).assertIsDisplayed()
    }
    
    /**
     * 로딩 상태 테스트
     */
    @Test
    fun loginContent_whenLoading_shouldDisableInputs() {
        // Given: 로딩 중인 상태
        val uiState = com.example.feature_login.LoginUiState(
            email = "",
            password = "",
            isLoading = true
        )
        
        // When: LoginContent 렌더링
        composeTestRule.setContent {
            com.example.feature_login.LoginContent(
                uiState = uiState,
                emailFocusRequester = FocusRequester(),
                passwordFocusRequester = FocusRequester(),
                onEmailChange = {},
                onPasswordChange = {},
                onPasswordVisibilityToggle = {},
                onLoginClick = {},
                onFindPasswordClick = {},
                onSignUpClick = {}
            )
        }
        
        // Then: 로딩 인디케이터가 표시되고 입력 필드와 버튼이 비활성화되는지 확인
        composeTestRule.onNodeWithContentDescription("로딩 중").assertIsDisplayed()
        composeTestRule.onNode(hasText("이메일") and isEnabled()).assertDoesNotExist()
        composeTestRule.onNode(hasText("비밀번호") and isEnabled()).assertDoesNotExist()
        composeTestRule.onNode(hasText("로그인") and isEnabled()).assertDoesNotExist()
    }
    
    /**
     * 에러 메시지 표시 테스트
     */
    @Test
    fun loginContent_whenHasErrors_shouldDisplayErrorMessages() {
        // Given: 에러 메시지가 있는 상태
        val emailError = "유효한 이메일 주소를 입력해주세요."
        val passwordError = "비밀번호는 6자 이상이어야 합니다."
        val uiState = com.example.feature_login.LoginUiState(
            email = "invalid",
            password = "123",
            emailError = emailError,
            passwordError = passwordError
        )
        
        // When: LoginContent 렌더링
        composeTestRule.setContent {
            com.example.feature_login.LoginContent(
                uiState = uiState,
                emailFocusRequester = FocusRequester(),
                passwordFocusRequester = FocusRequester(),
                onEmailChange = {},
                onPasswordChange = {},
                onPasswordVisibilityToggle = {},
                onLoginClick = {},
                onFindPasswordClick = {},
                onSignUpClick = {}
            )
        }
        
        // Then: 에러 메시지가 표시되는지 확인
        composeTestRule.onNodeWithText(emailError).assertIsDisplayed()
        composeTestRule.onNodeWithText(passwordError).assertIsDisplayed()
    }
    
    /**
     * 상호작용 콜백 테스트
     */
    @Test
    fun loginContent_whenButtonsClicked_shouldTriggerCallbacks() {
        // Given: 콜백 상태 추적을 위한 변수
        var loginClicked = false
        var findPasswordClicked = false
        var signUpClicked = false
        
        val uiState = com.example.feature_login.LoginUiState(
            email = "test@example.com",
            password = "password123",
        )
        
        // When: LoginContent 렌더링
        composeTestRule.setContent {
            com.example.feature_login.LoginContent(
                uiState = uiState,
                emailFocusRequester = FocusRequester(),
                passwordFocusRequester = FocusRequester(),
                onEmailChange = {},
                onPasswordChange = {},
                onPasswordVisibilityToggle = {},
                onLoginClick = { loginClicked = true },
                onFindPasswordClick = { findPasswordClicked = true },
                onSignUpClick = { signUpClicked = true }
            )
        }
        
        // 로그인 버튼 클릭
        composeTestRule.onNodeWithText("로그인").performClick()
        
        // Then: 로그인 콜백이 호출되었는지 확인
        assert(loginClicked) { "로그인 버튼 클릭 시 콜백이 호출되지 않음" }
        
        // 비밀번호 찾기 버튼 클릭
        composeTestRule.onNodeWithText("비밀번호 찾기").performClick()
        
        // Then: 비밀번호 찾기 콜백이 호출되었는지 확인
        assert(findPasswordClicked) { "비밀번호 찾기 버튼 클릭 시 콜백이 호출되지 않음" }
        
        // 회원가입 버튼 클릭
        composeTestRule.onNodeWithText("회원가입").performClick()
        
        // Then: 회원가입 콜백이 호출되었는지 확인
        assert(signUpClicked) { "회원가입 버튼 클릭 시 콜백이 호출되지 않음" }
    }
} 
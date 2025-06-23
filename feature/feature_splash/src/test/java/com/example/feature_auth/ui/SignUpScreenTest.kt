package com.example.feature_auth.ui

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.feature_signup.SignUpUiState
import org.junit.Rule
import org.junit.Test

/**
 * SignUpScreen UI 테스트
 *
 * 이 테스트는 SignUpScreen의 UI 컴포넌트 렌더링과 기본적인 상호작용을 검증합니다.
 * 테스트에서는 stateless 컴포넌트인 SignUpContent를 직접 테스트합니다.
 */
class SignUpScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * 기본 회원가입 화면 렌더링 테스트
     */
    @Test
    fun signUpContent_renders_allBasicElements() {
        // Given: 기본 UI 상태
        val uiState = com.example.feature_signup.SignUpUiState(
            email = "",
            password = "",
            passwordConfirm = "",
            name = "",
            isPasswordVisible = false
        )
        
        // When: SignUpContent 렌더링
        composeTestRule.setContent {
            com.example.feature_signup.SignUpContent(
                uiState = uiState,
                emailFocusRequester = FocusRequester(),
                passwordFocusRequester = FocusRequester(),
                passwordConfirmFocusRequester = FocusRequester(),
                nameFocusRequester = FocusRequester(),
                onEmailChange = {},
                onPasswordChange = {},
                onPasswordConfirmChange = {},
                onNameChange = {},
                onPasswordVisibilityToggle = {},
                onSignUpClick = {},
                onNavigateBack = {},
                onEmailFocus = {},
                onPasswordFocus = {},
                onPasswordConfirmFocus = {},
                onNameFocus = {}
            )
        }
        
        // Then: 모든 필수 UI 요소 확인
        // 상단 앱바와 제목
        composeTestRule.onNodeWithText("회원가입").assertIsDisplayed()
        
        // 뒤로가기 버튼
        composeTestRule.onNodeWithContentDescription("뒤로 가기").assertIsDisplayed()
        
        // 입력 필드
        composeTestRule.onNodeWithText("이메일").assertIsDisplayed()
        composeTestRule.onNodeWithText("비밀번호 (6자 이상)").assertIsDisplayed()
        composeTestRule.onNodeWithText("비밀번호 확인").assertIsDisplayed()
        composeTestRule.onNodeWithText("이름").assertIsDisplayed()
        
        // 회원가입 버튼
        composeTestRule.onNodeWithText("회원가입하기").assertIsDisplayed()
    }
    
    /**
     * 입력 필드 업데이트 테스트
     */
    @Test
    fun signUpContent_whenFieldsPopulated_shouldShowEnteredText() {
        // Given: 값이 입력된 UI 상태
        val uiState = com.example.feature_signup.SignUpUiState(
            email = "test@example.com",
            password = "password123",
            passwordConfirm = "password123",
            name = "홍길동",
            isPasswordVisible = true
        )
        
        // When: SignUpContent 렌더링
        composeTestRule.setContent {
            com.example.feature_signup.SignUpContent(
                uiState = uiState,
                emailFocusRequester = FocusRequester(),
                passwordFocusRequester = FocusRequester(),
                passwordConfirmFocusRequester = FocusRequester(),
                nameFocusRequester = FocusRequester(),
                onEmailChange = {},
                onPasswordChange = {},
                onPasswordConfirmChange = {},
                onNameChange = {},
                onPasswordVisibilityToggle = {},
                onSignUpClick = {},
                onNavigateBack = {},
                onEmailFocus = {},
                onPasswordFocus = {},
                onPasswordConfirmFocus = {},
                onNameFocus = {}
            )
        }
        
        // Then: 입력된 값이 표시되는지 확인
        composeTestRule.onNodeWithText("test@example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("password123").assertExists() // 비밀번호는 visible 상태
        composeTestRule.onNodeWithText("홍길동").assertIsDisplayed()
    }
    
    /**
     * 비밀번호 가시성 토글 테스트
     */
    @Test
    fun signUpContent_whenPasswordVisibleFalse_shouldHidePassword() {
        // Given: 비밀번호가 숨겨진 상태
        val uiState = com.example.feature_signup.SignUpUiState(
            email = "",
            password = "password123",
            passwordConfirm = "password123",
            name = "",
            isPasswordVisible = false
        )
        
        // When: SignUpContent 렌더링
        composeTestRule.setContent {
            com.example.feature_signup.SignUpContent(
                uiState = uiState,
                emailFocusRequester = FocusRequester(),
                passwordFocusRequester = FocusRequester(),
                passwordConfirmFocusRequester = FocusRequester(),
                nameFocusRequester = FocusRequester(),
                onEmailChange = {},
                onPasswordChange = {},
                onPasswordConfirmChange = {},
                onNameChange = {},
                onPasswordVisibilityToggle = {},
                onSignUpClick = {},
                onNavigateBack = {},
                onEmailFocus = {},
                onPasswordFocus = {},
                onPasswordConfirmFocus = {},
                onNameFocus = {}
            )
        }
        
        // Then: 비밀번호 텍스트가 직접적으로 보이지 않아야 함
        composeTestRule.onNodeWithText("password123").assertDoesNotExist()
    }
    
    /**
     * 에러 메시지 표시 테스트
     */
    @Test
    fun signUpContent_whenFieldsHaveErrors_shouldDisplayErrorMessages() {
        // Given: 에러 메시지가 있는 상태
        val emailError = "유효한 이메일 주소를 입력해주세요."
        val passwordError = "비밀번호는 6자 이상이어야 합니다."
        val passwordConfirmError = "비밀번호가 일치하지 않습니다."
        val nameError = "이름을 입력해주세요."
        
        val uiState = com.example.feature_signup.SignUpUiState(
            email = "invalid",
            password = "123",
            passwordConfirm = "456",
            name = "",
            emailError = emailError,
            passwordError = passwordError,
            passwordConfirmError = passwordConfirmError,
            nameError = nameError
        )
        
        // When: SignUpContent 렌더링
        composeTestRule.setContent {
            com.example.feature_signup.SignUpContent(
                uiState = uiState,
                emailFocusRequester = FocusRequester(),
                passwordFocusRequester = FocusRequester(),
                passwordConfirmFocusRequester = FocusRequester(),
                nameFocusRequester = FocusRequester(),
                onEmailChange = {},
                onPasswordChange = {},
                onPasswordConfirmChange = {},
                onNameChange = {},
                onPasswordVisibilityToggle = {},
                onSignUpClick = {},
                onNavigateBack = {},
                onEmailFocus = {},
                onPasswordFocus = {},
                onPasswordConfirmFocus = {},
                onNameFocus = {}
            )
        }
        
        // Then: 에러 메시지가 표시되는지 확인
        composeTestRule.onNodeWithText(emailError).assertIsDisplayed()
        composeTestRule.onNodeWithText(passwordError).assertIsDisplayed()
        composeTestRule.onNodeWithText(passwordConfirmError).assertIsDisplayed()
        composeTestRule.onNodeWithText(nameError).assertIsDisplayed()
    }
    
    /**
     * 로딩 상태 테스트
     */
    @Test
    fun signUpContent_whenLoading_shouldDisableInputsAndShowIndicator() {
        // Given: 로딩 중인 상태
        val uiState = com.example.feature_signup.SignUpUiState(
            email = "test@example.com",
            password = "password123",
            passwordConfirm = "password123",
            name = "홍길동",
            isLoading = true
        )
        
        // When: SignUpContent 렌더링
        composeTestRule.setContent {
            com.example.feature_signup.SignUpContent(
                uiState = uiState,
                emailFocusRequester = FocusRequester(),
                passwordFocusRequester = FocusRequester(),
                passwordConfirmFocusRequester = FocusRequester(),
                nameFocusRequester = FocusRequester(),
                onEmailChange = {},
                onPasswordChange = {},
                onPasswordConfirmChange = {},
                onNameChange = {},
                onPasswordVisibilityToggle = {},
                onSignUpClick = {},
                onNavigateBack = {},
                onEmailFocus = {},
                onPasswordFocus = {},
                onPasswordConfirmFocus = {},
                onNameFocus = {}
            )
        }
        
        // Then: 로딩 인디케이터가 표시되고 입력 필드가 비활성화되는지 확인
        composeTestRule.onNodeWithContentDescription("로딩 중").assertIsDisplayed()
        
        // 입력 필드 비활성화 확인
        composeTestRule.onNodeWithText("이메일").onParent().assertIsNotEnabled()
        composeTestRule.onNodeWithText("비밀번호 (6자 이상)").onParent().assertIsNotEnabled()
        composeTestRule.onNodeWithText("비밀번호 확인").onParent().assertIsNotEnabled()
        composeTestRule.onNodeWithText("이름").onParent().assertIsNotEnabled()
        
        // 버튼 비활성화 확인
        composeTestRule.onNodeWithText("회원가입하기").assertIsNotEnabled()
    }
    
    /**
     * 이벤트 콜백 테스트
     */
    @Test
    fun signUpContent_whenButtonsClicked_shouldTriggerCallbacks() {
        // Given: 콜백 상태 추적을 위한 변수들
        var signUpClicked = false
        var navigateBackClicked = false
        var passwordVisibilityToggled = false
        
        val uiState = com.example.feature_signup.SignUpUiState(
            email = "test@example.com",
            password = "password123",
            passwordConfirm = "password123",
            name = "홍길동"
        )
        
        // When: SignUpContent 렌더링 및 버튼 클릭
        composeTestRule.setContent {
            com.example.feature_signup.SignUpContent(
                uiState = uiState,
                emailFocusRequester = FocusRequester(),
                passwordFocusRequester = FocusRequester(),
                passwordConfirmFocusRequester = FocusRequester(),
                nameFocusRequester = FocusRequester(),
                onEmailChange = {},
                onPasswordChange = {},
                onPasswordConfirmChange = {},
                onNameChange = {},
                onPasswordVisibilityToggle = { passwordVisibilityToggled = true },
                onSignUpClick = { signUpClicked = true },
                onNavigateBack = { navigateBackClicked = true },
                onEmailFocus = {},
                onPasswordFocus = {},
                onPasswordConfirmFocus = {},
                onNameFocus = {}
            )
        }
        
        // 회원가입하기 버튼 클릭
        composeTestRule.onNodeWithText("회원가입하기").performClick()
        
        // Then: 회원가입 콜백이 호출되었는지 확인
        assert(signUpClicked) { "회원가입하기 버튼 클릭 시 콜백이 호출되지 않음" }
        
        // 뒤로가기 아이콘 클릭
        composeTestRule.onNodeWithContentDescription("뒤로 가기").performClick()
        
        // Then: 뒤로가기 콜백이 호출되었는지 확인
        assert(navigateBackClicked) { "뒤로가기 버튼 클릭 시 콜백이 호출되지 않음" }
        
        // 비밀번호 가시성 토글 아이콘 클릭
        composeTestRule.onNodeWithContentDescription("보이기").performClick()
        
        // Then: 비밀번호 가시성 토글 콜백이 호출되었는지 확인
        assert(passwordVisibilityToggled) { "비밀번호 가시성 토글 버튼 클릭 시 콜백이 호출되지 않음" }
    }
    
    /**
     * 입력 변경 콜백 테스트
     */
    @Test
    fun signUpContent_whenInputsChanged_shouldTriggerChangeCallbacks() {
        // Given: 입력 변경 추적을 위한 변수들
        var emailChanged = false
        var passwordChanged = false
        var passwordConfirmChanged = false
        var nameChanged = false
        
        val uiState = com.example.feature_signup.SignUpUiState()
        
        // When: SignUpContent 렌더링
        composeTestRule.setContent {
            com.example.feature_signup.SignUpContent(
                uiState = uiState,
                emailFocusRequester = FocusRequester(),
                passwordFocusRequester = FocusRequester(),
                passwordConfirmFocusRequester = FocusRequester(),
                nameFocusRequester = FocusRequester(),
                onEmailChange = { emailChanged = true },
                onPasswordChange = { passwordChanged = true },
                onPasswordConfirmChange = { passwordConfirmChanged = true },
                onNameChange = { nameChanged = true },
                onPasswordVisibilityToggle = {},
                onSignUpClick = {},
                onNavigateBack = {},
                onEmailFocus = {},
                onPasswordFocus = {},
                onPasswordConfirmFocus = {},
                onNameFocus = {}
            )
        }
        
        // 이메일 필드 입력
        composeTestRule.onNodeWithText("이메일").performTextInput("test@example.com")
        
        // Then: 이메일 변경 콜백이 호출되었는지 확인
        assert(emailChanged) { "이메일 입력 시 콜백이 호출되지 않음" }
        
        // 비밀번호 필드 입력
        composeTestRule.onNodeWithText("비밀번호 (6자 이상)").performTextInput("password123")
        
        // Then: 비밀번호 변경 콜백이 호출되었는지 확인
        assert(passwordChanged) { "비밀번호 입력 시 콜백이 호출되지 않음" }
        
        // 비밀번호 확인 필드 입력
        composeTestRule.onNodeWithText("비밀번호 확인").performTextInput("password123")
        
        // Then: 비밀번호 확인 변경 콜백이 호출되었는지 확인
        assert(passwordConfirmChanged) { "비밀번호 확인 입력 시 콜백이 호출되지 않음" }
        
        // 이름 필드 입력
        composeTestRule.onNodeWithText("이름").performTextInput("홍길동")
        
        // Then: 이름 변경 콜백이 호출되었는지 확인
        assert(nameChanged) { "이름 입력 시 콜백이 호출되지 않음" }
    }
} 
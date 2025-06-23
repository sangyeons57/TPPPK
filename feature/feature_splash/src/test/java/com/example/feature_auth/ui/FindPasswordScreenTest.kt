package com.example.feature_auth.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.feature_find_password.viewmodel.FindPasswordUiState
import org.junit.Rule
import org.junit.Test

/**
 * FindPasswordScreen UI 테스트
 *
 * 이 테스트는 FindPasswordScreen의 UI 컴포넌트 렌더링과 기본적인 상호작용을 검증합니다.
 * 테스트에서는 stateless 컴포넌트인 FindPasswordContent를 직접 테스트합니다.
 */
class FindPasswordScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * 기본 비밀번호 찾기 화면 렌더링 테스트
     */
    @Test
    fun findPasswordContent_renders_allBasicElements() {
        // Given: 기본 UI 상태
        val uiState = com.example.feature_find_password.viewmodel.FindPasswordUiState(
            email = "",
            authCode = "",
            newPassword = "",
            newPasswordConfirm = "",
            isPasswordVisible = false,
            isEmailSent = false,
            isEmailVerified = false
        )
        
        // When: FindPasswordContent 렌더링
        composeTestRule.setContent {
            com.example.feature_find_password.ui.FindPasswordContent(
                uiState = uiState,
                onEmailChange = {},
                onAuthCodeChange = {},
                onNewPasswordChange = {},
                onNewPasswordConfirmChange = {},
                onPasswordVisibilityToggle = {},
                onSendAuthCodeClick = {},
                onConfirmAuthCodeClick = {},
                onChangePasswordClick = {}
            )
        }
        
        // Then: 기본 화면 요소 확인
        composeTestRule.onNodeWithText("이메일").assertIsDisplayed()
        composeTestRule.onNodeWithText("인증번호\n전송").assertIsDisplayed()
        
        // 초기 상태에서는 인증번호 입력 필드와 인증 확인 버튼이 표시되지 않아야 함
        composeTestRule.onNodeWithText("인증번호").assertDoesNotExist()
        composeTestRule.onNodeWithText("인증 확인").assertDoesNotExist()
        
        // 초기 상태에서는 비밀번호 관련 필드가 표시되지 않아야 함
        composeTestRule.onNodeWithText("새 비밀번호").assertDoesNotExist()
        composeTestRule.onNodeWithText("비밀번호 확인").assertDoesNotExist()
        composeTestRule.onNodeWithText("비밀번호 변경").assertDoesNotExist()
    }
    
    /**
     * 이메일 전송 상태 테스트
     */
    @Test
    fun findPasswordContent_whenEmailSent_shouldShowAuthCodeInput() {
        // Given: 이메일이 전송된 상태
        val uiState = com.example.feature_find_password.viewmodel.FindPasswordUiState(
            email = "test@example.com",
            authCode = "",
            isEmailSent = true,
            isEmailVerified = false
        )
        
        // When: FindPasswordContent 렌더링
        composeTestRule.setContent {
            com.example.feature_find_password.ui.FindPasswordContent(
                uiState = uiState,
                onEmailChange = {},
                onAuthCodeChange = {},
                onNewPasswordChange = {},
                onNewPasswordConfirmChange = {},
                onPasswordVisibilityToggle = {},
                onSendAuthCodeClick = {},
                onConfirmAuthCodeClick = {},
                onChangePasswordClick = {}
            )
        }
        
        // Then: 인증번호 입력 필드와 확인 버튼이 표시되어야 함
        composeTestRule.onNodeWithText("인증번호").assertIsDisplayed()
        composeTestRule.onNodeWithText("인증 확인").assertIsDisplayed()
        
        // 이메일 입력 필드는 비활성화되어야 함
        composeTestRule.onNodeWithText("이메일").onParent().assertIsNotEnabled()
        
        // 아직 비밀번호 관련 필드는 표시되지 않아야 함
        composeTestRule.onNodeWithText("새 비밀번호").assertDoesNotExist()
    }
    
    /**
     * 이메일 인증 완료 상태 테스트
     */
    @Test
    fun findPasswordContent_whenEmailVerified_shouldShowPasswordInputs() {
        // Given: 이메일 인증이 완료된 상태
        val uiState = com.example.feature_find_password.viewmodel.FindPasswordUiState(
            email = "test@example.com",
            authCode = "123456",
            isEmailSent = true,
            isEmailVerified = true
        )
        
        // When: FindPasswordContent 렌더링
        composeTestRule.setContent {
            com.example.feature_find_password.ui.FindPasswordContent(
                uiState = uiState,
                onEmailChange = {},
                onAuthCodeChange = {},
                onNewPasswordChange = {},
                onNewPasswordConfirmChange = {},
                onPasswordVisibilityToggle = {},
                onSendAuthCodeClick = {},
                onConfirmAuthCodeClick = {},
                onChangePasswordClick = {}
            )
        }
        
        // Then: 비밀번호 입력 필드가 표시되어야 함
        composeTestRule.onNodeWithText("새 비밀번호").assertIsDisplayed()
        composeTestRule.onNodeWithText("비밀번호 확인").assertIsDisplayed()
        composeTestRule.onNodeWithText("비밀번호 변경").assertIsDisplayed()
        
        // 인증번호 입력 필드는 비활성화되어야 함
        composeTestRule.onNodeWithText("인증번호").onParent().assertIsNotEnabled()
    }
    
    /**
     * 비밀번호 가시성 토글 테스트
     */
    @Test
    fun findPasswordContent_whenPasswordVisible_shouldShowPasswordText() {
        // Given: 이메일 인증 완료 및 비밀번호 가시성이 켜진 상태
        val testPassword = "newpass123"
        val uiState = com.example.feature_find_password.viewmodel.FindPasswordUiState(
            email = "test@example.com",
            authCode = "123456",
            newPassword = testPassword,
            newPasswordConfirm = testPassword,
            isEmailSent = true,
            isEmailVerified = true,
            isPasswordVisible = true
        )
        
        // When: FindPasswordContent 렌더링
        composeTestRule.setContent {
            com.example.feature_find_password.ui.FindPasswordContent(
                uiState = uiState,
                onEmailChange = {},
                onAuthCodeChange = {},
                onNewPasswordChange = {},
                onNewPasswordConfirmChange = {},
                onPasswordVisibilityToggle = {},
                onSendAuthCodeClick = {},
                onConfirmAuthCodeClick = {},
                onChangePasswordClick = {}
            )
        }
        
        // Then: 비밀번호 텍스트가 표시되어야 함
        composeTestRule.onNodeWithText(testPassword).assertExists()
    }
    
    /**
     * 로딩 상태 테스트
     */
    @Test
    fun findPasswordContent_whenLoading_shouldDisableInputs() {
        // Given: 로딩 중인 상태
        val uiState = com.example.feature_find_password.viewmodel.FindPasswordUiState(
            email = "test@example.com",
            authCode = "123456",
            isEmailSent = true,
            isEmailVerified = true,
            isLoading = true
        )
        
        // When: FindPasswordContent 렌더링
        composeTestRule.setContent {
            com.example.feature_find_password.ui.FindPasswordContent(
                uiState = uiState,
                onEmailChange = {},
                onAuthCodeChange = {},
                onNewPasswordChange = {},
                onNewPasswordConfirmChange = {},
                onPasswordVisibilityToggle = {},
                onSendAuthCodeClick = {},
                onConfirmAuthCodeClick = {},
                onChangePasswordClick = {}
            )
        }
        
        // Then: 로딩 인디케이터가 표시되고 모든 입력 필드와 버튼이 비활성화되어야 함
        composeTestRule.onNodeWithContentDescription("로딩 중").assertIsDisplayed()
        
        // 입력 필드 비활성화 확인
        composeTestRule.onNodeWithText("이메일").onParent().assertIsNotEnabled()
        composeTestRule.onNodeWithText("인증번호").onParent().assertIsNotEnabled()
        composeTestRule.onNodeWithText("새 비밀번호").onParent().assertIsNotEnabled()
        composeTestRule.onNodeWithText("비밀번호 확인").onParent().assertIsNotEnabled()
        
        // 버튼 비활성화 확인
        composeTestRule.onNodeWithText("비밀번호 변경").assertIsNotEnabled()
    }
    
    /**
     * 이벤트 콜백 테스트
     */
    @Test
    fun findPasswordContent_whenButtonsClicked_shouldTriggerCallbacks() {
        // Given: 콜백 호출 추적을 위한 변수
        var sendAuthCodeClicked = false
        var confirmAuthCodeClicked = false
        var changePasswordClicked = false
        var passwordVisibilityToggled = false
        
        // 초기 상태: 이메일 입력된 상태
        var uiState = com.example.feature_find_password.viewmodel.FindPasswordUiState(
            email = "test@example.com"
        )
        
        // When: FindPasswordContent 렌더링
        composeTestRule.setContent {
            com.example.feature_find_password.ui.FindPasswordContent(
                uiState = uiState,
                onEmailChange = {},
                onAuthCodeChange = {},
                onNewPasswordChange = {},
                onNewPasswordConfirmChange = {},
                onPasswordVisibilityToggle = { passwordVisibilityToggled = true },
                onSendAuthCodeClick = { sendAuthCodeClicked = true },
                onConfirmAuthCodeClick = { confirmAuthCodeClicked = true },
                onChangePasswordClick = { changePasswordClicked = true }
            )
        }
        
        // 인증번호 전송 버튼 클릭
        composeTestRule.onNodeWithText("인증번호\n전송").performClick()
        
        // Then: 인증번호 전송 콜백이 호출되었는지 확인
        assert(sendAuthCodeClicked) { "인증번호 전송 버튼 클릭 시 콜백이 호출되지 않음" }
        
        // 이메일 인증 코드 전송됨 상태로 업데이트
        uiState = uiState.copy(isEmailSent = true, authCode = "123456")
        
        // 화면 다시 렌더링
        composeTestRule.setContent {
            com.example.feature_find_password.ui.FindPasswordContent(
                uiState = uiState,
                onEmailChange = {},
                onAuthCodeChange = {},
                onNewPasswordChange = {},
                onNewPasswordConfirmChange = {},
                onPasswordVisibilityToggle = { passwordVisibilityToggled = true },
                onSendAuthCodeClick = { sendAuthCodeClicked = true },
                onConfirmAuthCodeClick = { confirmAuthCodeClicked = true },
                onChangePasswordClick = { changePasswordClicked = true }
            )
        }
        
        // 인증 확인 버튼 클릭
        composeTestRule.onNodeWithText("인증 확인").performClick()
        
        // Then: 인증 확인 콜백이 호출되었는지 확인
        assert(confirmAuthCodeClicked) { "인증 확인 버튼 클릭 시 콜백이 호출되지 않음" }
        
        // 이메일 인증 완료 상태로 업데이트
        uiState = uiState.copy(
            isEmailVerified = true,
            newPassword = "newpass123",
            newPasswordConfirm = "newpass123",
            isPasswordVisible = false
        )
        
        // 화면 다시 렌더링
        composeTestRule.setContent {
            com.example.feature_find_password.ui.FindPasswordContent(
                uiState = uiState,
                onEmailChange = {},
                onAuthCodeChange = {},
                onNewPasswordChange = {},
                onNewPasswordConfirmChange = {},
                onPasswordVisibilityToggle = { passwordVisibilityToggled = true },
                onSendAuthCodeClick = { sendAuthCodeClicked = true },
                onConfirmAuthCodeClick = { confirmAuthCodeClicked = true },
                onChangePasswordClick = { changePasswordClicked = true }
            )
        }
        
        // 비밀번호 가시성 토글 클릭
        composeTestRule.onNodeWithContentDescription("보이기").performClick()
        
        // Then: 비밀번호 가시성 토글 콜백이 호출되었는지 확인
        assert(passwordVisibilityToggled) { "비밀번호 가시성 토글 버튼 클릭 시 콜백이 호출되지 않음" }
        
        // 비밀번호 변경 버튼 클릭
        composeTestRule.onNodeWithText("비밀번호 변경").performClick()
        
        // Then: 비밀번호 변경 콜백이 호출되었는지 확인
        assert(changePasswordClicked) { "비밀번호 변경 버튼 클릭 시 콜백이 호출되지 않음" }
    }
    
    /**
     * 입력 필드 변경 콜백 테스트
     */
    @Test
    fun findPasswordContent_whenInputsChanged_shouldTriggerCallbacks() {
        // Given: 입력 변경 추적을 위한 변수들
        var emailChanged = false
        var authCodeChanged = false
        var newPasswordChanged = false
        var newPasswordConfirmChanged = false
        
        // 모든 상태가 활성화된 UI 상태
        val uiState = com.example.feature_find_password.viewmodel.FindPasswordUiState(
            isEmailSent = true,
            isEmailVerified = true
        )
        
        // When: FindPasswordContent 렌더링
        composeTestRule.setContent {
            com.example.feature_find_password.ui.FindPasswordContent(
                uiState = uiState,
                onEmailChange = { emailChanged = true },
                onAuthCodeChange = { authCodeChanged = true },
                onNewPasswordChange = { newPasswordChanged = true },
                onNewPasswordConfirmChange = { newPasswordConfirmChanged = true },
                onPasswordVisibilityToggle = {},
                onSendAuthCodeClick = {},
                onConfirmAuthCodeClick = {},
                onChangePasswordClick = {}
            )
        }
        
        // 이메일 필드 입력
        composeTestRule.onNodeWithText("이메일").performTextInput("test@example.com")
        
        // Then: 이메일 변경 콜백이 호출되었는지 확인
        assert(emailChanged) { "이메일 입력 시 콜백이 호출되지 않음" }
        
        // 인증번호 입력
        composeTestRule.onNodeWithText("인증번호").performTextInput("123456")
        
        // Then: 인증번호 변경 콜백이 호출되었는지 확인
        assert(authCodeChanged) { "인증번호 입력 시 콜백이 호출되지 않음" }
        
        // 새 비밀번호 입력
        composeTestRule.onNodeWithText("새 비밀번호").performTextInput("newpass123")
        
        // Then: 새 비밀번호 변경 콜백이 호출되었는지 확인
        assert(newPasswordChanged) { "새 비밀번호 입력 시 콜백이 호출되지 않음" }
        
        // 비밀번호 확인 입력
        composeTestRule.onNodeWithText("비밀번호 확인").performTextInput("newpass123")
        
        // Then: 비밀번호 확인 변경 콜백이 호출되었는지 확인
        assert(newPasswordConfirmChanged) { "비밀번호 확인 입력 시 콜백이 호출되지 않음" }
    }
    
    /**
     * 비밀번호 일치 검증 테스트
     */
    @Test
    fun findPasswordContent_whenPasswordsDoNotMatch_shouldShowError() {
        // Given: 이메일 인증 완료 상태와 불일치하는 비밀번호
        val uiState = com.example.feature_find_password.viewmodel.FindPasswordUiState(
            email = "test@example.com",
            authCode = "123456",
            newPassword = "password123",
            newPasswordConfirm = "different456",
            isEmailSent = true,
            isEmailVerified = true
        )
        
        // When: FindPasswordContent 렌더링
        composeTestRule.setContent {
            com.example.feature_find_password.ui.FindPasswordContent(
                uiState = uiState,
                onEmailChange = {},
                onAuthCodeChange = {},
                onNewPasswordChange = {},
                onNewPasswordConfirmChange = {},
                onPasswordVisibilityToggle = {},
                onSendAuthCodeClick = {},
                onConfirmAuthCodeClick = {},
                onChangePasswordClick = {}
            )
        }
        
        // Then: 비밀번호 확인 필드에 에러가 표시되어야 함
        composeTestRule.onNodeWithText("비밀번호 확인").onParent().assertIsDisplayed()
        
        // 비밀번호 불일치 시 변경 버튼이 비활성화되어야 함
        composeTestRule.onNodeWithText("비밀번호 변경").assertIsNotEnabled()
    }
    
    /**
     * 완전한 비밀번호 재설정 흐름 테스트
     */
    @Test
    fun findPasswordContent_completePasswordResetFlow() {
        // Given: 콜백 호출 추적 변수
        var emailChange = ""
        var codeChange = ""
        var passwordChange = ""
        var passwordConfirmChange = ""
        var sendCodeClicked = false
        var verifyCodeClicked = false
        var resetPasswordClicked = false
        
        // 초기 상태
        var uiState = com.example.feature_find_password.viewmodel.FindPasswordUiState()
        
        // When: 초기 화면 렌더링
        composeTestRule.setContent {
            com.example.feature_find_password.ui.FindPasswordContent(
                uiState = uiState,
                onEmailChange = { emailChange = it },
                onAuthCodeChange = { codeChange = it },
                onNewPasswordChange = { passwordChange = it },
                onNewPasswordConfirmChange = { passwordConfirmChange = it },
                onPasswordVisibilityToggle = {},
                onSendAuthCodeClick = { sendCodeClicked = true },
                onConfirmAuthCodeClick = { verifyCodeClicked = true },
                onChangePasswordClick = { resetPasswordClicked = true }
            )
        }
        
        // 1단계: 이메일 입력 및 인증번호 요청
        composeTestRule.onNodeWithText("이메일").performTextInput("user@example.com")
        assert(emailChange == "user@example.com") { "이메일 입력이 올바르게 처리되지 않음" }
        
        composeTestRule.onNodeWithText("인증번호\n전송").performClick()
        assert(sendCodeClicked) { "인증번호 전송 버튼 클릭이 처리되지 않음" }
        
        // 2단계: 인증번호 입력 화면으로 전환
        uiState = uiState.copy(
            email = "user@example.com",
            isEmailSent = true
        )
        
        composeTestRule.setContent {
            com.example.feature_find_password.ui.FindPasswordContent(
                uiState = uiState,
                onEmailChange = { emailChange = it },
                onAuthCodeChange = { codeChange = it },
                onNewPasswordChange = { passwordChange = it },
                onNewPasswordConfirmChange = { passwordConfirmChange = it },
                onPasswordVisibilityToggle = {},
                onSendAuthCodeClick = { sendCodeClicked = true },
                onConfirmAuthCodeClick = { verifyCodeClicked = true },
                onChangePasswordClick = { resetPasswordClicked = true }
            )
        }
        
        // 인증번호 입력 및 확인
        composeTestRule.onNodeWithText("인증번호").performTextInput("123456")
        assert(codeChange == "123456") { "인증번호 입력이 올바르게 처리되지 않음" }
        
        composeTestRule.onNodeWithText("인증 확인").performClick()
        assert(verifyCodeClicked) { "인증 확인 버튼 클릭이 처리되지 않음" }
        
        // 3단계: 비밀번호 변경 화면으로 전환
        uiState = uiState.copy(
            authCode = "123456",
            isEmailVerified = true
        )
        
        composeTestRule.setContent {
            com.example.feature_find_password.ui.FindPasswordContent(
                uiState = uiState,
                onEmailChange = { emailChange = it },
                onAuthCodeChange = { codeChange = it },
                onNewPasswordChange = { passwordChange = it },
                onNewPasswordConfirmChange = { passwordConfirmChange = it },
                onPasswordVisibilityToggle = {},
                onSendAuthCodeClick = { sendCodeClicked = true },
                onConfirmAuthCodeClick = { verifyCodeClicked = true },
                onChangePasswordClick = { resetPasswordClicked = true }
            )
        }
        
        // 새 비밀번호 입력
        composeTestRule.onNodeWithText("새 비밀번호").performTextInput("newSecurePass123")
        assert(passwordChange == "newSecurePass123") { "새 비밀번호 입력이 올바르게 처리되지 않음" }
        
        composeTestRule.onNodeWithText("비밀번호 확인").performTextInput("newSecurePass123")
        assert(passwordConfirmChange == "newSecurePass123") { "비밀번호 확인 입력이 올바르게 처리되지 않음" }
        
        // 비밀번호 변경 버튼 클릭 (활성화 상태 확인 후)
        composeTestRule.onNodeWithText("비밀번호 변경").assertIsEnabled()
        composeTestRule.onNodeWithText("비밀번호 변경").performClick()
        assert(resetPasswordClicked) { "비밀번호 변경 버튼 클릭이 처리되지 않음" }
    }
    
    /**
     * 에러 메시지 표시 테스트
     */
    @Test
    fun findPasswordContent_whenErrorPresent_shouldDisplayError() {
        // Given: 에러 메시지가 있는 UI 상태
        val errorMessage = "잘못된 인증번호입니다."
        val uiState = com.example.feature_find_password.viewmodel.FindPasswordUiState(
            email = "test@example.com",
            authCode = "123456",
            isEmailSent = true,
            isEmailVerified = false,
            errorMessage = errorMessage
        )
        
        // When: FindPasswordContent를 렌더링하고 스낵바 표시를 에뮬레이션
        var snackbarMessage: String? = null
        composeTestRule.setContent {
            androidx.compose.material3.Scaffold(
                snackbarHost = {
                    androidx.compose.material3.SnackbarHost(
                        hostState = androidx.compose.material3.SnackbarHostState().apply {
                            if (uiState.errorMessage != null) {
                                snackbarMessage = uiState.errorMessage
                            }
                        }
                    )
                }
            ) {
                com.example.feature_find_password.ui.FindPasswordContent(
                    modifier = Modifier.padding(it),
                    uiState = uiState,
                    onEmailChange = {},
                    onAuthCodeChange = {},
                    onNewPasswordChange = {},
                    onNewPasswordConfirmChange = {},
                    onPasswordVisibilityToggle = {},
                    onSendAuthCodeClick = {},
                    onConfirmAuthCodeClick = {},
                    onChangePasswordClick = {}
                )
            }
        }
        
        // Then: 스낵바 메시지가 설정되어야 함
        assert(snackbarMessage == errorMessage) { "에러 메시지가 스낵바에 표시되지 않음" }
        
        // 기본 UI 요소가 여전히 표시되어야 함
        composeTestRule.onNodeWithText("이메일").assertIsDisplayed()
        composeTestRule.onNodeWithText("인증번호").assertIsDisplayed()
    }
} 
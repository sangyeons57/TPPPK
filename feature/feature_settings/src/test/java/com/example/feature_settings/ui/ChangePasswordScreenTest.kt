package com.example.feature_settings.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.feature_change_password.viewmodel.ChangePasswordUiState
import org.junit.Rule
import org.junit.Test

/**
 * ChangePasswordScreen UI 테스트
 *
 * 이 테스트는 ChangePasswordScreen의 주요 UI 컴포넌트와 상호작용을 검증합니다.
 */
class ChangePasswordScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * 비밀번호 필드 표시 테스트
     */
    @Test
    fun changePasswordContent_displaysAllFields() {
        // When: ChangePasswordContent 렌더링
        composeTestRule.setContent {
            com.example.feature_change_password.ui.ChangePasswordContent(
                uiState = com.example.feature_change_password.viewmodel.ChangePasswordUiState(),
                onCurrentPasswordChange = {},
                onNewPasswordChange = {},
                onConfirmPasswordChange = {},
                onChangeClick = {}
            )
        }
        
        // Then: 필수 입력 필드와 버튼이 표시되는지 확인
        composeTestRule.onNodeWithText("현재 비밀번호").assertIsDisplayed()
        composeTestRule.onNodeWithText("새 비밀번호").assertIsDisplayed()
        composeTestRule.onNodeWithText("새 비밀번호 확인").assertIsDisplayed()
        composeTestRule.onNodeWithText("변경하기").assertIsDisplayed()
    }
    
    /**
     * 비밀번호 입력 테스트
     */
    @Test
    fun changePasswordContent_inputsPasswords() {
        // Given: 비밀번호 입력값 모니터링 변수
        var currentPassword = ""
        var newPassword = ""
        var confirmPassword = ""
        
        // When: ChangePasswordContent 렌더링 및 텍스트 입력
        composeTestRule.setContent {
            com.example.feature_change_password.ui.ChangePasswordContent(
                uiState = com.example.feature_change_password.viewmodel.ChangePasswordUiState(),
                onCurrentPasswordChange = { currentPassword = it },
                onNewPasswordChange = { newPassword = it },
                onConfirmPasswordChange = { confirmPassword = it },
                onChangeClick = {}
            )
        }
        
        // 각 필드에 텍스트 입력
        composeTestRule.onNodeWithText("현재 비밀번호").performTextInput("oldpassword")
        composeTestRule.onNodeWithText("새 비밀번호").performTextInput("newpassword")
        composeTestRule.onNodeWithText("새 비밀번호 확인").performTextInput("newpassword")
        
        // Then: 콜백 함수가 호출되어 각 필드의 값이 업데이트 되었는지 확인
        assert(currentPassword == "oldpassword") { "현재 비밀번호 입력값이 업데이트되지 않음" }
        assert(newPassword == "newpassword") { "새 비밀번호 입력값이 업데이트되지 않음" }
        assert(confirmPassword == "newpassword") { "새 비밀번호 확인 입력값이 업데이트되지 않음" }
    }
    
    /**
     * 에러 메시지 표시 테스트
     */
    @Test
    fun changePasswordContent_displaysErrorMessages() {
        // Given: 에러 메시지가 포함된 UI 상태
        val uiState = com.example.feature_change_password.viewmodel.ChangePasswordUiState(
            currentPasswordError = "현재 비밀번호가 일치하지 않습니다.",
            newPasswordError = "비밀번호는 6자 이상이어야 합니다.",
            confirmPasswordError = "새 비밀번호가 일치하지 않습니다."
        )
        
        // When: ChangePasswordContent 렌더링
        composeTestRule.setContent {
            com.example.feature_change_password.ui.ChangePasswordContent(
                uiState = uiState,
                onCurrentPasswordChange = {},
                onNewPasswordChange = {},
                onConfirmPasswordChange = {},
                onChangeClick = {}
            )
        }
        
        // Then: 에러 메시지가 표시되는지 확인
        composeTestRule.onNodeWithText("현재 비밀번호가 일치하지 않습니다.").assertIsDisplayed()
        composeTestRule.onNodeWithText("비밀번호는 6자 이상이어야 합니다.").assertIsDisplayed()
        composeTestRule.onNodeWithText("새 비밀번호가 일치하지 않습니다.").assertIsDisplayed()
    }
    
    /**
     * 비밀번호 보이기/숨기기 테스트
     */
    @Test
    fun changePasswordContent_togglesPasswordVisibility() {
        // When: ChangePasswordContent 렌더링
        composeTestRule.setContent {
            com.example.feature_change_password.ui.ChangePasswordContent(
                uiState = com.example.feature_change_password.viewmodel.ChangePasswordUiState(
                    currentPassword = "testpassword",
                    newPassword = "newpassword",
                    confirmPassword = "newpassword"
                ),
                onCurrentPasswordChange = {},
                onNewPasswordChange = {},
                onConfirmPasswordChange = {},
                onChangeClick = {}
            )
        }
        
        // 처음에는 비밀번호가 표시되지 않아야 함 (점으로 표시됨)
        // 비밀번호 보기 버튼을 클릭하여 비밀번호를 표시
        composeTestRule.onNodeWithContentDescription("비밀번호 보기")
            .performClick()
        
        // 비밀번호 숨기기 버튼으로 변경되었는지 확인
        composeTestRule.onNodeWithContentDescription("비밀번호 숨기기")
            .assertExists()
    }
    
    /**
     * 비밀번호 변경 버튼 클릭 테스트
     */
    @Test
    fun changePasswordContent_clickChangeButton() {
        // Given: 버튼 클릭 모니터링 변수
        var buttonClicked = false
        
        // When: ChangePasswordContent 렌더링 및 버튼 클릭
        composeTestRule.setContent {
            com.example.feature_change_password.ui.ChangePasswordContent(
                uiState = com.example.feature_change_password.viewmodel.ChangePasswordUiState(),
                onCurrentPasswordChange = {},
                onNewPasswordChange = {},
                onConfirmPasswordChange = {},
                onChangeClick = { buttonClicked = true }
            )
        }
        
        // 변경하기 버튼 클릭
        composeTestRule.onNodeWithText("변경하기").performClick()
        
        // Then: 버튼 클릭 콜백이 호출되었는지 확인
        assert(buttonClicked) { "변경하기 버튼 클릭 시 onChangeClick 콜백이 호출되지 않음" }
    }
    
    /**
     * 로딩 상태 표시 테스트
     */
    @Test
    fun changePasswordContent_showsLoadingState() {
        // Given: 로딩 중 상태의 UI
        val uiState =
            com.example.feature_change_password.viewmodel.ChangePasswordUiState(isLoading = true)
        
        // When: ChangePasswordContent 렌더링
        composeTestRule.setContent {
            com.example.feature_change_password.ui.ChangePasswordContent(
                uiState = uiState,
                onCurrentPasswordChange = {},
                onNewPasswordChange = {},
                onConfirmPasswordChange = {},
                onChangeClick = {}
            )
        }
        
        // Then: 버튼에 로딩 인디케이터가 표시되고 버튼이 비활성화되는지 확인
        composeTestRule.onNode(hasRole(androidx.compose.ui.semantics.Role.ProgressBar)).assertIsDisplayed()
        composeTestRule.onNodeWithText("변경하기").assertDoesNotExist() // 로딩 중에는 텍스트 대신 인디케이터 표시
    }
    
    /**
     * 필드 초기값 테스트
     */
    @Test
    fun changePasswordContent_displaysInitialValues() {
        // Given: 초기값이 설정된 UI 상태
        val uiState = com.example.feature_change_password.viewmodel.ChangePasswordUiState(
            currentPassword = "oldpassword",
            newPassword = "newpassword",
            confirmPassword = "newpassword"
        )
        
        // When: ChangePasswordContent 렌더링
        composeTestRule.setContent {
            com.example.feature_change_password.ui.ChangePasswordContent(
                uiState = uiState,
                onCurrentPasswordChange = {},
                onNewPasswordChange = {},
                onConfirmPasswordChange = {},
                onChangeClick = {}
            )
        }
        
        // Then: 텍스트 필드에 초기값이 표시되는지 확인
        // 비밀번호 필드는 직접 텍스트 확인이 어려우므로 EditableText를 사용
        composeTestRule.onNodeWithText("현재 비밀번호")
            .onSibling()
            .assertTextContains("oldpassword")
    }
} 
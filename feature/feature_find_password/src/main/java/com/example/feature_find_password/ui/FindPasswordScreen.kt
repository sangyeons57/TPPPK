package com.example.feature_find_password.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.NavigationManger
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_find_password.viewmodel.FindPasswordEvent
import com.example.feature_find_password.viewmodel.FindPasswordUiState
import com.example.feature_find_password.viewmodel.FindPasswordViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * FindPasswordScreen: 상태 관리 및 이벤트 처리 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindPasswordScreen(
    navigationManger: NavigationManger,
    modifier: Modifier = Modifier,
    viewModel: FindPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 이벤트 처리 (네비게이션, 스낵바)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is FindPasswordEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // 에러 메시지 스낵바로 표시
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            snackbarHostState.showSnackbar(
                message = uiState.errorMessage ?: "오류",
                duration = SnackbarDuration.Short
            )
            viewModel.errorMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("비밀번호 찾기") },
                navigationIcon = {
                    DebouncedBackButton(onClick = { viewModel.onBackClick() })
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        FindPasswordContent(
            modifier = modifier.padding(paddingValues),
            uiState = uiState,
            onEmailChange = viewModel::onEmailChange,
            onRequestPasswordResetEmail = viewModel::requestPasswordResetEmail,
            onDoneClick = viewModel::onDoneClicked
        )
    }
}

/**
 * FindPasswordContent: UI 렌더링 (Stateless)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindPasswordContent(
    modifier: Modifier = Modifier,
    uiState: FindPasswordUiState,
    onEmailChange: (String) -> Unit,
    onRequestPasswordResetEmail: () -> Unit,
    onDoneClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 안내 텍스트
        Text(
            text = "비밀번호를 잊으셨나요?",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Text(
            text = if (!uiState.isEmailSent) "가입 시 사용한 이메일 주소를 입력해주세요."
                   else "비밀번호 재설정 이메일을 보냈습니다.\n이메일을 확인하고 비밀번호를 변경해주세요.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 이메일 입력 필드 (isEmailSent가 false일 때만 활성화)
        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("이메일 주소") },
            placeholder = { Text("가입한 이메일 주소를 입력하세요") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = if (!uiState.isEmailSent) ImeAction.Done else ImeAction.None // No action after email sent
            ),
            keyboardActions = KeyboardActions(
                onDone = { 
                    if (!uiState.isEmailSent) {
                        focusManager.clearFocus() 
                        // Optionally trigger email sending if email is valid
                        // onRequestPasswordResetEmail() 
                    }
                }
            ),
            singleLine = true,
            enabled = !uiState.isEmailSent && !uiState.isLoading // Disable if email sent or loading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 버튼 (이메일 전송 / 완료)
        Button(
            onClick = if (!uiState.isEmailSent) onRequestPasswordResetEmail else onDoneClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = if (!uiState.isEmailSent) {
                !uiState.isLoading && uiState.email.isNotBlank() // Enable "Send" if not loading and email is not blank
            } else {
                !uiState.isLoading // Enable "Done" if not loading
            }
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(if (!uiState.isEmailSent) "이메일 전송" else "완료")
            }
        }
        // All UI elements for authCode, newPassword, newPasswordConfirm have been removed.
    }
}

@Preview(showBackground = true, name = "FindPasswordContent - Initial State")
@Composable
fun FindPasswordScreenInitialPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        FindPasswordContent(
            uiState = FindPasswordUiState(isEmailSent = false, isLoading = false, email = ""),
            onEmailChange = {},
            onRequestPasswordResetEmail = {},
            onDoneClick = {}
        )
    }
}

@Preview(showBackground = true, name = "FindPasswordContent - Email Input")
@Composable
fun FindPasswordScreenEmailInputPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        FindPasswordContent(
            uiState = FindPasswordUiState(isEmailSent = false, isLoading = false, email = "test@example.com"),
            onEmailChange = {},
            onRequestPasswordResetEmail = {},
            onDoneClick = {}
        )
    }
}

@Preview(showBackground = true, name = "FindPasswordContent - Loading Email Send")
@Composable
fun FindPasswordScreenLoadingEmailSendPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        FindPasswordContent(
            uiState = FindPasswordUiState(isEmailSent = false, isLoading = true, email = "test@example.com"),
            onEmailChange = {},
            onRequestPasswordResetEmail = {},
            onDoneClick = {}
        )
    }
}

@Preview(showBackground = true, name = "FindPasswordContent - Email Sent")
@Composable
fun FindPasswordScreenEmailSentPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        FindPasswordContent(
            uiState = FindPasswordUiState(isEmailSent = true, isLoading = false, email = "test@example.com"),
            onEmailChange = {},
            onRequestPasswordResetEmail = {},
            onDoneClick = {}
        )
    }
}

@Preview(showBackground = true, name = "FindPasswordContent - Loading Done")
@Composable
fun FindPasswordScreenLoadingDonePreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        FindPasswordContent(
            uiState = FindPasswordUiState(isEmailSent = true, isLoading = true, email = "test@example.com"),
            onEmailChange = {},
            onRequestPasswordResetEmail = {},
            onDoneClick = {}
        )
    }
}
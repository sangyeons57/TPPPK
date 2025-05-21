package com.example.feature_auth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.AppNavigator
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_auth.viewmodel.FindPasswordEvent
import com.example.feature_auth.viewmodel.FindPasswordUiState
import com.example.feature_auth.viewmodel.FindPasswordViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * FindPasswordScreen: 상태 관리 및 이벤트 처리 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindPasswordScreen(
    appNavigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: FindPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 이벤트 처리 (네비게이션, 스낵바)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is FindPasswordEvent.NavigateBack -> appNavigator.navigateBack()
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
            onAuthCodeChange = viewModel::onAuthCodeChange,
            onNewPasswordChange = viewModel::onNewPasswordChange,
            onNewPasswordConfirmChange = viewModel::onNewPasswordConfirmChange,
            onPasswordVisibilityToggle = viewModel::onPasswordVisibilityToggle,
            onRequestAuthCodeClick = viewModel::onRequestAuthCodeClick,
            onConfirmAuthCodeClick = viewModel::onConfirmAuthCodeClick,
            onCompletePasswordChangeClick = viewModel::onCompletePasswordChangeClick
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
    onAuthCodeChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onNewPasswordConfirmChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onRequestAuthCodeClick: () -> Unit,
    onConfirmAuthCodeClick: () -> Unit,
    onCompletePasswordChangeClick: () -> Unit
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
            text = "이메일 주소와 인증번호를 이용해 비밀번호를 재설정하세요.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 에러 메시지 표시
        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // 단계 1: 이메일 입력 및 인증번호 요청
        if (!uiState.isEmailSent) {
            // 이메일 입력 필드
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("이메일 주소") },
                placeholder = { Text("가입한 이메일 주소를 입력하세요") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true,
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 인증번호 요청 버튼
            Button(
                onClick = onRequestAuthCodeClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && uiState.email.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("인증번호 받기")
                }
            }
        }
        // 단계 2: 인증번호 입력 및 확인
        else if (!uiState.isEmailVerified) {
            // 이메일 표시 (수정 불가)
            OutlinedTextField(
                value = uiState.email,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("이메일 주소") },
                enabled = false,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 인증번호 입력
            OutlinedTextField(
                value = uiState.authCode,
                onValueChange = onAuthCodeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("인증번호") },
                placeholder = { Text("이메일로 받은 6자리 인증번호 입력") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true,
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 인증번호 확인 버튼
            Button(
                onClick = onConfirmAuthCodeClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && uiState.authCode.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("인증번호 확인")
                }
            }
        }
        // 단계 3: 새 비밀번호 입력
        else if (!uiState.passwordChangeSuccess) {
            // 새 비밀번호 입력
            OutlinedTextField(
                value = uiState.newPassword,
                onValueChange = onNewPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("새 비밀번호") },
                placeholder = { Text("8자 이상의 영문, 숫자, 특수문자 조합") },
                visualTransformation = 
                    if (uiState.isPasswordVisible) VisualTransformation.None 
                    else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                trailingIcon = {
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(
                            imageVector = if (uiState.isPasswordVisible) {
                                Icons.Default.Visibility
                            } else {
                                Icons.Default.VisibilityOff
                            },
                            contentDescription = "비밀번호 보기 전환"
                        )
                    }
                },
                singleLine = true,
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 새 비밀번호 확인 입력
            OutlinedTextField(
                value = uiState.newPasswordConfirm,
                onValueChange = onNewPasswordConfirmChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("새 비밀번호 확인") },
                placeholder = { Text("비밀번호를 다시 입력해주세요") },
                visualTransformation = 
                    if (uiState.isPasswordVisible) VisualTransformation.None 
                    else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                singleLine = true,
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 비밀번호 변경 완료 버튼
            Button(
                onClick = onCompletePasswordChangeClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && 
                         uiState.newPassword.isNotBlank() && 
                         uiState.newPasswordConfirm.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("비밀번호 변경 완료")
                }
            }
        }
        // 단계 4: 비밀번호 변경 성공
        else {
            Text(
                text = "비밀번호가 성공적으로 변경되었습니다.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Text(
                text = "새 비밀번호로 로그인해주세요.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FindPasswordScreenPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        FindPasswordContent(
            uiState = FindPasswordUiState(
                email = "user@example.com",
                authCode = "123456",
                isEmailSent = true,
                isEmailVerified = false
            ),
            onEmailChange = {},
            onAuthCodeChange = {},
            onNewPasswordChange = {},
            onNewPasswordConfirmChange = {},
            onPasswordVisibilityToggle = {},
            onRequestAuthCodeClick = {},
            onConfirmAuthCodeClick = {},
            onCompletePasswordChangeClick = {}
        )
    }
}
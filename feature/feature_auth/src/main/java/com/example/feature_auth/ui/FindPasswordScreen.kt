package com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.ui

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
import androidx.navigation.NavHostController
import com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.viewmodel.FindPasswordEvent
import com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.viewmodel.FindPasswordUiState
import com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.viewmodel.FindPasswordViewModel
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * FindPasswordScreen: 상태 관리 및 이벤트 처리 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindPasswordScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: FindPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 이벤트 처리 (네비게이션, 스낵바)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is FindPasswordEvent.NavigateBack -> {
                    navController.popBackStack()
                }
                is FindPasswordEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                }
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
                    IconButton(onClick = { navController.popBackStack() }) { // 뒤로가기
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
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
            onSendAuthCodeClick = viewModel::onSendAuthCodeClick,
            onConfirmAuthCodeClick = viewModel::onConfirmAuthCodeClick,
            onChangePasswordClick = viewModel::onChangePasswordClick
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
    onSendAuthCodeClick: () -> Unit,
    onConfirmAuthCodeClick: () -> Unit,
    onChangePasswordClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // 1단계: 이메일 입력 및 인증번호 전송
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                modifier = Modifier.weight(1f),
                label = { Text("이메일") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                enabled = !uiState.isEmailSent && !uiState.isLoading // 코드 전송 전 + 로딩 중 아닐 때
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSendAuthCodeClick,
                enabled = uiState.email.isNotBlank() && !uiState.isEmailSent && !uiState.isLoading
            ) {
                Text("인증번호\n전송", fontSize = 12.sp, lineHeight = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isEmailSent){
            // 2단계: 인증번호 입력 및 확인
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = uiState.authCode,
                    onValueChange = onAuthCodeChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("인증번호") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true,
                    enabled = !uiState.isEmailVerified && !uiState.isLoading, // 전송 후 + 인증 전 + 로딩 중 아닐 때
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirmAuthCodeClick,
                    enabled = uiState.authCode.isNotBlank() && !uiState.isEmailVerified && !uiState.isLoading
                ) {
                    Text("인증 확인", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.isEmailVerified){
            // 3단계: 새 비밀번호 입력
            OutlinedTextField(
                value = uiState.newPassword,
                onValueChange = onNewPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("새 비밀번호") },
                visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                enabled = uiState.isEmailVerified && !uiState.isLoading, // 이메일 인증 후 + 로딩 중 아닐 때
                trailingIcon = { // 비밀번호 토글 아이콘
                    val image = if (uiState.isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    IconButton(onClick = onPasswordVisibilityToggle, enabled = uiState.isEmailVerified && !uiState.isLoading) {
                        Icon(image, contentDescription = if (uiState.isPasswordVisible) "숨기기" else "보이기")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.newPasswordConfirm,
                onValueChange = onNewPasswordConfirmChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("새 비밀번호 확인") },
                visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if(uiState.isEmailVerified) onChangePasswordClick() // 조건 충족 시 바로 변경 시도
                }),
                singleLine = true,
                enabled = uiState.isEmailVerified && !uiState.isLoading,
                isError = uiState.newPassword.isNotEmpty() && uiState.newPasswordConfirm.isNotEmpty() && uiState.newPassword != uiState.newPasswordConfirm
            )
            // 비밀번호 불일치 에러 메시지 (필요 시 추가)

            Spacer(modifier = Modifier.height(44.dp))

            // 비밀번호 변경 버튼
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onChangePasswordClick()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = uiState.isEmailVerified && // 이메일 인증됨
                        uiState.newPassword.isNotBlank() && // 새 비밀번호 입력됨
                        uiState.newPassword == uiState.newPasswordConfirm && // 비밀번호 일치
                        !uiState.isLoading // 로딩 중 아님
            ) {
                if (uiState.isLoading && uiState.isEmailVerified) { // 변경 로딩 중에만 표시
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("비밀번호 변경", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


// --- Preview ---
@Preview(showBackground = true, name = "FindPassword Initial")
@Composable
fun FindPasswordContentPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        FindPasswordContent(
            uiState = FindPasswordUiState(),
            onEmailChange = {}, onAuthCodeChange = {}, onNewPasswordChange = {},
            onNewPasswordConfirmChange = {}, onPasswordVisibilityToggle = {},
            onSendAuthCodeClick = {}, onConfirmAuthCodeClick = {}, onChangePasswordClick = {}
        )
    }
}

@Preview(showBackground = true, name = "FindPassword Verified")
@Composable
fun FindPasswordContentVerifiedPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        FindPasswordContent(
            uiState = FindPasswordUiState(isEmailSent = true, isEmailVerified = true, email="test@test.com"), // 인증 완료 상태
            onEmailChange = {}, onAuthCodeChange = {}, onNewPasswordChange = {},
            onNewPasswordConfirmChange = {}, onPasswordVisibilityToggle = {},
            onSendAuthCodeClick = {}, onConfirmAuthCodeClick = {}, onChangePasswordClick = {}
        )
    }
}
package com.example.feature_auth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
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
import com.example.core_navigation.core.NavDestination
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.core.NavigationCommand
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.ui.enum.LoginFormFocusTarget
import com.example.feature_auth.viewmodel.LoginEvent
import com.example.feature_auth.viewmodel.LoginUiState
import com.example.feature_auth.viewmodel.LoginViewModel
import kotlinx.coroutines.flow.collectLatest


/**
 * LoginScreen: 상태 관리 및 이벤트 처리 담당 (Stateful)
 * - ViewModel로부터 UI 상태(uiState)와 이벤트(eventFlow)를 구독합니다.
 * - LaunchedEffect를 사용하여 이벤트(네비게이션, 스낵바)를 처리합니다.
 * - UI 렌더링은 LoginContent에 위임합니다.
 */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    appNavigator: AppNavigator,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    // ViewModel의 EventFlow 구독 및 처리
    LaunchedEffect(key1 = Unit) { // Unit key: 화면 진입 시 1회 실행
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is LoginEvent.NavigateToSignUp -> {
                    appNavigator.navigate(NavigationCommand.NavigateToRoute.fromRoute(AppRoutes.Auth.SignUp.path))
                }
                is LoginEvent.NavigateToFindPassword -> {
                    appNavigator.navigate(NavigationCommand.NavigateToRoute.fromRoute(AppRoutes.Auth.FindPassword.path))
                }
                is LoginEvent.LoginSuccess -> appNavigator.navigateClearingBackStack(NavigationCommand.NavigateClearingBackStack(destination = NavDestination.fromRoute(AppRoutes.Main.ROOT)))
                is LoginEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                    event.message,
                    duration = SnackbarDuration.Short
                )
                is LoginEvent.RequestFocus -> {
                    when (event.target) {
                        LoginFormFocusTarget.EMAIL -> emailFocusRequester.requestFocus()
                        LoginFormFocusTarget.PASSWORD -> passwordFocusRequester.requestFocus()
                        LoginFormFocusTarget.LOGIN_BUTTON -> { /* 로그인 버튼에는 특별한 포커스 처리가 필요 없음 */ }
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // LoginContent에 필요한 상태와 콜백 함수들을 전달
        LoginContent(
            modifier = modifier.padding(paddingValues),
            uiState = uiState,
            emailFocusRequester = emailFocusRequester,
            passwordFocusRequester = passwordFocusRequester,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onPasswordVisibilityToggle = viewModel::onPasswordVisibilityToggle,
            onLoginClick = viewModel::onLoginClick,
            onFindPasswordClick = viewModel::onFindPasswordClick,
            onSignUpClick = viewModel::onSignUpClick // 회원가입 버튼 클릭 콜백 전달
        )
    }
}

/**
 * LoginContent: 순수 UI 렌더링 담당 (Stateless)
 * - 필요한 UI 상태(uiState)와 이벤트 콜백 함수들을 파라미터로 받습니다.
 * - UI 요소를 그리고, 사용자 상호작용 시 전달받은 콜백 함수를 호출합니다.
 */
@Composable
fun LoginContent(
    modifier: Modifier = Modifier,
    uiState: LoginUiState, // 표시할 UI 상태
    emailFocusRequester: FocusRequester,
    passwordFocusRequester: FocusRequester,
    onEmailChange: (String) -> Unit, // 이벤트 콜백 함수들
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onLoginClick: () -> Unit,
    onFindPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 앱 이름
        Text(
            text = "프로젝팅",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // 이메일 입력 필드
        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocusRequester),
            label = { Text("이메일") },
            placeholder = { Text("이메일 주소를 입력하세요") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            enabled = !uiState.isLoading,
            isError = uiState.emailError != null,
            supportingText = {
                if (uiState.emailError != null) {
                    Text(
                        text = uiState.emailError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 비밀번호 입력 필드
        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester),
            label = { Text("비밀번호") },
            placeholder = { Text("비밀번호를 입력하세요") },
            singleLine = true,
            visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onLoginClick()
                }
            ),
            trailingIcon = {
                val image = if (uiState.isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                val description = if (uiState.isPasswordVisible) "비밀번호 숨기기" else "비밀번호 보기"
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(imageVector = image, contentDescription = description)
                }
            },
            enabled = !uiState.isLoading,
            isError = uiState.passwordError != null,
            supportingText = {
                if (uiState.passwordError != null) {
                    Text(
                        text = uiState.passwordError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(36.dp))

        // 로그인 버튼
        Button(
            onClick = {
                focusManager.clearFocus()
                onLoginClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !uiState.isLoading && uiState.isLoginEnabled // 로딩중 아닐 때 + 입력값 있을 때 활성화
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("로그인", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 비밀번호 찾기 / 회원가입 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 비밀번호 찾기 버튼
            TextButton(
                onClick = onFindPasswordClick,
                enabled = !uiState.isLoading,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text("비밀번호를 잊으셨나요?")
            }
            // *** 회원가입 버튼 ***
            TextButton(
                onClick = onSignUpClick,
                enabled = !uiState.isLoading,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text("회원가입")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun LoginContentWithSignUpButtonPreview() { // Preview 이름 변경
    TeamnovaPersonalProjectProjectingKotlinTheme {
        LoginContent(
            uiState = LoginUiState(),
            emailFocusRequester = remember { FocusRequester() },
            passwordFocusRequester = remember { FocusRequester() },
            onEmailChange = {},
            onPasswordChange = {},
            onPasswordVisibilityToggle = {},
            onLoginClick = {},
            onFindPasswordClick = {},
            onSignUpClick = {} // 회원가입 버튼 콜백 전달
        )
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun LoginContentPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        // Preview에서는 기본 상태와 빈 콜백 함수를 전달
        LoginContent(
            uiState = LoginUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onPasswordVisibilityToggle = {},
            onLoginClick = {},
            onFindPasswordClick = {},
            onSignUpClick = {},
            modifier = TODO(),
            emailFocusRequester = TODO(),
            passwordFocusRequester = TODO(),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginContentLoadingPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        LoginContent(
            uiState = LoginUiState(isLoading = true, email = "test", password = "pwd"), // 로딩 상태
            onEmailChange = {},
            onPasswordChange = {},
            onPasswordVisibilityToggle = {},
            onLoginClick = {},
            onFindPasswordClick = {},
            onSignUpClick = {},
            modifier = TODO(),
            emailFocusRequester = TODO(),
            passwordFocusRequester = TODO(),
        )
    }
}
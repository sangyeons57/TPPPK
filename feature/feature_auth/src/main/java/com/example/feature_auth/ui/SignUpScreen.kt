package com.example.feature_auth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // AutoMirrored 아이콘 사용 권장
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.core.NavigationCommand
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.SignUpFormFocusTarget
import com.example.feature_auth.viewmodel.SignUpEvent
import com.example.feature_auth.viewmodel.SignUpUiState
import com.example.feature_auth.viewmodel.SignUpViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SignUpScreen(
    navigationManager: ComposeNavigationHandler,
    modifier: Modifier = Modifier,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle();
    val snackbarHostState = remember { SnackbarHostState() }

    // FocusRequester 생성
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val passwordConfirmFocusRequester = remember { FocusRequester() }
    val nameFocusRequester = remember { FocusRequester() }

    // 이벤트 처리 (스낵바, 네비게이션)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is SignUpEvent.NavigateToLogin -> navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Auth.Login.path))
                is SignUpEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                }
                is SignUpEvent.RequestFocus -> { // 포커스 요청 처리
                    when (event.target) {
                        SignUpFormFocusTarget.EMAIL -> emailFocusRequester.requestFocus()
                        SignUpFormFocusTarget.PASSWORD -> passwordFocusRequester.requestFocus()
                        SignUpFormFocusTarget.PASSWORD_CONFIRM -> passwordConfirmFocusRequester.requestFocus()
                        SignUpFormFocusTarget.NAME -> nameFocusRequester.requestFocus()
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        SignUpContent(
            modifier = modifier.padding(paddingValues),
            uiState = uiState,

            emailFocusRequester = emailFocusRequester,
            passwordFocusRequester = passwordFocusRequester,
            passwordConfirmFocusRequester = passwordConfirmFocusRequester,
            nameFocusRequester = nameFocusRequester,

            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onPasswordConfirmChange = viewModel::onPasswordConfirmChange,
            onNameChange = viewModel::onNameChange,
            onPasswordVisibilityToggle = viewModel::onPasswordVisibilityToggle,
            onSignUpClick = viewModel::signUp,

            onEmailFocus = viewModel::onEmailFocus,
            onPasswordFocus = viewModel::onPasswordFocus,
            onPasswordConfirmFocus = viewModel::onPasswordConfirmFocus,
            onNameFocus = viewModel::onNameFocus,

            onNavigateBack = { navigationManager.navigateBack() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpContent(
    modifier: Modifier = Modifier,
    uiState: SignUpUiState,
    // FocusRequester 파라미터 추가
    emailFocusRequester: FocusRequester,
    passwordFocusRequester: FocusRequester,
    passwordConfirmFocusRequester: FocusRequester,
    nameFocusRequester: FocusRequester,
    // 이벤트 핸들러는 동일
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSignUpClick: () -> Unit,
    onNavigateBack: () -> Unit,
    // 필드 포커스 아웃 이벤트 핸들러
    onEmailFocus: (androidx.compose.ui.focus.FocusState) -> Unit,
    onPasswordFocus: (androidx.compose.ui.focus.FocusState) -> Unit,
    onPasswordConfirmFocus: (androidx.compose.ui.focus.FocusState) -> Unit,
    onNameFocus: (androidx.compose.ui.focus.FocusState) -> Unit
) {

    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("회원가입") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 이메일 입력
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(emailFocusRequester)
                    .onFocusChanged(onEmailFocus),
                label = { Text("이메일") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                enabled = !uiState.isLoading,
                isError = uiState.emailError != null, // isError 연결
                supportingText = { // supportingText로 에러 메시지 표시
                    if (uiState.emailError != null) {
                        Text(
                            text = uiState.emailError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp)) // 에러 메시지 공간

            // 비밀번호 입력
            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocusRequester)
                    .onFocusChanged(onPasswordFocus),
                label = { Text("비밀번호 (6자 이상)") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (uiState.isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(image, contentDescription = if (uiState.isPasswordVisible) "숨기기" else "보이기")
                    }
                },
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                enabled = !uiState.isLoading,
                isError = uiState.passwordError != null, // isError 연결
                supportingText = { // supportingText로 에러 메시지 표시
                    if (uiState.passwordError != null) {
                        Text(
                            text = uiState.passwordError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp)) // 에러 메시지 공간

            // 비밀번호 확인 입력
            OutlinedTextField(
                value = uiState.passwordConfirm,
                onValueChange = onPasswordConfirmChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordConfirmFocusRequester)
                    .onFocusChanged(onPasswordConfirmFocus),
                label = { Text("비밀번호 확인") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (uiState.isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(image, contentDescription = if (uiState.isPasswordVisible) "숨기기" else "보이기")
                    }
                },
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                enabled = !uiState.isLoading,
                isError = uiState.passwordConfirmError != null, // isError 연결 (비밀번호 일치 여부는 ViewModel에서 설정)
                supportingText = { // supportingText로 에러 메시지 표시
                    if (uiState.passwordConfirmError != null) {
                        Text(
                            text = uiState.passwordConfirmError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp)) // 에러 메시지 공간

            // 사용자 이름 입력
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocusRequester)
                    .onFocusChanged(onNameFocus),
                label = { Text("이름") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onSignUpClick()
                }),
                singleLine = true,
                enabled = !uiState.isLoading,
                isError = uiState.nameError != null, // isError 연결
                supportingText = { // supportingText로 에러 메시지 표시
                    if (uiState.nameError != null) {
                        Text(
                            text = uiState.nameError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(36.dp)) // 에러 메시지 공간 고려

            // 회원 가입 버튼
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSignUpClick()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading // 버튼 활성화 로직 단순화 (ViewModel에서 유효성 검사 후 API 호출)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                } else {
                    Text("회원 가입")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// --- Preview ---
@Preview(showBackground = true, name = "SignUp with Errors")
@Composable
fun SignUpContentWithErrorAndFocusPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        SignUpContent(
            uiState = SignUpUiState(
                email = "invalid-email",
                password = "short",
                passwordConfirm = "mismatch",
                name = "",
                emailError = "올바른 이메일을 입력해주세요.",
                passwordError = "비밀번호는 6자 이상 입력해주세요.",
                passwordConfirmError = "비밀번호가 일치하지 않습니다.",
                nameError = "이름을 입력해주세요."
            ),
            emailFocusRequester = remember { FocusRequester() },
            passwordFocusRequester = remember { FocusRequester() },
            passwordConfirmFocusRequester = remember { FocusRequester() },
            nameFocusRequester = remember { FocusRequester() },
            onEmailChange = {}, onPasswordChange = {}, onPasswordConfirmChange = {},
            onNameChange = {}, onPasswordVisibilityToggle = {}, onSignUpClick = {}, onNavigateBack = {},
            onEmailFocus = {}, onPasswordFocus = {}, onPasswordConfirmFocus = {}, onNameFocus = {}
        )
    }
}
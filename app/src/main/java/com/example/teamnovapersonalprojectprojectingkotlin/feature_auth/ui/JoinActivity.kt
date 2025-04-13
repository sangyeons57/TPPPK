package com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.ui

// JoinScreenEntry.kt (ui/join 패키지 등에 위치)

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.* // Material 2 사용 시
// import androidx.compose.material3.* // Material 3 사용 시
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // 임시 색상용
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // 기본 ViewModel 주입
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.viewmodel.JoinEvent
import com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.viewmodel.JoinViewModel
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.* // 프로젝트 테마의 색상 사용
import kotlinx.coroutines.flow.collectLatest

// --- 네비게이션 라우트 정의 (이전과 동일) ---
object JoinDestinations {
    const val EMAIL_ROUTE = "join_email"
    const val AUTH_CODE_ROUTE = "join_auth_code/{email}"
    const val SET_NAME_ROUTE = "join_set_name/{email}"
    const val SET_PASSWORD_ROUTE = "join_set_password/{email}/{name}"

    fun authCodeRoute(email: String) = "join_auth_code/$email"
    fun setNameRoute(email: String) = "join_set_name/$email"
    fun setPasswordRoute(email: String, name: String) = "join_set_password/$email/$name"
}

// --- 메인 Composable (NavHost 포함 - Material 3 기준) ---
@Composable
fun JoinScreenEntryM3( // 함수 이름 변경 (Material 3 명시)
    joinViewModel: JoinViewModel = viewModel(),
    onNavigateBackToLogin: () -> Unit,
    onRegistrationComplete: (email: String, name: String) -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() } // Material 3 스낵바 상태

    // ViewModel 이벤트 구독 및 처리
    LaunchedEffect(key1 = Unit) {
        joinViewModel.eventFlow.collectLatest { event ->
            when (event) {
                is JoinEvent.NavigateTo -> navController.navigate(event.destination)
                JoinEvent.NavigateBack -> navController.popBackStack()
                JoinEvent.RegistrationSuccess -> {
                    val finalState = joinViewModel.uiState.value
                    onRegistrationComplete(finalState.emailState.value, finalState.nameState.value)
                }
                is JoinEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold( // Material 3 Scaffold 사용
        snackbarHost = { SnackbarHost(snackbarHostState) }, // 스낵바 호스트 연결
        modifier = Modifier.fillMaxSize()
    ) { innerPadding -> // Scaffold로부터 받은 내부 패딩
        NavHost(
            navController = navController,
            startDestination = JoinDestinations.EMAIL_ROUTE,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // 내부 패딩 적용
                .background(MaterialTheme.colorScheme.background) // 테마 배경색 사용
        ) {
            // 각 단계별 화면 정의 (Material 3 Composable 호출)
            composable(JoinDestinations.EMAIL_ROUTE) {
                EmailInputScreenM3( // Material 3 버전 호출
                    viewModel = joinViewModel,
                    onNavigateBack = onNavigateBackToLogin
                )
            }
            composable(
                route = JoinDestinations.AUTH_CODE_ROUTE,
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) {
                AuthCodeInputScreenM3(viewModel = joinViewModel) // Material 3 버전 호출
            }
            composable(
                route = JoinDestinations.SET_NAME_ROUTE,
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) {
                SetNameInputScreenM3(viewModel = joinViewModel) // Material 3 버전 호출
            }
            composable(
                route = JoinDestinations.SET_PASSWORD_ROUTE,
                arguments = listOf(
                    navArgument("email") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType }
                )
            ) {
                SetPasswordInputScreenM3(viewModel = joinViewModel) // Material 3 버전 호출
            }
        }
    }
}

// --- 각 단계별 화면 Composable (Material 3 기준) ---

@OptIn(ExperimentalMaterial3Api::class) // TopAppBar 등 실험적인 API 사용 시 필요
@Composable
fun EmailInputScreenM3(viewModel: JoinViewModel, onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val emailState = uiState.emailState

    Scaffold(
        topBar = { JoinTopAppBarM3(title = "회원가입 (1/4): 이메일", onNavigateBack = onNavigateBack) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = emailState.value,
                onValueChange = viewModel::onEmailChange,
                label = { Text("이메일 주소") },
                isError = !emailState.isValid,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.sendVerificationCode() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = getTextFieldColorsM3() // Material 3 색상 적용
            )
            ErrorTextM3(isValid = emailState.isValid, errorMessage = emailState.errorMessage)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = viewModel::sendVerificationCode,
                enabled = emailState.isValid && emailState.value.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = getButtonColorsM3() // Material 3 색상 적용
            ) {
                Text("인증번호 발송")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthCodeInputScreenM3(viewModel: JoinViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val authCodeState = uiState.authCodeState

    Scaffold(
        topBar = { JoinTopAppBarM3(title = "회원가입 (2/4): 인증번호", onNavigateBack = viewModel::navigateBack) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "이메일 [${uiState.emailState.value}] 로 전송된 인증번호 6자리를 입력하세요.",
                color = MaterialTheme.colorScheme.onBackground // 테마 색상 사용
            )

            OutlinedTextField(
                value = authCodeState.value,
                onValueChange = viewModel::onAuthCodeChange,
                label = { Text("인증번호 6자리") },
                isError = !authCodeState.isValid,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.verifyAuthCode() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = getTextFieldColorsM3()
            )
            ErrorTextM3(isValid = authCodeState.isValid, errorMessage = authCodeState.errorMessage)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = viewModel::verifyAuthCode,
                enabled = authCodeState.isValid && authCodeState.value.length == 6,
                modifier = Modifier.fillMaxWidth(),
                colors = getButtonColorsM3()
            ) {
                Text("인증번호 확인")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetNameInputScreenM3(viewModel: JoinViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val nameState = uiState.nameState

    Scaffold(
        topBar = { JoinTopAppBarM3(title = "회원가입 (3/4): 이름 설정", onNavigateBack = viewModel::navigateBack) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = nameState.value,
                onValueChange = viewModel::onNameChange,
                label = { Text("사용할 이름") },
                isError = !nameState.isValid,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.submitName() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = getTextFieldColorsM3()
            )
            ErrorTextM3(isValid = nameState.isValid, errorMessage = nameState.errorMessage)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = viewModel::submitName,
                enabled = nameState.isValid && nameState.value.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = getButtonColorsM3()
            ) {
                Text("다음")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetPasswordInputScreenM3(viewModel: JoinViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val passwordState = uiState.passwordState
    val confirmPasswordState = uiState.confirmPasswordState

    Scaffold(
        topBar = { JoinTopAppBarM3(title = "회원가입 (4/4): 비밀번호 설정", onNavigateBack = viewModel::navigateBack) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = passwordState.value,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("비밀번호 (8자 이상)") },
                isError = !passwordState.isValid,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = getTextFieldColorsM3()
            )
            ErrorTextM3(isValid = passwordState.isValid, errorMessage = passwordState.errorMessage)

            OutlinedTextField(
                value = confirmPasswordState.value,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = { Text("비밀번호 확인") },
                isError = !uiState.passwordsMatch && confirmPasswordState.value.isNotEmpty(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.submitPasswordAndRegister() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = getTextFieldColorsM3()
            )
            ErrorTextM3(isValid = uiState.passwordsMatch || confirmPasswordState.value.isEmpty(), errorMessage = if (!uiState.passwordsMatch && confirmPasswordState.value.isNotEmpty()) "비밀번호가 일치하지 않습니다." else null)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = viewModel::submitPasswordAndRegister,
                enabled = passwordState.isValid && passwordState.value.isNotEmpty() && uiState.passwordsMatch && confirmPasswordState.value.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = getButtonColorsM3()
            ) {
                Text("회원가입 완료")
            }
        }
    }
}

// --- Helper Composables (Material 3 기준) ---

@OptIn(ExperimentalMaterial3Api::class) // CenterAlignedTopAppBar가 실험적 API일 수 있음
@Composable
fun JoinTopAppBarM3(title: String, onNavigateBack: () -> Unit) {
    // CenterAlignedTopAppBar 또는 TopAppBar 사용 가능
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, // RTL 지원 아이콘
                    contentDescription = "Back"
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors( // 또는 topAppBarColors
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun ErrorTextM3(isValid: Boolean, errorMessage: String?) {
    // 에러 메시지를 표시할 공간을 미리 확보하거나, 메시지 있을 때만 표시
    val errorTextHeight = MaterialTheme.typography.bodySmall.lineHeight.value.dp + 4.dp // 대략적인 높이
    Box(modifier = Modifier.height(errorTextHeight)) {
        if (!isValid && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall, // M3 타이포그래피 사용
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp) // 필요시 패딩 조정
            )
        }
    }
}

// TextField 공통 색상 반환 함수 (Material 3 기준)
@Composable
fun getTextFieldColorsM3(): TextFieldColors = OutlinedTextFieldDefaults.colors( // M3 Defaults 사용
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest, // M3 권장
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh, // M3 권장
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorLabelColor = MaterialTheme.colorScheme.error,
    // 필요에 따라 placeholderColor 등 추가 설정
)

// Button 공통 색상 반환 함수 (Material 3 기준)
@Composable
fun getButtonColorsM3(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), // M3 비활성화 스타일
    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) // M3 비활성화 스타일
)

// --- Preview ---
@Preview(showBackground = true, name = "Join Screen Entry Preview M3")
@Composable
fun JoinScreenEntryM3Preview() {
    TeamnovaPersonalProjectProjectingKotlinTheme { // 앱 테마 적용
        EmailInputScreenM3(viewModel = JoinViewModel(), onNavigateBack = {}) // 첫 화면 예시
    }
}
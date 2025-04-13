package com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.ui

import android.os.Bundle
import android.widget.Toast // 스낵바 대신 간단한 토스트 예시용
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels // ViewModel 주입을 위한 의존성 필요 (implementation "androidx.activity:activity-compose:...")
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.teamnovapersonalprojectprojectingkotlin.R
import androidx.lifecycle.repeatOnLifecycle
import com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.viewmodel.LoginEvent
import com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.viewmodel.LoginViewModel
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * LoginActivity - 사용자 로그인을 처리하는 화면 (MVVM 적용)
 */
class LoginActivity : ComponentActivity() {

    // activity-compose 라이브러리를 통해 ViewModel 주입
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ViewModel의 이벤트를 관찰하여 처리 (네비게이션, 스낵바 등)
        lifecycleScope.launch {
            // UI 상태가 최소 STARTED일 때만 이벤트 수신 및 처리
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                loginViewModel.eventFlow.collectLatest { event ->
                    when (event) {
                        LoginEvent.NavigateToFindPassword -> {
                            // TODO: 실제 비밀번호 찾기 화면으로 이동하는 코드 구현
                            // 예시: startActivity(Intent(this@LoginActivity, FindPasswordActivity::class.java))
                            Toast.makeText(this@LoginActivity, "비밀번호 찾기 화면으로 이동", Toast.LENGTH_SHORT).show()
                        }
                        LoginEvent.NavigateToSignUp -> {
                            // TODO: 실제 회원가입 화면으로 이동하는 코드 구현
                            // 예시: startActivity(Intent(this@LoginActivity, SignUpActivity::class.java))
                            Toast.makeText(this@LoginActivity, "회원가입 화면으로 이동", Toast.LENGTH_SHORT).show()
                        }
                        is LoginEvent.ShowSnackbar -> {
                            // TODO: Snackbar 또는 Toast 등으로 메시지 표시
                            Toast.makeText(this@LoginActivity, event.message, Toast.LENGTH_SHORT).show()
                        }
                        // LoginEvent.LoginSuccess -> {
                        //     // TODO: 로그인 성공 후 메인 화면 등으로 이동
                        //     Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()
                        // }
                    }
                }
            }
        }

        setContent {
            TeamnovaPersonalProjectProjectingKotlinTheme {
                // LoginScreen에 ViewModel 전달
                LoginScreen(viewModel = loginViewModel)
            }
        }
    }
}

/**
 * LoginScreen - 로그인 화면의 메인 Composable (Stateless에 가깝게)
 * @param viewModel 로그인 상태 및 로직을 관리하는 ViewModel
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel // ViewModel을 파라미터로 받음
) {
    // ViewModel의 uiState를 구독하여 UI 상태 가져오기
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Material 3 테마에 맞는 TextField, Button 색상 설정
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest, // Material 3 권장
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh, // Material 3 권장
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorBorderColor = MaterialTheme.colorScheme.error,
        errorLabelColor = MaterialTheme.colorScheme.error
    )

    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // 테마 배경색 사용
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // 로고 텍스트
        Text(
            text = "프로젝팅", // 앱 이름
            fontSize = 70.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, // 테마 Primary 색상 사용
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(80.dp))

        // 이메일 입력 필드
        OutlinedTextField(
            value = uiState.email, // ViewModel 상태 사용
            onValueChange = viewModel::onEmailChange, // ViewModel 함수 호출
            label = { Text("이메일") },
            placeholder = { Text("이메일 주소를 입력하세요") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            colors = textFieldColors // Material 3 색상 적용
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 비밀번호 입력 필드
        OutlinedTextField(
            value = uiState.password, // ViewModel 상태 사용
            onValueChange = viewModel::onPasswordChange, // ViewModel 함수 호출
            label = { Text("비밀번호") },
            placeholder = { Text("비밀번호를 입력하세요") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (uiState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), // ViewModel 상태 사용
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.onLoginClick() // ViewModel 함수 호출
                }
            ),
            // 비밀번호 보이기/숨기기 아이콘 버튼
            trailingIcon = {

                val image = if (uiState.passwordVisible)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff
                val description = if (uiState.passwordVisible) "비밀번호 숨기기" else "비밀번호 보기"


                IconButton(onClick = viewModel::onPasswordVisibilityToggle){ // ViewModel 함수 호출
                    Icon(imageVector = image, contentDescription = description)
                }
            },
            colors = textFieldColors // Material 3 색상 적용
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 로그인 버튼
        Button(
            onClick = {
                focusManager.clearFocus()
                viewModel.onLoginClick() // ViewModel 함수 호출
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = uiState.isLoginEnabled, // ViewModel 상태 사용
            colors = buttonColors // Material 3 색상 적용
        ) {
            // 로딩 상태 표시 (ViewModel에서 isLoading 관리 시)
            // if (uiState.isLoading) {
            //    CircularProgressIndicator(
            //        modifier = Modifier.size(24.dp),
            //        color = MaterialTheme.colorScheme.onPrimary,
            //        strokeWidth = 2.dp
            //    )
            // } else {
            Text("로그인", fontSize = 16.sp)
            // }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 비밀번호 찾기 / 회원가입 링크
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = viewModel::onFindPasswordClick) { // ViewModel 함수 호출
                Text(
                    "비밀번호 찾기",
                    color = MaterialTheme.colorScheme.primary // 테마 색상 사용
                )
            }
            TextButton(onClick = viewModel::onSignUpClick) { // ViewModel 함수 호출
                Text(
                    "회원가입",
                    color = MaterialTheme.colorScheme.primary // 테마 색상 사용
                )
            }
        }
    }
}

/**
 * Preview 함수 (ViewModel 의존성 없이 UI만 미리보기)
 */
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        // Preview에서는 실제 ViewModel 대신 가짜 데이터나 Mock ViewModel을 사용할 수 있습니다.
        // 여기서는 상태를 직접 관리하는 간단한 ViewModel을 Mock으로 만듭니다.
        val mockViewModel = LoginViewModel() // 실제 앱에서는 이렇게 사용하지 않음
        LoginScreen(viewModel = mockViewModel)
    }
}
package com.example.feature_settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility // 비밀번호 보기 아이콘
import androidx.compose.material.icons.filled.VisibilityOff // 비밀번호 숨기기 아이콘
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.AppNavigator
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_settings.viewmodel.ChangePasswordEvent
import com.example.feature_settings.viewmodel.ChangePasswordUiState
import com.example.feature_settings.viewmodel.ChangePasswordViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * ChangePasswordScreen: 비밀번호 변경 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    appNavigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: ChangePasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ChangePasswordEvent.NavigateBack -> appNavigator.navigateBack()
                is ChangePasswordEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is ChangePasswordEvent.ClearFocus -> focusManager.clearFocus()
            }
        }
    }

    // 변경 성공 시 자동으로 뒤로가기
    LaunchedEffect(uiState.changeSuccess) {
        if (uiState.changeSuccess) {
            appNavigator.navigateBack()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("비밀번호 변경") },
                navigationIcon = {
                    IconButton(onClick = { appNavigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        ChangePasswordContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
            onNewPasswordChange = viewModel::onNewPasswordChange,
            onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
            onChangeClick = {
                focusManager.clearFocus() // 버튼 클릭 시 포커스 해제
                viewModel.changePassword()
            }
        )
    }
}

/**
 * ChangePasswordContent: 비밀번호 변경 UI 요소 (Stateless)
 */
@Composable
fun ChangePasswordContent(
    modifier: Modifier = Modifier,
    uiState: ChangePasswordUiState,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onChangeClick: () -> Unit
) {
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp) // 필드 간 간격
    ) {
        // 현재 비밀번호 입력 필드
        OutlinedTextField(
            value = uiState.currentPassword,
            onValueChange = onCurrentPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("현재 비밀번호") },
            singleLine = true,
            visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next // 다음 입력 필드로 이동
            ),
            trailingIcon = {
                val image = if (currentPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                    Icon(imageVector = image, contentDescription = if (currentPasswordVisible) "비밀번호 숨기기" else "비밀번호 보기")
                }
            },
            isError = uiState.currentPasswordError != null
        )
        if (uiState.currentPasswordError != null) {
            Text(uiState.currentPasswordError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        } else {
            Spacer(modifier = Modifier.height(MaterialTheme.typography.bodySmall.lineHeight.value.dp / 2)) // 에러 없을 때 공간 절반
        }

        // 새 비밀번호 입력 필드
        OutlinedTextField(
            value = uiState.newPassword,
            onValueChange = onNewPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("새 비밀번호") },
            singleLine = true,
            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            trailingIcon = {
                val image = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                    Icon(imageVector = image, contentDescription = if (newPasswordVisible) "비밀번호 숨기기" else "비밀번호 보기")
                }
            },
            isError = uiState.newPasswordError != null
        )
        if (uiState.newPasswordError != null) {
            Text(uiState.newPasswordError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        } else {
            Spacer(modifier = Modifier.height(MaterialTheme.typography.bodySmall.lineHeight.value.dp / 2))
        }


        // 새 비밀번호 확인 입력 필드
        OutlinedTextField(
            value = uiState.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("새 비밀번호 확인") },
            singleLine = true,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done // 완료 액션
            ),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                onChangeClick() // 완료 시 변경 시도
            }),
            trailingIcon = {
                val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(imageVector = image, contentDescription = if (confirmPasswordVisible) "비밀번호 숨기기" else "비밀번호 보기")
                }
            },
            isError = uiState.confirmPasswordError != null
        )
        if (uiState.confirmPasswordError != null) {
            Text(uiState.confirmPasswordError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        } else {
            Spacer(modifier = Modifier.height(MaterialTheme.typography.bodySmall.lineHeight.value.dp / 2))
        }


        Spacer(modifier = Modifier.height(16.dp)) // 버튼과의 간격

        // 변경하기 버튼
        Button(
            onClick = onChangeClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading // 로딩 중 아닐 때만 활성화
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("변경하기")
            }
        }
    }
}


// --- Preview ---
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun ChangePasswordContentPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("비밀번호 변경") }) }) { padding ->
            ChangePasswordContent(
                modifier = Modifier.padding(padding),
                uiState = ChangePasswordUiState(),
                onCurrentPasswordChange = {},
                onNewPasswordChange = {},
                onConfirmPasswordChange = {},
                onChangeClick = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Change Password Error")
@Composable
private fun ChangePasswordContentErrorPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("비밀번호 변경") }) }) { padding ->
            ChangePasswordContent(
                modifier = Modifier.padding(padding),
                uiState = ChangePasswordUiState(
                    currentPassword = "wrongpassword",
                    currentPasswordError = "현재 비밀번호가 일치하지 않습니다.",
                    newPassword = "new",
                    newPasswordError = "비밀번호는 6자 이상이어야 합니다.",
                    confirmPassword = "mismatch",
                    confirmPasswordError = "새 비밀번호가 일치하지 않습니다."
                ),
                onCurrentPasswordChange = {},
                onNewPasswordChange = {},
                onConfirmPasswordChange = {},
                onChangeClick = {}
            )
        }
    }
}

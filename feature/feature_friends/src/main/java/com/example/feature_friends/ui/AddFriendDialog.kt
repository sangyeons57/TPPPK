package com.example.feature_friends.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_friends.viewmodel.AddFriendEvent
import com.example.feature_friends.viewmodel.AddFriendUiState
import com.example.feature_friends.viewmodel.AddFriendViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * AddFriendDialog: 사용자 이름으로 친구 요청을 보내는 다이얼로그 (Stateful)
 *
 * @param onDismissRequest 다이얼로그 닫기 요청 콜백
 * @param viewModel ViewModel 인스턴스 (Hilt 주입)
 */
@Composable
fun AddFriendDialog(
    onDismissRequest: () -> Unit,
    viewModel: AddFriendViewModel = hiltViewModel() // ViewModel 직접 사용
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() } // 스낵바는 호출하는 곳에서 관리

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddFriendEvent.DismissDialog -> onDismissRequest()
                is AddFriendEvent.ShowSnackbar -> {
                    // 필요 시 스낵바 메시지를 호출한 화면으로 전달하거나,
                    // 다이얼로그 내부에 임시 메시지 표시 상태 사용
                    println("Snackbar requested: ${event.message}")
                    // snackbarHostState.showSnackbar(event.message) // 다이얼로그 내 스낵바 어려움
                }
                is AddFriendEvent.ClearFocus -> { /* FocusManager 필요 시 */ }
            }
        }
    }

    // 친구 추가 성공 시 다이얼로그 닫기
    LaunchedEffect(uiState.addFriendSuccess) {
        if (uiState.addFriendSuccess) {
            onDismissRequest()
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(shape = MaterialTheme.shapes.large) {
            AddFriendDialogContent(
                uiState = uiState,
                onUsernameChange = viewModel::onUsernameChange,
                onSendRequestClick = viewModel::sendFriendRequest,
                onDismiss = onDismissRequest // 취소 버튼용
            )
        }
    }
}

/**
 * AddFriendDialogContent: 친구 추가 다이얼로그 내부 UI (Stateless)
 */
@Composable
fun AddFriendDialogContent(
    uiState: AddFriendUiState,
    onUsernameChange: (String) -> Unit,
    onSendRequestClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("친구 추가", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = uiState.username,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            label = { Text("사용자 이름") },
            placeholder = { Text("친구 요청 보낼 사용자 이름") },
            singleLine = true,
            isError = uiState.error != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                focusManager.clearFocus()
                onSendRequestClick()
            })
        )

        // 정보/에러 메시지 표시 영역
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp)) { // 최소 높이 확보
            Text(
                text = uiState.infoMessage ?: uiState.error ?: "", // 정보 메시지 또는 에러 메시지
                color = if (uiState.error != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }


        // 하단 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSendRequestClick()
                },
                enabled = uiState.username.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("요청 보내기")
                }
            }
        }
    }

    // 다이얼로그 표시될 때 포커스 요청
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}


// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun AddFriendDialogContentPreview_Initial() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            AddFriendDialogContent(
                uiState = AddFriendUiState(),
                onUsernameChange = {},
                onSendRequestClick = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddFriendDialogContentPreview_Success() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            AddFriendDialogContent(
                uiState = AddFriendUiState(username = "친구이름", infoMessage = "'친구이름'님에게 요청을 보냈습니다."),
                onUsernameChange = {},
                onSendRequestClick = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddFriendDialogContentPreview_Error() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            AddFriendDialogContent(
                uiState = AddFriendUiState(username = "없는이름", error = "'없는이름' 사용자를 찾을 수 없습니다."),
                onUsernameChange = {},
                onSendRequestClick = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddFriendDialogContentPreview_Loading() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            AddFriendDialogContent(
                uiState = AddFriendUiState(username = "요청중", isLoading = true),
                onUsernameChange = {},
                onSendRequestClick = {},
                onDismiss = {}
            )
        }
    }
}
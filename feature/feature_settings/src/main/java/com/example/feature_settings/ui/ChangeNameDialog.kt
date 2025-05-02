package com.example.feature_settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel // Hilt ViewModel 주입
import androidx.lifecycle.compose.collectAsStateWithLifecycle // 상태 구독
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_settings.viewmodel.ChangeNameEvent
import com.example.feature_settings.viewmodel.ChangeNameViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * ChangeNameDialog: 사용자 이름 변경 다이얼로그 (Stateful)
 * 이제 내부에서 ViewModel을 사용합니다.
 */
@Composable
fun ChangeNameDialog(
    modifier: Modifier = Modifier,
    viewModel: ChangeNameViewModel = hiltViewModel(), // ★ ViewModel 주입
    onDismissRequest: () -> Unit // 다이얼로그 닫기 요청 콜백
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle() // ★ 상태 구독

    // 이벤트 처리 (다이얼로그 닫기 등)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ChangeNameEvent.DismissDialog -> onDismissRequest()
                // 스낵바는 보통 다이얼로그를 호출한 화면에서 표시하므로 여기서는 처리하지 않을 수 있음
                is ChangeNameEvent.ShowSnackbar -> {
                    // TODO: 필요시 다이얼로그 내 스낵바 표시 또는 상위로 이벤트 전달
                }
            }
        }
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text("이름 변경") },
        text = {
            Column {
                OutlinedTextField(
                    value = uiState.newName, // ★ 상태 바인딩
                    onValueChange = viewModel::onNameChange, // ★ 콜백 연결
                    label = { Text("새 이름") },
                    singleLine = true,
                    isError = uiState.error != null // ★ 에러 상태 반영
                )
                // 에러 메시지 표시
                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = viewModel::updateUserName, // ★ 콜백 연결
                enabled = !uiState.isLoading // ★ 로딩 상태 반영
            ) {
                // 로딩 중 인디케이터 표시
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("확인")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !uiState.isLoading // 로딩 중 아닐 때 활성화
            ) {
                Text("취소")
            }
        }
    )
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun ChangeNameDialogPreview_Initial() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        // Preview에서는 실제 ViewModel 주입 대신 AlertDialog 직접 호출
        AlertDialog(
            onDismissRequest = {},
            title = { Text("이름 변경") },
            text = {
                Column {
                    OutlinedTextField(value = "기존이름", onValueChange = {}, label = { Text("새 이름") })
                }
            },
            confirmButton = { Button(onClick = {}) { Text("확인") } },
            dismissButton = { TextButton(onClick = {}) { Text("취소") } }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChangeNameDialogPreview_Loading() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("이름 변경") },
            text = {
                Column {
                    OutlinedTextField(value = "새 이름 입력 중", onValueChange = {}, label = { Text("새 이름") })
                }
            },
            confirmButton = { Button(onClick = {}, enabled = false) { CircularProgressIndicator(modifier=Modifier.size(24.dp)) } }, // 로딩 상태
            dismissButton = { TextButton(onClick = {}, enabled = false) { Text("취소") } }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChangeNameDialogPreview_Error() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("이름 변경") },
            text = {
                Column {
                    OutlinedTextField(value = "잘못된이름", onValueChange = {}, label = { Text("새 이름") }, isError = true) // 에러 상태
                    Text("이름 변경 실패: 서버 오류", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) // 에러 메시지
                }
            },
            confirmButton = { Button(onClick = {}) { Text("확인") } },
            dismissButton = { TextButton(onClick = {}) { Text("취소") } }
        )
    }
}
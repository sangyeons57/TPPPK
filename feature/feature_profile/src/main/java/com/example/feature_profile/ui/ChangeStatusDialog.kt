package com.example.teamnovapersonalprojectprojectingkotlin.feature_profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
// Domain 모델 및 ViewModel 관련 요소 Import
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.UserStatus
import com.example.teamnovapersonalprojectprojectingkotlin.feature_profile.viewmodel.ChangeStatusEvent
import com.example.teamnovapersonalprojectprojectingkotlin.feature_profile.viewmodel.ChangeStatusUiState
import com.example.teamnovapersonalprojectprojectingkotlin.feature_profile.viewmodel.ChangeStatusViewModel
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * ChangeStatusDialog: 사용자 상태 변경 다이얼로그 (Stateful)
 */
@Composable
fun ChangeStatusDialog(
    modifier: Modifier = Modifier,
    viewModel: ChangeStatusViewModel = hiltViewModel(), // ★ ViewModel 주입
    onDismissRequest: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle() // ★ 상태 구독

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ChangeStatusEvent.DismissDialog -> onDismissRequest()
                is ChangeStatusEvent.ShowSnackbar -> {
                    // TODO: 스낵바 처리 (보통 다이얼로그를 띄운 화면에서 처리)
                }
            }
        }
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text("상태 변경") },
        text = {
            // 로딩 또는 에러 처리
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Text("오류: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    // 상태 선택 라디오 버튼 그룹
                    Column(Modifier.selectableGroup()) { // 라디오 그룹 접근성 지원
                        uiState.availableStatuses.forEach { status ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp) // 적절한 높이
                                    .selectable(
                                        selected = (uiState.selectedStatus == status),
                                        onClick = { viewModel.onStatusSelected(status) }, // ★ 콜백 연결
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (uiState.selectedStatus == status), // ★ 상태 바인딩
                                    onClick = null // Row의 selectable에서 처리하므로 null
                                )
                                Text(
                                    text = status.displayName, // ★ Enum의 표시 이름 사용
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = viewModel::updateStatus, // ★ 콜백 연결
                // 선택된 상태가 있고, 로딩/업데이트 중이 아닐 때 활성화
                enabled = uiState.selectedStatus != null && !uiState.isLoading && !uiState.isUpdating
            ) {
                if (uiState.isUpdating) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                } else {
                    Text("확인")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !uiState.isUpdating // 업데이트 중 아닐 때 활성화
            ) {
                Text("취소")
            }
        }
    )
}


// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun ChangeStatusDialogPreview() {
    val previewUiState = ChangeStatusUiState(
        currentStatus = UserStatus.ONLINE,
        selectedStatus = UserStatus.ONLINE
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        // AlertDialog 직접 호출하여 프리뷰
        AlertDialog(
            onDismissRequest = { },
            title = { Text("상태 변경") },
            text = {
                Column(Modifier.selectableGroup()) {
                    previewUiState.availableStatuses.forEach { status ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (previewUiState.selectedStatus == status),
                                    onClick = { /* Preview에선 동작 안 함 */ },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (previewUiState.selectedStatus == status),
                                onClick = null
                            )
                            Text(
                                text = status.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = {}) { Text("확인") } },
            dismissButton = { TextButton(onClick = {}) { Text("취소") } }
        )
    }
}
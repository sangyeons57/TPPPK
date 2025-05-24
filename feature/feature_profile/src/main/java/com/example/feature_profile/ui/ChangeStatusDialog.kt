package com.example.feature_profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
// import androidx.compose.foundation.selection.selectable // Keep if still needed for Row, but primary selection is via clickable
// import androidx.compose.foundation.selection.selectableGroup // Keep if still needed for Column, but primary selection is via clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role // May not be needed if RadioButtons are fully removed
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.UserStatus
import com.example.feature_profile.viewmodel.ChangeStatusEvent
import com.example.feature_profile.viewmodel.ChangeStatusUiState
import com.example.feature_profile.viewmodel.ChangeStatusViewModel
// Domain 모델 및 ViewModel 관련 요소 Import
import kotlinx.coroutines.flow.collectLatest

/**
 * ChangeStatusDialog: 사용자 상태 변경 다이얼로그 (Stateful)
 */
@Composable
fun ChangeStatusDialog(
    modifier: Modifier = Modifier,
    viewModel: ChangeStatusViewModel = hiltViewModel(),
    onDismissRequest: () -> Unit,
    onSuccess: (statusName: String) -> Unit // Callback for successful status change
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 이벤트 처리 및 성공 콜백 호출
    LaunchedEffect(uiState.updateSuccess, uiState.selectedStatus) {
        if (uiState.updateSuccess) {
            // Ensure selectedStatus is not null before calling onSuccess
            uiState.selectedStatus?.name?.let { statusName ->
                onSuccess(statusName)
            }
            // Dialog dismissal is now handled by ProfileViewModel after success
            // or can still be handled here if preferred, but ProfileViewModel also needs to know.
            // For now, let ProfileViewModel handle dismissal via onChangeStatusSuccess.
        }
    }

    // Handle general events like snackbar messages from ChangeStatusViewModel (optional)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ChangeStatusEvent.DismissDialog -> {
                    // This is tricky. If success, ProfileViewModel dismisses.
                    // If user cancels, this onDismissRequest is called.
                    // If update fails, it stays open.
                    // Let's assume DismissDialog from eventFlow is for user-initiated dismisses or explicit VM dismisses.
                    if (!uiState.updateSuccess) { // Only call if not already handled by success path
                        onDismissRequest()
                    }
                }
                is ChangeStatusEvent.ShowSnackbar -> {
                    // This dialog doesn't show its own snackbar, ProfileScreen does.
                    // However, this event could be used for other purposes if needed.
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
                        // Column remains for layout, selectableGroup might be optional without RadioButtons
                        // but doesn't harm if kept for accessibility structure.
                        Column(Modifier.selectableGroup()) {
                            uiState.availableStatuses.forEach { status ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp) // 적절한 높이
                                        .selectable(
                                            selected = (uiState.selectedStatus == status),
                                            onClick = { viewModel.onStatusSelected(status) },
                                            role = Role.RadioButton // Keep for accessibility, even without visible radio button
                                        )
                                        .background(
                                            color = if (uiState.selectedStatus == status) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                Color.Transparent // Or MaterialTheme.colorScheme.surface
                                            }
                                        )
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // RadioButton is removed.
                                    Text(
                                        text = status.name, // Displaying status.name as per instruction
                                        style = MaterialTheme.typography.bodyLarge,
                                        // Modifier.padding(start = 16.dp) // Adjust padding if RadioButton was removed
                                    )
                                }
                                if (status != uiState.availableStatuses.last()) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = viewModel::updateStatus,
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
                Column(Modifier.selectableGroup()) { // Keep selectableGroup for structure
                    previewUiState.availableStatuses.forEach { status ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable( // Use selectable on the Row
                                    selected = (previewUiState.selectedStatus == status),
                                    onClick = { /* Preview: Update selectedStatus for visual feedback */ },
                                    role = Role.RadioButton // Keep for accessibility
                                )
                                .background(
                                    if (previewUiState.selectedStatus == status) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // RadioButton removed
                            Text(
                                text = status.name,
                                style = MaterialTheme.typography.bodyLarge,
                                // modifier = Modifier.padding(start = 16.dp) // Padding adjusted
                            )
                        }
                         if (status != previewUiState.availableStatuses.last()) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = {}) { Text("확인") } },
            dismissButton = { TextButton(onClick = {}) { Text("취소") } }
        )
    }
}
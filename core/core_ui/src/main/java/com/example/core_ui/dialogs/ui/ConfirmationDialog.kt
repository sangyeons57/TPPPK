package com.example.core_ui.dialogs.ui

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.core_ui.R
import com.example.core_ui.dialogs.viewmodel.ConfirmationDialogState
import com.example.core_ui.dialogs.viewmodel.ConfirmationDialogViewModel
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme

/**
 * 재사용 가능한 확인 다이얼로그 UI입니다.
 *
 * @param state 다이얼로그의 상태를 나타내는 [ConfirmationDialogState] 객체.
 * @param onConfirm 사용자가 확인 버튼을 클릭했을 때 호출되는 함수입니다. 일반적으로 ViewModel의 `handleConfirm` 메서드와 연결됩니다.
 * @param onDismiss 사용자가 취소 버튼을 클릭했거나 다이얼로그 외부를 클릭하여 다이얼로그가 닫힐 때 호출되는 함수입니다. 일반적으로 ViewModel의 `handleDismiss` 메서드와 연결됩니다.
 * @param modifier 이 다이얼로그에 적용할 [Modifier]입니다.
 */
@Composable
fun ConfirmationDialog(
    state: ConfirmationDialogState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isVisible) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            title = {
                Text(text = state.title)
            },
            text = {
                Text(text = state.message)
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(state.confirmButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(state.dismissButtonText)
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmationDialogPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ConfirmationDialog(
            state = ConfirmationDialogState(
                isVisible = true,
                title = "미리보기 제목",
                message = "이것은 미리보기 메시지입니다. 정말로 진행하시겠습니까?",
                confirmButtonText = "예",
                dismissButtonText = "아니오"
            ),
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmationDialogHiddenPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ConfirmationDialog(
            state = ConfirmationDialogState(
                isVisible = false,
                title = "숨겨진 미리보기",
                message = "이 메시지는 보이지 않아야 합니다."
            ),
            onConfirm = {},
            onDismiss = {}
        )
    }
} 
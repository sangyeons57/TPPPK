package com.example.feature_settings.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.feature_settings.R // Assuming you'll add string resources

/**
 * A dialog to confirm user account withdrawal.
 * @param onConfirm Callback when the user confirms withdrawal.
 * @param onDismiss Callback when the user dismisses the dialog.
 */
@Composable
fun WithdrawalDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            // It's good practice to use string resources.
            // Text(stringResource(R.string.settings_withdrawal_dialog_title))
            Text("회원 탈퇴") // Placeholder, replace with string resource
        },
        text = {
            // Text(stringResource(R.string.settings_withdrawal_dialog_message))
            Text("정말로 계정을 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다.") // Placeholder
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                // Text(stringResource(R.string.settings_withdrawal_dialog_confirm))
                Text("탈퇴") // Placeholder
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                // Text(stringResource(R.string.settings_withdrawal_dialog_cancel))
                Text("취소") // Placeholder
            }
        }
    )
}

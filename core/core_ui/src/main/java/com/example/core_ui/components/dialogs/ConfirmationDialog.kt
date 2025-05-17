package com.example.core_ui.components.dialogs

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.core_ui.R

/**
 * A reusable confirmation dialog with consistent styling.
 *
 * @param title The title of the dialog
 * @param message The message to display in the dialog
 * @param confirmText The text for the confirm button (defaults to "Confirm")
 * @param dismissText The text for the dismiss button (defaults to "Cancel")
 * @param onConfirm Callback when the confirm button is clicked
 * @param onDismiss Callback when the dialog is dismissed or the dismiss button is clicked
 * @param isDestructive If true, the confirm button will be styled to indicate a destructive action
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = stringResource(id = R.string.confirm),
    dismissText: String = stringResource(id = R.string.cancel),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            ) 
        },
        text = { 
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            ) 
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Text(
                    text = confirmText,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = dismissText,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}

/**
 * A confirmation dialog that uses string resources for localization.
 *
 * @param titleRes The string resource ID for the title
 * @param messageRes The string resource ID for the message
 * @param confirmTextRes The string resource ID for the confirm button text
 * @param dismissTextRes The string resource ID for the dismiss button text
 * @param onConfirm Callback when the confirm button is clicked
 * @param onDismiss Callback when the dialog is dismissed or the dismiss button is clicked
 * @param isDestructive If true, the confirm button will be styled to indicate a destructive action
 */
@Composable
fun ConfirmationDialog(
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    @StringRes confirmTextRes: Int = R.string.confirm,
    @StringRes dismissTextRes: Int = R.string.cancel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    ConfirmationDialog(
        title = stringResource(id = titleRes),
        message = stringResource(id = messageRes),
        confirmText = stringResource(id = confirmTextRes),
        dismissText = stringResource(id = dismissTextRes),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDestructive = isDestructive
    )
}

package com.example.feature_home.dialog.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme

/**
 * 새 카테고리를 추가할 때 사용자로부터 카테고리 이름을 입력받는 다이얼로그입니다.
 *
 * @param initialCategoryName 초기 카테고리 이름 (기본값: "새 카테고리").
 * @param onDismiss 다이얼로그가 닫힐 때 호출되는 콜백입니다.
 * @param onConfirm 사용자가 확인 버튼을 클릭했을 때 호출되는 콜백입니다. 입력된 카테고리 이름을 전달합니다.
 */
@Composable
fun AddCategoryDialog(
    initialCategoryName: String = "새 카테고리",
    onDismiss: () -> Unit,
    onConfirm: (categoryName: String) -> Unit
) {
    var categoryName by remember { mutableStateOf(initialCategoryName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 카테고리 추가") },
        text = {
            Column {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("카테고리 이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("카테고리 이름을 입력해주세요.")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(categoryName) },
                enabled = categoryName.isNotBlank()
            ) {
                Text("추가")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun AddCategoryDialogPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        AddCategoryDialog(
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddCategoryDialogWithInitialNamePreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        AddCategoryDialog(
            initialCategoryName = "중요 채널",
            onDismiss = {},
            onConfirm = {}
        )
    }
} 
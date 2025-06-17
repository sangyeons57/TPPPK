package com.example.core_ui.components.bottom_sheet_dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetDialog(
    items: List<BottomSheetDialogItem>,
    onDismiss: () -> Unit
) {
    Log.d("BottomSheetDialog", "BottomSheetDialog is being composed")
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = modalBottomSheetState,
    ) {
        Column(
            // BottomSheet의 기본 패딩을 제거하여 Spacer 등이 정확하게 적용되도록 함
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            // 전달받은 아이템 목록을 순회하며 타입에 맞는 Composable을 렌더링
            items.forEach { item ->
                when (item) {
                    is BottomSheetDialogItem.Button -> {
                        ListItem(
                            headlineContent = { Text(item.label) },
                            leadingContent = {
                                item.icon?.let {
                                    Icon(imageVector = it, contentDescription = item.label)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    item.onClick()
                                }
                        )
                    }
                    is BottomSheetDialogItem.Text -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(text = item.text, style = item.style())
                        }
                    }
                    is BottomSheetDialogItem.Spacer -> {
                        Spacer(modifier = Modifier.height(item.height))
                    }
                }
            }
        }
    }
}
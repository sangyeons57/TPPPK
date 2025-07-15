package com.example.core_ui.components.reorder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 클릭 기반 순서 변경 다이얼로그
 * 드래그 앤 드롭 대신 위/아래 화살표 버튼으로 순서 변경
 */
@Composable
fun <T> SimpleReorderDialog(
    title: String,
    items: List<T>,
    itemKey: (T) -> String,
    itemLabel: (T) -> String,
    onDismiss: () -> Unit,
    onReorderComplete: (List<T>) -> Unit,
    modifier: Modifier = Modifier
) {
    var reorderedItems by remember { mutableStateOf(items) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 아이템 목록
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = reorderedItems,
                        key = { _, item -> itemKey(item) }
                    ) { index, item ->
                        ReorderableItem(
                            itemLabel = itemLabel(item),
                            isFirst = index == 0,
                            isLast = index == reorderedItems.lastIndex,
                            onMoveUp = {
                                if (index > 0) {
                                    val newList = reorderedItems.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index - 1]
                                    newList[index - 1] = temp
                                    reorderedItems = newList
                                }
                            },
                            onMoveDown = {
                                if (index < reorderedItems.lastIndex) {
                                    val newList = reorderedItems.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index + 1]
                                    newList[index + 1] = temp
                                    reorderedItems = newList
                                }
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 버튼
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
                            onReorderComplete(reorderedItems)
                            onDismiss()
                        }
                    ) {
                        Text("저장")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderableItem(
    itemLabel: String,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = itemLabel,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            
            // 위로 이동 버튼
            IconButton(
                onClick = onMoveUp,
                enabled = !isFirst
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "위로 이동",
                    tint = if (isFirst) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    else 
                        MaterialTheme.colorScheme.primary
                )
            }
            
            // 아래로 이동 버튼
            IconButton(
                onClick = onMoveDown,
                enabled = !isLast
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "아래로 이동",
                    tint = if (isLast) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    else 
                        MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
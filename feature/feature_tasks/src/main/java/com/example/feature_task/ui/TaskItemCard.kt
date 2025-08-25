package com.example.feature_task.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature_task.model.TaskUiModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TaskItem(
    task: TaskUiModel,
    isEditMode: Boolean,
    currentEditingTaskId: String?,
    editingContentMap: Map<String, String>,
    onEditingStateChange: (String) -> Unit,
    onContentChange: (String, String) -> Unit,
    onStatusChange: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
) {
    val isEditingThisCard = currentEditingTaskId == task.id.value
    val editingContent = editingContentMap[task.id.value] ?: task.content.value

    TaskCard(
        task = task,
        isEditMode = isEditMode,
        isEditingThisCard = isEditingThisCard,
        editingContent = editingContent,
        onEditingStateChange = onEditingStateChange,
        onContentChange = onContentChange,
        onStatusChange = onStatusChange,
        onDelete = onDelete,
        onMoveUp = onMoveUp,
        onMoveDown = onMoveDown,
    )
}

@Composable
fun TaskCard(
    task: TaskUiModel,
    isEditMode: Boolean,
    isEditingThisCard: Boolean,
    editingContent: String,
    onEditingStateChange: (String) -> Unit,
    onContentChange: (String, String) -> Unit,
    onStatusChange: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
) {
    val focusRequester = remember(task.id) { FocusRequester() }

    LaunchedEffect(isEditingThisCard) {
        if (isEditingThisCard) {
            try {
                kotlinx.coroutines.delay(50)
                focusRequester.requestFocus()
            } catch (e: IllegalStateException) {
                android.util.Log.w("TaskCard", "Focus request failed: ${e.message}")
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEditMode) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (task.taskType.isCheckbox()) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = if (isEditMode) null else { isChecked ->
                            onStatusChange(task.id.value, isChecked)
                        },
                        enabled = !isEditMode
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (isEditMode && isEditingThisCard) {
                        val focusManager = LocalFocusManager.current
                        OutlinedTextField(
                            value = editingContent,
                            onValueChange = { newContent ->
                                onContentChange(
                                    task.id.value,
                                    newContent
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    onEditingStateChange(task.id.value)
                                    focusManager.clearFocus()
                                }
                            ),
                            minLines = 1,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                    } else if (isEditMode) {
                        Text(
                            text = task.content.value,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .clickable { onEditingStateChange(task.id.value) }
                        )
                    } else {
                        Text(
                            text = task.content.value,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )

                        if (task.isCompleted && task.checkedBy != null && task.checkedAt != null && task.checkedByName != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "체크됨: ${task.checkedByName} • ${
                                    formatTime(
                                        task.checkedAt!!
                                    )
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            if (isEditMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onMoveUp(task.id.value) }) {
                        Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "위로")
                    }
                    IconButton(onClick = { onMoveDown(task.id.value) }) {
                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "아래로")
                    }
                    IconButton(onClick = { onDelete(task.id.value) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "삭제",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(instant: Instant): String {
    val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val now = LocalDateTime.now()

    return when {
        localDateTime.toLocalDate() == now.toLocalDate() -> localDateTime.format(
            DateTimeFormatter.ofPattern(
                "HH:mm"
            )
        )

        localDateTime.toLocalDate() == now.toLocalDate().minusDays(1) -> "어제 ${
            localDateTime.format(
                DateTimeFormatter.ofPattern("HH:mm")
            )
        }"

        localDateTime.year == now.year -> localDateTime.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
        else -> localDateTime.format(DateTimeFormatter.ofPattern("yy/MM/dd HH:mm"))
    }
}

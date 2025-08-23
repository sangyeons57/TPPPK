package com.example.feature_task.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Comment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.core_ui.components.fab.ExtendableFab
import com.example.core_ui.components.fab.FabLabelStyle
import com.example.core_ui.components.fab.FabMenuItem
import com.example.domain.vo.task.TaskType

@Composable
fun TaskCreationFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCreateTask: (TaskType) -> Unit
) {
    val menuItems = remember {
        listOf(
            FabMenuItem(
                icon = Icons.Default.CheckBox,
                text = "체크리스트",
                contentDescription = "체크리스트 작업 생성",
                onClick = {
                    onCreateTask(TaskType.CHECKLIST)
                    onExpandedChange(false)
                }
            ),
            FabMenuItem(
                icon = Icons.Default.Comment,
                text = "메모",
                contentDescription = "메모 작업 생성",
                onClick = {
                    onCreateTask(TaskType.COMMENT)
                    onExpandedChange(false)
                }
            )
        )
    }

    ExtendableFab(
        menuItems = menuItems,
        isExpanded = expanded,
        onExpandedChange = onExpandedChange,
        labelStyle = FabLabelStyle.CARD
    )
}


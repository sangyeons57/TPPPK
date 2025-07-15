package com.example.feature_edit_channel.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.feature_edit_channel.viewmodel.ChannelReorderTestViewModel
import com.example.feature_edit_channel.viewmodel.ReorderItem

@Composable
fun ChannelReorderTestDialog(
    projectId: String,
    onDismissRequest: () -> Unit,
    viewModel: ChannelReorderTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(projectId) {
        viewModel.initialize(projectId)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(MaterialTheme.shapes.medium),
            tonalElevation = 8.dp
        ) {
            if (uiState.currentCategory == null) {
                RootReorderContent(
                    items = uiState.items,
                    onMoveUp = viewModel::moveItemUp,
                    onMoveDown = viewModel::moveItemDown,
                    onCategoryClick = viewModel::onCategorySelected
                )
            } else {
                val category = uiState.currentCategory
                CategoryChannelReorderContent(
                    categoryName = category!!.name.value,
                    channels = uiState.categoryChannels,
                    onMoveUp = viewModel::moveChannelUp,
                    onMoveDown = viewModel::moveChannelDown,
                    onBack = viewModel::onBackToRoot
                )
            }
        }
    }
}

@Composable
private fun RootReorderContent(
    items: List<ReorderItem>,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onCategoryClick: (ReorderItem.CategoryItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "프로젝트 구조 테스트",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Divider()
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(items) { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = item is ReorderItem.CategoryItem) {
                            if (item is ReorderItem.CategoryItem) onCategoryClick(item)
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (item) {
                            is ReorderItem.CategoryItem -> "[C] ${'$'}{item.category.name.value}"
                            is ReorderItem.ChannelItem -> "[Ch] ${'$'}{item.channel.channelName.value}"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onMoveUp(index) }) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Up")
                    }
                    IconButton(onClick = { onMoveDown(index) }) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Down")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChannelReorderContent(
    categoryName: String,
    channels: List<com.example.domain.model.base.ProjectChannel>,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowUpward, contentDescription = "Back")
            }
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Divider()
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(channels) { index, channel ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = channel.channelName.value,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onMoveUp(index) }) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Up")
                    }
                    IconButton(onClick = { onMoveDown(index) }) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Down")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChannelReorderTestDialogPreview() {
    ChannelReorderTestDialog(projectId = "test_project", onDismissRequest = {})
}


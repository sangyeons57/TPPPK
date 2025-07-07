package com.example.feature_project_detail.ui

// Import new UI models and VM-specific data classes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.NavigationManger
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.project.ProjectName
import com.example.feature_model.CategoryUiModel
import com.example.feature_model.ChannelUiModel
import com.example.feature_project_detail.viewmodel.CreateChannelDialogData
import com.example.feature_project_detail.viewmodel.ProjectDetailViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    navigationManger: NavigationManger,
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar on error
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            // Optionally clear the error in the viewmodel after showing
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.projectName.ifEmpty { ProjectName("프로젝트 상세") }.value) }, // TODO: Use actual project name
                navigationIcon = {
                    DebouncedBackButton(onClick = { navigationManger.navigateBack() })
                },
                actions = {
                    // TODO: Add Project Settings Button
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateDirectChannelDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "직속 채널 추가")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            ProjectStructureList(
                modifier = Modifier.padding(paddingValues),
                categories = uiState.categories,
                directChannels = uiState.directChannels,
                onChannelClick = { channel ->
                    // Navigate to ChatScreen using new direct API
                    navigationManger.navigateToChat(channel.id.value)
                },
                onAddChannelInCategoryClick = { categoryId ->
                    viewModel.showCreateCategoryChannelDialog(categoryId)
                }
            )
        }
    }

    // Create Channel Dialog
    if (uiState.showCreateChannelDialog && uiState.createChannelDialogData != null) {
        CreateChannelDialog(
            dialogData = uiState.createChannelDialogData,
            onDismiss = viewModel::dismissCreateChannelDialog,
            onConfirm = viewModel::confirmCreateChannel,
            onNameChange = viewModel::updateCreateChannelDialogName,
            onChannelModeChange = viewModel::updateCreateChannelDialogChannelMode
        )
    }
}

@Composable
private fun ProjectStructureList(
    modifier: Modifier = Modifier,
    categories: List<CategoryUiModel>,
    directChannels: List<ChannelUiModel>,
    onChannelClick: (ChannelUiModel) -> Unit,
    onAddChannelInCategoryClick: (categoryId: DocumentId) -> Unit
) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        // Direct Channels Section
        if (directChannels.isNotEmpty()) {
            item {
                Text("직속 채널", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(items = directChannels, key = { it.id.value }) {
                ChannelItem(channel = it, onClick = { onChannelClick(it) })
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Categories Section
        items(items = categories, key = { it.id.value }) { category ->
            CategoryItem(
                category = category,
                onChannelClick = onChannelClick,
                onAddChannelClick = { onAddChannelInCategoryClick(category.id) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CategoryItem(
    category: CategoryUiModel,
    onChannelClick: (ChannelUiModel) -> Unit,
    onAddChannelClick: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier=Modifier.fillMaxWidth()) {
            Text(
                category.name.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onAddChannelClick, modifier=Modifier.size(24.dp)) {
                 Icon(Icons.Default.Add, contentDescription = "${category.name}에 채널 추가", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (category.channels.isEmpty()) {
            Text("채널 없음", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                category.channels.forEach {
                    ChannelItem(channel = it, onClick = { onChannelClick(it) })
                }
            }
        }
    }
}

@Composable
private fun ChannelItem(
    channel: ChannelUiModel,
    onClick: () -> Unit
) {
    Text(
        text = "# ${channel.name}",
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 8.dp),
        style = MaterialTheme.typography.bodyLarge
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateChannelDialog(
    dialogData: CreateChannelDialogData?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onNameChange: (String) -> Unit,
    onChannelModeChange: (ProjectChannelType) -> Unit
) {
    if (dialogData == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (dialogData.categoryId == null) "직속 채널 생성" else "카테고리 채널 생성") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dialogData.channelName.value,
                    onValueChange = { onNameChange },
                    label = { Text("채널 이름") },
                    singleLine = true
                )
                // TODO: Add Channel Type Selection (e.g., Radio buttons or Dropdown for TEXT, VOICE)
                Text("채널 모드: ${dialogData.channelMode}") // Placeholder, assuming channelType is now channelMode string
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("생성")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
} 
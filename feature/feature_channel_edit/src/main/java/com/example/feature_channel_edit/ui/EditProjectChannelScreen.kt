package com.example.feature_channel_edit.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_ui.components.loading.LoadingIndicator
import com.example.feature_channel_edit.viewmodel.EditChannelEvent
import com.example.feature_channel_edit.viewmodel.EditProjectChannelViewModel

/**
 * Composable screen for editing a project channel.
 *
 * @param onNavigateBack Callback to navigate back to the previous screen.
 * @param viewModel The ViewModel for this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProjectChannelScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditProjectChannelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current // For potential Toast messages, though Snackbar is preferred
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is EditChannelEvent.SaveSuccess -> {
                    onNavigateBack()
                }
                is EditChannelEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Channel") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.saveChannel() }) {
                Text("Save") // Consider an Icon (e.g., Icons.Filled.Save)
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading && !uiState.isSaveAttempted) { // Show full screen loading only on initial load
            LoadingIndicator(modifier= Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.generalError != null) {
                    Text(
                        text = uiState.generalError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = uiState.channelNameInput,
                    onValueChange = { viewModel.onNameChange(it) },
                    label = { Text("Channel Name") },
                    isError = uiState.nameError != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (uiState.nameError != null) {
                    Text(
                        text = uiState.nameError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                OutlinedTextField(
                    value = uiState.channelOrderInput,
                    onValueChange = { viewModel.onOrderChange(it) },
                    label = { Text("Channel Order (e.g., 1.01, 2.50)") },
                    isError = uiState.orderError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (uiState.orderError != null) {
                    Text(
                        text = uiState.orderError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                if (uiState.isLoading && uiState.isSaveAttempted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator()
                }
            }
        }
    }
}

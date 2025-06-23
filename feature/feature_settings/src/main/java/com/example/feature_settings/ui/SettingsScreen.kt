package com.example.feature_settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.core_navigation.core.NavigationManger
import com.example.feature_settings.viewmodel.SettingsViewModel // Import the ViewModel
import com.example.feature_settings.viewmodel.SettingsViewModel.WithdrawalUiEvent
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigationManger: NavigationManger,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val showWithdrawalDialog by viewModel.showWithdrawalDialog.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect UI events from the ViewModel to show Snackbars
    LaunchedEffect(key1 = viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is WithdrawalUiEvent.Success -> {
                    snackbarHostState.showSnackbar(message = event.message)
                    // Navigation is handled in the ViewModel, so no action is needed here.
                }
                is WithdrawalUiEvent.Error -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navigationManger.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Other settings items can be added here

            Spacer(modifier = Modifier.height(16.dp))

            // Withdraw Account option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.onWithdrawAccountClick() } // Use ViewModel
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "회원 탈퇴",
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "회원 탈퇴",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Potentially add a Divider here
            // HorizontalDivider()
        }

        if (showWithdrawalDialog) { // Observe ViewModel state
            WithdrawalDialog(
                onConfirm = {
                    viewModel.confirmWithdrawal()
                },
                onDismiss = {
                    viewModel.dismissWithdrawalDialog()
                }
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        navigationManger = TODO(),
        viewModel = TODO()
    )
}
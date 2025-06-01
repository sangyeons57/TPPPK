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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.feature_settings.viewmodel.SettingsViewModel // Import the ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val showWithdrawalDialog by viewModel.showWithdrawalDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                    contentDescription = "Withdraw Account",
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Withdraw Account",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Potentially add a Divider here
            // HorizontalDivider()
        }

        if (showWithdrawalDialog) { // Observe ViewModel state
            /**
            WithdrawalDialog(
                onConfirm = {
                    viewModel.confirmWithdrawal() // Use ViewModel
                },
                onDismiss = {
                    viewModel.dismissWithdrawalDialog() // Use ViewModel
                }
            )
            **/
        }
    }
}

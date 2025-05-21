package com.example.feature_main.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.core_common.dispatcher.DispatcherProvider // For MockViewModel
import com.example.core_navigation.core.AppNavigator
import com.example.core_navigation.core.NavDestination
import com.example.core_navigation.core.NavigationCommand
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.User // For MockViewModel and Previews
import com.example.feature_main.viewmodel.EditProfileEvent
import com.example.feature_main.viewmodel.EditProfileUiState
import com.example.feature_main.viewmodel.EditProfileViewModel
// AppRoutes and other navigation imports are fine if AppNavigator handles them
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    appNavigator: AppNavigator,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            viewModel.handleImageSelection(uri)
        }
    )

    LaunchedEffect(key1 = viewModel.eventFlow) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditProfileEvent.NavigateBack -> {
                    appNavigator.navigate(NavigationCommand.NavigateBack)
                }
                is EditProfileEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is EditProfileEvent.RequestImagePick -> {
                    imagePickerLauncher.launch("image/*")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("프로필 수정") },
                navigationIcon = {
                    // IconButton(onClick = { appNavigator.navigate(NavigationCommand.NavigateBack) }) {
                    //     Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기")
                    // }
                    // For now, let ViewModel handle navigation back on save or if a dedicated back button in UI is pressed
                }
            )
        },
        content = { paddingValues ->
            EditProfileContent(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onNameChanged = viewModel::onNameChanged,
                onProfileImageClicked = viewModel::onProfileImageClicked,
                onSaveProfileClicked = viewModel::onSaveProfileClicked
            )
        }
    )
}

@Composable
fun EditProfileContent(
    modifier: Modifier = Modifier,
    uiState: EditProfileUiState, // uiState now contains user: User?
    onNameChanged: (String) -> Unit,
    onProfileImageClicked: () -> Unit,
    onSaveProfileClicked: () -> Unit
) {
    // val currentUser = uiState.user // No need for this local var, can use uiState.user directly

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Image
        AsyncImage(
            model = uiState.user?.profileImageUrl, // Use uiState.user directly
            contentDescription = "Profile Image",
            error = rememberVectorPainter(Icons.Filled.AccountCircle),
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .clickable { onProfileImageClicked() }
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.user?.name ?: "", // Use uiState.user directly
            onValueChange = onNameChanged,
            label = { Text("이름") },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.user != null // Disable if user data is not loaded
        )

        Spacer(modifier = Modifier.weight(1F)) // Pushes save button to bottom

        Button(
            onClick = onSaveProfileClicked,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("저장하기")
            }
        }

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun EditProfileContentPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        EditProfileContent(
            uiState = EditProfileUiState(user = User(id="prev", name = "김철수", email="e"), isLoading = false),
            onNameChanged = {},
            onProfileImageClicked = {},
            onSaveProfileClicked = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EditProfileContentLoadingPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        EditProfileContent(
            uiState = EditProfileUiState(user = null, isLoading = true), // User is null during loading
            onNameChanged = {},
            onProfileImageClicked = {},
            onSaveProfileClicked = {}
        )
    }
}

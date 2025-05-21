package com.example.feature_profile.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.core_navigation.core.AppNavigator
import com.example.core_navigation.core.NavigationCommand
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.User // For MockViewModel and Previews
import com.example.feature_profile.viewmodel.EditProfileEvent
import com.example.feature_profile.viewmodel.EditProfileUiState
import com.example.feature_profile.viewmodel.EditProfileViewModel
// AppRoutes and other navigation imports are fine if AppNavigator handles them
import kotlinx.coroutines.flow.collectLatest
import android.Manifest // 권한 import
import android.os.Build // Build version 확인
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext // Context 접근
import com.google.accompanist.permissions.ExperimentalPermissionsApi // Accompanist
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * EditProfileScreen: 프로필 편집 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class) // ExperimentalPermissionsApi 추가
@Composable
fun EditProfileScreen(
    appNavigator: AppNavigator,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 이미지 읽기 권한 설정 (Android 버전에 따라 분기)
    val readImagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(permission = readImagePermission)

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
                    // 권한 상태에 따라 이미지 선택기 실행 또는 권한 요청
                    if (permissionState.status.isGranted) {
                        imagePickerLauncher.launch("image/*")
                    } else {
                        // 권한이 없다면 요청
                        // shouldShowRationale은 사용자가 이전에 권한 요청을 거부한 경우 true를 반환
                        // 이 경우 사용자에게 왜 권한이 필요한지 설명하는 것이 좋음
                        permissionState.launchPermissionRequest()
                    }
                }
            }
        }
    }

    // 권한 상태 변경 시 스낵바 알림 (선택 사항)
    LaunchedEffect(permissionState.status) {
        if (!permissionState.status.isGranted && permissionState.status.shouldShowRationale) {
            // 사용자가 권한을 거부했지만, 설명을 다시 보여줄 수 있는 경우
            snackbarHostState.showSnackbar(
                message = "프로필 이미지 변경을 위해 사진 접근 권한이 필요합니다.",
                duration = SnackbarDuration.Long
            )
        } else if (!permissionState.status.isGranted && !permissionState.status.shouldShowRationale && permissionState.permissionRequested) {
            // 사용자가 권한을 영구적으로 거부한 경우 (다시 묻지 않음 선택)
            // 이 경우 설정 앱으로 이동하여 권한을 직접 변경하도록 안내할 수 있음
            snackbarHostState.showSnackbar(
                message = "사진 접근 권한이 거부되었습니다. 설정에서 권한을 허용해주세요.",
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("프로필 수정") },
                navigationIcon = {
                    DebouncedBackButton(onClick = { appNavigator.navigate(NavigationCommand.NavigateBack) })
                }
            )
        },
        content = { paddingValues ->
            if (uiState.isLoading && uiState.user == null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                EditProfileContent(
                    modifier = Modifier.padding(paddingValues),
                    uiState = uiState,
                    onNameChanged = viewModel::onNameChanged,
                    onProfileImageClicked = viewModel::onProfileImageClicked,
                    onSaveProfileClicked = viewModel::onSaveProfileClicked
                )
            }
        }
    )
}

/**
 * EditProfileContent: 프로필 편집 UI 요소 (Stateless)
 */
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
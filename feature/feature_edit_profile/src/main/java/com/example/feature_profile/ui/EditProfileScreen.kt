package com.example.feature_profile.ui

// Removed direct Coil imports, will use UserProfileImage
// AppRoutes and other navigation imports are fine if AppNavigator handles them
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.core_navigation.core.NavigationManger
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.components.user.UserProfileImage
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.base.User
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.enum.UserStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.model.vo.user.UserName
import com.example.feature_profile.viewmodel.EditProfileEvent
import com.example.feature_profile.viewmodel.EditProfileUiState
import com.example.feature_profile.viewmodel.EditProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * EditProfileScreen: 프로필 편집 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navigationManger: NavigationManger,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 이미지 읽기 권한 설정 (Android 버전에 따라 분기)
    val readImagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            viewModel.handleImageSelection(uri)
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                imagePickerLauncher.launch("image/*")
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "사진 접근 권한이 거부되었습니다. 설정에서 권한을 허용해주세요.",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    )


    LaunchedEffect(key1 = viewModel.eventFlow) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditProfileEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is EditProfileEvent.RequestImagePick -> {
                    Log.d("EditProfileScreen", "Requesting image pick...")
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(context, readImagePermission) -> {
                            imagePickerLauncher.launch("image/*")
                        }
                        else -> {
                            permissionLauncher.launch(readImagePermission)
                        }
                    }
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
                    DebouncedBackButton(onClick = { navigationManger.navigateBack() })
                }
            )
        },
        content = { paddingValues ->
            if (uiState.isLoading && uiState.user == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
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
        UserProfileImage(
            profileImageUrl = uiState.user?.profileImageUrl?.value,
            contentDescription = "Profile Image",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .clickable { onProfileImageClicked() }
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))


        OutlinedTextField(
            value = uiState.user?.name?.value ?: "", // Use uiState.user directly
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
            uiState = EditProfileUiState(
                user = User.fromDataSource(
                    id = DocumentId("preview-user"),
                    email = UserEmail("preview@test.com"),
                    name = UserName("Preview User"),
                    consentTimeStamp = Instant.now(),
                    profileImageUrl = ImageUrl(""),
                    memo = null,
                    userStatus = UserStatus.ONLINE,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                    fcmToken = null,
                    accountStatus = UserAccountStatus.ACTIVE
                )
            ),
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
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
import androidx.compose.foundation.layout.width
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
import coil.compose.AsyncImage
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
                        "이미지 접근 권한이 필요합니다. 설정 > 앱 권한에서 미디어 권한을 허용해주세요.",
                        duration = SnackbarDuration.Long
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
                    
                    // Android 13 (API 33) 이상에서는 PhotoPicker 사용 시 권한이 필요 없음
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        imagePickerLauncher.launch("image/*")
                    } else {
                        // Android 12 이하에서는 READ_EXTERNAL_STORAGE 권한 필요
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

                is EditProfileEvent.ShowRemoveImageConfirmation -> TODO()
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
                    onSaveProfileClicked = viewModel::onSaveProfileClicked,
                    onSetDefaultProfileClicked = viewModel::onSetDefaultProfileClicked
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
    onSaveProfileClicked: () -> Unit,
    onSetDefaultProfileClicked: () -> Unit
) {
    // val currentUser = uiState.user // No need for this local var, can use uiState.user directly

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Image - 선택된 이미지가 있으면 미리보기, 없으면 기존 이미지
        if (uiState.selectedImageUri != null) {
            // 선택된 이미지 미리보기
            AsyncImage(
                model = uiState.selectedImageUri,
                contentDescription = "Selected Profile Image",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable { onProfileImageClicked() }
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // 기존 프로필 이미지
            UserProfileImage(
                userId = uiState.user?.id?.value,
                contentDescription = "Profile Image",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable { onProfileImageClicked() }
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        
        // 이미지 선택 안내 텍스트
        if (uiState.selectedImageUri != null) {
            Text(
                text = "새 이미지가 선택되었습니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = "프로필 이미지를 탭하여 변경하세요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 기본 프로필 사용 버튼
        Button(
            onClick = onSetDefaultProfileClicked,
            enabled = !uiState.isRemovingImage && !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
        ) {
            if (uiState.isRemovingImage) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("설정 중...")
            } else {
                Text("기본 프로필 사용")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))


        OutlinedTextField(
            value = uiState.nameInput,
            onValueChange = onNameChanged,
            label = { Text("이름") },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.user != null
        )

        OutlinedTextField(
            value = uiState.user?.email?.value ?: "",
            onValueChange = { },
            label = { Text("이메일") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            readOnly = true
        )

        Spacer(modifier = Modifier.weight(1F)) // Pushes save button to bottom

        Button(
            onClick = onSaveProfileClicked,
            enabled = !uiState.isLoading && uiState.hasChanges,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(if (uiState.hasChanges) "저장하기" else "변경사항 없음")
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
                    memo = null,
                    userStatus = UserStatus.ONLINE,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                    fcmToken = null,
                    accountStatus = UserAccountStatus.ACTIVE
                ),
                originalUser = User.fromDataSource(
                    id = DocumentId("preview-user"),
                    email = UserEmail("preview@test.com"),
                    name = UserName("Original User"),
                    consentTimeStamp = Instant.now(),
                    memo = null,
                    userStatus = UserStatus.ONLINE,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                    fcmToken = null,
                    accountStatus = UserAccountStatus.ACTIVE
                ),
                hasChanges = true
            ),
            onNameChanged = {},
            onProfileImageClicked = {},
            onSaveProfileClicked = {},
            onSetDefaultProfileClicked = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EditProfileContentLoadingPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        EditProfileContent(
            uiState = EditProfileUiState(
                user = null, 
                originalUser = null,
                isLoading = true
            ), // User is null during loading
            onNameChanged = {},
            onProfileImageClicked = {},
            onSaveProfileClicked = {},
            onSetDefaultProfileClicked = {}
        )
    }
}
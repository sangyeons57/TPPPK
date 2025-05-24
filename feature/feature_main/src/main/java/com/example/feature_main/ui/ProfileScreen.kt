package com.example.feature_main.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit // 편집 아이콘
import androidx.compose.material.icons.filled.PhotoCamera // 카메라/갤러리 아이콘
import androidx.compose.material.icons.filled.Settings // 설정 아이콘
import androidx.compose.material.icons.filled.Person // 사용자 아이콘 (프로필 수정용으로 사용 가능)
import androidx.compose.material.icons.filled.People // 친구 아이콘
import androidx.compose.material.icons.filled.Done // 완료 아이콘
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import android.util.Log
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.feature_main.viewmodel.ProfileEvent
import com.example.feature_main.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core_navigation.core.NavigationCommand
import com.example.core_navigation.core.AppNavigator
import com.example.core_navigation.core.NavDestination
import com.example.core_navigation.destination.AppRoutes
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.User
import com.example.domain.model.UserProfileData
import com.example.feature_profile.ui.ChangeStatusDialog // Import ChangeStatusDialog

/**
 * ProfileScreen: 상태 관리 및 이벤트 처리 (Stateful)
 **/
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    appNavigator: AppNavigator,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // State for in-place status editing
    var isEditingStatusMessage by rememberSaveable { mutableStateOf(false) }
    var editableStatusMessage by rememberSaveable { mutableStateOf(uiState.userProfile?.statusMessage ?: "") }

    // Initialize editableStatusMessage when profile data is loaded or changed
    LaunchedEffect(uiState.userProfile?.statusMessage) {
        if (!isEditingStatusMessage) {
            editableStatusMessage = uiState.userProfile?.statusMessage ?: ""
        }
    }

    // 이미지 선택기를 위한 ActivityResultLauncher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            // 결과 URI를 ViewModel로 전달
            viewModel.changeProfileImage(uri)
        }
    )

    // 이벤트 처리 (네비게이션, 다이얼로그, 스낵바, 이미지 피커)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ProfileEvent.NavigateToSettings -> appNavigator.navigate(NavigationCommand.NavigateToRoute(NavDestination.fromRoute(AppRoutes.Settings.EDIT_MY_PROFILE)))
                is ProfileEvent.NavigateToEditProfile -> {
                    Log.d("ProfileScreen", "Navigating to Edit Profile Screen")
                    appNavigator.navigate(NavigationCommand.NavigateToRoute(NavDestination.fromRoute(AppRoutes.Settings.EDIT_MY_PROFILE)))
                }
                is ProfileEvent.NavigateToFriends -> appNavigator.navigate(NavigationCommand.NavigateToRoute(NavDestination.fromRoute(AppRoutes.Friends.LIST)))
                // NavigateToStatus event is removed, dialog is shown via UiState
                is ProfileEvent.PickProfileImage -> {
                    // 이미지 선택기 실행 (MIME 타입 지정)
                    imagePickerLauncher.launch("image/*")
                }
                is ProfileEvent.LogoutCompleted -> {
                    // 로그인 화면으로 이동 (스택 클리어는 NavigationHandler 구현에서 처리하거나 별도 Command 필요)
                    appNavigator.navigateClearingBackStack(NavigationCommand.NavigateClearingBackStack(destination = NavDestination.fromRoute(AppRoutes.Auth.Login.path)))
                }
                is ProfileEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // 에러 메시지 스낵바로 표시
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            snackbarHostState.showSnackbar(uiState.errorMessage!!)
            viewModel.errorMessageShown()
        }
    }

    // Scaffold 사용은 선택 사항 (MainScreen에서 이미 사용 중이므로 Column만 사용해도 무방)
    Box(modifier = modifier.fillMaxSize()) { // Box 안에 배치하여 로딩 인디케이터 오버레이
        if (uiState.isLoading && uiState.userProfile == null) { // 초기 전체 로딩 시
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            // 로딩이 아니거나, 부분 로딩(업데이트) 중일 때는 콘텐츠 표시
            ProfileContent(
                modifier = Modifier.fillMaxSize(), // Box 채우도록
                isLoading = uiState.isLoading, // 부분 로딩 상태 전달
                profile = uiState.userProfile,
                onEditProfileImageClick = viewModel::onEditProfileImageClick,
                // onEditStatusClick = viewModel::onEditStatusClick, // Replaced by direct state manipulation
                onSettingsClick = viewModel::onSettingsClick,
                onLogoutClick = viewModel::onLogoutClick,
                onFriendsClick = viewModel::onFriendsClick,
                onStatusClick = viewModel::onChangeStatusClick, // Updated to call renamed function
                onEditProfileClick = viewModel::onEditProfileClicked, // 추가된 부분
                // Pass state and event handlers for status editing
                isEditingStatusMessage = isEditingStatusMessage,
                editableStatusMessage = editableStatusMessage,
                onStatusMessageChange = { newMessage ->
                    if (newMessage.length <= 50) { // Character limit
                        editableStatusMessage = newMessage
                    }
                },
                onToggleEditStatus = {
                    isEditingStatusMessage = !isEditingStatusMessage
                    if (isEditingStatusMessage) {
                        // When starting to edit, sync with the latest profile status
                        editableStatusMessage = uiState.userProfile?.statusMessage ?: ""
                    }
                },
                onSubmitStatusMessage = {
                    if (editableStatusMessage != uiState.userProfile?.statusMessage) {
                        viewModel.changeStatusMessage(editableStatusMessage)
                    }
                    isEditingStatusMessage = false
                }
            )
        }
        // 전체 화면 로딩 인디케이터 (선택 사항)
        if (uiState.isLoading && uiState.userProfile != null) { // 데이터가 있는데 업데이트 중일 때
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(40.dp) // 중앙에 작게
            )
        }

        // Conditionally display ChangeStatusDialog
        if (uiState.showChangeStatusDialog) {
            ChangeStatusDialog(
                onDismissRequest = viewModel::onDismissChangeStatusDialog,
                onSuccess = viewModel::onChangeStatusSuccess // Pass the success callback
                // viewModel for ChangeStatusDialog is Hilt-injected by default
            )
        }
    }
}

/**
 * ProfileContent: UI 렌더링 (Stateless)
 */
@Composable
fun ProfileContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean, // 부분 로딩 상태 (예: 버튼 비활성화용)
    profile: UserProfileData?, // Nullable 사용자 프로필
    onEditProfileImageClick: () -> Unit,
    // onEditStatusClick: () -> Unit, // Replaced
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onStatusClick: () -> Unit,
    onEditProfileClick: () -> Unit, // 추가된 파라미터
    // Parameters for in-place status editing
    isEditingStatusMessage: Boolean,
    editableStatusMessage: String,
    onStatusMessageChange: (String) -> Unit,
    onToggleEditStatus: () -> Unit,
    onSubmitStatusMessage: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // 스크롤 가능하도록
            .padding(bottom = 32.dp), // 하단 여백 추가
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // 프로필 이미지 영역
        Box(contentAlignment = Alignment.BottomEnd) {
            // 이미지 편집 버튼
            IconButton(
                onClick = onEditProfileImageClick,
                modifier = Modifier
                    .size(36.dp)
                    .offset(x = 4.dp, y = 4.dp) // 위치 미세 조정
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera, // 또는 Edit 아이콘
                    contentDescription = "프로필 이미지 변경",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 사용자 이름
        Text(
            text = profile?.name ?: "사용자 이름",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 상태 메시지 (In-place Editable)
        OutlinedTextField(
            value = editableStatusMessage,
            onValueChange = onStatusMessageChange,
            label = { Text("상태 메시지") },
            singleLine = true,
            readOnly = !isEditingStatusMessage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && isEditingStatusMessage) {
                        onSubmitStatusMessage() // Submit when focus is lost
                    }
                },
            trailingIcon = {
                IconButton(onClick = {
                    if (isEditingStatusMessage) {
                        onSubmitStatusMessage() // Submit if already editing (acts as a save button)
                        focusManager.clearFocus() // Clear focus after submit
                    } else {
                        onToggleEditStatus() // Start editing
                        // It's good practice to request focus *after* the field becomes editable
                        // LaunchedEffect can be used here if focusRequester.requestFocus() needs to be called
                        // after the recomposition that makes the field editable.
                        // For simplicity, direct call might work on some Compose versions or scenarios.
                        // If focus is not reliably gained, use LaunchedEffect(isEditingStatusMessage) { ... }
                        if (isEditingStatusMessage) { // Check again as onToggleEditStatus changes it
                             LaunchedEffect(Unit) { // Request focus after recomposition
                                focusRequester.requestFocus()
                            }
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (isEditingStatusMessage) Icons.Filled.Done else Icons.Filled.Edit,
                        contentDescription = if (isEditingStatusMessage) "상태 메시지 저장" else "상태 메시지 변경"
                    )
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                onSubmitStatusMessage()
                focusManager.clearFocus() // Clear focus after submitting with Done action
            }),
            supportingText = {
                Text(text = "${editableStatusMessage.length} / 50")
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 구분선
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // 메뉴 아이템들
        ProfileMenuItem(
            text = "프로필 수정",
            icon = Icons.Filled.Edit, // 또는 Icons.Filled.Person
            onClick = onEditProfileClick,
            enabled = !isLoading
        )

        ProfileMenuItem(
            text = "상태 표시",
            icon = Icons.Filled.Settings,
            onClick = onStatusClick,
            enabled = !isLoading // 로딩 중 아닐 때만 클릭 가능
        )

        ProfileMenuItem(
            text = "친구",
            icon = Icons.Filled.People, // 아이콘 변경
            onClick = onFriendsClick,
            enabled = !isLoading // 로딩 중 아닐 때만 클릭 가능
        )

        ProfileMenuItem(
            text = "설정",
            icon = Icons.Filled.Settings,
            onClick = onSettingsClick,
            enabled = !isLoading // 로딩 중 아닐 때만 클릭 가능
        )
        ProfileMenuItem(
            text = "로그아웃",
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            onClick = onLogoutClick,
            enabled = !isLoading
        )
        // TODO: 다른 메뉴 아이템 추가 가능 (예: 공지사항, 고객센터 등)

    }
}

// 프로필 화면 메뉴 아이템 Composable (재사용 가능)
@Composable
fun ProfileMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick) // 클릭 효과 및 활성화 상태
            .padding(horizontal = 16.dp, vertical = 16.dp), // 패딩 조정
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if(enabled) 1f else 0.5f) // 비활성화 시 투명도 조절
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = LocalContentColor.current.copy(alpha = if(enabled) 1f else 0.5f)
        )
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun ProfileContentPreview() {
    val previewProfile = User("id", "김미리", "preview@example.com", "Compose 공부 중!", null).toUserProfileData()
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ProfileContent(
            isLoading = false,
            profile = previewProfile,
            onEditProfileImageClick = {},
            // onEditStatusClick = {}, // Removed
            onSettingsClick = {},
            onLogoutClick = {},
            onFriendsClick = {},
            onStatusClick = {},
            onEditProfileClick = {}, // Preview에 추가
            // Preview parameters for status editing
            isEditingStatusMessage = false,
            editableStatusMessage = previewProfile.statusMessage ?: "Compose 공부 중!",
            onStatusMessageChange = {},
            onToggleEditStatus = {},
            onSubmitStatusMessage = {}
        )
    }
}

@Preview(showBackground = true, name = "ProfileContent Editing Status")
@Composable
fun ProfileContentEditingPreview() {
    val previewProfile = User("id", "김미리", "preview@example.com", "Compose 공부 중!", null).toUserProfileData()
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ProfileContent(
            isLoading = false,
            profile = previewProfile,
            onEditProfileImageClick = {},
            onSettingsClick = {},
            onLogoutClick = {},
            onFriendsClick = {},
            onStatusClick = {},
            onEditProfileClick = {},
            isEditingStatusMessage = true, // Editing mode
            editableStatusMessage = "새로운 상태 메시지 입력 중...",
            onStatusMessageChange = {},
            onToggleEditStatus = {},
            onSubmitStatusMessage = {}
        )
    }
}


@Preview(showBackground = true, name = "Profile Loading")
@Composable
fun ProfileContentLoadingPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ProfileContent(
            isLoading = true, // 로딩 상태
            profile = null, // 데이터 없는 상태
            onEditProfileImageClick = {},
            // onEditStatusClick = {}, // Removed
            onSettingsClick = {},
            onLogoutClick = {},
            onFriendsClick = {},
            onStatusClick = {},
            onEditProfileClick = {}, // Preview에 추가
            // Preview parameters for status editing
            isEditingStatusMessage = false,
            editableStatusMessage = "",
            onStatusMessageChange = {},
            onToggleEditStatus = {},
            onSubmitStatusMessage = {}
        )
    }
}
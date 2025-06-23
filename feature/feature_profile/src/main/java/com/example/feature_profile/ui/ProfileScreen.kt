package com.example.feature_profile.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.core.NavDestination
import com.example.core_navigation.core.NavigationCommand
import com.example.core_navigation.destination.AppRoutes
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.enum.UserStatus
import com.example.feature_profile.dialog.ChangeStatusDialog
import com.example.feature_profile.viewmodel.ProfileEvent
import com.example.feature_profile.viewmodel.ProfileViewModel
import com.example.feature_profile.viewmodel.UserProfileData
import kotlinx.coroutines.flow.collectLatest

/**
 * ProfileScreen: 상태 관리 및 이벤트 처리 (Stateful)
 **/
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    navigationManger: NavigationManger,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for in-place status editing
    var isEditingStatusMessage by rememberSaveable { mutableStateOf(false) }
    var editableStatusMessage by rememberSaveable { mutableStateOf(uiState.userProfile?.memo ?: "") }

    // Initialize editableStatusMessage when profile data is loaded or changed
    LaunchedEffect(uiState.userProfile?.memo) {
        if (!isEditingStatusMessage) {
            editableStatusMessage = uiState.userProfile?.memo ?: ""
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            viewModel.changeProfileImage(uri)
        }
    )

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ProfileEvent.NavigateToSettings -> navigationManger.navigate(NavigationCommand.NavigateToRoute(NavDestination.fromRoute(AppRoutes.Settings.APP_SETTINGS)))
                is ProfileEvent.NavigateToEditProfile -> navigationManger.navigate(NavigationCommand.NavigateToRoute(NavDestination.fromRoute(AppRoutes.Settings.EDIT_MY_PROFILE)))
                is ProfileEvent.NavigateToFriends -> navigationManger.navigate(NavigationCommand.NavigateToRoute(NavDestination.fromRoute(AppRoutes.Friends.LIST)))
                is ProfileEvent.PickProfileImage -> imagePickerLauncher.launch("image/*")
                is ProfileEvent.LogoutCompleted -> navigationManger.navigateClearingBackStack(NavigationCommand.NavigateClearingBackStack(destination = NavDestination.fromRoute(AppRoutes.Auth.Login.path)))
                is ProfileEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            snackbarHostState.showSnackbar(uiState.errorMessage!!)
            viewModel.errorMessageShown()
        }
    }

    if (uiState.showChangeStatusDialog) {
        uiState.tempSelectedStatus?.let { currentSelection ->
            ChangeStatusDialog(
                currentStatus = currentSelection,
                onStatusSelected = viewModel::onStatusSelectedInDialog,
                onDismissRequest = viewModel::onDismissChangeStatusDialog
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading && uiState.userProfile == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            ProfileContent(
                modifier = Modifier.fillMaxSize(),
                isLoading = uiState.isLoading,
                profile = uiState.userProfile,
                onEditProfileImageClick = viewModel::onProfileImageClick,
                onSettingsClick = viewModel::onSettingsClick,
                onLogoutClick = viewModel::onLogoutClick,
                onFriendsClick = viewModel::onFriendsClick,
                onStatusClick = viewModel::onChangeStatusClick,
                onEditProfileClick = viewModel::onEditProfileClicked,
                isEditingStatusMessage = isEditingStatusMessage,
                editableStatusMessage = editableStatusMessage,
                onStatusMessageChange = { newMessage ->
                    if (newMessage.length <= 50) {
                        editableStatusMessage = newMessage
                    }
                },
                onToggleEditStatus = {
                    isEditingStatusMessage = !isEditingStatusMessage
                    if (isEditingStatusMessage) {
                        editableStatusMessage = uiState.userProfile?.memo ?: ""
                    }
                },
                onSubmitStatusMessage = {
                    if (editableStatusMessage != uiState.userProfile?.memo) {
                        viewModel.changeMemo(editableStatusMessage)
                    }
                    isEditingStatusMessage = false
                }
            )
        }
        if (uiState.isLoading && uiState.userProfile != null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(40.dp)
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
    isLoading: Boolean,
    profile: UserProfileData?,
    onEditProfileImageClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onStatusClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    isEditingStatusMessage: Boolean,
    editableStatusMessage: String,
    onStatusMessageChange: (String) -> Unit,
    onToggleEditStatus: () -> Unit,
    onSubmitStatusMessage: () -> Unit
) {
    val currentStatusIcon = getStatusIcon(status = profile?.userStatus ?: UserStatus.UNKNOWN)
    val currentStatusText = profile?.userStatus?.value ?: UserStatus.UNKNOWN.value
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        LaunchedEffect(isEditingStatusMessage) {
            if (isEditingStatusMessage) {
                focusRequester.requestFocus()
            }
        }

        // 프로필 이미지 영역
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = profile?.profileImageUrl,
                contentDescription = "프로필 이미지",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )

        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = profile?.name ?: "사용자 이름",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 상태 메시지
        TextField(
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
                        onSubmitStatusMessage()
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, // No ripple on the text field itself
                    enabled = true
                ) {
                    if (!isEditingStatusMessage) {
                        onToggleEditStatus()
                    }
                },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                disabledIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            trailingIcon = {
                if (isEditingStatusMessage) {
                    IconButton(onClick = {
                        onSubmitStatusMessage()
                        focusManager.clearFocus()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = "상태 메시지 저장"
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                onSubmitStatusMessage()
                focusManager.clearFocus()
            }),
            supportingText = {
                if (isEditingStatusMessage) {
                    Text(text = "${editableStatusMessage.length} / 50")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Current Status Display Item
        ProfileMenuItem(
            text = currentStatusText,
            icon = currentStatusIcon,
            onClick = onStatusClick,
            enabled = !isLoading
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

        // Other Menu Items
        ProfileMenuItem(
            text = "프로필 편집",
            icon = Icons.Filled.Edit,
            onClick = onEditProfileClick,
            enabled = !isLoading
        )
        ProfileMenuItem(
            text = "친구 목록",
            icon = Icons.Filled.People,
            onClick = onFriendsClick,
            enabled = !isLoading
        )
        ProfileMenuItem(
            text = "설정",
            icon = Icons.Filled.Settings,
            onClick = onSettingsClick,
            enabled = !isLoading
        )
        ProfileMenuItem(
            text = "로그아웃",
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            onClick = onLogoutClick,
            enabled = !isLoading
        )
    }
}

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
            .clickable(onClick = onClick, enabled = enabled)
            .padding(horizontal = 16.dp, vertical = 12.dp),
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

// Helper function to get icon based on UserStatus
@Composable
private fun getStatusIcon(status: UserStatus): ImageVector {
    return when (status) {
        UserStatus.ONLINE -> Icons.Filled.CheckCircle
        UserStatus.OFFLINE -> Icons.Filled.Cancel
        UserStatus.AWAY -> Icons.Filled.AccessTime
        UserStatus.DO_NOT_DISTURB -> Icons.Filled.DoNotDisturbOn
        UserStatus.UNKNOWN -> Icons.Filled.HelpOutline
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun ProfileContentPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ProfileContent(
            isLoading = false,
            profile = TODO(),
            onEditProfileImageClick = {},
            // onEditStatusClick = {}, // Removed
            onSettingsClick = {},
            onLogoutClick = {},
            onFriendsClick = {},
            onStatusClick = {},
            onEditProfileClick = {}, // Preview에 추가
            // Preview parameters for status editing
            isEditingStatusMessage = false,
            editableStatusMessage = "Compose 공부 중!",
            onStatusMessageChange = {},
            onToggleEditStatus = {},
            onSubmitStatusMessage = {}
        )
    }
}

@Preview(showBackground = true, name = "ProfileContent Editing Status")
@Composable
fun ProfileContentEditingPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ProfileContent(
            isLoading = false,
            profile = TODO(),
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
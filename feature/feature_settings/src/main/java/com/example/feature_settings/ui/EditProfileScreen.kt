package com.example.feature_settings.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera // 이미지 변경 아이콘
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core_navigation.core.AppNavigator
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.core_ui.R
import com.example.domain.model.User
import com.example.domain.model.UserProfileData
import com.example.feature_settings.viewmodel.EditProfileEvent
import com.example.feature_settings.viewmodel.EditProfileUiState
import com.example.feature_settings.viewmodel.EditProfileViewModel
// 네비게이션 관련 임포트 업데이트
import kotlinx.coroutines.flow.collectLatest

/**
 * EditProfileScreen: 프로필 편집 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    appNavigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: EditProfileViewModel = hiltViewModel(),
    onChangeNameClick: () -> Unit, // 이름 변경 다이얼로그 표시 콜백
    onChangeStatusClick: () -> Unit // 상태 변경 다이얼로그 표시 콜백
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 이미지 선택기 런처
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? -> viewModel.onImagePicked(uri) } // 선택 결과 ViewModel에 전달
    )

    // 이미지 제거 확인 다이얼로그 상태
    var showRemoveImageConfirmDialog by remember { mutableStateOf(false) }

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditProfileEvent.NavigateBack -> appNavigator.navigateBack()
                is EditProfileEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is EditProfileEvent.RequestImagePicker -> {
                    // 이미지 선택기 실행
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                is EditProfileEvent.RequestProfileImageRemoveConfirm -> {
                    showRemoveImageConfirmDialog = true // 삭제 확인 다이얼로그 표시
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("프로필 편집") },
                navigationIcon = {
                    IconButton(onClick = { appNavigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            // 초기 로딩
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            // 에러 발생
            uiState.error != null && uiState.userProfile == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("오류: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                }
            }
            // 프로필 정보 로드 완료
            uiState.userProfile != null -> {
                EditProfileContent(
                    modifier = Modifier.padding(paddingValues),
                    uiState = uiState,
                    onSelectImageClick = viewModel::onSelectImageClick,
                    onRemoveImageClick = viewModel::onRemoveImageClick,
                    onChangeNameClick = onChangeNameClick,
                    onChangeStatusClick = onChangeStatusClick
                )
            }
            // 그 외 (이론상 발생 안 함)
            else -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("프로필 정보를 불러올 수 없습니다.")
                }
            }
        }
    }

    // 이미지 제거 확인 다이얼로그
    if (showRemoveImageConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveImageConfirmDialog = false },
            title = { Text("프로필 이미지 삭제") },
            text = { Text("프로필 이미지를 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmRemoveProfileImage() // ViewModel의 제거 확정 함수 호출
                        showRemoveImageConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveImageConfirmDialog = false }) { Text("취소") }
            }
        )
    }
}

/**
 * EditProfileContent: 프로필 편집 UI 요소 (Stateless)
 */
@Composable
fun EditProfileContent(
    modifier: Modifier = Modifier,
    uiState: EditProfileUiState,
    onSelectImageClick: () -> Unit,
    onRemoveImageClick: () -> Unit,
    onChangeNameClick: () -> Unit,
    onChangeStatusClick: () -> Unit
) {
    // UserProfile이 null이 아님을 가정 (호출하는 곳에서 보장)
    val user = uiState.userProfile!!

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // 내용 많아질 경우 스크롤
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 프로필 이미지 섹션
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    // 사용자가 이미지를 선택했으면 그 URI를, 아니면 서버 URL 사용
                    .data(uiState.selectedImageUri ?: user.profileImageUrl ?: R.drawable.ic_account_circle_24)
                    .error(R.drawable.ic_account_circle_24)
                    .placeholder(R.drawable.ic_account_circle_24)
                    .crossfade(true)
                    .build(),
                contentDescription = "프로필 이미지",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant), // 이미지 없을 때 배경색
                contentScale = ContentScale.Crop
            )
            // 이미지 변경 버튼 (카메라 아이콘)
            FloatingActionButton(
                onClick = onSelectImageClick,
                modifier = Modifier.size(40.dp), // 작은 사이즈 FAB
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = "프로필 이미지 변경")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 이미지 업로드 중 인디케이터
        if (uiState.isUploading) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 이미지 제거 버튼 (텍스트 버튼)
        TextButton(
            onClick = onRemoveImageClick,
            enabled = !uiState.isUploading && user.profileImageUrl != null // 업로드 중이 아니고, 이미지가 있을 때 활성화
        ) {
            Text("프로필 이미지 제거")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 프로필 정보 섹션
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start // 좌측 정렬
        ) {
            ProfileInfoRow(label = "이메일", value = user.email)
            HorizontalDivider()
            ProfileInfoRow(label = "이름", value = user.name, onClick = onChangeNameClick) // 클릭 시 이름 변경
            HorizontalDivider()
            ProfileInfoRow(label = "상태 메시지", value = user.statusMessage.toString(), onClick = onChangeStatusClick) // 클릭 시 상태 변경
            HorizontalDivider()
        }
    }
}

/**
 * ProfileInfoRow: 프로필 정보 항목 행 (Stateless)
 */
@Composable
fun ProfileInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null // 클릭 가능 여부
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() } // 클릭 가능하면 콜백 실행
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (onClick != null) MaterialTheme.colorScheme.primary else LocalContentColor.current // 클릭 가능하면 색상 강조
            )
            // 클릭 가능하면 수정 아이콘 표시
            if (onClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "$label 변경",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Edit Profile Loading")
@Composable
private fun EditProfileScreenLoadingPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("프로필 편집") }) }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
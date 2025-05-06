package com.example.feature_user.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
// import androidx.compose.material.icons.filled.Edit // 프로필 수정 아이콘 (필요시)
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core_navigation.core.ComposeNavigationHandler
// import com.example.core_navigation.routes.AppRoutes // EditProfile 등 네비게이션 시 필요
// import com.example.core_navigation.core.NavigationCommand
import com.example.feature_user.viewmodel.UserProfileUiState
import com.example.feature_user.viewmodel.UserProfileViewModel

/**
 * 사용자 프로필 정보를 표시하는 화면입니다. (Placeholder)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    navigationManager: ComposeNavigationHandler,
    modifier: Modifier = Modifier,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = if (uiState.isLoading) "프로필 로딩 중..." else uiState.userName) },
                navigationIcon = {
                    IconButton(onClick = { navigationManager.navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                }
                // actions = { // 프로필 수정 버튼 등 (필요시)
                //     uiState.userId?.let { userId ->
                //         IconButton(onClick = { /* navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Settings.editProfile(userId))) */ }) {
                //             Icon(imageVector = Icons.Filled.Edit, contentDescription = "프로필 수정")
                //         }
                //     }
                // }
            )
        }
    ) { paddingValues ->
        UserProfileContent(
            paddingValues = paddingValues,
            uiState = uiState
        )
    }
}

/**
 * 사용자 프로필 화면의 내용을 표시합니다.
 */
@Composable
fun UserProfileContent(
    paddingValues: PaddingValues,
    uiState: UserProfileUiState
) {
    Box(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator()
            }
            uiState.error != null -> {
                Text(text = "오류: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            }
            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(uiState.profileImageUrl ?: "https://via.placeholder.com/150") // Placeholder 이미지
                            .crossfade(true)
                            .build(),
                        contentDescription = "사용자 프로필 이미지",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.userName,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.userEmail,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "ID: ${uiState.userId}")
                    Text(text = "추가적인 사용자 정보가 여기에 표시됩니다.")
                    // TODO: 실제 사용자 프로필 상세 UI 구현
                }
            }
        }
    }
} 
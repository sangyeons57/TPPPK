package com.example.feature_project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings // 프로젝트 설정 아이콘
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.core_navigation.routes.AppRoutes // AppRoutes 임포트
import com.example.core_navigation.core.NavigationCommand // NavigationCommand 임포트
import com.example.feature_project.viewmodel.ProjectDetailEvent
import com.example.feature_project.viewmodel.ProjectDetailUiState
import com.example.feature_project.viewmodel.ProjectDetailViewModel
// import kotlinx.coroutines.flow.collectLatest // 필요한 경우

/**
 * 프로젝트 상세 정보를 표시하는 화면입니다. (Placeholder)
 * @param navigationManager Navigation 처리를 위한 핸들러.
 * @param viewModel 화면의 상태 및 로직을 관리하는 ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    navigationManager: ComposeNavigationHandler,
    modifier: Modifier = Modifier,
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 이벤트 처리 (필요한 경우)
    // LaunchedEffect(Unit) {
    //     viewModel.eventFlow.collectLatest { event ->
    //         when (event) {
    //             is ProjectDetailEvent.NavigateToProjectSettings -> {
    //                 navigationManager.navigate(
    //                     NavigationCommand.NavigateToRoute(AppRoutes.Project.settings(event.projectId)) // AppRoutes 사용
    //                 )
    //             }
    //         }
    //     }
    // }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.projectName) },
                navigationIcon = {
                    IconButton(onClick = { navigationManager.navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                },
                actions = {
                    // 프로젝트 ID가 있을 때만 설정 버튼 표시
                    uiState.projectId?.let { projId ->
                        IconButton(onClick = {
                            navigationManager.navigate(
                                NavigationCommand.NavigateToRoute(AppRoutes.Project.settings(projId)) // AppRoutes 사용
                            )
                            // 또는 viewModel.onProjectSettingsClicked() 호출
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "프로젝트 설정"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        ProjectDetailContent(
            paddingValues = paddingValues,
            uiState = uiState
        )
    }
}

/**
 * 프로젝트 상세 화면의 내용을 표시합니다.
 * @param paddingValues Scaffold로부터 전달받은 Padding 값.
 * @param uiState 화면에 표시할 UI 상태.
 */
@Composable
fun ProjectDetailContent(
    paddingValues: PaddingValues,
    uiState: ProjectDetailUiState
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
                    Text(
                        text = "프로젝트 상세 (ID: ${uiState.projectId})",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "이곳에 프로젝트의 상세 내용이 표시됩니다.")
                    Text(text = "예: 작업 목록, 멤버, 진행 상황 등")
                    // TODO: 실제 프로젝트 상세 UI 구현
                }
            }
        }
    }
} 
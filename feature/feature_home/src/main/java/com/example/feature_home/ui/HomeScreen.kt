package com.example.feature_home.ui

import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.user.UserName
import com.example.feature_home.component.DmListComponent
import com.example.feature_home.component.UnifiedProjectStructureList
import com.example.feature_home.component.ProjectListScreen
import com.example.feature_home.model.CategoryUiModel
import com.example.feature_home.model.ChannelUiModel
import com.example.feature_home.model.DmUiModel
import com.example.feature_home.model.ProjectStructureUiState
import com.example.feature_home.model.ProjectUiModel
import com.example.feature_home.ui.component.DialogStates
import com.example.feature_home.ui.component.HomeScreenEffects
import com.example.feature_home.ui.component.HomeScreenScaffold
import com.example.feature_home.ui.component.HomeScreenStateManager
import com.example.feature_home.ui.component.rememberDialogStates
import com.example.feature_home.viewmodel.HomeUiState
import com.example.feature_home.viewmodel.HomeViewModel
import com.example.feature_home.viewmodel.TopSection

private const val HOME_SCREEN_STATE_KEY = "home_screen_state"

/**
 * 리팩토링된 HomeScreen: 디스코드 스타일 홈 화면
 * - 왼쪽: 프로필/프로젝트 사이드바
 * - 중간: DM 목록 또는 선택한 프로젝트의 채널 목록
 * - 오른쪽: (채팅 화면으로 이동)
 *
 * @param modifier UI 요소에 적용할 Modifier.
 * @param viewModel HomeViewModel 인스턴스, 화면의 상태 및 비즈니스 로직 관리.
 * @param savedState 화면 상태를 복원하기 위한 Bundle (탭 전환 시 상태 유지).
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    savedState: Bundle? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 다이얼로그 상태 관리
    var dialogStates by remember { mutableStateOf(DialogStates()) }
    
    // 상태 관리자
    HomeScreenStateManager(
        viewModel = viewModel,
        savedState = savedState,
        showFloatingMenu = dialogStates.showFloatingMenu,
        onShowFloatingMenuChange = { expanded ->
            dialogStates = dialogStates.copy(showFloatingMenu = expanded)
        },
        selectedTopSection = uiState.selectedTopSection,
        selectedProjectId = uiState.selectedProjectId
    )
    
    // 부수효과 처리
    HomeScreenEffects(
        viewModel = viewModel,
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        dialogStates = dialogStates,
        onDialogStateChange = { newState ->
            dialogStates = newState
        }
    )
    
    // Scaffold 구성
    HomeScreenScaffold(
        modifier = modifier,
        viewModel = viewModel,
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        dialogStates = dialogStates,
        onDialogStateChange = { newState ->
            dialogStates = newState
        }
    ) {
        // 메인 콘텐츠 (HomeContent)
        HomeContent(
            modifier = Modifier.fillMaxSize(),
            uiState = uiState,
            onProjectSelect = { projectId ->
                viewModel.onProjectClick(projectId)
                viewModel.onTopSectionSelect(TopSection.PROJECTS)
            },
            onProfileClick = {
                viewModel.onTopSectionSelect(TopSection.DMS)
            },
            onCategoryClick = viewModel::onCategoryClick,
            onClickTopSection = viewModel::onClickTopSection,
            onCategoryLongPress = viewModel::onCategoryLongPress,
            onChannelClick = viewModel::onChannelClick,
            onChannelLongPress = viewModel::onChannelLongPress,
            onDmItemClick = viewModel::onDmItemClick,
            viewModel = viewModel
        )
    }
}

/**
 * HomeContent: 디스코드 스타일 UI 렌더링
 */
@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    onProjectSelect: (projectId: DocumentId) -> Unit,
    onProfileClick: () -> Unit,
    onClickTopSection: () -> Unit,
    onCategoryClick: (category: CategoryUiModel) -> Unit,
    onCategoryLongPress: (category: CategoryUiModel) -> Unit = {},
    onChannelClick: (channel: ChannelUiModel) -> Unit,
    onChannelLongPress: (channel: ChannelUiModel) -> Unit = {},
    onDmItemClick: (dm: DmUiModel) -> Unit,
    viewModel: HomeViewModel
) {
    Row(modifier = modifier.fillMaxSize()) {
        // 1. 왼쪽 사이드바: 프로필과 프로젝트 목록
        Log.d("HomeContent", "projects: ${uiState.projects}")
        ProjectListScreen(
            projects = uiState.projects.map { projectItem -> 
                //Log.d("HomeContent", "Converting ProjectItem: id=${projectItem.id}, name=${projectItem.name}")
                ProjectUiModel(
                    id = projectItem.id,
                    name = projectItem.name,
                    imageUrl = projectItem.imageUrl
                ) 
            },
            selectedProjectId = uiState.selectedProjectId,
            isDmSelected = uiState.selectedTopSection == TopSection.DMS,
            onProfileClick = onProfileClick,
            onProjectClick = onProjectSelect,
            userInitial = uiState.userInitial,
            profileImageUrl = uiState.userProfileImageUrl,
            modifier = Modifier.fillMaxHeight()
        )

        // 2. 중간 섹션: DM 목록 또는 프로젝트 채널
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.35f)
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(0.5.dp))
                .padding(start = 8.dp, end = 4.dp)
        ) {
            // 섹션 헤더
            HomeMiddleSectionHeader(
                uiState = uiState, 
                onClickTopSection = onClickTopSection
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 컨텐츠 분기
            when (uiState.selectedTopSection) {
                TopSection.PROJECTS -> {
                    if (uiState.selectedProjectId != null) {
                        UnifiedProjectStructureList(
                            structureUiState = uiState.projectStructure,
                            onCategoryClick = onCategoryClick,
                            onCategoryLongPress = onCategoryLongPress,
                            onChannelClick = onChannelClick,
                            onChannelLongPress = onChannelLongPress,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        EmptyStateMessage("프로젝트를 선택하세요")
                    }
                }
                TopSection.DMS -> {
                    if (uiState.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.dms.isEmpty()) {
                        EmptyStateMessage("DM이 없습니다")
                    } else {
                        DmListComponent(
                            dms = uiState.dms,
                            onDmItemClick = onDmItemClick,
                            onDmItemLongClick = viewModel::onDmLongPress,
                            isLoading = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

/**
 * HomeScreen 중간 영역의 헤더 (프로젝트명 또는 "Direct Messages")
 */
@Composable
fun HomeMiddleSectionHeader(
    onClickTopSection: () -> Unit,
    uiState: HomeUiState
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClickTopSection),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .height(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (uiState.selectedTopSection == TopSection.PROJECTS && uiState.selectedProjectId != null) {
                    uiState.projectName.ifBlank { "프로젝트" }
                } else {
                    "Direct Messages"
                },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 빈 상태 메시지 표시
 */
@Composable
fun EmptyStateMessage(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message, 
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}


// --- HomeScreen Preview ---

@Preview(showBackground = true, name = "HomeScreen Default Preview")
@Composable
fun HomeScreenPreview_Default() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        // HomeScreen preview requires actual ViewModel instance
        // HomeScreen(modifier = Modifier, viewModel = TODO())
    }
}

@Preview(showBackground = true, name = "HomeScreen InScaffold Preview")
@Composable
fun HomeScreenInScaffoldPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                FloatingActionButton(onClick = { Log.d("Preview", "FAB Clicked") }) {
                    Icon(Icons.Filled.Add, contentDescription = "추가")
                }
            },
            floatingActionButtonPosition = FabPosition.End
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {

            }
        }
    }
}
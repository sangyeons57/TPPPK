package com.example.feature_main.ui

import com.example.feature_main.ui.DmUiModel // Added import
import com.example.feature_main.ui.ProjectUiModel // Added import
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_common.util.DateTimeUtil
import com.example.core_navigation.core.AppNavigator
import com.example.core_navigation.core.NavigationCommand
import com.example.core_navigation.destination.AppRoutes
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_main.viewmodel.HomeEvent
import com.example.feature_main.viewmodel.HomeViewModel
import com.example.feature_main.viewmodel.ProjectItem
import com.example.feature_main.viewmodel.TopSection
import kotlinx.coroutines.flow.collectLatest
import com.example.feature_main.ui.project.ProjectChannelList
import com.example.feature_main.viewmodel.HomeUiState
import com.example.feature_main.ui.components.ExtendableFloatingActionMenu
import com.example.feature_main.ui.wrapper.AddDmUserDialogWrapper
import com.example.feature_main.ui.wrapper.ProjectStructureEditDialogWrapper
import androidx.navigation.NavHostController
import com.example.feature_main.ui.project.CategoryUiModel
import com.example.feature_main.ui.project.ChannelUiModel
import com.example.feature_main.ui.project.ProjectStructureUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.example.core_navigation.core.NavDestination
import com.example.domain.model.enum.ProjectChannelType

// 오버레이 투명도 상수
private const val OVERLAY_ALPHA = 0.7f

// 상태 저장 키 상수
private object HomeScreenStateKeys {
    const val SELECTED_TOP_SECTION = "selected_top_section"
    const val SELECTED_PROJECT_ID = "selected_project_id"
    const val EXPANDED_CATEGORIES = "expanded_categories"
    const val SHOW_ADD_DM_DIALOG = "show_add_dm_dialog"
    const val SHOW_PROJECT_STRUCTURE_DIALOG = "show_project_structure_dialog"
    const val SHOW_FLOATING_MENU = "show_floating_menu"
}

/**
 * HomeScreen: 디스코드 스타일 홈 화면
 * - 왼쪽: 프로필/프로젝트 사이드바
 * - 중간: DM 목록 또는 선택한 프로젝트의 채널 목록
 * - 오른쪽: (채팅 화면으로 이동)
 * 
 * @param savedState 화면 상태를 복원하기 위한 Bundle (탭 전환 시 상태 유지)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    appNavigator: AppNavigator,
    viewModel: HomeViewModel = hiltViewModel(),
    savedState: Bundle? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 다이얼로그 상태 추가
    var showAddDmDialog by remember { mutableStateOf(false) }
    var showProjectStructureEditDialog by remember { mutableStateOf(false) }
    var showFloatingMenu by remember { mutableStateOf(false) }

    // 상태 복원 (탭 전환 시)
    LaunchedEffect(savedState) {
        savedState?.let { bundle ->
            Log.d("HomeScreen", "상태 복원: $bundle")
            
            // TopSection 복원
            bundle.getString(HomeScreenStateKeys.SELECTED_TOP_SECTION)?.let { sectionName ->
                try {
                    val section = TopSection.valueOf(sectionName)
                    viewModel.onTopSectionSelect(section)
                } catch (e: IllegalArgumentException) {
                    Log.e("HomeScreen", "Invalid section name: $sectionName", e)
                }
            }
            
            // 선택된 프로젝트 ID 복원
            bundle.getString(HomeScreenStateKeys.SELECTED_PROJECT_ID)?.let { projectId ->
                if (projectId.isNotEmpty()) {
                    viewModel.onProjectClick(projectId)
                }
            }
            
            // 다이얼로그 상태 복원
            showAddDmDialog = bundle.getBoolean(HomeScreenStateKeys.SHOW_ADD_DM_DIALOG, false)
            showProjectStructureEditDialog = bundle.getBoolean(HomeScreenStateKeys.SHOW_PROJECT_STRUCTURE_DIALOG, false)
            showFloatingMenu = bundle.getBoolean(HomeScreenStateKeys.SHOW_FLOATING_MENU, false)
            
            // 확장된 카테고리 복원
            bundle.getStringArray(HomeScreenStateKeys.EXPANDED_CATEGORIES)?.let { expandedIds ->
                viewModel.restoreExpandedCategories(expandedIds.toList())
            }
        }
    }
    
    // 화면 상태 저장 (탭 전환 시)
    DisposableEffect(
        uiState.selectedTopSection,
        uiState.selectedProjectId,
        showAddDmDialog,
        showProjectStructureEditDialog,
        showFloatingMenu
    ) {
        onDispose {
            // 화면이 비활성화될 때 상태 저장
            val screenState = Bundle().apply {
                putString(HomeScreenStateKeys.SELECTED_TOP_SECTION, uiState.selectedTopSection.name)
                putString(HomeScreenStateKeys.SELECTED_PROJECT_ID, uiState.selectedProjectId ?: "")
                putBoolean(HomeScreenStateKeys.SHOW_ADD_DM_DIALOG, showAddDmDialog)
                putBoolean(HomeScreenStateKeys.SHOW_PROJECT_STRUCTURE_DIALOG, showProjectStructureEditDialog)
                putBoolean(HomeScreenStateKeys.SHOW_FLOATING_MENU, showFloatingMenu)
            }
            
            // NavigationHandler를 통해 상태 저장
            // 현재 화면 경로를 키로 사용
            val screenKey = AppRoutes.Main.Home.ROOT_CONTENT
            appNavigator.saveScreenState(screenKey, screenState)
            Log.d("HomeScreen", "상태 저장: $screenState for key $screenKey")
        }
    }

    // 앱 시작 시 DM 섹션을 기본 선택되도록 설정 (상태가 없는 경우만)
    LaunchedEffect(Unit) {
        if (savedState == null) {
        viewModel.onTopSectionSelect(TopSection.DMS)
        }
    }

    // 이벤트 처리 (스낵바, 다이얼로그 등)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is HomeEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                }
                is HomeEvent.ShowAddProjectDialog -> {
                    snackbarHostState.showSnackbar("프로젝트 추가 다이얼로그 (미구현)")
                }
                is HomeEvent.ShowAddFriendDialog -> {
                    // DM 추가 다이얼로그 표시
                    showAddDmDialog = true
                }
                is HomeEvent.NavigateToAddProject -> {
                    appNavigator.navigate(NavigationCommand.NavigateToRoute(NavDestination.fromRoute(AppRoutes.Project.ADD)))
                }
                is HomeEvent.NavigateToProjectSettings -> {
                    appNavigator.navigate(NavigationCommand.NavigateToRoute(NavDestination.fromRoute(AppRoutes.Project.settings(event.projectId))))
                }
                is HomeEvent.NavigateToDmChat -> {
                    Log.d("HomeScreen", "Navigating to DM Chat with ID: ${event.dmId}")
                    appNavigator.navigate(NavigationCommand.NavigateToRoute(NavDestination.fromRoute(AppRoutes.Chat.screen(event.dmId))))
                }
                is HomeEvent.NavigateToChannel -> {
                    appNavigator.navigate(NavigationCommand.NavigateToRoute(NavDestination.fromRoute(AppRoutes.Chat.screen(event.channelId))))
                }
                is HomeEvent.EditProjectStructure -> {
                    // 프로젝트 구조 편집 다이얼로그 표시
                    showProjectStructureEditDialog = true
                }
            }
        }
    }

    // 에러 메시지 스낵바로 표시
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != "default" && uiState.errorMessage.isNotBlank()) {
            snackbarHostState.showSnackbar(uiState.errorMessage, duration = SnackbarDuration.Short)
            viewModel.errorMessageShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendableFloatingActionMenu(
                currentSection = uiState.selectedTopSection,
                isExpanded = showFloatingMenu,
                onExpandedChange = { showFloatingMenu = it },
                onAddProject = viewModel::onProjectAddButtonClick,
                onAddDm = viewModel::onAddFriendClick,
                onEditProjectStructure = viewModel::onEditProjectStructureClick
            )
        }
    ) { paddingValues ->
        // 메인 콘텐츠와 오버레이를 포함하는 Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                onChannelClick = viewModel::onChannelClick,
                onDmItemClick = viewModel::onDmItemClick
            )
            
            // 배경 오버레이 (다이얼로그 또는 메뉴가 열렸을 때 표시)
            AnimatedVisibility(
                visible = showAddDmDialog || showProjectStructureEditDialog || showFloatingMenu,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = OVERLAY_ALPHA))
                        .clickable {
                            if (showFloatingMenu) {
                                showFloatingMenu = false
                            }
                            // 다른 다이얼로그가 열려있을 때는 클릭으로 닫지 않음
                        }
                )
            }
            
            // 다이얼로그들 (zIndex를 높게 설정하여 오버레이 위에 표시)
            
            // AddDmUserDialog 추가
            if (showAddDmDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f),
                    contentAlignment = Alignment.Center
                ) {
                    AddDmUserDialogWrapper(
                        onDismiss = { showAddDmDialog = false },
                        onNavigateToDm = { dmChannelId ->
                            appNavigator.navigate(NavigationCommand.NavigateToRoute(NavDestination.fromRoute(AppRoutes.Chat.screen(dmChannelId))))
                        },
                        onShowSnackbar = { message ->
                            // 스코프 없이 suspend 함수를 직접 호출할 수 없음
                            // 따라서 람다 내에서는 로깅만 수행하고, 이벤트를 통해 스낵바 표시
                            Log.d("HomeScreen", "Show snackbar: $message")
                            viewModel.showErrorMessage(message)
                        }
                    )
                }
            }
            
            // 프로젝트 구조 편집 다이얼로그 추가
            if (showProjectStructureEditDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f),
                    contentAlignment = Alignment.Center
                ) {
                    ProjectStructureEditDialogWrapper(
                        projectId = uiState.selectedProjectId ?: "",
                        onDismiss = { showProjectStructureEditDialog = false },
                        onStructureUpdated = {
                            // ViewModel에 프로젝트 구조를 새로고침하도록 요청
                            // selectedProjectId가 null이 아님을 보장하거나, null 처리 필요
                            uiState.selectedProjectId?.let { pid ->
                                viewModel.refreshProjectStructure(pid)
                            }
                        },
                        onShowSnackbar = { message ->
                            viewModel.showErrorMessage(message)
                        }
                    )
                }
            }
        }
    }
}

/**
 * HomeContent: 디스코드 스타일 UI 렌더링
 */
@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    onProjectSelect: (projectId: String) -> Unit,
    onProfileClick: () -> Unit,
    onCategoryClick: (category: CategoryUiModel) -> Unit,
    onChannelClick: (channel: ChannelUiModel) -> Unit,
    onDmItemClick: (dm: DmUiModel) -> Unit
) {
    Row(modifier = modifier.fillMaxSize()) {
        // 1. 왼쪽 사이드바: 프로필과 프로젝트 목록
        Log.d("HomeContent", "projects: ${uiState.projects}")
        ProjectListScreen(
            projects = uiState.projects.map { projectItem -> 
                Log.d("HomeContent", "Converting ProjectItem: id=${projectItem.id}, name=${projectItem.name}")
                ProjectUiModel(
                    id = projectItem.id, 
                    name = projectItem.name, 
                    description = projectItem.description, 
                    imageUrl = null
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
            HomeMiddleSectionHeader(uiState = uiState)
            Spacer(modifier = Modifier.height(8.dp))

            // 컨텐츠 분기
            when (uiState.selectedTopSection) {
                TopSection.PROJECTS -> {
                    if (uiState.selectedProjectId != null) {
                        ProjectChannelList(
                            structureUiState = uiState.projectStructure,
                            onCategoryClick = onCategoryClick,
                            onChannelClick = onChannelClick,
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
                        DmListScreen(
                            dms = uiState.dms,
                            onDmItemClick = onDmItemClick,
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
    uiState: HomeUiState
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (uiState.selectedTopSection == TopSection.PROJECTS && uiState.selectedProjectId != null) {
                    uiState.projectName.ifBlank { "프로젝트" }
                } else {
                    "Direct Messages"
                },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message, 
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

// DmListScreen 및 관련 컴포넌트는 이제 별도의 파일(DmListScreen.kt)로 이동했습니다.

/**
 * 프로젝트 상세 정보를 표시하는 화면
 */
@Composable
fun ProjectDetailContent(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                // 로딩 중 표시
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != "default" && uiState.errorMessage.isNotBlank() -> {
                // 오류 표시
                Box(
                modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "오류: ${uiState.errorMessage}", 
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                // 프로젝트 상세 정보 표시
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // 프로젝트 헤더
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = uiState.projectName,
                                style = MaterialTheme.typography.headlineMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            if (!uiState.projectDescription.isNullOrBlank()) {
                    Text(
                                    text = uiState.projectDescription,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        // 프로젝트 설정 버튼
                        IconButton(
                            onClick = { 
                                val projectId = uiState.selectedProjectId
                                if (projectId != null) {
                                    viewModel.onProjectSettingsClick(projectId)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "프로젝트 설정"
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                    
                    // 프로젝트 컨텐츠 영역
                    ProjectContentArea(
                        uiState = uiState,
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 프로젝트 컨텐츠 영역 (카테고리 및 채널 목록 + 선택된 채널의 컨텐츠)
 */
@Composable
fun ProjectContentArea(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    // 프로젝트 구조 로딩 중이면 로딩 인디케이터 표시
    if (uiState.projectStructure.isLoading) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    // 프로젝트 구조 오류 발생 시 오류 메시지 표시
    if (uiState.projectStructure.error != null) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "오류: ${uiState.projectStructure.error}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }
    
    // 카테고리 및 채널 목록이 없으면 안내 메시지 표시
    if (uiState.projectStructure.categories.isEmpty() && uiState.projectStructure.directChannel.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "카테고리 및 채널이 없습니다.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "프로젝트 설정에서 카테고리와 채널을 추가해보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        return
    }
    
    // 프로젝트 컨텐츠 (카테고리 및 채널 목록 + 선택된 채널의 컨텐츠)
    Row(
        modifier = modifier
    ) {
        // 좌측: 채널 목록 (전체 폭의 30%)
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.3f)
                .padding(end = 8.dp),
            shape = MaterialTheme.shapes.small,
            tonalElevation = 1.dp
        ) {
            ProjectChannelList(
                structureUiState = uiState.projectStructure,
                onCategoryClick = { category ->
                    viewModel.onCategoryClick(category)
                },
                onChannelClick = { channel ->
                    viewModel.onChannelClick(channel)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // 우측: 선택된 채널의 컨텐츠 (전체 폭의 70%)
            Surface(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.7f),
            shape = MaterialTheme.shapes.small,
                tonalElevation = 1.dp
        ) {
            // 선택된 채널이 없으면 안내 메시지 표시
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "채널을 선택하세요",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "좌측 패널에서 채널을 선택하면 해당 채널의 컨텐츠가 표시됩니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            // TODO: 선택된 채널의 컨텐츠 표시 (ChannelScreen 또는 필요한 컴포넌트)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeContentProjectsPreview() {
    val previewState = HomeUiState(
        selectedTopSection = TopSection.PROJECTS,
        projects = List(5) { i ->
            ProjectUiModel(
                id = "p$i",
                name = "미리보기 프로젝트 ${i + 1}",
                description = "설명 ${i + 1}",
                imageUrl = null, // Or some placeholder image URL
                memberCount = i + 2, // Example member count
                lastActivity = "${i}분 전"
            )
        },
        userInitial = "U", // 사용자 이니셜 추가
        userProfileImageUrl = null, // 프로필 이미지 URL 추가
        isLoading = false
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        HomeContent(
            uiState = previewState, 
            onProjectSelect = { Log.d("Preview", "Project selected: $it") }, 
            onProfileClick = { Log.d("Preview", "Profile clicked") }, // 프로필 클릭 핸들러 추가
            onCategoryClick = { Log.d("Preview", "Category clicked: ${it.name}") }, 
            onChannelClick = { Log.d("Preview", "Channel clicked: ${it.name}") }, 
            onDmItemClick = { Log.d("Preview", "DM clicked: ${it.partnerName}") }
        );
    }
}

@Preview(showBackground = true)
@Composable
fun HomeContentDmsPreview() {
    val previewState = HomeUiState(
        selectedTopSection = TopSection.DMS,
        dms = List(3) { i -> DmUiModel("dm$i", "친구 ${i + 1}", "미리보기 메시지 ${i + 1}", "TimeStamp", DateTimeUtil.nowInstant()) },
        userInitial = "U", // 사용자 이니셜 추가
        userProfileImageUrl = null, // 프로필 이미지 URL 추가
        isLoading = false
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        HomeContent(
            uiState = previewState, 
            onProjectSelect = { Log.d("Preview", "Project selected: $it") }, 
            onProfileClick = { Log.d("Preview", "Profile clicked") }, // 프로필 클릭 핸들러 추가
            onCategoryClick = { Log.d("Preview", "Category clicked: ${it.name}") }, 
            onChannelClick = { Log.d("Preview", "Channel clicked: ${it.name}") }, 
            onDmItemClick = { Log.d("Preview", "DM clicked: ${it.partnerName}") }
        );
    }
}

@Preview(showBackground = true)
@Composable
fun HomeContentLoadingPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        HomeContent(
            uiState = HomeUiState(
                isLoading = true,
                userInitial = "U", // 사용자 이니셜 추가
                userProfileImageUrl = null, // 프로필 이미지 URL 추가
            ), 
            onProjectSelect = { Log.d("Preview", "Project selected: $it") }, 
            onProfileClick = { Log.d("Preview", "Profile clicked") }, // 프로필 클릭 핸들러 추가
            onCategoryClick = { Log.d("Preview", "Category clicked: ${it.name}") }, 
            onChannelClick = { Log.d("Preview", "Channel clicked: ${it.name}") }, 
            onDmItemClick = { Log.d("Preview", "DM clicked: ${it.partnerName}") }
        );
    }
}

// --- HomeScreen Preview ---

@Preview(showBackground = true, name = "HomeScreen Default Preview")
@Composable
fun HomeScreenPreview_Default() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        HomeScreen(modifier = TODO(), appNavigator = TODO(), viewModel = TODO())
    }
}

@Preview(showBackground = true, name = "HomeScreen With Data Preview")
@Composable
fun HomeScreenPreview_WithData() {

    val sampleUiState = HomeUiState(
        projects = listOf(
            ProjectUiModel(
                id = "proj1",
                name = "Project Alpha",
                description = "Description for Alpha",
                imageUrl = null,
                memberCount = 3, // Example
                lastActivity = "10m ago"
            ),
            ProjectUiModel(
                id = "proj2",
                name = "Project Beta",
                description = "Description for Beta",
                imageUrl = null,
                memberCount = 5, // Example
                lastActivity = "1h ago"
            )
        ),
        dms = listOf(
            DmUiModel(
                channelId = "dm1",
                partnerName = "Alice",
                partnerProfileImageUrl = null,
                lastMessage = "Hey there!",
                lastMessageTimestamp = DateTimeUtil.nowInstant().minusSeconds(3600)
            ),
            DmUiModel(
                channelId = "dm2",
                partnerName = "Bob",
                partnerProfileImageUrl = null,
                lastMessage = "See you soon!",
                lastMessageTimestamp = DateTimeUtil.nowInstant().minusSeconds(7200)
            )
        ),
        selectedTopSection = TopSection.PROJECTS,
        selectedProjectId = "proj1",
        projectName = "Project Alpha",
        userInitial = "U", // 사용자 이니셜 추가
        userProfileImageUrl = null, // 프로필 이미지 URL 추가
        projectStructure = ProjectStructureUiState(
            directChannel = listOf(
                ChannelUiModel(
                    id = "ch1",
                    name = "general",
                    mode = ProjectChannelType.MESSAGES,
                    isSelected = false
                )
            ),
            categories = listOf(
                CategoryUiModel(
                    id = "cat1",
                    name = "Text Channels",
                    channels = listOf(
                        ChannelUiModel(
                            id = "ch2",
                            name = "announcements",
                            mode = ProjectChannelType.MESSAGES,
                            isSelected = true
                        )
                    ),
                    isExpanded = true
                )
            ),
            selectedChannelId = "ch2"
        ),
        isLoading = false,
        errorMessage = ""
    )

    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(remember { SnackbarHostState() }) },
            floatingActionButton = {
                // 확장형 FloatingActionMenu 적용 (프리뷰 용)
                FloatingActionButton(onClick = { Log.d("Preview", "FAB Clicked") }) {
                    Icon(Icons.Filled.Add, contentDescription = "추가")
                }
            }
        ) { paddingValues ->
             HomeContent(
                 modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()),
                 uiState = sampleUiState, // Pass the state directly
                 onProjectSelect = { projectId -> Log.d("Preview", "Project selected in HomeContent: $projectId") },
                 onProfileClick = { Log.d("Preview", "Profile clicked") }, // 프로필 클릭 핸들러 추가
                 onCategoryClick = { category -> Log.d("Preview", "Category clicked in HomeContent: ${category.name}") },
                 onChannelClick = { channel -> Log.d("Preview", "Channel clicked in HomeContent: ${channel.name}") },
                 onDmItemClick = { dm -> Log.d("Preview", "DM clicked in HomeContent: ${dm.partnerName}") }
             )
        }
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
                ExtendableFloatingActionMenu(
                    currentSection = TopSection.PROJECTS, // 또는 TopSection.DMS
                    onAddProject = { Log.d("Preview", "Add Project Clicked") },
                    onAddDm = { Log.d("Preview", "Add DM Clicked") },
                    onEditProjectStructure = { Log.d("Preview", "Edit Project Structure Clicked") },
                    isExpanded = TODO(),
                    onExpandedChange = TODO(),
                )
            },
            floatingActionButtonPosition = FabPosition.End
        ) { innerPadding ->
            // 실제 HomeScreen을 호출하되, Scaffold의 innerPadding을 활용하도록 가정
            // HomeScreen 내부에서 이 padding을 어떻게 사용하는지에 따라 결과가 달라짐
            // HomeScreen이 자체 Scaffold를 사용한다면, 여기서는 별도 패딩 없이 호출
            HomeScreen(
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()), // HomeScreen이 Scaffold 없이 바로 Content를 그린다고 가정
                appNavigator = remember { // Preview용 더미 AppNavigator
                    object : AppNavigator {
                        override fun navigate(command: NavigationCommand) {
                            Log.d("Preview", "Navigate: $command")
                        }
                        
                        override fun navigateBack(): Boolean {
                            Log.d("Preview", "NavigateBack")
                            return true
                        }

                        override fun <T> setResult(key: String, result: T) {
                            Log.d("Preview", "SetResult: $key")
                        }

                        override fun <T> getResult(key: String): T? {
                            Log.d("Preview", "GetResult: $key")
                            return null
                        }

                        override fun <T> getResultFlow(key: String): Flow<T> {
                            Log.d("Preview", "GetResultFlow: $key")
                            return emptyFlow()
                        }

                        override fun navigateClearingBackStack(command: NavigationCommand.NavigateClearingBackStack) {
                            Log.d("Preview", "NavigateClearingBackStack: ${command.route}")
                        }


                        override fun setNavController(navController: NavHostController) {
                            Log.d("Preview", "SetNavController")
                        }

                        override fun getNavController(): NavHostController? {
                            Log.d("Preview", "GetNavController")
                            return null;
                        }

                        override fun setChildNavController(navController: NavHostController?) {
                            Log.d("Preview", "SetChildNavController")
                        }
                        
                        override fun getChildNavController(): NavHostController? = null
                        
                        override fun saveScreenState(screenRoute: String, state: Bundle) {
                            Log.d("Preview", "SaveScreenState for $screenRoute")
                        }

                        override fun getScreenState(screenRoute: String): Bundle? {
                            Log.d("Preview", "GetScreenState for $screenRoute")
                            return null
                        }

                        override fun navigateToProjectDetailsNested(projectId: String, command: NavigationCommand.NavigateToRoute) {
                            Log.d("Preview", "NavigateToProjectDetailsNested: $projectId, route: ${command.route}")
                        }
                        
                        override fun isValidRoute(route: String): Boolean {
                            return true
                        }
                    }
                }
                // viewModel은 hiltViewModel()로 주입되므로 Preview에서는 기본 생성자 사용 또는 Mock 필요
                // 여기서는 기본 hiltViewModel() 동작에 의존 (실제 데이터 로딩은 안될 수 있음)
            )
        }
    }
}
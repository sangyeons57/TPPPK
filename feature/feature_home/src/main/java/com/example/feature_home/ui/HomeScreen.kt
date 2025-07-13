package com.example.feature_home.ui

import android.os.Bundle
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.core.NavigationResultManager
import com.example.core_navigation.core.TypeSafeRoute
import com.example.core_ui.components.bottom_sheet_dialog.BottomSheetDialog
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.user.UserName
import com.example.feature_edit_category.ui.EditCategoryDialog
import com.example.feature_edit_channel.ui.EditChannelDialog
import com.example.feature_home.component.DmListComponent
import com.example.feature_home.component.ExtendableFloatingActionMenu
import com.example.feature_home.component.MainHomeFloatingButton
import com.example.feature_home.component.ProjectChannelList
import com.example.feature_home.component.ProjectListScreen
import com.example.feature_home.dialog.ui.AddProjectElementDialog
import com.example.feature_home.model.CategoryUiModel
import com.example.feature_home.model.ChannelUiModel
import com.example.feature_home.model.DmUiModel
import com.example.feature_home.model.ProjectStructureUiState
import com.example.feature_home.model.ProjectUiModel
import com.example.feature_home.viewmodel.HomeEvent
import com.example.feature_home.viewmodel.HomeUiState
import com.example.feature_home.viewmodel.HomeViewModel
import com.example.feature_home.viewmodel.TopSection
import kotlinx.coroutines.flow.collectLatest

// 오버레이 투명도 상수
private const val OVERLAY_ALPHA = 0.7f

private const val HOME_SCREEN_STATE_KEY = "home_screen_state"

// 상태 저장 키 상수
private object HomeScreenStateKeys {
    const val SELECTED_TOP_SECTION = "selected_top_section"
    const val SELECTED_PROJECT_ID = "selected_project_id"
    const val EXPANDED_CATEGORIES = "expanded_categories"
    const val SHOW_FLOATING_MENU = "show_floating_menu"
}

/**
 * HomeScreen: 디스코드 스타일 홈 화면
 * - 왼쪽: 프로필/프로젝트 사이드바
 * - 중간: DM 목록 또는 선택한 프로젝트의 채널 목록
 * - 오른쪽: (채팅 화면으로 이동)
 *
 * @param modifier UI 요소에 적용할 Modifier.
 * @param navigationManger 내비게이션 이벤트를 처리하는 AppNavigator.
 * @param viewModel HomeViewModel 인스턴스, 화면의 상태 및 비즈니스 로직 관리.
 * @param savedState 화면 상태를 복원하기 위한 Bundle (탭 전환 시 상태 유지).
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navigationManger: NavigationManger,
    viewModel: HomeViewModel = hiltViewModel(),
    savedState: Bundle? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    rememberCoroutineScope()
    
    // 다이얼로그 상태 추가
    var showAddProjectElementDialog by remember { mutableStateOf(false) }
    var showFloatingMenu by remember { mutableStateOf(false) }
    var currentProjectIdForDialog by remember { mutableStateOf<DocumentId?>(null) }
    
    // 편집 다이얼로그 상태
    var showEditCategoryDialog by remember { mutableStateOf(false) }
    var showEditChannelDialog by remember { mutableStateOf(false) }
    var editCategoryName by remember { mutableStateOf("") }
    var editChannelName by remember { mutableStateOf("") }
    var editCategoryProjectId by remember { mutableStateOf("") }
    var editCategoryId by remember { mutableStateOf("") }
    var editChannelProjectId by remember { mutableStateOf("") }
    var editChannelId by remember { mutableStateOf("") }

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
                    viewModel.onProjectClick(DocumentId(projectId))
                }
            }
            
            // 다이얼로그 상태 복원
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
        showFloatingMenu
    ) {
        onDispose {
            // 화면이 비활성화될 때 상태 저장
            val screenState = Bundle().apply {
                putString(HomeScreenStateKeys.SELECTED_TOP_SECTION, uiState.selectedTopSection.name)
                putString(
                    HomeScreenStateKeys.SELECTED_PROJECT_ID,
                    uiState.selectedProjectId?.value ?: ""
                )
                putBoolean(HomeScreenStateKeys.SHOW_FLOATING_MENU, showFloatingMenu)
            }
            
            // NavigationHandler를 통해 상태 저장
            // 현재 화면 경로를 키로 사용
            navigationManger.saveScreenState(HOME_SCREEN_STATE_KEY, screenState)
            Log.d("HomeScreen", "상태 저장: $screenState for key $HOME_SCREEN_STATE_KEY")
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
                is HomeEvent.NavigateToProjectSettings -> {
                    if (event.projectId == null) {
                        Log.d("HomeScreen", "Project ID is null")
                        return@collectLatest
                    }
                    navigationManger.navigateToProjectSettings(event.projectId.value)
                }
                is HomeEvent.ShowAddProjectDialog -> {
                    snackbarHostState.showSnackbar("프로젝트 추가 다이얼로그 (미구현)")
                }
                is HomeEvent.ShowAddFriendDialog -> {
                    // TODO: Implement friend dialog or navigation
                }
                is HomeEvent.NavigateToAddProject -> {
                    navigationManger.navigateToAddProject()
                }
                is HomeEvent.NavigateToDmChat -> {
                    Log.d("HomeScreen", "Navigating to DM Chat with ID: ${event.dmId}")
                    navigationManger.navigateToChat(event.dmId.value)
                }
                is HomeEvent.NavigateToChannel -> {
                    navigationManger.navigateToChat(event.channelId)
                }
                is HomeEvent.ShowAddProjectElementDialog -> {
                    currentProjectIdForDialog = event.projectId
                    showAddProjectElementDialog = true
                }

                is HomeEvent.NavigateToEditCategory -> {
                    // Find the category name for the dialog
                    val category = uiState.projectStructure.categories.find { it.id == event.categoryId }
                    if (category != null) {
                        editCategoryName = category.name.value
                        editCategoryProjectId = event.projectId.value
                        editCategoryId = event.categoryId.value
                        showEditCategoryDialog = true
                    }
                }
                is HomeEvent.NavigateToEditChannel -> {
                    // Find the channel name for the dialog
                    val channel = uiState.projectStructure.categories
                        .flatMap { it.channels }
                        .find { it.id.value == event.channelId }
                    if (channel != null) {
                        editChannelName = channel.name.value
                        editChannelProjectId = event.projectId.value
                        editChannelId = event.channelId
                        showEditChannelDialog = true
                    }
                }
                is HomeEvent.NavigateToReorderCategory -> TODO()
                is HomeEvent.NavigateToReorderChannel -> TODO()
                
                is HomeEvent.ProjectDeleted -> {
                    // 삭제된 프로젝트에 대한 사용자 친화적인 메시지 표시
                    val message = "프로젝트 '${event.projectName}'이(가) 삭제되어 목록에서 제거되었습니다."
                    snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
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

    if(uiState.showBottomSheet) {
        BottomSheetDialog(
            items = uiState.showBottomSheetItems,
            onDismiss = viewModel::onProjectItemActionSheetDismiss
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            MainHomeFloatingButton(
                currentSection = uiState.selectedTopSection,
                isExpanded = showFloatingMenu,
                onExpandedChange = { showFloatingMenu = it },
                onAddProject = viewModel::onProjectAddButtonClick,
                onAddDm = viewModel::onAddFriendClick,
                onAddProjectElement = { uiState.selectedProjectId?.let { viewModel.onAddProjectElement(it) } }
            )
        }
    ) { paddingValues ->
        // 메인 콘텐츠와 오버레이를 포함하는 Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // AddProjectElementDialog
            if (showAddProjectElementDialog && currentProjectIdForDialog != null) {
                AddProjectElementDialog(
                    onDismissRequest = {
                        showAddProjectElementDialog = false
                        currentProjectIdForDialog = null
                        // 다이얼로그가 닫힐 때 프로젝트 구조를 새로고침합니다.
                        // 이것이 채널/카테고리 추가 후 UI가 업데이트되지 않는 문제를 해결합니다.
                        uiState.selectedProjectId?.let { viewModel.refreshProjectStructure(it) }
                    },
                    onAddCategoryClick = {
                        // TODO: Navigate to actual category creation screen
                    },
                    onAddChannelClick = {
                        // TODO: Navigate to actual channel creation screen  
                    }
                )
            }
            
            // EditCategoryDialog
            if (showEditCategoryDialog) {
                EditCategoryDialog(
                    categoryName = editCategoryName,
                    projectId = editCategoryProjectId,
                    categoryId = editCategoryId,
                    onDismissRequest = { showEditCategoryDialog = false },
                    onNavigateToEditCategory = {
                        // Navigation handled by ViewModel
                    },
                    onNavigateToCreateChannel = {
                        // Open AddProjectElementDialog for channel creation
                        currentProjectIdForDialog = uiState.selectedProjectId
                        showAddProjectElementDialog = true
                    }
                )
            }
            
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
            
            // 배경 오버레이 (다이얼로그 또는 메뉴가 열렸을 때 표시)
            AnimatedVisibility(
                visible = showFloatingMenu,
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
            
            // AddProjectElementDialog
            if (showAddProjectElementDialog && currentProjectIdForDialog != null) {
                AddProjectElementDialog(
                    onDismissRequest = {
                        showAddProjectElementDialog = false
                        currentProjectIdForDialog = null
                        // 다이얼로그가 닫힐 때 프로젝트 구조를 새로고침합니다.
                        // 이것이 채널/카테고리 추가 후 UI가 업데이트되지 않는 문제를 해결합니다.
                        uiState.selectedProjectId?.let { viewModel.refreshProjectStructure(it) }
                    },
                    onAddCategoryClick = {
                        // TODO: Navigate to actual category creation screen
                    },
                    onAddChannelClick = {
                        // TODO: Navigate to actual channel creation screen  
                    }
                )
            }
            
            // EditCategoryDialog
            if (showEditCategoryDialog) {
                EditCategoryDialog(
                    categoryName = editCategoryName,
                    projectId = editCategoryProjectId,
                    categoryId = editCategoryId,
                    onDismissRequest = { showEditCategoryDialog = false },
                    onNavigateToEditCategory = {
                        // Navigation handled by ViewModel
                    },
                    onNavigateToCreateChannel = {
                        // Open AddProjectElementDialog for channel creation
                        currentProjectIdForDialog = uiState.selectedProjectId
                        showAddProjectElementDialog = true
                    }
                )
            }
            
            // EditChannelDialog  
            if (showEditChannelDialog) {
                EditChannelDialog(
                    channelName = editChannelName,
                    projectId = editChannelProjectId,
                    channelId = editChannelId,
                    onDismissRequest = { showEditChannelDialog = false },
                    onNavigateToEditChannel = {
                        // Navigation handled by ViewModel
                    }
                )
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
            HomeMiddleSectionHeader(uiState = uiState, onClickTopSection = onClickTopSection)
            Spacer(modifier = Modifier.height(8.dp))

            // 컨텐츠 분기
            when (uiState.selectedTopSection) {
                TopSection.PROJECTS -> {
                    if (uiState.selectedProjectId != null) {
                        ProjectChannelList(
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

@Preview(showBackground = true)
@Composable
fun HomeContentProjectsPreview() {
    val previewState = HomeUiState(
        selectedTopSection = TopSection.PROJECTS,
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
            onCategoryLongPress = {},
            onChannelClick = { Log.d("Preview", "Channel clicked: ${it.name}") },
            onChannelLongPress = {},
            onDmItemClick = { Log.d("Preview", "DM clicked: ${it.partnerName}") },
            modifier = TODO(),
            onClickTopSection = TODO(),
            viewModel = TODO()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeContentDmsPreview() {
    val previewState = HomeUiState(
        selectedTopSection = TopSection.DMS,
        dms = List(3) { i ->
            DmUiModel(
                DocumentId("dm$i"), UserName("친구 ${i + 1}"),
                ImageUrl("url$i")
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
            onCategoryLongPress = {},
            onChannelClick = { Log.d("Preview", "Channel clicked: ${it.name}") },
            onChannelLongPress = {},
            onDmItemClick = { Log.d("Preview", "DM clicked: ${it.partnerName}") },
            modifier = TODO(),
            onClickTopSection = TODO(),
            viewModel = TODO()
        )
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
            onCategoryLongPress = {},
            onChannelClick = { Log.d("Preview", "Channel clicked: ${it.name}") },
            onChannelLongPress = {},
            onDmItemClick = { Log.d("Preview", "DM clicked: ${it.partnerName}") },
            modifier = TODO(),
            onClickTopSection = TODO(),
            viewModel = TODO()
        )
    }
}

// --- HomeScreen Preview ---

@Preview(showBackground = true, name = "HomeScreen Default Preview")
@Composable
fun HomeScreenPreview_Default() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        HomeScreen(modifier = TODO(), navigationManger = TODO(), viewModel = TODO())
    }
}

@Preview(showBackground = true, name = "HomeScreen With Data Preview")
@Composable
fun HomeScreenPreview_WithData() {

    val sampleUiState = HomeUiState(
        dms = listOf(
            DmUiModel(
                channelId = DocumentId("dm1"),
                partnerName = UserName("Alice"),
                partnerProfileImageUrl = null,
            ),
            DmUiModel(
                channelId = DocumentId("dm2"),
                partnerName = UserName("Bob"),
                partnerProfileImageUrl = null,
            )
        ),
        selectedTopSection = TopSection.PROJECTS,
        selectedProjectId = DocumentId("proj1"),
        projectName = "Project Alpha",
        userInitial = "U", // 사용자 이니셜 추가
        userProfileImageUrl = null, // 프로필 이미지 URL 추가
        projectStructure = ProjectStructureUiState(
            directChannel = listOf(
                ChannelUiModel(
                    id = DocumentId("ch1"),
                    name = Name("general"),
                    mode = ProjectChannelType.MESSAGES,
                    isSelected = false
                )
            ),
            categories = listOf(
                CategoryUiModel(
                    id = DocumentId("cat1"),
                    name = CategoryName("Text Channels"),
                    channels = listOf(
                        ChannelUiModel(
                            id = DocumentId("ch2"),
                            name = Name("announcements"),
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
                 onProjectSelect = { projectId ->
                     Log.d(
                         "Preview",
                         "Project selected in HomeContent: $projectId"
                     )
                 },
                 onProfileClick = { Log.d("Preview", "Profile clicked") }, // 프로필 클릭 핸들러 추가
                 onCategoryClick = { category ->
                     Log.d(
                         "Preview",
                         "Category clicked in HomeContent: ${category.name}"
                     )
                 },
                 onCategoryLongPress = {},
                 onChannelClick = { channel ->
                     Log.d(
                         "Preview",
                         "Channel clicked in HomeContent: ${channel.name}"
                     )
                 },
                 onChannelLongPress = {},
                 onDmItemClick = { dm ->
                     Log.d(
                         "Preview",
                         "DM clicked in HomeContent: ${dm.partnerName}"
                     )
                 },
                 onClickTopSection = TODO(),
                 viewModel = TODO()
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
                    isExpanded = TODO(),
                    onExpandedChange = TODO(),
                    modifier = TODO(),
                    menuItems = TODO(),
                )
            },
            floatingActionButtonPosition = FabPosition.End
        ) { innerPadding ->
            // 실제 HomeScreen을 호출하되, Scaffold의 innerPadding을 활용하도록 가정
            // HomeScreen 내부에서 이 padding을 어떻게 사용하는지에 따라 결과가 달라짐
            // HomeScreen이 자체 Scaffold를 사용한다면, 여기서는 별도 패딩 없이 호출
            HomeScreen(
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()), // HomeScreen이 Scaffold 없이 바로 Content를 그린다고 가정
                navigationManger = TODO()
            )
        }
    }
}
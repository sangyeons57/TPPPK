package com.example.feature_main.ui

import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.feature_main.viewmodel.TopSection
import kotlinx.coroutines.flow.collectLatest
import com.example.feature_main.ui.project.ProjectChannelList
import com.example.feature_main.viewmodel.HomeUiState
import com.example.feature_main.ui.components.ExtendableFloatingActionMenu
import com.example.feature_main.ui.wrapper.AddDmUserDialogWrapper
import androidx.navigation.NavHostController
import com.example.feature_main.ui.project.CategoryUiModel
import com.example.feature_main.ui.project.ChannelUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.example.core_navigation.core.NavDestination
import com.example.domain.model.enum.ProjectChannelType
import com.example.feature_main.ui.components.MainHomeFloatingButton
import com.example.feature_main.ui.dialog.AddProjectElementDialog
import androidx.compose.runtime.rememberCoroutineScope
import com.example.core_ui.components.bottom_sheet_dialog.BottomSheetDialog
import com.example.feature_main.ui.project.ProjectStructureUiState

// 오버레이 투명도 상수
private const val OVERLAY_ALPHA = 0.7f

// 상태 저장 키 상수
private object HomeScreenStateKeys {
    const val SELECTED_TOP_SECTION = "selected_top_section"
    const val SELECTED_PROJECT_ID = "selected_project_id"
    const val EXPANDED_CATEGORIES = "expanded_categories"
    const val SHOW_ADD_DM_DIALOG = "show_add_dm_dialog"

    const val SHOW_FLOATING_MENU = "show_floating_menu"
}

/**
 * HomeScreen: 디스코드 스타일 홈 화면
 * - 왼쪽: 프로필/프로젝트 사이드바
 * - 중간: DM 목록 또는 선택한 프로젝트의 채널 목록
 * - 오른쪽: (채팅 화면으로 이동)
 *
 * @param modifier UI 요소에 적용할 Modifier.
 * @param appNavigator 내비게이션 이벤트를 처리하는 AppNavigator.
 * @param viewModel HomeViewModel 인스턴스, 화면의 상태 및 비즈니스 로직 관리.
 * @param savedState 화면 상태를 복원하기 위한 Bundle (탭 전환 시 상태 유지).
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    appNavigator: AppNavigator,
    viewModel: HomeViewModel = hiltViewModel(),
    savedState: Bundle? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // 다이얼로그 상태 추가
    var showAddDmDialog by remember { mutableStateOf(false) }
    var showAddProjectElementDialog by remember { mutableStateOf(false) }
    var showFloatingMenu by remember { mutableStateOf(false) }
    var currentProjectIdForDialog by remember { mutableStateOf<String?>(null) }

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
        showFloatingMenu
    ) {
        onDispose {
            // 화면이 비활성화될 때 상태 저장
            val screenState = Bundle().apply {
                putString(HomeScreenStateKeys.SELECTED_TOP_SECTION, uiState.selectedTopSection.name)
                putString(HomeScreenStateKeys.SELECTED_PROJECT_ID, uiState.selectedProjectId ?: "")
                putBoolean(HomeScreenStateKeys.SHOW_ADD_DM_DIALOG, showAddDmDialog)
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
                is HomeEvent.NavigateToProjectSettings -> {
                    appNavigator.navigate(NavigationCommand.NavigateToRoute(NavDestination.fromRoute(AppRoutes.Project.settings(event.projectId))))
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
                is HomeEvent.NavigateToDmChat -> {
                    Log.d("HomeScreen", "Navigating to DM Chat with ID: ${event.dmId}")
                    appNavigator.navigate(NavigationCommand.NavigateToRoute(NavDestination.fromRoute(AppRoutes.Chat.screen(event.dmId))))
                }
                is HomeEvent.NavigateToChannel -> {
                    appNavigator.navigate(NavigationCommand.NavigateToRoute(NavDestination.fromRoute(AppRoutes.Chat.screen(event.channelId))))
                }
                is HomeEvent.ShowAddProjectElementDialog -> {
                    currentProjectIdForDialog = event.projectId
                    showAddProjectElementDialog = true
                }

                is HomeEvent.NavigateToEditCategory -> TODO()
                is HomeEvent.NavigateToEditChannel -> TODO()
                is HomeEvent.NavigateToReorderCategory -> TODO()
                is HomeEvent.NavigateToReorderChannel -> TODO()
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
                    projectId = currentProjectIdForDialog!!,
                    onDismissRequest = {
                        showAddProjectElementDialog = false
                        currentProjectIdForDialog = null
                        // 다이얼로그가 닫힐 때 프로젝트 구조를 새로고침합니다.
                        // 이것이 채널/카테고리 추가 후 UI가 업데이트되지 않는 문제를 해결합니다.
                        uiState.selectedProjectId?.let { viewModel.refreshProjectStructure(it) }
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
                onDmItemClick = viewModel::onDmItemClick
            )
            
            // 배경 오버레이 (다이얼로그 또는 메뉴가 열렸을 때 표시)
            AnimatedVisibility(
                visible = showAddDmDialog || showFloatingMenu,
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
    onClickTopSection: () -> Unit,
    onCategoryClick: (category: CategoryUiModel) -> Unit,
    onCategoryLongPress: (category: CategoryUiModel) -> Unit = {},
    onChannelClick: (channel: ChannelUiModel) -> Unit,
    onChannelLongPress: (channel: ChannelUiModel) -> Unit = {},
    onDmItemClick: (dm: DmUiModel) -> Unit
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
        modifier = Modifier.fillMaxWidth()
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
            onClickTopSection = TODO()
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
            onCategoryLongPress = {},
            onChannelClick = { Log.d("Preview", "Channel clicked: ${it.name}") },
            onChannelLongPress = {},
            onDmItemClick = { Log.d("Preview", "DM clicked: ${it.partnerName}") },
            modifier = TODO(),
            onClickTopSection = TODO()
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
            onCategoryLongPress = {},
            onChannelClick = { Log.d("Preview", "Channel clicked: ${it.name}") },
            onChannelLongPress = {},
            onDmItemClick = { Log.d("Preview", "DM clicked: ${it.partnerName}") },
            modifier = TODO(),
            onClickTopSection = TODO()
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
                 onClickTopSection = TODO()
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
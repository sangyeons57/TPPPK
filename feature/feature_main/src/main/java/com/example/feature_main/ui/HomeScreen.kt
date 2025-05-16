package com.example.feature_main.ui

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_common.util.DateTimeUtil
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.core_navigation.core.NavigationCommand
import com.example.core_navigation.destination.AppRoutes
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.ui.DmUiModel
import com.example.domain.model.ui.ProjectUiModel
import com.example.feature_main.viewmodel.HomeEvent
import com.example.feature_main.viewmodel.HomeViewModel
import com.example.feature_main.viewmodel.ProjectItem
import com.example.feature_main.viewmodel.TopSection
import kotlinx.coroutines.flow.collectLatest
import com.example.feature_main.ui.project.ProjectChannelList
import com.example.feature_main.viewmodel.HomeUiState

/**
 * HomeScreen: 디스코드 스타일 홈 화면
 * - 왼쪽: 프로필/프로젝트 사이드바
 * - 중간: DM 목록 또는 선택한 프로젝트의 채널 목록
 * - 오른쪽: (채팅 화면으로 이동)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navigationHandler: ComposeNavigationHandler,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 앱 시작 시 DM 섹션을 기본 선택되도록 설정
    LaunchedEffect(Unit) {
        viewModel.onTopSectionSelect(TopSection.DMS)
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
                    snackbarHostState.showSnackbar("친구 추가 다이얼로그 (미구현)")
                }
                is HomeEvent.NavigateToAddProject -> {
                    navigationHandler.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Project.ADD))
                }
                is HomeEvent.NavigateToProjectSettings -> {
                    navigationHandler.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Project.settings(event.projectId)))
                }
                is HomeEvent.NavigateToDmChat -> {
                    Log.d("HomeScreen", "Navigating to DM Chat with ID: ${event.dmId}")
                    navigationHandler.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Chat.screen(event.dmId)))
                }
                is HomeEvent.NavigateToChannel -> {
                    navigationHandler.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Chat.screen(event.channelId)))
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
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // 선택된 탭에 따라 FAB 동작 변경
            FloatingActionButton(onClick = {
                if (uiState.selectedTopSection == TopSection.PROJECTS) {
                    viewModel.onProjectAddButtonClick()
                } else {
                    viewModel.onAddFriendClick() // 이 메소드를 ViewModel에 추가해야 합니다
                }
            }) {
                Icon(Icons.Filled.Add, contentDescription = "추가")
            }
        }
    ) { paddingValues ->
        HomeContent(
            modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()),
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
    }
}

/**
 * HomeContent: 디스코드 스타일 UI 렌더링
 */
@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    uiState: com.example.feature_main.viewmodel.HomeUiState,
    onProjectSelect: (projectId: String) -> Unit,
    onProfileClick: () -> Unit,
    onCategoryClick: (category: com.example.feature_main.ui.project.CategoryUiModel) -> Unit,
    onChannelClick: (channel: com.example.feature_main.ui.project.ChannelUiModel) -> Unit,
    onDmItemClick: (dm: DmUiModel) -> Unit
) {
    Row(modifier = modifier.fillMaxSize()) {
        // 1. 왼쪽 사이드바: 프로필과 프로젝트 목록
        ProjectListScreen(
            projects = uiState.projects.map { ProjectUiModel(id = it.id, name = it.name, description = it.description, imageUrl = null) },
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
    uiState: com.example.feature_main.viewmodel.HomeUiState
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
    uiState: com.example.feature_main.viewmodel.HomeUiState,
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
    if (uiState.projectStructure.categories.isEmpty() && uiState.projectStructure.generalChannels.isEmpty()) {
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

// ProjectListItem을 이미지 기반 디자인으로 수정
@Composable
fun ProjectListItem(
    projectName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = projectName.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        if (!isCompact) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = projectName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}


// 사이드바의 프로젝트 리스트 부분도 수정
@Composable
fun ProjectsList(
    projects: List<ProjectItem>,
    onProjectClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            items = projects,
            key = { project -> project.id }
        ) { project ->
            ProjectListItem(
                projectName = project.name,
                onClick = { onProjectClick(project.id) },
                isCompact = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeContentProjectsPreview() {
    val previewState = HomeUiState(
        selectedTopSection = TopSection.PROJECTS,
        projects = List(5) { i ->
            ProjectItem(
                "p$i",
                "미리보기 프로젝트 ${i + 1}",
                "설명 ${i + 1}",
                "${i}분 전"
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
    val previewState = com.example.feature_main.viewmodel.HomeUiState(
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
        HomeScreen(
            navigationHandler = TODO("preview"),
        )
    }
}

@Preview(showBackground = true, name = "HomeScreen With Data Preview")
@Composable
fun HomeScreenPreview_WithData() {

    val sampleUiState = HomeUiState(
        projects = listOf(
            ProjectItem("proj1", "Project Alpha", "Description for Alpha", "10m ago"),
            ProjectItem("proj2", "Project Beta", "Description for Beta", "1h ago")
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
        projectStructure = com.example.feature_main.ui.project.ProjectStructureUiState(
            generalChannels = listOf(
                com.example.feature_main.ui.project.ChannelUiModel(
                    id = "ch1",
                    name = "general",
                    mode = com.example.domain.model.ChannelMode.TEXT,
                    isSelected = false
                )
            ),
            categories = listOf(
                com.example.feature_main.ui.project.CategoryUiModel(
                    id = "cat1",
                    name = "Text Channels",
                    channels = listOf(
                        com.example.feature_main.ui.project.ChannelUiModel(
                            id = "ch2",
                            name = "announcements",
                            mode = com.example.domain.model.ChannelMode.TEXT,
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
                FloatingActionButton(onClick = { Log.d("Preview", "FAB Clicked") }) { // Simple log for preview
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
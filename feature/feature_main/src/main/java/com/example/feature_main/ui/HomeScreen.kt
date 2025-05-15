package com.example.feature_main.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.ui.tooling.preview.Preview
import com.example.feature_main.viewmodel.HomeUiState

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.core_navigation.core.NavigationCommand
import com.example.core_navigation.destination.AppRoutes
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_main.viewmodel.HomeEvent
import com.example.feature_main.viewmodel.HomeViewModel
import com.example.feature_main.viewmodel.ProjectItem
import com.example.feature_main.viewmodel.TopSection
import kotlinx.coroutines.flow.collectLatest
import com.example.feature_main.ui.project.ProjectChannelList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.ui.DmUiModel

/**
 * HomeScreen: 상태 관리 및 이벤트 처리 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class) // TabRow, FAB 등 사용
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navigationHandler: ComposeNavigationHandler,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 이벤트 처리 (스낵바, 다이얼로그 등)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is HomeEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                }
                is HomeEvent.ShowAddProjectDialog -> {
                    // TODO: 프로젝트 추가 다이얼로그 표시 로직
                    snackbarHostState.showSnackbar("프로젝트 추가 다이얼로그 (미구현)")
                }
                is HomeEvent.ShowAddFriendDialog -> {
                    // TODO: 친구 추가 다이얼로그 표시 로직
                    snackbarHostState.showSnackbar("친구 추가 다이얼로그 (미구현)")
                }
                is HomeEvent.NavigateToAddProject -> {
                    navigationHandler.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Project.ADD))
                }
                is HomeEvent.NavigateToProjectSettings -> {
                    // 프로젝트 설정 화면으로 이동 (별도 화면)
                    navigationHandler.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Project.settings(event.projectId)))
                }
                is HomeEvent.NavigateToDmChat -> {
                    // DM 채팅 화면으로 이동
                    // Assuming AppRoutes.Chat.chat(channelId, chatName?) exists or similar
                    // For DMs, chatName can be the partner's name, fetched via ViewModel if needed or passed directly
                    // For now, let's pass null for chatName, ChatViewModel can fetch participant details
                    Log.d("HomeScreen", "Navigating to DM Chat with ID: ${event.dmId}")
                    navigationHandler.navigateToChat(event.dmId, null) 
                }
                is HomeEvent.NavigateToChannel -> {
                    // 채널 화면으로 이동
                    navigationHandler.navigateToChat(event.channelId, null)
                }
            }
        }
    }


    // 에러 메시지 스낵바로 표시
    LaunchedEffect(uiState.errorMessage) {
        snackbarHostState.showSnackbar(uiState.errorMessage, duration = SnackbarDuration.Short)
        viewModel.errorMessageShown()
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick =
                viewModel::onProjectAddButtonClick
                //viewModel::onAddButtonClick
            ) {
                Icon(Icons.Filled.Add, contentDescription = "추가")
            }
        }
    ) { paddingValues -> // Scaffold는 content 영역에 패딩 제공
        HomeContent(
            modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()), // 패딩 적용
            uiState = uiState,
            viewModel = viewModel,
        )
    }
}

/**
 * HomeContent: UI 렌더링 (Stateless)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    viewModel: HomeViewModel,
) {
    Row(modifier = modifier.fillMaxSize()) {
        // 1. 왼쪽 사이드바
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(100.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 상단부 (프로필 + 프로젝트 리스트)
                Column(modifier = Modifier.weight(1f)) {
                    // DM 모드 전환을 위한 헤더 이미지
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { viewModel.onTopSectionSelect(TopSection.DMS) },
                        contentAlignment = Alignment.Center
                    ) {
                        // 프로필 이미지 (원형)
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Me",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // 프로젝트 리스트
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        ProjectsList(
                            projects = uiState.projects,
                            onProjectClick = {
                                viewModel.onProjectClick(it)
                                viewModel.onTopSectionSelect(TopSection.PROJECTS)
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                        Column(
                            modifier = modifier.padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ProjectListItem(
                                projectName = "프로젝트 추가 버튼",
                                onClick = viewModel::onProjectAddButtonClick,
                                isCompact = true
                            )
                        }
                    }
                }
            }
        }

        // 2. 오른쪽 메인 콘텐츠 영역
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp)
        ) {
            MainContentTopSection(uiState)
            Spacer(modifier = Modifier.height(8.dp))
            // 메인 콘텐츠
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                MainContent(uiState, viewModel)
            }
        }
    }
}

@Composable
fun MainContentTopSection(uiState: HomeUiState) {
    // 상단 탭 (선택된 모드에 따라 다른 텍스트 표시)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        when (uiState.selectedTopSection) {
            TopSection.PROJECTS -> MainContentTopSection_Projects(uiState)
            TopSection.DMS -> MainContentTopSection_Dms(uiState)
        }
    }
}

@Composable
fun MainContentTopSection_Projects(uiState: HomeUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
    ) {
        Text(
            text = "프로젝트 이름",
            style = MaterialTheme.typography.headlineLarge, // 28sp
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(4.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { /* 검색 로직 */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "검색하기",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Button(
                onClick = { /* 멤버 추가 로직 */ },
                modifier = Modifier.width(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "+",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun MainContentTopSection_Dms(uiState: HomeUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = "메시지",
            style = MaterialTheme.typography.headlineLarge, // 28sp
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(4.dp)
        )
        
        Button(
            onClick = { /* 친구 추가 로직 */ },
            modifier = Modifier
                .width(200.dp)
                .align(Alignment.End)
                .padding(end = 20.dp, bottom = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer // default_app_color_bl1에 해당
            )
        ) {
            Text(
                text = "친구 추가하기",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun MainContent(
    uiState: HomeUiState,
    viewModel: HomeViewModel
) {
    when (uiState.selectedTopSection) {
        TopSection.PROJECTS -> {
            if (uiState.selectedProjectId != null) {
                // 프로젝트 구조 (카테고리 및 채널 목록) 표시
                ProjectContentScreen(
                    uiState = uiState.projectStructure,
                    onChannelClick = { channelId, categoryId ->  // Modified lambda
                        viewModel.onChannelClick(channelId, categoryId, uiState.selectedProjectId ?: "")
                    },
                    onCategoryHeaderClick = { categoryId ->
                        viewModel.toggleCategoryExpanded(categoryId)
                    },
                    selectedChannelId = uiState.projectStructure.selectedChannelId
                )
            } else {
                // TODO: "프로젝트를 선택하세요" 또는 기본 프로젝트 대시보드
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("프로젝트를 선택해주세요.")
                }
            }
        }
        TopSection.DMS -> {
            DmListScreen( // New Composable for DMs
                dms = uiState.dms,
                onDmItemClick = { dmItem ->
                    viewModel.onDmItemClick(dmItem)
                },
                isLoading = uiState.isLoading // Pass isLoading for DMs specifically
            )
        }
    }
}

@Composable
fun DmListScreen(
    dms: List<DmUiModel>,
    onDmItemClick: (DmUiModel) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading && dms.isEmpty()) { // Show loader only if list is empty and loading
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (dms.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("활성화된 DM이 없습니다.")
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(dms, key = { it.channelId }) { dmItem ->
            DmListItem(dmItem = dmItem, onClick = { onDmItemClick(dmItem) })
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // For Badge
@Composable
fun DmListItem(
    dmItem: DmUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TODO: Replace with CoilAsyncImage or similar for network images
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(dmItem.partnerName.firstOrNull()?.toString() ?: "P", color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dmItem.partnerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dmItem.lastMessage ?: "메시지가 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = dmItem.lastMessageTimestamp ?: "default",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DmListItemPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        DmListItem(
            dmItem = DmUiModel(
                channelId = "dm1",
                partnerName = "김철수",
                partnerProfileImageUrl = null,
                lastMessage = "내일 회의 시간에 맞춰서 와주세요. 장소는 동일합니다.",
                lastMessageTimestamp = "2023-09-01T10:30:00",
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DmListScreenPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        val sampleDms = listOf(
            DmUiModel("1", "u1", "User One", null, DateTimeUtil.nowInstant()),
            DmUiModel("2", "u2", "User Two With A Very Long Name To Test Ellipsis", null, DateTimeUtil.nowInstant()),
            DmUiModel("3", "u3", "User Three", null, DateTimeUtil.nowInstant())
        )
        DmListScreen(dms = sampleDms, onDmItemClick = {}, isLoading = false)
    }
}

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
            key = { it.id }
        ) { project ->
            ProjectListItem(
                projectName = project.name,
                onClick = { onProjectClick(project.id) },
                isCompact = true
            )
        }
    }
}

@Composable
fun EmptyStateMessage(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
    }
}


// --- Preview ---
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
        isLoading = false
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        HomeContent(uiState = previewState, viewModel = hiltViewModel());
    }
}

@Preview(showBackground = true)
@Composable
fun HomeContentDmsPreview() {
    val previewState = HomeUiState(
        selectedTopSection = TopSection.DMS,
        dms = List(3) { i -> DmUiModel("dm$i", "친구 ${i + 1}", "미리보기 메시지 ${i + 1}", "timestamp", null) },
        isLoading = false
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        HomeContent(uiState = previewState, viewModel = hiltViewModel());
    }
}

@Preview(showBackground = true)
@Composable
fun HomeContentLoadingPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        HomeContent(uiState = HomeUiState(isLoading = true), viewModel = hiltViewModel());
    }
}
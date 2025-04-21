package com.example.teamnovapersonalprojectprojectingkotlin.feature_main.ui

import androidx.compose.foundation.background
import androidx.compose.ui.tooling.preview.Preview
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.viewmodel.HomeUiState
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.mainScreenBottomBar
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.viewmodel.DmItem
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.viewmodel.HomeEvent
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.viewmodel.HomeViewModel
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.viewmodel.ProjectItem
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.viewmodel.TopSection
import com.example.teamnovapersonalprojectprojectingkotlin.navigation.AddProject
import com.example.teamnovapersonalprojectprojectingkotlin.navigation.mainBottomNavItems
import kotlinx.coroutines.flow.collectLatest

/**
 * HomeScreen: 상태 관리 및 이벤트 처리 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class) // TabRow, FAB 등 사용
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
     navController: NavHostController, // 외부 화면 이동 시 필요
    onNavigateToAddProject: () -> Unit,
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
                is HomeEvent.NavigateToAddProject -> { // 추가된 이벤트 처리
                    onNavigateToAddProject()
                }
                // is HomeEvent.NavigateToProjectDetails -> navController.navigate(...)
                // is HomeEvent.NavigateToDmChat -> navController.navigate(...)
                else -> {snackbarHostState.showSnackbar("Else")}
            }
        }
    }


    // 에러 메시지 스낵바로 표시
    LaunchedEffect(uiState.errorMessage) {
        snackbarHostState.showSnackbar(uiState.errorMessage!!, duration = SnackbarDuration.Short)
        viewModel.errorMessageShown()
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onAddButtonClick) {
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
                MainContent(uiState)
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
fun MainContent(uiState: HomeUiState) {
    when (uiState.selectedTopSection) {
        TopSection.PROJECTS -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "프로젝트 상세 정보",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "선택된 프로젝트의 상세 정보가 표시됩니다.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        TopSection.DMS -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                tonalElevation = 1.dp
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.dms.isEmpty()) {
                        item {
                            EmptyStateMessage("메시지가 없습니다.")
                        }
                    } else {
                        items(
                            items = uiState.dms,
                            key = { it.id }
                        ) { dm ->
                            DmListItem(dm = dm, onClick = { /** DM 클릭시 **/})
                        }
                    }
                }
            }
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
fun DmListItem(
    dm: DmItem,
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
        // TODO: 상대방 프로필 이미지 표시 (예: AsyncImage)
        /**
        AsyncImage( // Coil 라이브러리 필요: implementation("io.coil-kt:coil-compose:...")
            model = dm.partnerProfileUrl ?: com.example.teamnovapersonalprojectprojectingkotlin.R.drawable.ic_launcher_foreground, // 기본 이미지 지정
            contentDescription = "${dm.partnerName} 프로필",
            modifier = Modifier.size(48.dp)//.clip(CircleShape) // 원형 이미지
        )
        **/
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = dm.partnerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (dm.lastMessage != null) {
                Text(
                    text = dm.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        // 안 읽은 메시지 뱃지 (예시)
        if (dm.unreadCount > 0) {
            Badge { Text("${dm.unreadCount}") } // Material 3 Badge
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
        projects = List(5) { i -> ProjectItem("p$i", "미리보기 프로젝트 ${i + 1}", "설명 ${i + 1}", "${i}분 전") },
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
        dms = List(3) { i -> DmItem("dm$i", "친구 ${i+1}", "미리보기 메시지 ${i+1}", i, null) },
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
package com.example.feature_project.roles.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.AppNavigator
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.core.NavigationCommand
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_project.roles.viewmodel.RoleItem
import com.example.feature_project.roles.viewmodel.RoleListEvent
import com.example.feature_project.roles.viewmodel.RoleListViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * RoleListScreen: 프로젝트 역할 목록 표시 및 관리 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleListScreen(
    appNavigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: RoleListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 이벤트 처리 (네비게이션, 스낵바)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is RoleListEvent.NavigateToAddRole -> appNavigator.navigate(
                    NavigationCommand.NavigateToRoute.fromRoute(
                        AppRoutes.Project.addRole(uiState.projectId)
                    )
                )
                is RoleListEvent.NavigateToEditRole -> appNavigator.navigate(
                    NavigationCommand.NavigateToRoute.fromRoute(
                        AppRoutes.Project.editRole(uiState.projectId, event.roleId)
                    )
                )
                is RoleListEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("역할 관리") },
                navigationIcon = {
                    IconButton(onClick = { appNavigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onAddRoleClick) {
                Icon(Icons.Filled.Add, contentDescription = "역할 추가")
            }
        }
    ) { paddingValues ->
        // 로딩 및 에러 상태 처리
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("오류: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                }
            }
            uiState.roles.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("생성된 역할이 없습니다.")
                }
            }
            else -> {
                RoleListContent(
                    modifier = Modifier.padding(paddingValues),
                    roles = uiState.roles,
                    onRoleClick = viewModel::onRoleClick // ViewModel의 함수 호출
                )
            }
        }
    }
}

/**
 * RoleListContent: 역할 목록 UI (Stateless)
 */
@Composable
fun RoleListContent(
    modifier: Modifier = Modifier,
    roles: List<RoleItem>,
    onRoleClick: (String) -> Unit // roleId 전달
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = roles,
            key = { it.id }
        ) { role ->
            RoleListItem(
                role = role,
                onClick = { onRoleClick(role.id) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

/**
 * RoleListItem: 개별 역할 아이템 UI (Stateless)
 */
@Composable
fun RoleListItem(
    role: RoleItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = role.name,
            style = MaterialTheme.typography.bodyLarge
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "역할 편집",
            tint = MaterialTheme.colorScheme.outline
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun RoleListContentPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { /*TODO*/ }) {
                    Icon(Icons.Filled.Add, contentDescription = "역할 추가")
                }
            }
        ) { padding ->
            RoleListContent(
                modifier = Modifier.padding(padding),
                roles = listOf(
                    RoleItem("1", "관리자"),
                    RoleItem("2", "팀 리더"),
                    RoleItem("3", "멤버"),
                    RoleItem("4", "방문자")
                ),
                onRoleClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Role List Empty")
@Composable
private fun RoleListEmptyPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        // Create a simpler preview without NavigationManager dependency
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("생성된 역할이 없습니다.")
        }
    }
}

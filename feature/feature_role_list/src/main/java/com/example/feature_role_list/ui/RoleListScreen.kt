package com.example.feature_role_list.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.AddRoleRoute
import com.example.core_navigation.core.EditRoleRoute
import com.example.core_navigation.core.NavigationManger
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.feature_role_list.viewmodel.RoleItem
import com.example.feature_role_list.viewmodel.RoleListEvent
import com.example.feature_role_list.viewmodel.RoleListViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * RoleListScreen: 프로젝트 역할 목록 표시 및 관리 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleListScreen(
    navigationManger: NavigationManger,
    modifier: Modifier = Modifier,
    viewModel: RoleListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteRoleDialog by remember { mutableStateOf<RoleItem?>(null) } // Added

    // 이벤트 처리 (네비게이션, 스낵바, 다이얼로그)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is RoleListEvent.NavigateToAddRole -> navigationManger.navigateTo(
                    AddRoleRoute(uiState.projectId)
                )

                is RoleListEvent.NavigateToEditRole -> navigationManger.navigateTo(
                    EditRoleRoute(uiState.projectId, event.roleId)
                )
                is RoleListEvent.ShowDeleteRoleConfirmDialog -> { // Added
                    showDeleteRoleDialog = event.roleItem
                }
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
                    IconButton(onClick = { navigationManger.navigateBack() }) {
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
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("오류: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                }
            }
            uiState.roles.isEmpty() -> {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("생성된 역할이 없습니다.")
                }
            }
            else -> {
                RoleListContent(
                    modifier = Modifier.padding(paddingValues),
                    roles = uiState.roles,
                    onRoleClick = viewModel::onRoleClick,
                    onRequestDeleteRole = viewModel::requestDeleteRole // Added
                )
            }
        }
    }

    // Delete confirmation dialog
    showDeleteRoleDialog?.let { roleToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteRoleDialog = null },
            title = { Text("역할 삭제") },
            text = { Text("'${roleToDelete.name}' 역할을 삭제하시겠습니까? 이 역할이 할당된 모든 멤버에게서 제거됩니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmDeleteRole(roleToDelete.id)
                        showDeleteRoleDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteRoleDialog = null }) { Text("취소") }
            }
        )
    }
}

/**
 * RoleListContent: 역할 목록 UI (Stateless)
 */
@Composable
fun RoleListContent(
    modifier: Modifier = Modifier,
    roles: List<RoleItem>,
    onRoleClick: (DocumentId) -> Unit,
    onRequestDeleteRole: (RoleItem) -> Unit // Added
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = roles,
            key = { it.id.value }
        ) { role ->
            RoleListItem(
                role = role,
                onClick = { onRoleClick(role.id) },
                onDeleteClick = { onRequestDeleteRole(role) } // Pass role item
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
    onDeleteClick: () -> Unit, // Added for delete action
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp), // Adjusted padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = role.name.value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDeleteClick) { // Added delete button
            Icon(Icons.Filled.Delete, contentDescription = "역할 삭제", tint = MaterialTheme.colorScheme.error)
        }
        Icon( // Existing navigation arrow
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
                    RoleItem(DocumentId("1"), Name("관리자")),
                    RoleItem(DocumentId("2"), Name("팀 리더")),
                    RoleItem(DocumentId("3"), Name("멤버")),
                    RoleItem(DocumentId("4"), Name("방문자"))
                ),
                onRoleClick = {},
                onRequestDeleteRole = {}
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

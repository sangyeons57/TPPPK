package com.example.feature_add_role.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.NavigationManger
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.vo.permission.PermissionCategory
import com.example.domain.model.vo.permission.PermissionType
import com.example.feature_add_role.viewmodel.AddRoleEvent
import com.example.feature_add_role.viewmodel.AddRoleUiState
import com.example.feature_add_role.viewmodel.AddRoleViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * AddRoleScreen: 새 역할 생성 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRoleScreen(
    navigationManger: NavigationManger,
    modifier: Modifier = Modifier,
    viewModel: AddRoleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddRoleEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is AddRoleEvent.ClearFocus -> focusManager.clearFocus()
                is AddRoleEvent.NavigateBack -> navigationManger.navigateBack()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("새 역할 추가") },
                navigationIcon = {
                    IconButton(onClick = { navigationManger.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        AddRoleContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onRoleNameChange = viewModel::onRoleNameChange,
            onIsDefaultChange = viewModel::onIsDefaultChange,
            onPermissionCheckedChange = viewModel::onPermissionCheckedChange,
            onCreateClick = viewModel::createRole
        )
    }
}

/**
 * AddRoleContent: 역할 생성 UI 요소 (Stateless)
 */
@Composable
fun AddRoleContent(
    modifier: Modifier = Modifier,
    uiState: AddRoleUiState,
    onRoleNameChange: (String) -> Unit,
    onIsDefaultChange: (Boolean) -> Unit,
    onPermissionCheckedChange: (PermissionType, Boolean) -> Unit,
    onCreateClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 역할 이름 입력
        OutlinedTextField(
            value = uiState.roleName,
            onValueChange = onRoleNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("역할 이름") },
            singleLine = true,
            isError = uiState.error?.contains("이름") == true
        )

        // 기본 역할 설정
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "기본 역할",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "새 멤버에게 자동으로 할당되는 역할",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.isDefault,
                    onCheckedChange = onIsDefaultChange,
                    enabled = !uiState.isLoading
                )
            }
        }

        // 권한 설정 섹션
        Text(
            "권한 설정",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        // 권한을 카테고리별로 그룹화하여 표시
        val permissionsByCategory = PermissionType.getPermissionsByCategory()
        permissionsByCategory.forEach { (category, permissions) ->
            PermissionCategorySection(
                category = category,
                permissions = permissions,
                permissionStates = uiState.permissions,
                onPermissionCheckedChange = onPermissionCheckedChange,
                enabled = !uiState.isLoading
            )
        }

        // 에러 메시지 표시
        if (uiState.error != null) {
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 생성 버튼
        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.roleName.isNotBlank() && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("역할 생성")
            }
        }
    }
}

/**
 * PermissionCategorySection: 권한 카테고리별 섹션 UI
 */
@Composable
fun PermissionCategorySection(
    category: PermissionCategory,
    permissions: List<PermissionType>,
    permissionStates: Map<PermissionType, Boolean>,
    onPermissionCheckedChange: (PermissionType, Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            permissions.forEach { permission ->
                PermissionSwitchRow(
                    permission = permission,
                    isChecked = permissionStates[permission] ?: false,
                    onCheckedChange = { isChecked ->
                        onPermissionCheckedChange(permission, isChecked)
                    },
                    enabled = enabled
                )
            }
        }
    }
}

/**
 * PermissionSwitchRow: 개별 권한 설정 행 UI (Stateless)
 */
@Composable
fun PermissionSwitchRow(
    permission: PermissionType,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = permission.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) LocalContentColor.current else MaterialTheme.colorScheme.outline
            )
            Text(
                text = permission.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun AddRoleContentPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        AddRoleContent(
            uiState = AddRoleUiState(),
            onRoleNameChange = {},
            onIsDefaultChange = {},
            onPermissionCheckedChange = { _, _ -> },
            onCreateClick = {}
        )
    }
}
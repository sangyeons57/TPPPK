package com.example.feature_edit_role.ui

// Domain 모델 및 ViewModel 관련 요소 Import
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
import androidx.compose.material.icons.filled.Check
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
import com.example.domain.vo.DocumentId
import com.example.domain.vo.Name
import com.example.domain.vo.permission.PermissionCategory
import com.example.domain.vo.permission.PermissionType
import com.example.domain.vo.role.RoleIsDefault
import com.example.feature_edit_role.viewmodel.EditRoleEvent
import com.example.feature_edit_role.viewmodel.EditRoleUiState
import com.example.feature_edit_role.viewmodel.EditRoleViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * EditRoleScreen: 프로젝트 역할 수정 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoleScreen(
    navigationManger: NavigationManger,
    modifier: Modifier = Modifier,
    viewModel: EditRoleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditRoleEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is EditRoleEvent.ClearFocus -> focusManager.clearFocus()
            }
        }
    }

    // 저장 성공 시 뒤로가기
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            navigationManger.navigateBack()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("역할 편집") },
                navigationIcon = {
                    IconButton(onClick = { navigationManger.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::saveRole,
                        enabled = uiState.roleName.isNotBlank() && !uiState.isLoading && uiState.hasChanges
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Filled.Check, contentDescription = "저장")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        // 초기 로딩 처리
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null && uiState.originalRoleName.value.isEmpty()) { // 로딩 에러 처리
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), contentAlignment = Alignment.Center
            ) {
                Text("오류: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            }
        } else {
            // 메인 콘텐츠 표시
            EditRoleContent(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onRoleNameChange = { name -> viewModel.onRoleNameChange(Name(name)) },
                onIsDefaultChange = { isDefault -> viewModel.onIsDefaultChange(RoleIsDefault(isDefault)) },
                onPermissionCheckedChange = viewModel::onPermissionCheckedChange
            )
        }
    }

}

/**
 * EditRoleContent: 역할 편집 UI 요소 (Stateless)
 */
@Composable
fun EditRoleContent(
    modifier: Modifier = Modifier,
    uiState: EditRoleUiState,
    onRoleNameChange: (String) -> Unit,
    onIsDefaultChange: (Boolean) -> Unit,
    onPermissionCheckedChange: (PermissionType, Boolean) -> Unit
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
            value = uiState.roleName.value,
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
                    checked = uiState.isDefault.value,
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
        } else {
            Spacer(modifier = Modifier.height(MaterialTheme.typography.bodySmall.lineHeight.value.dp))
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
private fun EditRoleContent_Preview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        EditRoleContent(
            uiState = EditRoleUiState(
                roleId = DocumentId("r1"),
                roleName = Name("운영진"),
                isDefault = RoleIsDefault(false),
                permissions = mapOf(PermissionType.MANAGE_ROLES to true)
            ),
            onRoleNameChange = {},
            onIsDefaultChange = {},
            onPermissionCheckedChange = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "Edit Role Loading")
@Composable
private fun EditRoleContentLoadingPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        EditRoleContent(
            uiState = EditRoleUiState(
                roleId = DocumentId("r1"),
                roleName = Name("운영진"),
                isLoading = true
            ),
            onRoleNameChange = {},
            onIsDefaultChange = {},
            onPermissionCheckedChange = { _, _ -> }
        )
    }
}

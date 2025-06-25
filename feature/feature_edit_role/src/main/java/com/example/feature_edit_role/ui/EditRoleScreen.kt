package com.example.feature_project.roles.ui

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.NavigationManger
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.data.project.RolePermission
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.feature_edit_role.viewmodel.EditRoleEvent
import com.example.feature_edit_role.viewmodel.EditRoleUiState
import com.example.feature_edit_role.viewmodel.EditRoleViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * EditRoleScreen: 프로젝트 역할 추가 또는 수정 화면 (Stateful)
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
    val isCreating = uiState.roleId == null // 역할 ID가 없으면 생성 모드

    // 삭제 확인 다이얼로그 상태
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditRoleEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is EditRoleEvent.ClearFocus -> focusManager.clearFocus()
                is EditRoleEvent.ShowDeleteConfirmation -> showDeleteConfirmationDialog = true // ★ 삭제 확인 요청
            }
        }
    }

    // 저장 또는 삭제 성공 시 뒤로가기
    LaunchedEffect(uiState.saveSuccess, uiState.deleteSuccess) {
        if (uiState.saveSuccess || uiState.deleteSuccess) {
            navigationManger.navigateBack()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (isCreating) "역할 추가" else "역할 편집") },
                navigationIcon = {
                    IconButton(onClick = { navigationManger.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                },
                actions = {
                    // 수정 모드일 때만 삭제 버튼 표시
                    if (!isCreating) {
                        IconButton(
                            onClick = viewModel::requestDeleteRoleConfirmation, // ★ ViewModel 함수 호출
                            enabled = !uiState.isLoading // 로딩 중 아닐 때 활성화
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "역할 삭제",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        // 초기 로딩 처리 (수정 모드 시)
        if (uiState.isLoading && !isCreating) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null && !isCreating && uiState.originalRoleName.value.isEmpty()) { // 로딩 에러 처리
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("오류: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            }
        } else {
            // 메인 콘텐츠 표시
            EditRoleContent(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onRoleNameChange = { name -> viewModel.onRoleNameChange(Name(name)) },
                onPermissionCheckedChange = viewModel::onPermissionCheckedChange,
                onSaveClick = viewModel::saveRole // 저장/생성 함수 호출
            )
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("역할 삭제 확인") },
            text = { Text("'${uiState.originalRoleName}' 역할을 정말로 삭제하시겠습니까? 이 역할이 할당된 멤버들에게서 역할이 제거됩니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmDeleteRole() // ★ ViewModel의 삭제 확정 함수 호출
                        showDeleteConfirmationDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("취소")
                }
            }
        )
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
    onPermissionCheckedChange: (RolePermission, Boolean) -> Unit,
    onSaveClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // 권한 많을 경우 스크롤
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp) // 요소 간 간격
    ) {
        // 역할 이름 입력
        OutlinedTextField(
            value = uiState.roleName.value, // ViewModel 상태 바인딩
            onValueChange = onRoleNameChange, // ViewModel 콜백 연결
            modifier = Modifier.fillMaxWidth(),
            label = { Text("역할 이름") },
            singleLine = true,
            isError = uiState.error?.contains("이름") == true // 이름 관련 에러 메시지 있을 때
        )

        // 권한 설정 섹션
        Text(
            "권한 설정",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        // 각 권한에 대한 스위치 행 표시
        // ★ Domain에서 가져온 RolePermission enum 사용
        RolePermission.values().forEach { permission ->
            PermissionSwitchRow(
                permission = permission,
                // ★ uiState의 permissions 맵에서 현재 권한 상태 가져옴
                isChecked = uiState.permissions[permission] ?: false,
                // ★ ViewModel 콜백 연결
                onCheckedChange = { isChecked ->
                    onPermissionCheckedChange(permission, isChecked)
                },
                enabled = !uiState.isLoading // 로딩 중 아닐 때 활성화
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
            // 에러 없을 때 공간 차지 않도록 빈 공간 추가
            Spacer(modifier = Modifier.height(MaterialTheme.typography.bodySmall.lineHeight.value.dp))
        }

        Spacer(modifier = Modifier.weight(1f)) // 버튼 하단 배치

        // 저장(또는 생성) 버튼
        Button(
            onClick = onSaveClick, // ViewModel 콜백 연결
            modifier = Modifier.fillMaxWidth(),
            // 이름이 비어있지 않고, 로딩 중이 아니며, (수정 모드일 경우) 변경사항이 있을 때 활성화
            enabled = uiState.roleName.isNotBlank() && !uiState.isLoading && (uiState.roleId == null || uiState.hasChanges)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text(if (uiState.roleId == null) "역할 생성" else "변경사항 저장")
            }
        }
    }
}

/**
 * PermissionSwitchRow: 개별 권한 설정 행 UI (Stateless)
 */
@Composable
fun PermissionSwitchRow(
    permission: RolePermission, // ★ Domain의 Enum 타입 사용
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // 텍스트와 스위치 양쪽 정렬
    ) {
        Text(
            text = permission.name, // Enum의 설명 사용
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp), // 스위치와 간격 확보
            color = if (enabled) LocalContentColor.current else MaterialTheme.colorScheme.outline
        )
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
private fun EditRoleContent_CreateModePreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        EditRoleContent(
            uiState = EditRoleUiState(roleId = null), // 생성 모드
            onRoleNameChange = {},
            onPermissionCheckedChange = { _, _ -> },
            onSaveClick = {}
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
            onPermissionCheckedChange = { _, _ -> },
            onSaveClick = {}
        )
    }
}